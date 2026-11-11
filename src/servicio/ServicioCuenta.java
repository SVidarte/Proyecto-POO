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

import modelo.DatosInvalidosException;

/**
 * Cuentas de acceso (usuario y contraseña) de los tres tipos de usuario: administrador, propietario y cliente.
 * Todas se guardan en un único archivo, cuentas.csv (se crea solo si no existe).
 *
 * Formato de cada línea:  rol;usuario;sal;hash;idPerfil
 *  - La contraseña nunca se guarda: se guarda su hash SHA-256 calculado con una sal aleatoria propia de cada usuario.
 *  - idPerfil es el id del Propietario o Cliente asociado a la cuenta ("-" mientras no haya registrado sus datos).
 *  - Cada instancia trabaja solo con las cuentas de su rol: el usuario es único dentro de cada rol.
 */
public class ServicioCuenta {

    public static final String ARCHIVO = "cuentas.csv";

    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_PROPIETARIO = "PROPIETARIO";
    public static final String ROL_CLIENTE = "CLIENTE";

    private static final String SIN_PERFIL = "-";
    private static final int LONGITUD_MINIMA_CONTRASENA = 4;

    // columnas de cada fila
    private static final int ROL = 0;
    private static final int USUARIO = 1;
    private static final int SAL = 2;
    private static final int HASH = 3;
    private static final int PERFIL = 4;

    private final File archivo = new File(ARCHIVO);
    private final String rol;

    public ServicioCuenta(String rol) {
        this.rol = rol;
        if (!archivo.exists()) {
            try {
                archivo.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo crear " + ARCHIVO + ": " + e.getMessage());
            }
            importarArchivosAnteriores();
        }
    }

    public static ServicioCuenta administradores() {
        return new ServicioCuenta(ROL_ADMINISTRADOR);
    }

    public static ServicioCuenta propietarios() {
        return new ServicioCuenta(ROL_PROPIETARIO);
    }

    public static ServicioCuenta clientes() {
        return new ServicioCuenta(ROL_CLIENTE);
    }

    // ---------------------------------------------------------------
    // Registro e inicio de sesión
    // ---------------------------------------------------------------

