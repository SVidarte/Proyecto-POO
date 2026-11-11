package vista;

import modelo.Cliente;
import servicio.ServicioAdministracion;
import servicio.ServicioCliente;
import servicio.ServicioCuenta;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Opciones del cliente: Pagos, Planificación de Pagos, Alquileres Disponibles, Alquileres Actuales,
 * Mis datos, cambiar contraseña y eliminar su cuenta.
 */
public class VistaMenuCliente extends JFrame {

    private final String idCliente;
    private final String usuario;
    private final JLabel lblBienvenida = new JLabel("", SwingConstants.CENTER);

    public VistaMenuCliente(String idCliente, String usuario) {
        this.idCliente = idCliente;
        this.usuario = usuario;

        setTitle("Cliente");
        setSize(520, 330);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        actualizarBienvenida();
        lblBienvenida.setFont(new Font("Arial", Font.BOLD, 14));
        add(lblBienvenida, BorderLayout.NORTH);

        JPanel botones = new JPanel(new GridLayout(4, 2, 10, 10));
        botones.add(crearBoton("Pagos", e -> new VistaPago(idCliente).setVisible(true)));
        botones.add(crearBoton("Planificación de Pagos", e -> new VistaPlanificacionPago(idCliente).setVisible(true)));
        botones.add(crearBoton("Alquileres Disponibles", e ->
                new VistaAlquileresCliente(idCliente, VistaAlquileresCliente.Modo.DISPONIBLES).setVisible(true)));
        botones.add(crearBoton("Alquileres Actuales", e ->
                new VistaAlquileresCliente(idCliente, VistaAlquileresCliente.Modo.ACTUALES).setVisible(true)));
        botones.add(crearBoton("Mis datos", e -> misDatos()));
        botones.add(crearBoton("Cambiar contraseña", e ->
                DialogosAcceso.cambiarContrasena(this, ServicioCuenta.clientes(), usuario)));
        botones.add(crearBoton("Eliminar mi cuenta", e -> eliminarMiCuenta()));
        add(botones, BorderLayout.CENTER);
    }

    private JButton crearBoton(String texto, java.awt.event.ActionListener accion) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("Arial", Font.PLAIN, 13));
        boton.addActionListener(accion);
        return boton;
    }

    private void actualizarBienvenida() {
        Cliente c = new ServicioCliente().buscar(idCliente);
        String nombre = (c != null) ? c.getNombres() + " " + c.getApellidos() : idCliente;
        lblBienvenida.setText("Cliente: " + nombre + " (" + idCliente + ")");
    }

    private void misDatos() {
        Cliente c = new ServicioCliente().buscar(idCliente);
        if (c == null) {
            JOptionPane.showMessageDialog(this, "No se encontraron sus datos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new DialogoPerfilCliente(this, c).setVisible(true);
        actualizarBienvenida();
    }

    private void eliminarMiCuenta() {
        ServicioCuenta cuentas = ServicioCuenta.clientes();
        if (!DialogosAcceso.confirmarContrasena(this, cuentas, usuario,
                "Se eliminarán su cuenta, sus datos y sus alquileres. ¿Continuar?")) {
            return;
        }
        try {
            ServicioAdministracion.Resumen r = new ServicioAdministracion().eliminarClienteYCuenta(idCliente, usuario);
            JOptionPane.showMessageDialog(this, "Su cuenta fue eliminada.\nAlquileres eliminados: " + r.alquileresEliminados);
            dispose();
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar la cuenta: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}