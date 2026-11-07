package vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import servicio.ServicioAlquiler;
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
    private final JTextField txtIdCliente = new JTextField();
    private final JTextField txtIdPropietario = new JTextField();
    private final JTextField txtIdDepartamento = new JTextField();
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

        panel.add(new JLabel("ID cliente:"));
        panel.add(txtIdCliente);
        panel.add(new JLabel("ID propietario:"));
        panel.add(txtIdPropietario);

        panel.add(new JLabel("ID departamento:"));
        panel.add(txtIdDepartamento);
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
                txtIdCliente.getText().trim(),
                txtIdPropietario.getText().trim(),
                txtIdDepartamento.getText().trim(),
                inicio, fin, monto);
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
        txtIdCliente.setText(a.getIdCliente());
        txtIdPropietario.setText(a.getIdPropietario());
        txtIdDepartamento.setText(a.getIdDepartamento());
        txtFechaInicial.setText(a.getFechaInicial().format(Alquiler.FORMATO_FECHA));
        txtFechaFinal.setText(a.getFechaFinal().format(Alquiler.FORMATO_FECHA));
        txtMonto.setText(String.valueOf(a.getMontoPagar()));
    }

    private void limpiar() {
        idSeleccionado = null;
        txtId.setText(TEXTO_AUTOMATICO);
        txtIdCliente.setText("");
        txtIdPropietario.setText("");
        txtIdDepartamento.setText("");
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
        txtIdCliente.setText(String.valueOf(modeloTabla.getValueAt(fila, 1)));
        txtIdPropietario.setText(String.valueOf(modeloTabla.getValueAt(fila, 2)));
        txtIdDepartamento.setText(String.valueOf(modeloTabla.getValueAt(fila, 3)));
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
