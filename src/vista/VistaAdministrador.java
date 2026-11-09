package vista;

import modelo.Propietario;
import servicio.ServicioAdministracion;
import servicio.ServicioPropietario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Opciones del administrador: ver los propietarios y eliminar al que esté seleccionado. */
public class VistaAdministrador extends JFrame {

    private final ServicioAdministracion administracion;
    private List<Propietario> propietarios = new ArrayList<>(); // mismo orden que las filas de la tabla

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new String[]{"DNI", "Nombres", "Apellidos", "Email", "Celular", "Nro. de cuenta", "Banco"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel lblClientes = new JLabel("Seleccione un propietario para ver sus clientes.");

    public VistaAdministrador(String usuario) {
        try {
            administracion = new ServicioAdministracion();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir alquiler.csv: " + e.getMessage());
        }
        setTitle("Administrador: " + usuario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Propietarios registrados"));
        add(norte, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarClientes();
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton btnEliminar = new JButton("Eliminar propietario");
        JButton btnActualizar = new JButton("Actualizar lista");
        btnEliminar.addActionListener(e -> eliminarPropietario());
        btnActualizar.addActionListener(e -> cargarTabla());

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sur.add(lblClientes);
        sur.add(btnEliminar);
        sur.add(btnActualizar);
        add(sur, BorderLayout.SOUTH);

        cargarTabla();
    }

    private void cargarTabla() {
        propietarios = new ArrayList<>(new ServicioPropietario().listar());
        modeloTabla.setRowCount(0);
        for (Propietario p : propietarios) {
            modeloTabla.addRow(new Object[]{p.getDni(), p.getNombres(), p.getApellidos(), p.getEmail(),
                p.getCelular(), p.getNumeroCuenta(), p.getBanco()});
        }
        lblClientes.setText("Seleccione un propietario para ver sus clientes.");
    }

    private Propietario propietarioSeleccionado() {
        int fila = tabla.getSelectedRow();
        return (fila >= 0 && fila < propietarios.size()) ? propietarios.get(fila) : null;
    }

    private void mostrarClientes() {
        Propietario p = propietarioSeleccionado();
        if (p == null) {
            return;
        }
        try {
            int clientes = administracion.contarClientesDePropietario(p.getId());
            lblClientes.setText("Clientes con alquiler en departamentos de " + p.getNombres() + ": " + clientes);
        } catch (IOException e) {
            lblClientes.setText("No se pudo leer alquiler.csv");
        }
    }

    private void eliminarPropietario() {
        Propietario p = propietarioSeleccionado();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un propietario de la tabla.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar a " + p.getNombres() + " " + p.getApellidos() + " (" + p.getId() + ")?\n"
                + "Se eliminarán sus datos, su cuenta, sus departamentos y sus alquileres.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            ServicioAdministracion.Resumen r = administracion.eliminarPropietario(p.getId());
            String mensaje = "Propietario eliminado.\nDepartamentos eliminados: " + r.departamentosEliminados
                    + "\nAlquileres eliminados: " + r.alquileresEliminados;
            if (r.alquileresConservados > 0) {
                mensaje += "\nAlquileres conservados hasta su fecha final (todas sus cuotas están pagadas): "
                        + r.alquileresConservados;
            }
            JOptionPane.showMessageDialog(this, mensaje);
            cargarTabla();
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
