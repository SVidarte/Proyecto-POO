package vista;

import modelo.Alquiler;
import modelo.AlquilerDepartamento;
import modelo.Departamento;
import modelo.Propietario;
import servicio.ServicioAlquiler;
import servicio.ServicioDepartamento;
import servicio.ServicioPropietario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Alquileres de un propietario. Se crean sin cliente: el cliente los acepta desde "Alquileres Disponibles".
 * Mientras no tengan cliente, el propietario puede modificarlos o eliminarlos.
 */
public class VistaAlquiler extends JFrame {

    private static final String[] COLUMNAS = {
        "ID", "Cliente", "ID Departamento", "Fecha inicial", "Fecha final", "Monto a pagar"
    };
    private static final String TEXTO_AUTOMATICO = "(automático)";
    private static final String SIN_CLIENTE = "(sin cliente)";

    private final String idPropietario;
    private final ServicioAlquiler servicio;
    private List<Alquiler> alquileresMostrados = new ArrayList<>();
    private String idSeleccionado = null; // alquiler elegido en la tabla (o encontrado con Buscar)

    private final JTextField txtId = new JTextField();
    private final JTextField txtPropietario = new JTextField();
    private final JTextField txtCliente = new JTextField();
    private final JComboBox<String> cmbDepartamento = new JComboBox<>();
    private final JTextField txtFechaInicial = new JTextField();
    private final JTextField txtFechaFinal = new JTextField();
    private final JTextField txtMonto = new JTextField();
    private final JTextField txtBuscar = new JTextField(12);

    private final ModeloTabla modeloTabla = new ModeloTabla(COLUMNAS);
    private final JTable tabla = new JTable(modeloTabla);

    public VistaAlquiler(String idPropietario) {
        this.idPropietario = idPropietario;
        ServicioAlquiler s;
        try {
            s = new ServicioAlquiler();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir alquiler.csv: " + e.getMessage());
        }
        this.servicio = s;

        armarVentana();
        limpiar();
        refrescarTabla();
    }

    // ---------------------------------------------------------------------
    // Construcción de la ventana
    // ---------------------------------------------------------------------

