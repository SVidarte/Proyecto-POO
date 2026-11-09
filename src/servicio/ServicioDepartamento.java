package servicio;

import modelo.Alquiler;
import modelo.Departamento;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Administra los departamentos guardados en departamentos.csv (datos separados por ";").
 * El id (DP0001, DP0002, ...) lo genera este servicio, y cada departamento se asigna a un
 * propietario existente (ServicioPropietario).
 */
public class ServicioDepartamento {
    private static final String ARCHIVO = "departamentos.csv";
    private static final String CABECERA =
            "id_departamento;id_propietario;piso;numero_departamento;direccion;precio_mensual;estado";
    private static final String PREFIJO = "DP";

    public ServicioDepartamento() {
        crearArchivoSiNoExiste();
    }

    /**
     * Guarda un departamento nuevo: le asigna el siguiente id y lo vincula a su propietario.
     *
     * @return el id generado (por ejemplo DP0001)
     * @throws IllegalArgumentException si el propietario no existe
     */
    public String agregarDepartamento(Departamento d) {
        validarPropietario(d.getId_propietario());
        List<Departamento> lista = listarDepartamento();
        d.setId_departamento(generarId(lista));
        lista.add(d);
        sincronizarArchivo(lista);
        return d.getId_departamento();
    }

    /** @return true si encontró el departamento y lo eliminó */
    public boolean eliminarDepartamento(String id) {
        try {
            for (Alquiler a : new ServicioAlquiler().listar()) {
                if (a.getIdDepartamento().equalsIgnoreCase(id)) {
                    throw new IllegalArgumentException(
                            "El departamento tiene alquileres registrados. Elimine primero esos alquileres.");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer alquiler.csv: " + e.getMessage());
        }
        List<Departamento> lista = listarDepartamento();
        boolean eliminado = lista.removeIf(d -> d.getId_departamento().equalsIgnoreCase(id));
        if (eliminado) {
            sincronizarArchivo(lista);
        }
        return eliminado;
    }

    public List<Departamento> listarDepartamento() {
        List<Departamento> lista = new ArrayList<>();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            return lista;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("id_departamento;")) {
                    continue; // línea vacía o cabecera
                }
                String[] datos = linea.split(";", -1);
                if (datos.length < 7) {
                    continue; // línea incompleta
                }
                try {
                    lista.add(new Departamento(datos[0], datos[1], Integer.parseInt(datos[2].trim()),
                            datos[3], datos[4], Double.parseDouble(datos[5].trim()), datos[6]));
                } catch (IllegalArgumentException e) { // incluye NumberFormatException
                    System.err.println("Línea ignorada en " + ARCHIVO + ": " + linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ARCHIVO + ": " + e.getMessage());
        }
        return lista;
    }

    public Departamento buscarPorId(String id) {
        for (Departamento d : listarDepartamento()) {
            if (d.getId_departamento().equalsIgnoreCase(id)) {
                return d;
            }
        }
        return null;
    }

    public List<Departamento> buscarPorDireccion(String direccion) {
        List<Departamento> resultado = new ArrayList<>();
        String buscada = direccion.replaceAll("\\s+", "");
        for (Departamento d : listarDepartamento()) {
            if (d.getDireccion().replaceAll("\\s+", "").equalsIgnoreCase(buscada)) {
                resultado.add(d);
            }
        }
        return resultado;
    }

    /**
     * Reemplaza los datos del departamento que tenga el mismo id.
     *
     * @return true si lo encontró y lo modificó
     * @throws IllegalArgumentException si el propietario no existe
     */
    public boolean modificarDepartamento(Departamento d) {
        validarPropietario(d.getId_propietario());
        List<Departamento> lista = listarDepartamento();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId_departamento().equalsIgnoreCase(d.getId_departamento())) {
                lista.set(i, d);
                sincronizarArchivo(lista);
                return true;
            }
        }
        return false;
    }

    /** Departamentos que pertenecen a un propietario. */
    public List<Departamento> listarPorPropietario(String idPropietario) {
        List<Departamento> resultado = new ArrayList<>();
        for (Departamento d : listarDepartamento()) {
            if (d.getId_propietario().equalsIgnoreCase(idPropietario)) {
                resultado.add(d);
            }
        }
        return resultado;
    }

    /** Elimina todos los departamentos de un propietario (se usa al eliminar al propietario). @return cuántos eliminó */
    public int eliminarPorPropietario(String idPropietario) {
        List<Departamento> lista = listarDepartamento();
        int antes = lista.size();
        lista.removeIf(d -> d.getId_propietario().equalsIgnoreCase(idPropietario));
        int eliminados = antes - lista.size();
        if (eliminados > 0) {
            sincronizarArchivo(lista);
        }
        return eliminados;
    }

    /** Cambia el estado (Disponible u Ocupado) de un departamento. @return true si lo encontró */
    public boolean cambiarEstado(String id, String estado) {
        List<Departamento> lista = listarDepartamento();
        for (Departamento d : lista) {
            if (d.getId_departamento().equalsIgnoreCase(id)) {
                d.setEstado(estado);
                sincronizarArchivo(lista);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Métodos privados
    // ---------------------------------------------------------------------

    /** Toma el número más alto registrado y le suma 1: DP0001, DP0002, ... */
    private String generarId(List<Departamento> lista) {
        int mayor = 0;
        for (Departamento d : lista) {
            try {
                int numero = Integer.parseInt(d.getId_departamento().substring(PREFIJO.length()));
                if (numero > mayor) {
                    mayor = numero;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // id con formato raro: se ignora
            }
        }
        mayor = Math.max(mayor, UtilIds.mayorNumero("alquiler.csv", 3, PREFIJO)); // no reutilizar ids de alquileres existentes
        return String.format("%s%04d", PREFIJO, mayor + 1);
    }

    /** El propietario debe existir en propietarios.csv (se vuelve a leer el archivo para tener datos al día). */
    private void validarPropietario(String idPropietario) {
        if (new ServicioPropietario().buscar(idPropietario) == null) {
            throw new IllegalArgumentException("El propietario " + idPropietario + " no existe.");
        }
    }

    private void crearArchivoSiNoExiste() {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.println(CABECERA);
            } catch (IOException e) {
                throw new IllegalStateException("Error al crear " + ARCHIVO + ": " + e.getMessage());
            }
        }
    }

    private void sincronizarArchivo(List<Departamento> lista) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO))) {
            pw.println(CABECERA);
            for (Departamento d : lista) {
                pw.println(d.getId_departamento() + ";"
                        + d.getId_propietario() + ";"
                        + d.getPiso() + ";"
                        + d.getNumero_departamento() + ";"
                        + d.getDireccion() + ";"
                        + String.format(Locale.US, "%.2f", d.getPrecio_mensual()) + ";"
                        + d.getEstado());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error al guardar en " + ARCHIVO + ": " + e.getMessage());
        }
    }
}


    