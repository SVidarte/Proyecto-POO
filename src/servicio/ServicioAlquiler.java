package servicio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import modelo.Alquiler;
import modelo.AlquilerDepartamento;

/**
 * Servicio que administra los alquileres guardados en el archivo alquiler.csv.
 * Cada dato se separa con ";".
 */
public class ServicioAlquiler {

    private static final String ARCHIVO = "alquiler.csv";
    private static final String CABECERA =
            "id;idCliente;idPropietario;idDepartamento;fechaInicial;fechaFinal;montoPagar";
    private static final String PREFIJO = "A";

    private final File archivo;

    public ServicioAlquiler() throws IOException {
        this(ARCHIVO);
    }

    public ServicioAlquiler(String ruta) throws IOException {
        archivo = new File(ruta);
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.println(CABECERA);
            }
        }
    }

    /** Lee el archivo y devuelve todos los alquileres en una lista. */
    public List<Alquiler> listar() throws IOException {
        List<Alquiler> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("id" + Alquiler.SEPARADOR)) {
                    continue; // línea vacía o cabecera
                }
                Alquiler alquiler = convertirLinea(linea);
                if (alquiler != null) {
                    lista.add(alquiler);
                }
            }
        }
        return lista;
    }

    /**
     * Guarda un alquiler nuevo. El id se genera automáticamente (A0001, A0002, ...).
     *
     * @return el id generado
     */
    public String guardar(Alquiler alquiler) throws IOException {
        validar(alquiler);
        alquiler.setId(generarId());
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
            pw.println(alquiler.toCSV());
        }
        return alquiler.getId();
    }

    /**
     * Reemplaza los datos del alquiler que tenga el mismo id.
     *
     * @return true si lo encontró y lo modificó
     */
    public boolean modificar(Alquiler alquiler) throws IOException {
        validar(alquiler);
        List<Alquiler> lista = listar();
        boolean encontrado = false;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(alquiler.getId())) {
                lista.set(i, alquiler);
                encontrado = true;
                break;
            }
        }
        if (encontrado) {
            reescribir(lista);
        }
        return encontrado;
    }

    /** Busca un alquiler por su id. Devuelve null si no existe. */
    public Alquiler buscar(String id) throws IOException {
        for (Alquiler alquiler : listar()) {
            if (alquiler.getId().equalsIgnoreCase(id)) {
                return alquiler;
            }
        }
        return null;
    }

    /**
     * Elimina el alquiler con ese id.
     *
     * @return true si lo encontró y lo eliminó
     */
    public boolean eliminar(String id) throws IOException {
        List<Alquiler> lista = listar();
        boolean eliminado = lista.removeIf(a -> a.getId().equalsIgnoreCase(id));
        if (eliminado) {
            reescribir(lista);
        }
        return eliminado;
    }

    /** Genera el siguiente id: toma el número más alto registrado y le suma 1. */
    public String generarId() throws IOException {
        int maximo = 0;
        for (Alquiler alquiler : listar()) {
            try {
                int numero = Integer.parseInt(alquiler.getId().substring(PREFIJO.length()));
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

    private Alquiler convertirLinea(String linea) {
        String[] p = linea.split(Alquiler.SEPARADOR, -1);
        if (p.length < 7) {
            return null; // línea incompleta: se omite
        }
        try {
            LocalDate inicio = LocalDate.parse(p[4].trim(), Alquiler.FORMATO_FECHA);
            LocalDate fin = LocalDate.parse(p[5].trim(), Alquiler.FORMATO_FECHA);
            double monto = Double.parseDouble(p[6].trim());
            return new AlquilerDepartamento(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                    inicio, fin, monto);
        } catch (DateTimeParseException | NumberFormatException e) {
            return null; // línea con datos dañados: se omite
        }
    }

    private void reescribir(List<Alquiler> lista) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println(CABECERA);
            for (Alquiler alquiler : lista) {
                pw.println(alquiler.toCSV());
            }
        }
    }

    private void validar(Alquiler alquiler) {
        validarTexto(alquiler.getIdCliente(), "El id del cliente");
        validarTexto(alquiler.getIdPropietario(), "El id del propietario");
        validarTexto(alquiler.getIdDepartamento(), "El id del departamento");
        if (alquiler.getFechaInicial() == null || alquiler.getFechaFinal() == null) {
            throw new IllegalArgumentException("Las fechas son obligatorias.");
        }
        if (alquiler.getFechaFinal().isBefore(alquiler.getFechaInicial())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        if (alquiler.getMontoPagar() < 0) {
            throw new IllegalArgumentException("El monto a pagar no puede ser negativo.");
        }
    }

    private void validarTexto(String valor, String nombre) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(nombre + " es obligatorio.");
        }
        if (valor.contains(Alquiler.SEPARADOR)) {
            throw new IllegalArgumentException(nombre + " no puede contener el carácter \";\".");
        }
    }
}
