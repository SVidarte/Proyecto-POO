package vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import modelo.Alquiler;
import modelo.AlquilerDepartamento;
import modelo.Cliente;
import modelo.Departamento;
import modelo.Propietario;
import servicio.ServicioAlquiler;
import servicio.ServicioCliente;
import servicio.ServicioDepartamento;
import servicio.ServicioPropietario;
import servicio.ServicioDetalleAlquiler;

/**
 * Vista principal: registro, búsqueda, modificación y eliminación de alquileres.
 * Desde aquí también se abre la vista de detalle.
 */
public class VistaAlquiler extends JFrame {

    private static final String[] COLUMNAS = {
        "ID", "ID Cliente", "ID Propietario", "ID Departamento",
        "Fecha inicial", "Fecha final", "Monto a pagar", "Detalle"
    };
    private static final int COL_DETALLE = 7; // última columna: botón "Ver detalle"
    private static final String TEXTO_BOTON_DETALLE = "Ver detalle";
    private static final String TEXTO_AUTOMATICO = "(automático)";

    private final ServicioAlquiler servicio;
    private final ServicioDetalleAlquiler servicioDetalle;

    // Id del alquiler seleccionado en la tabla (o encontrado con Buscar). null = ninguno.
    private String idSeleccionado = null;

    private final JTextField txtId = new JTextField();
    // Listas desplegables: se llenan con clientes.csv, propietarios.csv y departamentos.csv
    private final JComboBox<String> cmbCliente = new JComboBox<>();
    private final JComboBox<String> cmbPropietario = new JComboBox<>();
    private final JComboBox<String> cmbDepartamento = new JComboBox<>();
    private final JTextField txtFechaInicial = new JTextField();
    private final JTextField txtFechaFinal = new JTextField();
    private final JTextField txtMonto = new JTextField();
    private final JTextField txtBuscar = new JTextField(12);

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    public VistaAlquiler(ServicioAlquiler servicio, ServicioDetalleAlquiler servicioDetalle) {
        this.servicio = servicio;
        this.servicioDetalle = servicioDetalle;
        armarVentana();
        limpiar();
        refrescarTabla();
    }

    // ---------------------------------------------------------------------
    // Construcción de la ventana
    // ---------------------------------------------------------------------

