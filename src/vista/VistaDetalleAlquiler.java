package vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
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
import javax.swing.table.DefaultTableModel;
import modelo.Alquiler;
import modelo.DetalleAlquiler;
import servicio.ServicioAlquiler;
import servicio.ServicioDetalleAlquiler;

/**
 * Vista de detalle de alquiler. Se abre desde VistaAlquiler.
 * Si se abre con un alquiler seleccionado, muestra solo los detalles de ese alquiler.
 */
public class VistaDetalleAlquiler extends JFrame {

    private static final String[] COLUMNAS = {
        "ID Detalle", "ID Alquiler", "ID Cliente", "ID Propietario",
        "ID Departamento", "Fecha inicial", "Fecha final", "Monto a pagar"
    };
    private static final String TEXTO_AUTOMATICO = "(automático)";

    private final ServicioAlquiler servicioAlquiler;
    private final ServicioDetalleAlquiler servicioDetalle;

    // Detalle seleccionado en la tabla (null = ninguno)
    private String idDetalleSeleccionado = null;
    // Alquiler por el que se está filtrando la tabla (null = todos)
    private String filtroAlquiler;

    private final JTextField txtIdDetalle = new JTextField(10);
    private final JComboBox<String> cmbAlquiler = new JComboBox<>();
    private final JTextField txtBuscar = new JTextField(10);

    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    /**
     * @param idAlquilerInicial alquiler seleccionado en VistaAlquiler (puede ser null)
     */
    public VistaDetalleAlquiler(ServicioAlquiler servicioAlquiler, ServicioDetalleAlquiler servicioDetalle,
                                String idAlquilerInicial) {
        this.servicioAlquiler = servicioAlquiler;
        this.servicioDetalle = servicioDetalle;
        this.filtroAlquiler = idAlquilerInicial;

        armarVentana();
        cargarCombo();
        if (idAlquilerInicial != null) {
            cmbAlquiler.setSelectedItem(idAlquilerInicial);
        }
        limpiar();
        refrescarTabla();
    }

    // ---------------------------------------------------------------------
    // Construcción de la ventana
    // ---------------------------------------------------------------------

    private void armarVentana() {
        setTitle("Detalle de Alquiler");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // cierra solo esta ventana
        setSize(950, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel norte = new JPanel(new BorderLayout(5, 5));
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
        JButton btnFiltrar = new JButton("Filtrar por alquiler elegido");
        JButton btnTodos = new JButton("Ver todos");

        btnBuscar.addActionListener(e -> buscar());
        btnFiltrar.addActionListener(e -> {
            filtroAlquiler = (String) cmbAlquiler.getSelectedItem();
            refrescarTabla();
        });
        btnTodos.addActionListener(e -> {
            filtroAlquiler = null;
            limpiar();
            refrescarTabla();
        });

        panel.add(new JLabel("Buscar por ID detalle:"));
        panel.add(txtBuscar);
        panel.add(btnBuscar);
        panel.add(btnFiltrar);
        panel.add(btnTodos);
        return panel;
    }

    private JPanel crearPanelFormulario() {
        txtIdDetalle.setEditable(false);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Datos del detalle"));
        panel.add(new JLabel("ID detalle:"));
        panel.add(txtIdDetalle);
        panel.add(new JLabel("Alquiler:"));
        panel.add(cmbAlquiler);
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
        String idAlquiler = (String) cmbAlquiler.getSelectedItem();
        if (idAlquiler == null) {
            mostrarError("No hay alquileres registrados. Registre uno primero en la vista de alquileres.");
            return;
        }
        try {
            DetalleAlquiler detalle = servicioDetalle.guardar(idAlquiler);
            mostrarMensaje("Detalle guardado con ID " + detalle.getIdDetalle() + ".");
            limpiar();
            refrescarTabla();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void modificar() {
        if (idDetalleSeleccionado == null) {
            mostrarError("Seleccione un detalle de la tabla para modificarlo.");
            return;
        }
        String nuevoIdAlquiler = (String) cmbAlquiler.getSelectedItem();
        if (nuevoIdAlquiler == null) {
            mostrarError("Elija el alquiler al que pertenecerá el detalle.");
            return;
        }
        try {
            if (servicioDetalle.modificar(idDetalleSeleccionado, nuevoIdAlquiler)) {
                mostrarMensaje("Detalle " + idDetalleSeleccionado + " modificado.");
                limpiar();
                refrescarTabla();
            } else {
                mostrarError("No se encontró el detalle " + idDetalleSeleccionado + ".");
            }
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void eliminar() {
        if (idDetalleSeleccionado == null) {
            mostrarError("Seleccione un detalle de la tabla para eliminarlo.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el detalle " + idDetalleSeleccionado + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (servicioDetalle.eliminar(idDetalleSeleccionado)) {
                mostrarMensaje("Detalle eliminado.");
                limpiar();
                refrescarTabla();
            } else {
                mostrarError("No se encontró el detalle " + idDetalleSeleccionado + ".");
            }
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void buscar() {
        String id = txtBuscar.getText().trim();
        if (id.isEmpty()) {
            mostrarError("Escriba el ID del detalle que desea buscar (por ejemplo D0001).");
            return;
        }
        try {
            DetalleAlquiler detalle = servicioDetalle.buscar(id);
            if (detalle == null) {
                mostrarMensaje("No se encontró ningún detalle con ID " + id.toUpperCase() + ".");
                return;
            }
            cargarTabla(Collections.singletonList(detalle));
            tabla.setRowSelectionInterval(0, 0);
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------------------------

    private void cargarCombo() {
        cmbAlquiler.removeAllItems();
        try {
            for (Alquiler alquiler : servicioAlquiler.listar()) {
                cmbAlquiler.addItem(alquiler.getId());
            }
        } catch (IOException ex) {
            mostrarError("No se pudo leer alquiler.csv: " + ex.getMessage());
        }
    }

    private void limpiar() {
        idDetalleSeleccionado = null;
        txtIdDetalle.setText(TEXTO_AUTOMATICO);
        txtBuscar.setText("");
        tabla.clearSelection();
    }

    private void refrescarTabla() {
        try {
            List<DetalleAlquiler> lista = (filtroAlquiler == null)
                    ? servicioDetalle.listar()
                    : servicioDetalle.listarPorAlquiler(filtroAlquiler);
            cargarTabla(lista);
            setTitle(filtroAlquiler == null
                    ? "Detalle de Alquiler - todos"
                    : "Detalle de Alquiler - " + filtroAlquiler);
        } catch (IOException ex) {
            mostrarError("No se pudo leer detalle_alquiler.csv: " + ex.getMessage());
        }
    }

    private void cargarTabla(List<DetalleAlquiler> lista) {
        modeloTabla.setRowCount(0);
        for (DetalleAlquiler d : lista) {
            modeloTabla.addRow(new Object[]{
                d.getIdDetalle(),
                d.getIdAlquiler(),
                d.getIdCliente(),
                d.getIdPropietario(),
                d.getIdDepartamento(),
                d.getFechaInicial().format(Alquiler.FORMATO_FECHA),
                d.getFechaFinal().format(Alquiler.FORMATO_FECHA),
                d.getMontoPagar()
            });
        }
    }

    /** Cuando el usuario hace clic en una fila, se llena el formulario. */
    private void cargarFilaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idDetalleSeleccionado = String.valueOf(modeloTabla.getValueAt(fila, 0));
        txtIdDetalle.setText(idDetalleSeleccionado);
        cmbAlquiler.setSelectedItem(String.valueOf(modeloTabla.getValueAt(fila, 1)));
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
