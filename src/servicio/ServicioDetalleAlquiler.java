package servicio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modelo.Alquiler;
import modelo.DetalleAlquiler;

/**
 * Servicio que administra los detalles guardados en detalle_alquiler.csv
 * (formato: idDetalle;idAlquiler). Usa ServicioAlquiler para obtener los
 * datos del alquiler al que pertenece cada detalle.
 */
public class ServicioDetalleAlquiler {

    private static final String ARCHIVO = "detalle_alquiler.csv";
    private static final String CABECERA = "idDetalle;idAlquiler";
    private static final String PREFIJO = "D";

    private final File archivo;
    private final ServicioAlquiler servicioAlquiler;

    public ServicioDetalleAlquiler(ServicioAlquiler servicioAlquiler) throws IOException {
        this(servicioAlquiler, ARCHIVO);
    }

    public ServicioDetalleAlquiler(ServicioAlquiler servicioAlquiler, String ruta) throws IOException {
        this.servicioAlquiler = servicioAlquiler;
        this.archivo = new File(ruta);
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.println(CABECERA);
            }
        }
    }

    /** Devuelve todos los detalles cuyo alquiler todavía existe. */
    public List<DetalleAlquiler> listar() throws IOException {
        Map<String, Alquiler> alquileres = new HashMap<>();
        for (Alquiler alquiler : servicioAlquiler.listar()) {
            alquileres.put(alquiler.getId(), alquiler);
        }
        List<DetalleAlquiler> lista = new ArrayList<>();
        for (String[] par : leerPares()) {
            Alquiler alquiler = alquileres.get(par[1]);
            if (alquiler != null) {
                lista.add(new DetalleAlquiler(par[0], alquiler));
            }
        }
        return lista;
    }

    /** Devuelve solo los detalles de un alquiler. */
    public List<DetalleAlquiler> listarPorAlquiler(String idAlquiler) throws IOException {
        List<DetalleAlquiler> lista = new ArrayList<>();
        for (DetalleAlquiler detalle : listar()) {
            if (detalle.getIdAlquiler().equalsIgnoreCase(idAlquiler)) {
                lista.add(detalle);
            }
        }
        return lista;
    }

    /**
     * Guarda un detalle nuevo para el alquiler indicado. El id se genera
     * automáticamente (D0001, D0002, ...).
     */
    public DetalleAlquiler guardar(String idAlquiler) throws IOException {
        Alquiler alquiler = servicioAlquiler.buscar(idAlquiler);
        if (alquiler == null) {
            throw new IllegalArgumentException("El alquiler " + idAlquiler + " no existe.");
        }
        String idDetalle = generarId();
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
            pw.println(idDetalle + Alquiler.SEPARADOR + alquiler.getId());
        }
        return new DetalleAlquiler(idDetalle, alquiler);
    }

    /**
     * Cambia el alquiler al que pertenece un detalle.
     *
     * @return true si encontró el detalle
     */
    public boolean modificar(String idDetalle, String nuevoIdAlquiler) throws IOException {
        Alquiler alquiler = servicioAlquiler.buscar(nuevoIdAlquiler);
        if (alquiler == null) {
            throw new IllegalArgumentException("El alquiler " + nuevoIdAlquiler + " no existe.");
        }
        List<String[]> pares = leerPares();
        boolean encontrado = false;
        for (String[] par : pares) {
            if (par[0].equalsIgnoreCase(idDetalle)) {
                par[1] = alquiler.getId();
                encontrado = true;
                break;
            }
        }
        if (encontrado) {
            reescribir(pares);
        }
        return encontrado;
    }

    /** Busca un detalle por su id. Devuelve null si no existe. */
    public DetalleAlquiler buscar(String idDetalle) throws IOException {
        for (DetalleAlquiler detalle : listar()) {
            if (detalle.getIdDetalle().equalsIgnoreCase(idDetalle)) {
                return detalle;
            }
        }
        return null;
    }

    /** Elimina un detalle. Devuelve true si lo encontró. */
    public boolean eliminar(String idDetalle) throws IOException {
        List<String[]> pares = leerPares();
        boolean eliminado = pares.removeIf(par -> par[0].equalsIgnoreCase(idDetalle));
        if (eliminado) {
            reescribir(pares);
        }
        return eliminado;
    }

    /**
     * Elimina todos los detalles de un alquiler (se usa cuando se elimina el alquiler).
     *
     * @return cantidad de detalles eliminados
     */
    public int eliminarPorAlquiler(String idAlquiler) throws IOException {
        List<String[]> pares = leerPares();
        int antes = pares.size();
        pares.removeIf(par -> par[1].equalsIgnoreCase(idAlquiler));
        int eliminados = antes - pares.size();
        if (eliminados > 0) {
            reescribir(pares);
        }
        return eliminados;
    }

    /** Genera el siguiente id: toma el número más alto registrado y le suma 1. */
    public String generarId() throws IOException {
        int maximo = 0;
        for (String[] par : leerPares()) {
            try {
                int numero = Integer.parseInt(par[0].substring(PREFIJO.length()));
                if (numero > maximo) {
                    maximo = numero;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // id con formato raro: se ignora
            }
        }
        return String.format("%s%04d", PREFIJO, maximo + 1);
    }

    // ---------------------------------------------------------------------
    // Métodos privados
    // ---------------------------------------------------------------------

    /** Lee el archivo y devuelve cada línea como un arreglo {idDetalle, idAlquiler}. */
    private List<String[]> leerPares() throws IOException {
        List<String[]> pares = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("idDetalle" + Alquiler.SEPARADOR)) {
                    continue; // línea vacía o cabecera
                }
                String[] partes = linea.split(Alquiler.SEPARADOR, -1);
                if (partes.length >= 2) {
                    pares.add(new String[]{partes[0].trim(), partes[1].trim()});
                }
            }
        }
        return pares;
    }

    private void reescribir(List<String[]> pares) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println(CABECERA);
            for (String[] par : pares) {
                pw.println(par[0] + Alquiler.SEPARADOR + par[1]);
            }
        }
    }
}