    private void armarVentana() {
        setTitle("Gestión de Alquileres");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                cargarCombos(true);
            }
        });
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        configurarColumnaDetalle();
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /** Dibuja un botón "Ver detalle" en cada fila y abre la vista de detalle al hacer clic en él. */
    private void configurarColumnaDetalle() {
        JButton botonCelda = new JButton(TEXTO_BOTON_DETALLE);
        tabla.getColumnModel().getColumn(COL_DETALLE).setCellRenderer(
                (tablaOrigen, valor, seleccionada, enfocada, fila, columna) -> botonCelda);
        tabla.getColumnModel().getColumn(COL_DETALLE).setPreferredWidth(100);

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                int columna = tabla.columnAtPoint(e.getPoint());
                if (fila >= 0 && columna == COL_DETALLE) {
                    abrirDetalle(String.valueOf(modeloTabla.getValueAt(fila, 0)));
                }
            }
        });
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

        JPanel panel = new JPanel(new GridLayout(4, 4, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Datos del alquiler"));

        panel.add(new JLabel("ID alquiler:"));
        panel.add(txtId);
        panel.add(new JLabel("Monto a pagar:"));
        panel.add(txtMonto);

        panel.add(new JLabel("Cliente:"));
        panel.add(cmbCliente);
        panel.add(new JLabel("Propietario:"));
        panel.add(cmbPropietario);

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
            Alquiler alquiler = leerFormulario(null);
            String id = servicio.guardar(alquiler);
            mostrarMensaje("Alquiler guardado con ID " + id + ".");
            limpiar();
            refrescarTabla();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void modificar() {
        if (idSeleccionado == null) {
            mostrarError("Seleccione un alquiler de la tabla (o búsquelo por ID) para modificarlo.");
            return;
        }
        try {
            Alquiler alquiler = leerFormulario(idSeleccionado);
            if (servicio.modificar(alquiler)) {
                mostrarMensaje("Alquiler " + idSeleccionado + " modificado.");
                limpiar();
                refrescarTabla();
            } else {
                mostrarError("No se encontró el alquiler " + idSeleccionado + ".");
            }
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void eliminar() {
        if (idSeleccionado == null) {
            mostrarError("Seleccione un alquiler de la tabla (o búsquelo por ID) para eliminarlo.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el alquiler " + idSeleccionado + " y sus detalles?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (servicio.eliminar(idSeleccionado)) {
                servicioDetalle.eliminarPorAlquiler(idSeleccionado);
                mostrarMensaje("Alquiler eliminado.");
                limpiar();
                refrescarTabla();
            } else {
                mostrarError("No se encontró el alquiler " + idSeleccionado + ".");
            }
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void buscar() {
        String id = txtBuscar.getText().trim();
        if (id.isEmpty()) {
            mostrarError("Escriba el ID que desea buscar (por ejemplo A0001).");
            return;
        }
        try {
            Alquiler alquiler = servicio.buscar(id);
            if (alquiler == null) {
                mostrarMensaje("No se encontró ningún alquiler con ID " + id.toUpperCase() + ".");
                return;
            }
            cargarTabla(Collections.singletonList(alquiler));
            llenarFormulario(alquiler);
            tabla.setRowSelectionInterval(0, 0);
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void abrirDetalle(String idAlquiler) {
        VistaDetalleAlquiler vista = new VistaDetalleAlquiler(servicio, servicioDetalle, idAlquiler);
        vista.setVisible(true);
    }

    // ---------------------------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------------------------

    /** Lee los campos del formulario y arma un alquiler. Lanza IllegalArgumentException si hay datos inválidos. */
    private Alquiler leerFormulario(String id) {
        LocalDate inicio = leerFecha(txtFechaInicial.getText(), "fecha inicial");
        LocalDate fin = leerFecha(txtFechaFinal.getText(), "fecha final");
        double monto;
        try {
            monto = Double.parseDouble(txtMonto.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El monto a pagar debe ser un número (ejemplo: 1500.50).");
        }
        return new AlquilerDepartamento(id,
                idDelCombo(cmbCliente, "No hay clientes registrados. Registre un cliente primero."),
                idDelCombo(cmbPropietario, "No hay propietarios registrados. Registre un propietario primero."),
                idDelCombo(cmbDepartamento, "No hay departamentos disponibles. Registre uno en la gestión de departamentos."),
                inicio, fin, monto);
    }

    // ---------------------------------------------------------------------
    // Listas desplegables de clientes, propietarios y departamentos
    // ---------------------------------------------------------------------

    /**
     * Llena las tres listas leyendo clientes.csv, propietarios.csv y departamentos.csv
     * (solo los departamentos con estado Disponible).
     *
     * @param conservarSeleccion true = mantiene lo que el usuario tenía elegido; false = vuelve al primer elemento
     */
    private void cargarCombos(boolean conservarSeleccion) {
        String idCliente = conservarSeleccion ? idDelComboONull(cmbCliente) : null;
        String idPropietario = conservarSeleccion ? idDelComboONull(cmbPropietario) : null;
        String idDepartamento = conservarSeleccion ? idDelComboONull(cmbDepartamento) : null;

        cmbCliente.removeAllItems();
        for (Cliente c : new ServicioCliente().listar()) {
            cmbCliente.addItem(etiquetaCliente(c));
        }
        cmbPropietario.removeAllItems();
        for (Propietario p : new ServicioPropietario().listar()) {
            cmbPropietario.addItem(etiquetaPropietario(p));
        }
        cmbDepartamento.removeAllItems();
        for (Departamento d : new ServicioDepartamento().listarDepartamento()) {
            if (Departamento.DISPONIBLE.equals(d.getEstado())) {
                cmbDepartamento.addItem(etiquetaDepartamento(d));
            }
        }

        if (idCliente != null) {
            seleccionarCliente(idCliente);
        }
        if (idPropietario != null) {
            seleccionarPropietario(idPropietario);
        }
        if (idDepartamento != null) {
            seleccionarDepartamento(idDepartamento);
        }
    }

    private String etiquetaCliente(Cliente c) {
        return c.getId() + " - " + c.getNombres() + " " + c.getApellidos();
    }

    private String etiquetaPropietario(Propietario p) {
        return p.getId() + " - " + p.getNombres() + " " + p.getApellidos();
    }

    private String etiquetaDepartamento(Departamento d) {
        return d.getId_departamento() + " - Dpto " + d.getNumero_departamento() + " - " + d.getDireccion();
    }

    /** Elige el cliente en la lista; si ya no está registrado lo agrega para poder mostrarlo. */
    private void seleccionarCliente(String id) {
        if (!seleccionarSiEsta(cmbCliente, id)) {
            Cliente c = new ServicioCliente().buscar(id);
            agregarYSeleccionar(cmbCliente, c != null ? etiquetaCliente(c) : id + " - (no registrado)");
        }
    }

    private void seleccionarPropietario(String id) {
        if (!seleccionarSiEsta(cmbPropietario, id)) {
            Propietario p = new ServicioPropietario().buscar(id);
            agregarYSeleccionar(cmbPropietario, p != null ? etiquetaPropietario(p) : id + " - (no registrado)");
        }
    }

    /**
     * Elige el departamento en la lista. Un alquiler ya guardado puede tener un departamento que ahora
     * está Ocupado (no aparece en la lista de disponibles): en ese caso se agrega solo para mostrarlo.
     */
    private void seleccionarDepartamento(String id) {
        if (!seleccionarSiEsta(cmbDepartamento, id)) {
            Departamento d = new ServicioDepartamento().buscarPorId(id);
            String etiqueta = (d != null)
                    ? etiquetaDepartamento(d) + " (" + d.getEstado() + ")"
                    : id + " - (no registrado)";
            agregarYSeleccionar(cmbDepartamento, etiqueta);
        }
    }

    private boolean seleccionarSiEsta(JComboBox<String> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).startsWith(id + " - ")) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    private void agregarYSeleccionar(JComboBox<String> combo, String etiqueta) {
        combo.addItem(etiqueta);
        combo.setSelectedIndex(combo.getItemCount() - 1);
    }

    /** Id (lo que está antes de " - ") del elemento elegido, o null si la lista está vacía. */
    private String idDelComboONull(JComboBox<String> combo) {
        Object item = combo.getSelectedItem();
        if (item == null) {
            return null;
        }
        String texto = item.toString();
        int corte = texto.indexOf(" - ");
        return corte >= 0 ? texto.substring(0, corte) : texto;
    }

    private String idDelCombo(JComboBox<String> combo, String mensajeSiVacio) {
        String id = idDelComboONull(combo);
        if (id == null) {
            throw new IllegalArgumentException(mensajeSiVacio);
        }
        return id;
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
        seleccionarCliente(a.getIdCliente());
        seleccionarPropietario(a.getIdPropietario());
        seleccionarDepartamento(a.getIdDepartamento());
        txtFechaInicial.setText(a.getFechaInicial().format(Alquiler.FORMATO_FECHA));
        txtFechaFinal.setText(a.getFechaFinal().format(Alquiler.FORMATO_FECHA));
        txtMonto.setText(String.valueOf(a.getMontoPagar()));
    }

    private void limpiar() {
        idSeleccionado = null;
        txtId.setText(TEXTO_AUTOMATICO);
        cargarCombos(false);
        txtFechaInicial.setText("");
        txtFechaFinal.setText("");
        txtMonto.setText("");
        txtBuscar.setText("");
        tabla.clearSelection();
    }

    private void refrescarTabla() {
        try {
            cargarTabla(servicio.listar());
        } catch (IOException ex) {
            mostrarError("No se pudo leer alquiler.csv: " + ex.getMessage());
        }
    }

    private void cargarTabla(List<Alquiler> lista) {
        modeloTabla.setRowCount(0);
        for (Alquiler a : lista) {
            modeloTabla.addRow(new Object[]{
                a.getId(),
                a.getIdCliente(),
                a.getIdPropietario(),
                a.getIdDepartamento(),
                a.getFechaInicial().format(Alquiler.FORMATO_FECHA),
                a.getFechaFinal().format(Alquiler.FORMATO_FECHA),
                a.getMontoPagar(),
                TEXTO_BOTON_DETALLE
            });
        }
    }

    /** Cuando el usuario hace clic en una fila, sus datos pasan al formulario. */
    private void cargarFilaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idSeleccionado = String.valueOf(modeloTabla.getValueAt(fila, 0));
        txtId.setText(idSeleccionado);
        seleccionarCliente(String.valueOf(modeloTabla.getValueAt(fila, 1)));
        seleccionarPropietario(String.valueOf(modeloTabla.getValueAt(fila, 2)));
        seleccionarDepartamento(String.valueOf(modeloTabla.getValueAt(fila, 3)));
        txtFechaInicial.setText(String.valueOf(modeloTabla.getValueAt(fila, 4)));
        txtFechaFinal.setText(String.valueOf(modeloTabla.getValueAt(fila, 5)));
        txtMonto.setText(String.valueOf(modeloTabla.getValueAt(fila, 6)));
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------------
    // Punto de entrada
    // ---------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ServicioAlquiler servicioAlquiler = new ServicioAlquiler();
                ServicioDetalleAlquiler servicioDetalle = new ServicioDetalleAlquiler(servicioAlquiler);
                new VistaAlquiler(servicioAlquiler, servicioDetalle).setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "No se pudieron abrir los archivos CSV: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
