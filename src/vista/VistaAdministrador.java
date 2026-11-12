package vista;

import modelo.Cliente;
import modelo.Propietario;
import servicio.ServicioAdministracion;
import servicio.ServicioCliente;
import servicio.ServicioCuenta;
import servicio.ServicioPropietario;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Opciones del administrador: ver y buscar propietarios (por DNI) y eliminar al que esté seleccionado, ver y
 * buscar clientes (por DNI), cambiar su contraseña y eliminar su cuenta.
 */
public class VistaAdministrador extends JFrame {

    private final String usuario;
    private final ServicioAdministracion administracion;
    private List<Propietario> propietarios = new ArrayList<>(); // mismo orden que las filas de la tabla

    private final ModeloTabla modeloPropietarios = new ModeloTabla(
            new String[]{"DNI", "Nombres", "Apellidos", "Email", "Celular", "Nro. de cuenta", "Banco"});
    private final JTable tablaPropietarios = new JTable(modeloPropietarios);
    private final JTextField txtBuscarPropietario = new JTextField(12);
    private final JLabel lblClientes = new JLabel("Seleccione un propietario para ver sus clientes.");

    private final ModeloTabla modeloClientes = new ModeloTabla(
            new String[]{"ID", "DNI", "Nombres", "Apellidos", "Email", "Celular", "Sexo", "Referencia"});
    private final JTable tablaClientes = new JTable(modeloClientes);
    private final JTextField txtBuscarCliente = new JTextField(12);

    public VistaAdministrador(String usuario) {
        this.usuario = usuario;
        try {
            administracion = new ServicioAdministracion();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir alquiler.csv: " + e.getMessage());
        }
        setTitle("Administrador: " + usuario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(crearPanelCuenta(), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Propietarios", crearPanelPropietarios());
        pestanas.addTab("Clientes", crearPanelClientes());
        add(pestanas, BorderLayout.CENTER);

        cargarPropietarios(new ServicioPropietario().listar());
        cargarClientes(new ServicioCliente().listar());
    }

    private JPanel crearPanelCuenta() {
        JButton btnContrasena = new JButton("Cambiar contraseña");
        JButton btnEliminarCuenta = new JButton("Eliminar mi cuenta");
        btnContrasena.addActionListener(e ->
                DialogosAcceso.cambiarContrasena(this, ServicioCuenta.administradores(), usuario));
        btnEliminarCuenta.addActionListener(e -> eliminarMiCuenta());

        JButton btnVolver = new JButton("Volver");
        btnVolver.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.add(btnVolver);
        panel.add(btnContrasena);
        panel.add(btnEliminarCuenta);
        return panel;
    }

    // ---------------------------------------------------------------
    // Pestaña Propietarios
    // ---------------------------------------------------------------

    private JPanel crearPanelPropietarios() {
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodos = new JButton("Mostrar todos");
        btnBuscar.addActionListener(e -> buscarPropietarios());
        btnTodos.addActionListener(e -> {
            txtBuscarPropietario.setText("");
            cargarPropietarios(new ServicioPropietario().listar());
        });
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busqueda.add(new JLabel("Buscar por DNI:"));
        busqueda.add(txtBuscarPropietario);
        busqueda.add(btnBuscar);
        busqueda.add(btnTodos);

        tablaPropietarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPropietarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarClientes();
            }
        });

        JButton btnEliminar = new JButton("Eliminar propietario");
        btnEliminar.addActionListener(e -> eliminarPropietario());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sur.add(lblClientes);
        sur.add(btnEliminar);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(busqueda, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaPropietarios), BorderLayout.CENTER);
        panel.add(sur, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarPropietarios(List<Propietario> lista) {
        propietarios = new ArrayList<>(lista);
        modeloPropietarios.setRowCount(0);
        for (Propietario p : propietarios) {
            modeloPropietarios.addRow(new Object[]{p.getDni(), p.getNombres(), p.getApellidos(), p.getEmail(),
                p.getCelular(), p.getNumeroCuenta(), p.getBanco()});
        }
        lblClientes.setText("Seleccione un propietario para ver sus clientes.");
    }

    private void buscarPropietarios() {
        String dni = txtBuscarPropietario.getText().trim();
        if (dni.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba el DNI que desea buscar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Propietario> resultado = new ServicioPropietario().buscarPorDni(dni);
        cargarPropietarios(resultado);
        if (resultado.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron propietarios con ese DNI.");
        }
    }

    private Propietario propietarioSeleccionado() {
        int fila = tablaPropietarios.getSelectedRow();
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
            cargarPropietarios(new ServicioPropietario().listar());
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------
    // Pestaña Clientes
    // ---------------------------------------------------------------

    private JPanel crearPanelClientes() {
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodos = new JButton("Mostrar todos");
        btnBuscar.addActionListener(e -> buscarClientes());
        btnTodos.addActionListener(e -> {
            txtBuscarCliente.setText("");
            cargarClientes(new ServicioCliente().listar());
        });
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busqueda.add(new JLabel("Buscar por DNI:"));
        busqueda.add(txtBuscarCliente);
        busqueda.add(btnBuscar);
        busqueda.add(btnTodos);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(busqueda, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);
        return panel;
    }

    private void cargarClientes(List<Cliente> lista) {
        modeloClientes.setRowCount(0);
        for (Cliente c : lista) {
            modeloClientes.addRow(new Object[]{c.getId(), c.getDni(), c.getNombres(), c.getApellidos(),
                c.getEmail(), c.getCelular(), c.getSexo(), c.getReferencia()});
        }
    }

    private void buscarClientes() {
        String dni = txtBuscarCliente.getText().trim();
        if (dni.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba el DNI que desea buscar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Cliente> resultado = new ServicioCliente().buscarPorDni(dni);
        cargarClientes(resultado);
        if (resultado.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron clientes con ese DNI.");
        }
    }

    // ---------------------------------------------------------------
    // Cuenta del administrador
    // ---------------------------------------------------------------

    private void eliminarMiCuenta() {
        ServicioCuenta cuentas = ServicioCuenta.administradores();
        if (!DialogosAcceso.confirmarContrasena(this, cuentas, usuario,
                "Se eliminará su cuenta de administrador. ¿Continuar?")) {
            return;
        }
        cuentas.eliminarCuenta(usuario);
        JOptionPane.showMessageDialog(this, "Su cuenta fue eliminada.");
        dispose();
    }
}
