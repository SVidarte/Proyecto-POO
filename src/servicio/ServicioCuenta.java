package servicio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuentas de acceso (usuario y contraseña) de un tipo de usuario: administrador, propietario o cliente.
 * Cada tipo tiene su archivo: dadm.csv, dprop.csv y dcli.csv (se crean solos si no existen).
 *
 * Formato de cada línea:  usuario;sal;hash;idPerfil
 *  - La contraseña nunca se guarda: se guarda su hash SHA-256 calculado con una sal aleatoria propia de cada usuario.
 *  - idPerfil es el id del Propietario o Cliente asociado a la cuenta ("-" mientras no haya registrado sus datos).
 */
public class ServicioCuenta {

    public static final String ARCHIVO_ADMINISTRADOR = "dadm.csv";
    public static final String ARCHIVO_PROPIETARIO = "dprop.csv";
    public static final String ARCHIVO_CLIENTE = "dcli.csv";

    private static final String SIN_PERFIL = "-";
    private static final int LONGITUD_MINIMA_CONTRASENA = 4;

    private final File archivo;

    public ServicioCuenta(String ruta) {
        archivo = new File(ruta);
        if (!archivo.exists()) {
            try {
                archivo.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo crear " + ruta + ": " + e.getMessage());
            }
        }
    }

    public static ServicioCuenta administradores() {
        return new ServicioCuenta(ARCHIVO_ADMINISTRADOR);
    }

    public static ServicioCuenta propietarios() {
        return new ServicioCuenta(ARCHIVO_PROPIETARIO);
    }

    public static ServicioCuenta clientes() {
        return new ServicioCuenta(ARCHIVO_CLIENTE);
    }

    // ---------------------------------------------------------------
    // Registro e inicio de sesión
    // ---------------------------------------------------------------

    /**
     * Crea una cuenta nueva.
     *
     * @throws IllegalArgumentException si falta algún dato, las contraseñas no coinciden o el usuario ya existe
     */
    public void registrar(String usuario, String contrasena, String confirmacion) {
        String u = usuario == null ? "" : usuario.trim();
        if (u.isEmpty()) {
            throw new IllegalArgumentException("Ingrese un usuario.");
        }
        if (u.contains(";")) {
            throw new IllegalArgumentException("El usuario no puede contener el carácter \";\".");
        }
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        List<String[]> cuentas = leer();
        if (buscar(cuentas, u) != null) {
            throw new IllegalArgumentException("El usuario \"" + u + "\" ya existe.");
        }
        String sal = nuevaSal();
        cuentas.add(new String[]{u, sal, hash(sal, contrasena), SIN_PERFIL});
        escribir(cuentas);
    }

    /** true si el usuario existe y la contraseña es correcta. */
    public boolean validar(String usuario, String contrasena) {
        if (usuario == null || contrasena == null) {
            return false;
        }
        String[] cuenta = buscar(leer(), usuario.trim());
        if (cuenta == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(cuenta[1], contrasena).getBytes(StandardCharsets.UTF_8),
                cuenta[2].getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------
    // Perfil asociado (Propietario o Cliente)
    // ---------------------------------------------------------------

    /** Id del perfil asociado al usuario, o null si todavía no registró sus datos. */
    public String perfilDe(String usuario) {
        String[] cuenta = buscar(leer(), usuario == null ? "" : usuario.trim());
        if (cuenta == null || SIN_PERFIL.equals(cuenta[3])) {
            return null;
        }
        return cuenta[3];
    }

    /** Asocia el perfil (por ejemplo P0001) a la cuenta del usuario. */
    public void vincularPerfil(String usuario, String idPerfil) {
        List<String[]> cuentas = leer();
        String[] cuenta = buscar(cuentas, usuario == null ? "" : usuario.trim());
        if (cuenta == null) {
            throw new IllegalArgumentException("El usuario no existe.");
        }
        cuenta[3] = idPerfil;
        escribir(cuentas);
    }

    /** Deja sin perfil la cuenta asociada a ese id: al iniciar sesión tendrá que registrar sus datos otra vez. */
    public void desvincularPerfil(String idPerfil) {
        List<String[]> cuentas = leer();
        boolean cambio = false;
        for (String[] c : cuentas) {
            if (c[3].equalsIgnoreCase(idPerfil)) {
                c[3] = SIN_PERFIL;
                cambio = true;
            }
        }
        if (cambio) {
            escribir(cuentas);
        }
    }

    /** Elimina la cuenta asociada a ese id. */
    public void eliminarPorPerfil(String idPerfil) {
        List<String[]> cuentas = leer();
        if (cuentas.removeIf(c -> c[3].equalsIgnoreCase(idPerfil))) {
            escribir(cuentas);
        }
    }

    // ---------------------------------------------------------------
    // Archivo y SHA-256
    // ---------------------------------------------------------------

    private String[] buscar(List<String[]> cuentas, String usuario) {
        for (String[] c : cuentas) {
            if (c[0].equalsIgnoreCase(usuario)) {
                return c;
            }
        }
        return null;
    }

    private List<String[]> leer() {
        List<String[]> cuentas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";", -1);
                if (partes.length == 4) {
                    cuentas.add(partes);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo.getName() + ": " + e.getMessage());
        }
        return cuentas;
    }

    private void escribir(List<String[]> cuentas) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String[] c : cuentas) {
                pw.println(c[0] + ";" + c[1] + ";" + c[2] + ";" + c[3]);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar " + archivo.getName() + ": " + e.getMessage());
        }
    }

    private String nuevaSal() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return aHex(bytes);
    }

    private String hash(String sal, String contrasena) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return aHex(md.digest((sal + contrasena).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible: " + e.getMessage());
        }
    }

    private String aHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
