package vista;

import modelo.Alquiler;
import modelo.Cliente;
import modelo.Departamento;
import modelo.Propietario;
import servicio.ServicioAdministracion;
import servicio.ServicioAlquiler;
import servicio.ServicioCliente;
import servicio.ServicioDepartamento;
import servicio.ServicioPropietario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Opciones del propietario: lista de sus departamentos (al elegir uno aparece abajo su cliente, que se puede
 * eliminar), "Crear departamento" y "Crear alquiler".
 */
public class VistaMenuPropietario extends JFrame {

    private final String idPropietario;
    private List<Departamento> departamentos = new ArrayList<>();
    private List<Cliente> clientes = new ArrayList<>();
    private boolean cargando = false;

    private final ModeloTabla modeloDepartamentos =
            new ModeloTabla(new String[]{"ID", "Piso", "Número", "Dirección", "Precio mensual", "Estado"});
    private final JTable tablaDepartamentos = new JTable(modeloDepartamentos);

    private final ModeloTabla modeloClientes =
            new ModeloTabla(new String[]{"ID", "DNI", "Nombres", "Apellidos", "Email", "Celular", "Referencia"});
    private final JTable tablaClientes = new JTable(modeloClientes);
    private final JPanel panelClientes = new JPanel(new BorderLayout(5, 5));
    private final JButton btnEliminarCliente = new JButton("Eliminar cliente");

    public VistaMenuPropietario(String idPropietario) {
        this.idPropietario = idPropietario;

        setTitle("Propietario");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(crearPanelSuperior(), BorderLayout.NORTH);

        tablaDepartamentos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaDepartamentos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargando) {
                mostrarClientesDelDepartamentoSeleccionado();
            }
        });
        JScrollPane scrollDepartamentos = new JScrollPane(tablaDepartamentos);
        scrollDepartamentos.setBorder(BorderFactory.createTitledBorder("Mis departamentos"));
        add(scrollDepartamentos, BorderLayout.CENTER);

        // Panel que aparece debajo de la lista al elegir un departamento
        tablaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnEliminarCliente.setEnabled(tablaClientes.getSelectedRow() >= 0);
            }
        });
        btnEliminarCliente.setEnabled(false);
        btnEliminarCliente.addActionListener(e -> eliminarCliente());
        JPanel botonCliente = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonCliente.add(btnEliminarCliente);
        panelClientes.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);
        panelClientes.add(botonCliente, BorderLayout.SOUTH);
        panelClientes.setPreferredSize(new Dimension(100, 200));
        panelClientes.setVisible(false);
        add(panelClientes, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                cargarDepartamentos(); // por si se creó o modificó algo en otra ventana
            }
        });
        cargarDepartamentos();
    }

    private JPanel crearPanelSuperior() {
        Propietario p = new ServicioPropietario().buscar(idPropietario);
        String nombre = (p != null) ? p.getNombres() + " " + p.getApellidos() : idPropietario;
        JLabel lblBienvenida = new JLabel("Propietario: " + nombre + " (" + idPropietario + ")");
        lblBienvenida.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnCrearDepartamento = new JButton("Crear departamento");
        JButton btnCrearAlquiler = new JButton("Crear alquiler");
        btnCrearDepartamento.addActionListener(e -> new VistaDepartamento(idPropietario).setVisible(true));
        btnCrearAlquiler.addActionListener(e -> new VistaAlquiler(idPropietario).setVisible(true));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.add(lblBienvenida);
        panel.add(btnCrearDepartamento);
        panel.add(btnCrearAlquiler);
        return panel;
    }

    // ---------------------------------------------------------------
    // Departamentos y clientes
    // ---------------------------------------------------------------

    private Departamento departamentoSeleccionado() {
        int fila = tablaDepartamentos.getSelectedRow();
        return (fila >= 0 && fila < departamentos.size()) ? departamentos.get(fila) : null;
    }

    /** Vuelve a leer los departamentos del propietario conservando el que estaba elegido. */
    private void cargarDepartamentos() {
        Departamento anterior = departamentoSeleccionado();
        String idAnterior = (anterior != null) ? anterior.getId_departamento() : null;

        cargando = true;
        departamentos = new ServicioDepartamento().listarPorPropietario(idPropietario);
        modeloDepartamentos.setRowCount(0);
        int indice = -1;
        for (int i = 0; i < departamentos.size(); i++) {
            Departamento d = departamentos.get(i);
            modeloDepartamentos.addRow(new Object[]{d.getId_departamento(), d.getPiso(), d.getNumero_departamento(),
                d.getDireccion(), String.format(Locale.US, "%.2f", d.getPrecio_mensual()), d.getEstado()});
            if (d.getId_departamento().equals(idAnterior)) {
                indice = i;
            }
        }
        if (indice >= 0) {
            tablaDepartamentos.setRowSelectionInterval(indice, indice);
        }
        cargando = false;

        mostrarClientesDelDepartamentoSeleccionado();
    }

    private void mostrarClientesDelDepartamentoSeleccionado() {
        Departamento d = departamentoSeleccionado();
        if (d == null) {
            panelClientes.setVisible(false);
            revalidate();
            return;
        }
        modeloClientes.setRowCount(0);
        clientes = new ArrayList<>();
        try {
            Set<String> idsClientes = new LinkedHashSet<>();
            for (Alquiler a : new ServicioAlquiler().listar()) {
                if (a.getIdDepartamento().equalsIgnoreCase(d.getId_departamento())
                        && a.getIdCliente() != null && !a.getIdCliente().trim().isEmpty()) {
                    idsClientes.add(a.getIdCliente().trim());
                }
            }
            ServicioCliente servicioCliente = new ServicioCliente();
            for (String id : idsClientes) {
                Cliente c = servicioCliente.buscar(id);
                if (c != null) {
                    clientes.add(c);
                    modeloClientes.addRow(new Object[]{c.getId(), c.getDni(), c.getNombres(), c.getApellidos(),
                        c.getEmail(), c.getCelular(), c.getReferencia()});
                }
            }
        } catch (IOException e) {
            error("No se pudo leer alquiler.csv: " + e.getMessage());
        }
        panelClientes.setBorder(BorderFactory.createTitledBorder(
                "Cliente del departamento " + d.getId_departamento()
                + (clientes.isEmpty() ? " (ninguno por ahora)" : "")));
        btnEliminarCliente.setEnabled(false);
        panelClientes.setVisible(true);
        revalidate();
        repaint();
    }

    private void eliminarCliente() {
        int fila = tablaClientes.getSelectedRow();
        if (fila < 0 || fila >= clientes.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente de la tabla.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Cliente c = clientes.get(fila);
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar al cliente " + c.getNombres() + " " + c.getApellidos() + " (" + c.getId() + ")?\n"
                + "También se eliminarán todos sus alquileres.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            ServicioAdministracion.Resumen r = new ServicioAdministracion().eliminarCliente(c.getId());
            String mensaje = "Cliente eliminado.\nAlquileres eliminados: " + r.alquileresEliminados;
            if (r.alquileresConservados > 0) {
                mensaje += "\nAlquileres conservados hasta su fecha final (todas sus cuotas están pagadas): "
                        + r.alquileresConservados;
            }
            JOptionPane.showMessageDialog(this, mensaje);
            cargarDepartamentos();
        } catch (IOException | RuntimeException ex) {
            error("Error al eliminar: " + ex.getMessage());
        }
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