    /**
     * Crea una cuenta nueva.
     *
     * @throws DatosInvalidosException si falta algún dato, las contraseñas no coinciden o el usuario ya existe
     */
    public void registrar(String usuario, String contrasena, String confirmacion) {
        String u = usuario == null ? "" : usuario.trim();
        if (u.isEmpty()) {
            throw new DatosInvalidosException("Ingrese un usuario.");
        }
        if (u.contains(";")) {
            throw new DatosInvalidosException("El usuario no puede contener el carácter \";\".");
        }
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new DatosInvalidosException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new DatosInvalidosException("Las contraseñas no coinciden.");
        }
        List<String[]> cuentas = leer();
        if (buscar(cuentas, u) != null) {
            throw new DatosInvalidosException("El usuario \"" + u + "\" ya existe.");
        }
        String sal = nuevaSal();
        cuentas.add(new String[]{rol, u, sal, hash(sal, contrasena), SIN_PERFIL});
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
                hash(cuenta[SAL], contrasena).getBytes(StandardCharsets.UTF_8),
                cuenta[HASH].getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Cambia la contraseña de una cuenta (guarda un hash nuevo con una sal nueva).
     *
     * @throws DatosInvalidosException si la contraseña actual es incorrecta o la nueva no es válida
     */
    public void cambiarContrasena(String usuario, String actual, String nueva, String confirmacion) {
        if (!validar(usuario, actual)) {
            throw new DatosInvalidosException("La contraseña actual es incorrecta.");
        }
        if (nueva == null || nueva.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new DatosInvalidosException(
                    "La contraseña nueva debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new DatosInvalidosException("Las contraseñas nuevas no coinciden.");
        }
        List<String[]> cuentas = leer();
        String[] cuenta = buscar(cuentas, usuario.trim());
        cuenta[SAL] = nuevaSal();
        cuenta[HASH] = hash(cuenta[SAL], nueva);
        escribir(cuentas);
    }

    /** Elimina la cuenta del usuario. @return true si existía */
    public boolean eliminarCuenta(String usuario) {
        List<String[]> cuentas = leer();
        String u = usuario == null ? "" : usuario.trim();
        boolean eliminada = cuentas.removeIf(c -> c[ROL].equals(rol) && c[USUARIO].equalsIgnoreCase(u));
        if (eliminada) {
            escribir(cuentas);
        }
        return eliminada;
    }

    // ---------------------------------------------------------------
    // Perfil asociado (Propietario o Cliente)
    // ---------------------------------------------------------------

    /** Id del perfil asociado al usuario, o null si todavía no registró sus datos. */
    public String perfilDe(String usuario) {
        String[] cuenta = buscar(leer(), usuario == null ? "" : usuario.trim());
        if (cuenta == null || SIN_PERFIL.equals(cuenta[PERFIL])) {
            return null;
        }
        return cuenta[PERFIL];
    }

    /** Asocia el perfil (por ejemplo P0001) a la cuenta del usuario. */
    public void vincularPerfil(String usuario, String idPerfil) {
        List<String[]> cuentas = leer();
        String[] cuenta = buscar(cuentas, usuario == null ? "" : usuario.trim());
        if (cuenta == null) {
            throw new DatosInvalidosException("El usuario no existe.");
        }
        cuenta[PERFIL] = idPerfil;
        escribir(cuentas);
    }

    /** Deja sin perfil la cuenta asociada a ese id: al iniciar sesión tendrá que registrar sus datos otra vez. */
    public void desvincularPerfil(String idPerfil) {
        List<String[]> cuentas = leer();
        boolean cambio = false;
        for (String[] c : cuentas) {
            if (c[ROL].equals(rol) && c[PERFIL].equalsIgnoreCase(idPerfil)) {
                c[PERFIL] = SIN_PERFIL;
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
        if (cuentas.removeIf(c -> c[ROL].equals(rol) && c[PERFIL].equalsIgnoreCase(idPerfil))) {
            escribir(cuentas);
        }
    }

    // ---------------------------------------------------------------
    // Archivo y SHA-256
    // ---------------------------------------------------------------

    /** Cuenta de este rol con ese usuario (sin importar mayúsculas), o null. */
    private String[] buscar(List<String[]> cuentas, String usuario) {
        for (String[] c : cuentas) {
            if (c[ROL].equals(rol) && c[USUARIO].equalsIgnoreCase(usuario)) {
                return c;
            }
        }
        return null;
    }

    /** Lee las cuentas de todos los roles (se reescriben completas para no perder las de los demás). */
    private List<String[]> leer() {
        List<String[]> cuentas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";", -1);
                if (partes.length == 5) {
                    cuentas.add(partes);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + ARCHIVO + ": " + e.getMessage());
        }
        return cuentas;
    }

    private void escribir(List<String[]> cuentas) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String[] c : cuentas) {
                pw.println(c[ROL] + ";" + c[USUARIO] + ";" + c[SAL] + ";" + c[HASH] + ";" + c[PERFIL]);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar " + ARCHIVO + ": " + e.getMessage());
        }
    }

    /**
     * Al crear cuentas.csv por primera vez, importa las cuentas de los archivos que se usaban antes
     * (dadm.csv, dprop.csv y dcli.csv, con el formato usuario;sal;hash;idPerfil). Esos archivos ya no se usan
     * y se pueden borrar.
     */
    private void importarArchivosAnteriores() {
        String[][] anteriores = {
            {"dadm.csv", ROL_ADMINISTRADOR}, {"dprop.csv", ROL_PROPIETARIO}, {"dcli.csv", ROL_CLIENTE}
        };
        List<String[]> cuentas = new ArrayList<>();
        for (String[] anterior : anteriores) {
            File f = new File(anterior[0]);
            if (!f.exists()) {
                continue;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] p = linea.split(";", -1);
                    if (p.length == 4) {
                        cuentas.add(new String[]{anterior[1], p[0], p[1], p[2], p[3]});
                    }
                }
            } catch (IOException e) {
                System.err.println("No se pudo importar " + anterior[0] + ": " + e.getMessage());
            }
        }
        if (!cuentas.isEmpty()) {
            escribir(cuentas);
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