    private void armarVentana() {
        setTitle("Mis Alquileres");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Al volver a esta ventana se actualizan los departamentos y la tabla
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                cargarDepartamentos(true);
                refrescarTabla();
            }
        });

        JPanel norte = new JPanel(new BorderLayout(5, 10));
        norte.add(crearPanelBusqueda(), BorderLayout.NORTH);
        norte.add(crearPanelFormulario(), BorderLayout.CENTER);
        norte.add(crearPanelBotones(), BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarFilaSeleccionada();
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    private JPanel crearPanelBusqueda() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodos = new JButton("Mostrar todos");
        btnBuscar.addActionListener(e -> buscar());
        btnTodos.addActionListener(e -> {
            limpiar();
            refrescarTabla();
        });
        panel.add(new JLabel("Buscar por ID:"));
        panel.add(txtBuscar);
        panel.add(btnBuscar);
        panel.add(btnTodos);
        return panel;
    }

    private JPanel crearPanelFormulario() {
        txtId.setEditable(false);
        txtCliente.setEditable(false);
        txtPropietario.setEditable(false);
        Propietario p = new ServicioPropietario().buscar(idPropietario);
        txtPropietario.setText(idPropietario + (p != null ? " - " + p.getNombres() + " " + p.getApellidos() : ""));

        JPanel panel = new JPanel(new GridLayout(4, 4, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Datos del alquiler"));

        panel.add(new JLabel("ID alquiler:"));
        panel.add(txtId);
        panel.add(new JLabel("Monto a pagar:"));
        panel.add(txtMonto);

        panel.add(new JLabel("Propietario:"));
        panel.add(txtPropietario);
        panel.add(new JLabel("Cliente:"));
        panel.add(txtCliente);

        panel.add(new JLabel("Departamento (disponibles):"));
        panel.add(cmbDepartamento);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));

        panel.add(new JLabel("Fecha inicial (dd/MM/yyyy):"));
        panel.add(txtFechaInicial);
        panel.add(new JLabel("Fecha final (dd/MM/yyyy):"));
        panel.add(txtFechaFinal);
        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnModificar = new JButton("Modificar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnLimpiar = new JButton("Limpiar");

        btnGuardar.addActionListener(e -> guardar());
        btnModificar.addActionListener(e -> modificar());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> limpiar());

        panel.add(btnGuardar);
        panel.add(btnModificar);
        panel.add(btnEliminar);
        panel.add(btnLimpiar);
        return panel;
    }

    // ---------------------------------------------------------------------
    // Acciones de los botones
    // ---------------------------------------------------------------------

    private void guardar() {
        try {
            String id = servicio.guardar(leerFormulario(null));
            mostrarMensaje("Alquiler guardado con ID " + id + ". Aparecerá en \"Alquileres Disponibles\" de los clientes.");
            limpiar();
            refrescarTabla();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void modificar() {
        try {
            Alquiler actual = alquilerSeleccionadoSinCliente("modificarlo");
            if (actual == null) {
                return;
            }
            servicio.modificar(leerFormulario(actual.getId()));
            mostrarMensaje("Alquiler " + actual.getId() + " modificado.");
            limpiar();
            refrescarTabla();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void eliminar() {
        try {
            Alquiler actual = alquilerSeleccionadoSinCliente("eliminarlo");
            if (actual == null) {
                return;
            }
            int opcion = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar el alquiler " + actual.getId() + "?",
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
            servicio.eliminar(actual.getId());
            mostrarMensaje("Alquiler eliminado.");
            limpiar();
            refrescarTabla();
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    /**
     * Devuelve el alquiler elegido (leído de nuevo del archivo) si todavía no tiene cliente.
     * Si no hay ninguno elegido, no existe o ya tiene cliente, muestra el motivo y devuelve null.
     */
    private Alquiler alquilerSeleccionadoSinCliente(String accion) throws IOException {
        if (idSeleccionado == null) {
            mostrarError("Seleccione un alquiler de la tabla (o búsquelo por ID) para " + accion + ".");
            return null;
        }
        Alquiler actual = servicio.buscar(idSeleccionado);
        if (actual == null || !actual.getIdPropietario().equalsIgnoreCase(idPropietario)) {
            mostrarError("No se encontró el alquiler " + idSeleccionado + ".");
            return null;
        }
        if (actual.getIdCliente() != null && !actual.getIdCliente().trim().isEmpty()) {
            mostrarError("El alquiler " + actual.getId() + " ya tiene cliente (" + actual.getIdCliente()
                    + "): solo se pueden " + (accion.equals("eliminarlo") ? "eliminar" : "modificar")
                    + " los alquileres sin cliente.");
            return null;
        }
        return actual;
    }

    private void buscar() {
        String id = txtBuscar.getText().trim();
        if (id.isEmpty()) {
            mostrarError("Escriba el ID que desea buscar (por ejemplo A0001).");
            return;
        }
        try {
            Alquiler alquiler = servicio.buscar(id);
            if (alquiler == null || !alquiler.getIdPropietario().equalsIgnoreCase(idPropietario)) {
                mostrarMensaje("No se encontró ningún alquiler suyo con ID " + id.toUpperCase() + ".");
                return;
            }
            cargarTabla(Collections.singletonList(alquiler));
            llenarFormulario(alquiler);
            tabla.setRowSelectionInterval(0, 0);
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------------------------

    /** Lee el formulario. Un alquiler del propietario siempre se guarda sin cliente. */
    private Alquiler leerFormulario(String id) {
        LocalDate inicio = leerFecha(txtFechaInicial.getText(), "fecha inicial");
        LocalDate fin = leerFecha(txtFechaFinal.getText(), "fecha final");
        double monto;
        try {
            monto = Double.parseDouble(txtMonto.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El monto a pagar debe ser un número (ejemplo: 1500.50).");
        }
        String idDepartamento = idDelCombo();
        if (idDepartamento == null) {
            throw new IllegalArgumentException(
                    "No tiene departamentos disponibles. Cree uno primero en \"Crear departamento\".");
        }
        return new AlquilerDepartamento(id, "", idPropietario, idDepartamento, inicio, fin, monto);
    }

    private LocalDate leerFecha(String texto, String nombreCampo) {
        try {
            return LocalDate.parse(texto.trim(), Alquiler.FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La " + nombreCampo + " no es válida. Use el formato dd/MM/yyyy.");
        }
    }

    private void llenarFormulario(Alquiler a) {
        idSeleccionado = a.getId();
        txtId.setText(a.getId());
        txtCliente.setText(textoCliente(a));
        txtFechaInicial.setText(a.getFechaInicial().format(Alquiler.FORMATO_FECHA));
        txtFechaFinal.setText(a.getFechaFinal().format(Alquiler.FORMATO_FECHA));
        txtMonto.setText(String.valueOf(a.getMontoPagar()));
        seleccionarDepartamento(a.getIdDepartamento());
    }

    private String textoCliente(Alquiler a) {
        return (a.getIdCliente() == null || a.getIdCliente().trim().isEmpty()) ? SIN_CLIENTE : a.getIdCliente();
    }

    private void limpiar() {
        idSeleccionado = null;
        txtId.setText(TEXTO_AUTOMATICO);
        txtCliente.setText(SIN_CLIENTE);
        txtFechaInicial.setText("");
        txtFechaFinal.setText("");
        txtMonto.setText("");
        txtBuscar.setText("");
        cargarDepartamentos(false);
        tabla.clearSelection();
    }

    private void refrescarTabla() {
        try {
            cargarTabla(servicio.listarPorPropietario(idPropietario));
        } catch (IOException ex) {
            mostrarError("No se pudo leer alquiler.csv: " + ex.getMessage());
        }
    }

    private void cargarTabla(List<Alquiler> lista) {
        alquileresMostrados = new ArrayList<>(lista);
        modeloTabla.setRowCount(0);
        for (Alquiler a : alquileresMostrados) {
            modeloTabla.addRow(new Object[]{
                a.getId(),
                textoCliente(a),
                a.getIdDepartamento(),
                a.getFechaInicial().format(Alquiler.FORMATO_FECHA),
                a.getFechaFinal().format(Alquiler.FORMATO_FECHA),
                String.format(Locale.US, "%.2f", a.getMontoPagar())
            });
        }
    }

    /** Cuando el usuario elige una fila, sus datos pasan al formulario. */
    private void cargarFilaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= alquileresMostrados.size()) {
            return;
        }
        llenarFormulario(alquileresMostrados.get(fila));
    }

    // ---------------------------------------------------------------------
    // Lista de departamentos (solo los del propietario que estén Disponibles)
    // ---------------------------------------------------------------------

    private void cargarDepartamentos(boolean conservarSeleccion) {
        String idElegido = conservarSeleccion ? idDelCombo() : null;
        cmbDepartamento.removeAllItems();
        for (Departamento d : new ServicioDepartamento().listarPorPropietario(idPropietario)) {
            if (Departamento.DISPONIBLE.equals(d.getEstado())) {
                cmbDepartamento.addItem(etiqueta(d));
            }
        }
        if (idElegido != null) {
            seleccionarDepartamento(idElegido);
        }
    }

    private String etiqueta(Departamento d) {
        return d.getId_departamento() + " - Dpto " + d.getNumero_departamento() + " - " + d.getDireccion();
    }

    /** Elige el departamento en la lista; si no está (por ejemplo, ya no está Disponible) lo agrega para mostrarlo. */
    private void seleccionarDepartamento(String id) {
        for (int i = 0; i < cmbDepartamento.getItemCount(); i++) {
            if (cmbDepartamento.getItemAt(i).startsWith(id + " - ")) {
                cmbDepartamento.setSelectedIndex(i);
                return;
            }
        }
        Departamento d = new ServicioDepartamento().buscarPorId(id);
        cmbDepartamento.addItem(d != null ? etiqueta(d) + " (" + d.getEstado() + ")" : id + " - (no registrado)");
        cmbDepartamento.setSelectedIndex(cmbDepartamento.getItemCount() - 1);
    }

    /** Id (lo que va antes de " - ") del departamento elegido, o null si la lista está vacía. */
    private String idDelCombo() {
        Object item = cmbDepartamento.getSelectedItem();
        if (item == null) {
            return null;
        }
        String texto = item.toString();
        int corte = texto.indexOf(" - ");
        return corte >= 0 ? texto.substring(0, corte) : texto;
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
