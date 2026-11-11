package vista;

import servicio.ServicioAlquiler;
import servicio.ServicioCliente;
import servicio.ServicioCuenta;
import servicio.ServicioPropietario;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/** Punto de entrada: elige el tipo de usuario (Administrador, Propietario o Cliente) y gestiona el acceso. */
public class MenuPrincipal extends JFrame {

    public MenuPrincipal() {
        // Se crean dadm.csv, dprop.csv y dcli.csv si todavía no existen
        ServicioCuenta.administradores();
        ServicioCuenta.propietarios();
        ServicioCuenta.clientes();
        eliminarAlquileresFinalizados();
        inicializarComponentes();
    }

    /** Un alquiler con todas sus cuotas pagadas se elimina el día en que llega su fecha final. */
    private void eliminarAlquileresFinalizados() {
        try {
            new ServicioAlquiler().limpiarFinalizados();
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo revisar los alquileres finalizados: " + e.getMessage());
        }
    }

    private void inicializarComponentes() {
        setTitle("Sistema de Alquileres - Menú Principal");
        setSize(400, 330);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel lblTitulo = new JLabel("Menú Principal", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitulo.setBounds(50, 30, 280, 30);
        add(lblTitulo);

        JButton btnAdministrador = crearBoton("Administrador", 90);
        JButton btnPropietario = crearBoton("Propietario", 150);
        JButton btnCliente = crearBoton("Cliente", 210);

        btnAdministrador.addActionListener(e -> accesoAdministrador());
        btnPropietario.addActionListener(e -> accesoPropietario());
        btnCliente.addActionListener(e -> accesoCliente());
    }

    private JButton crearBoton(String texto, int y) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("Arial", Font.PLAIN, 14));
        boton.setBounds(80, y, 220, 40);
        add(boton);
        return boton;
    }

    // ---------------------------------------------------------------
    // Acceso de cada tipo de usuario
    // ---------------------------------------------------------------

    private void accesoAdministrador() {
        String usuario = elegirAcceso("Administrador", ServicioCuenta.administradores());
        if (usuario != null) {
            new VistaAdministrador(usuario).setVisible(true);
        }
    }

    private void accesoPropietario() {
        ServicioCuenta cuentas = ServicioCuenta.propietarios();
        String usuario = elegirAcceso("Propietario", cuentas);
        if (usuario == null) {
            return;
        }
        String id = cuentas.perfilDe(usuario);
        if (id != null && new ServicioPropietario().buscar(id) == null) {
            id = null; // sus datos ya no existen: debe registrarlos otra vez
        }
        if (id == null) {
            DialogoPerfilPropietario dialogo = new DialogoPerfilPropietario(this);
            dialogo.setVisible(true);
            id = dialogo.getIdCreado();
            if (id == null) {
                JOptionPane.showMessageDialog(this, "Debe registrar sus datos para continuar.");
                return;
            }
            cuentas.vincularPerfil(usuario, id);
        }
        new VistaMenuPropietario(id, usuario).setVisible(true);
    }

    private void accesoCliente() {
        ServicioCuenta cuentas = ServicioCuenta.clientes();
        String usuario = elegirAcceso("Cliente", cuentas);
        if (usuario == null) {
            return;
        }
        String id = cuentas.perfilDe(usuario);
        if (id != null && new ServicioCliente().buscar(id) == null) {
            id = null; // sus datos ya no existen: debe registrarlos otra vez
        }
        if (id == null) {
            DialogoPerfilCliente dialogo = new DialogoPerfilCliente(this);
            dialogo.setVisible(true);
            id = dialogo.getIdCreado();
            if (id == null) {
                JOptionPane.showMessageDialog(this, "Debe registrar sus datos para continuar.");
                return;
            }
            cuentas.vincularPerfil(usuario, id);
        }
        new VistaMenuCliente(id, usuario).setVisible(true);
    }

    /** Muestra "Registrarse / Iniciar sesión" y devuelve el usuario que accedió (null si canceló). */
    private String elegirAcceso(String rol, ServicioCuenta cuentas) {
        Object[] opciones = {"Registrarse", "Iniciar sesión"};
        int opcion = JOptionPane.showOptionDialog(this, "¿Qué desea hacer?", rol,
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[1]);
        if (opcion == 0) {
            return DialogosAcceso.registrar(this, cuentas, rol + " - Registrarse");
        }
        if (opcion == 1) {
            return DialogosAcceso.iniciarSesion(this, cuentas, rol + " - Iniciar sesión");
        }
        return null;
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new MenuPrincipal().setVisible(true));
    }
}