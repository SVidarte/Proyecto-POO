package vista;

import modelo.Pago;
import servicio.ServicioPago;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class VistaPago extends JFrame {

    private final ServicioPago servicio = new ServicioPago();

    // Formulario
    private JTextField txtId;
    private JTextField txtPlanificacion;
    private JTextField txtMonto;
    private JTextField txtFecha;
    private JComboBox<String> cmbMetodo;
    private JLabel lblMetodo;

    // Búsqueda
    private JTextField txtBuscar;

    // Tabla
    private JTable tabla;
    private DefaultTableModel modeloTabla;

    public VistaPago() {
        setTitle("Registro de Pagos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);

        limpiarFormulario();
        cargarTabla(servicio.listar());
    }

    // ---------------------------------------------------------------
    // Construcción de la interfaz
    // ---------------------------------------------------------------

    private JPanel crearPanelSuperior() {
        JPanel panelFormulario = new JPanel(new GridLayout(5, 2, 5, 5));
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        txtId = new JTextField();
        txtId.setEditable(false);
        txtPlanificacion = new JTextField();
        txtMonto = new JTextField();
        txtFecha = new JTextField();
        cmbMetodo = new JComboBox<>(new String[]{"Transferencia", "Efectivo"});

        // Al escribir el id de la planificación se detecta el método de pago del propietario
        // y se bloquea la lista "Método"
        txtPlanificacion.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { actualizarMetodoSegunPropietario(); }
            @Override
            public void removeUpdate(DocumentEvent e) { actualizarMetodoSegunPropietario(); }
            @Override
            public void changedUpdate(DocumentEvent e) { actualizarMetodoSegunPropietario(); }
        });

        panelFormulario.add(new JLabel("ID de pago:"));
        panelFormulario.add(txtId);
        panelFormulario.add(new JLabel("ID planificación de pago:"));
        panelFormulario.add(txtPlanificacion);
        panelFormulario.add(new JLabel("Monto:"));
        panelFormulario.add(txtMonto);
        panelFormulario.add(new JLabel("Fecha (dd/MM/yyyy):"));
        panelFormulario.add(txtFecha);
        lblMetodo = new JLabel("Método:");
        panelFormulario.add(lblMetodo);
        panelFormulario.add(cmbMetodo);

        JButton btnGuardar = new JButton("Guardar");
        JButton btnModificar = new JButton("Modificar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnLimpiar = new JButton("Limpiar");

        btnGuardar.addActionListener(e -> guardar());
        btnModificar.addActionListener(e -> modificar());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> limpiarFormulario());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelBotones.add(btnGuardar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(panelFormulario, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);
        return panelSuperior;
    }

    private JPanel crearPanelCentral() {
        // Barra de búsqueda
        txtBuscar = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnListar = new JButton("Listar todo");

        btnBuscar.addActionListener(e -> buscar());
        btnListar.addActionListener(e -> {
            txtBuscar.setText("");
            cargarTabla(servicio.listar());
        });

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelBusqueda.add(new JLabel("Buscar por ID de pago o de planificación:"));
        panelBusqueda.add(txtBuscar);
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(btnListar);

        // Tabla
        modeloTabla = new DefaultTableModel(
                new String[]{"ID", "ID planificación", "Monto", "Fecha", "Método"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarFilaEnFormulario();
            }
        });

        JPanel panelCentral = new JPanel(new BorderLayout(5, 5));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        panelCentral.add(panelBusqueda, BorderLayout.NORTH);
        panelCentral.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panelCentral;
    }

    // ---------------------------------------------------------------
    // Acciones de los botones
    // ---------------------------------------------------------------

    private void guardar() {
        try {
            Pago pago = leerFormulario();
            servicio.guardar(pago);
            JOptionPane.showMessageDialog(this, "Pago guardado con id " + pago.getId() + ".");
            limpiarFormulario();
            cargarTabla(servicio.listar());
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void modificar() {
        if (txtId.getText().isEmpty()) {
            mostrarError("Selecciona un pago de la tabla para modificarlo.");
            return;
        }
        try {
            Pago pago = leerFormulario();
            pago.setId(txtId.getText());
            if (servicio.modificar(pago)) {
                JOptionPane.showMessageDialog(this, "Pago modificado correctamente.");
                limpiarFormulario();
                cargarTabla(servicio.listar());
            } else {
                mostrarError("No se encontró el pago " + txtId.getText() + ".");
            }
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void eliminar() {
        if (txtId.getText().isEmpty()) {
            mostrarError("Selecciona un pago de la tabla para eliminarlo.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas eliminar el pago " + txtId.getText() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            if (servicio.eliminar(txtId.getText())) {
                JOptionPane.showMessageDialog(this, "Pago eliminado correctamente.");
                limpiarFormulario();
                cargarTabla(servicio.listar());
            } else {
                mostrarError("No se encontró el pago " + txtId.getText() + ".");
            }
        }
    }

    private void buscar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            mostrarError("Escribe un id de pago o de planificación para buscar.");
            return;
        }
        List<Pago> resultado = servicio.buscar(texto);
        cargarTabla(resultado);
        if (resultado.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron resultados.");
        }
    }

    // ---------------------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------------------

    /** Lee y valida el formulario. Lanza IllegalArgumentException si algo está mal. */
    private Pago leerFormulario() {
        double monto;
        try {
            monto = Double.parseDouble(txtMonto.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El monto debe ser un número válido.");
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim(), Pago.FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha debe tener el formato dd/MM/yyyy y ser válida.");
        }

        Pago pago = new Pago();
        pago.setIdPlanificacionPago(txtPlanificacion.getText().trim());
        pago.setMonto(monto);
        pago.setFecha(fecha);
        pago.setMetodo(textoACodigo((String) cmbMetodo.getSelectedItem()));
        return pago;
    }

    private void cargarTabla(List<Pago> pagos) {
        modeloTabla.setRowCount(0);
        for (Pago p : pagos) {
            modeloTabla.addRow(new Object[]{
                    p.getId(),
                    p.getIdPlanificacionPago(),
                    String.format(Locale.US, "%.2f", p.getMonto()),
                    p.getFecha().format(Pago.FORMATO_FECHA),
                    codigoATexto(p.getMetodo())
            });
        }
    }

    private void cargarFilaEnFormulario() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        txtId.setText((String) modeloTabla.getValueAt(fila, 0));
        txtPlanificacion.setText((String) modeloTabla.getValueAt(fila, 1));
        txtMonto.setText((String) modeloTabla.getValueAt(fila, 2));
        txtFecha.setText((String) modeloTabla.getValueAt(fila, 3));
        if (cmbMetodo.isEnabled()) { // si el método está bloqueado por el propietario, se respeta ese
            cmbMetodo.setSelectedItem(modeloTabla.getValueAt(fila, 4));
        }
    }

    private void limpiarFormulario() {
        txtId.setText("");
        txtPlanificacion.setText("");
        txtMonto.setText("");
        txtFecha.setText(LocalDate.now().format(Pago.FORMATO_FECHA));
        cmbMetodo.setSelectedIndex(0);
        tabla.clearSelection();
    }

    /**
     * Busca el método de pago del propietario de la planificación escrita: si lo encuentra, lo elige
     * en la lista y la bloquea; si no (campo vacío o planificación inexistente), deja elegir libremente.
     */
    private void actualizarMetodoSegunPropietario() {
        String codigo = servicio.metodoDelPropietario(txtPlanificacion.getText().trim());
        if (codigo != null) {
            cmbMetodo.setSelectedItem(codigoATexto(codigo));
            cmbMetodo.setEnabled(false);
            lblMetodo.setText("Método (según el propietario):");
        } else {
            cmbMetodo.setEnabled(true);
            lblMetodo.setText("Método:");
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // El usuario ve "Transferencia"/"Efectivo"; internamente se guarda "T"/"E"
    private String textoACodigo(String texto) {
        return "Transferencia".equals(texto) ? Pago.TRANSFERENCIA : Pago.EFECTIVO;
    }

    private String codigoATexto(String codigo) {
        return Pago.TRANSFERENCIA.equals(codigo) ? "Transferencia" : "Efectivo";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VistaPago().setVisible(true));
    }
}
