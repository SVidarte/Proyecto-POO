package vista;

import servicio.ServicioCuenta;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.Arrays;

/** Cuadros de diálogo para registrarse e iniciar sesión (los usa el menú principal con los tres tipos de usuario). */
public final class DialogosAcceso {

    private static final int MAX_INTENTOS = 3;

    private DialogosAcceso() {
    }

    /**
     * Pide usuario, contraseña y confirmación de contraseña, y crea la cuenta.
     *
     * @return el usuario registrado, o null si se canceló
     */
    public static String registrar(Component padre, ServicioCuenta cuentas, String titulo) {
        JTextField txtUsuario = new JTextField(15);
        JPasswordField txtClave = new JPasswordField(15);
        JPasswordField txtConfirmar = new JPasswordField(15);
        Object[] campos = {"Usuario:", txtUsuario, "Contraseña:", txtClave, "Confirmar contraseña:", txtConfirmar};

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(padre, campos, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return null;
            }
            char[] clave = txtClave.getPassword();
            char[] confirmacion = txtConfirmar.getPassword();
            try {
                cuentas.registrar(txtUsuario.getText(), new String(clave), new String(confirmacion));
                JOptionPane.showMessageDialog(padre, "Cuenta creada correctamente.");
                return txtUsuario.getText().trim();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                JOptionPane.showMessageDialog(padre, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                Arrays.fill(clave, '\0'); // la contraseña no se deja en memoria
                Arrays.fill(confirmacion, '\0');
            }
        }
    }

    /**
     * Pide usuario y contraseña. Solo hay 3 intentos: si fallan los 3, la aplicación se cierra.
     *
     * @return el usuario que inició sesión, o null si se canceló
     */
    public static String iniciarSesion(Component padre, ServicioCuenta cuentas, String titulo) {
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            JTextField txtUsuario = new JTextField(15);
            JPasswordField txtClave = new JPasswordField(15);
            Object[] campos = {"Usuario:", txtUsuario, "Contraseña:", txtClave};

            int opcion = JOptionPane.showConfirmDialog(padre, campos,
                    titulo + " (intento " + intento + " de " + MAX_INTENTOS + ")",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return null;
            }
            char[] clave = txtClave.getPassword();
            boolean correcta;
            try {
                correcta = cuentas.validar(txtUsuario.getText(), new String(clave));
            } finally {
                Arrays.fill(clave, '\0'); // la contraseña no se deja en memoria
            }
            if (correcta) {
                return txtUsuario.getText().trim();
            }
            if (intento < MAX_INTENTOS) {
                JOptionPane.showMessageDialog(padre, "Usuario o contraseña incorrectos.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        System.exit(0); // tres intentos fallidos: la aplicación se cierra
        return null;
    }

    /**
     * Pide la contraseña actual y la nueva (dos veces).
     *
     * @return true si la contraseña se cambió, false si se canceló
     */
    public static boolean cambiarContrasena(Component padre, ServicioCuenta cuentas, String usuario) {
        JPasswordField txtActual = new JPasswordField(15);
        JPasswordField txtNueva = new JPasswordField(15);
        JPasswordField txtConfirmar = new JPasswordField(15);
        Object[] campos = {"Contraseña actual:", txtActual, "Contraseña nueva:", txtNueva,
            "Confirmar contraseña nueva:", txtConfirmar};

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(padre, campos, "Cambiar contraseña",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return false;
            }
            char[] actual = txtActual.getPassword();
            char[] nueva = txtNueva.getPassword();
            char[] confirmacion = txtConfirmar.getPassword();
            try {
                cuentas.cambiarContrasena(usuario, new String(actual), new String(nueva), new String(confirmacion));
                JOptionPane.showMessageDialog(padre, "Contraseña cambiada correctamente.");
                return true;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                JOptionPane.showMessageDialog(padre, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                Arrays.fill(actual, '\0');
                Arrays.fill(nueva, '\0');
                Arrays.fill(confirmacion, '\0');
            }
        }
    }

    /**
     * Muestra un mensaje y pide la contraseña del usuario para confirmar una acción delicada
     * (por ejemplo, eliminar la cuenta).
     *
     * @return true si la contraseña es correcta
     */
    public static boolean confirmarContrasena(Component padre, ServicioCuenta cuentas, String usuario, String mensaje) {
        JPasswordField txtClave = new JPasswordField(15);
        Object[] campos = {mensaje, "Contraseña:", txtClave};
        int opcion = JOptionPane.showConfirmDialog(padre, campos, "Confirmar con su contraseña",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return false;
        }
        char[] clave = txtClave.getPassword();
        try {
            if (cuentas.validar(usuario, new String(clave))) {
                return true;
            }
            JOptionPane.showMessageDialog(padre, "Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            Arrays.fill(clave, '\0');
        }
    }
}