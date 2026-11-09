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
import modelo.Departamento;

/**
 * Servicio que administra los alquileres guardados en el archivo alquiler.csv.
 * Cada dato se separa con ";".
 */
public class ServicioAlquiler {

    private static final String ARCHIVO = "alquiler.csv";
    private static final String CABECERA =
            "id;idCliente;idPropietario;idDepartamento;fechaInicial;fechaFinal;montoPagar";
    private static final String PREFIJO = "A";
    private static final String ARCHIVO_ELIMINADOS = "alquileres_eliminados.csv";

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
     * Elimina el alquiler con ese id. Si el departamento ya no tiene otro alquiler con cliente, vuelve a quedar
     * Disponible. Su id queda registrado en alquileres_eliminados.csv para
     * que nunca vuelva a usarse, y sus cuotas pendientes se eliminan de planificaciones_pago.csv
     * (las cuotas que ya estaban pagadas se conservan en planificacion_pagos_realizados.csv).
     *
     * @return true si lo encontró y lo eliminó
     */
    public boolean eliminar(String id) throws IOException {
        List<Alquiler> lista = listar();
        Alquiler objetivo = null;
        for (Alquiler a : lista) {
            if (a.getId().equalsIgnoreCase(id)) {
                objetivo = a;
                break;
            }
        }
        if (objetivo == null) {
            return false;
        }
        lista.remove(objetivo);
        reescribir(lista);
        registrarIdEliminado(objetivo.getId());
        liberarDepartamento(objetivo, lista);
        new ServicioPlanificacionPago().eliminarPorAlquiler(objetivo.getId());
        return true;
    }

    // ---------------------------------------------------------------------
    // Consultas por propietario y cliente
    // ---------------------------------------------------------------------

    public List<Alquiler> listarPorPropietario(String idPropietario) throws IOException {
        List<Alquiler> resultado = new ArrayList<>();
        for (Alquiler a : listar()) {
            if (a.getIdPropietario().equalsIgnoreCase(idPropietario)) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    public List<Alquiler> listarPorCliente(String idCliente) throws IOException {
        List<Alquiler> resultado = new ArrayList<>();
        for (Alquiler a : listar()) {
            if (a.getIdCliente() != null && a.getIdCliente().equalsIgnoreCase(idCliente)) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    /** Alquileres sin cliente cuyo departamento está Disponible: los que un cliente puede aceptar. */
    public List<Alquiler> listarDisponibles() throws IOException {
        List<Departamento> departamentos = new ServicioDepartamento().listarDepartamento();
        List<Alquiler> resultado = new ArrayList<>();
        for (Alquiler a : listar()) {
            if (a.getIdCliente() != null && !a.getIdCliente().trim().isEmpty()) {
                continue;
            }
            for (Departamento d : departamentos) {
                if (d.getId_departamento().equalsIgnoreCase(a.getIdDepartamento())
                        && Departamento.DISPONIBLE.equals(d.getEstado())) {
                    resultado.add(a);
                    break;
                }
            }
        }
        return resultado;
    }

    /**
     * Un cliente acepta un alquiler: el alquiler queda a su nombre, el departamento pasa a Ocupado y se
     * generan las cuotas mensuales.
     *
     * @return cantidad de cuotas generadas
     * @throws IllegalArgumentException si el alquiler ya no está disponible
     */
    public int aceptarAlquiler(String idAlquiler, String idCliente) throws IOException {
        Alquiler alquiler = buscar(idAlquiler);
        if (alquiler == null) {
            throw new IllegalArgumentException("El alquiler " + idAlquiler + " no existe.");
        }
        if (alquiler.getIdCliente() != null && !alquiler.getIdCliente().trim().isEmpty()) {
            throw new IllegalArgumentException("Este alquiler ya fue aceptado por otro cliente.");
        }
        ServicioDepartamento servicioDepartamento = new ServicioDepartamento();
        Departamento departamento = servicioDepartamento.buscarPorId(alquiler.getIdDepartamento());
        if (departamento == null || !Departamento.DISPONIBLE.equals(departamento.getEstado())) {
            throw new IllegalArgumentException("El departamento de este alquiler ya no está disponible.");
        }
        if (new ServicioCliente().buscar(idCliente) == null) {
            throw new IllegalArgumentException("El cliente " + idCliente + " no existe.");
        }

        alquiler.setIdCliente(idCliente);
        modificar(alquiler);
        try {
            int cuotas = new ServicioPlanificacionPago().generarCuotas(alquiler.getId()).size();
            servicioDepartamento.cambiarEstado(departamento.getId_departamento(), Departamento.OCUPADO);
            return cuotas;
        } catch (RuntimeException e) {
            alquiler.setIdCliente(""); // si fallan las cuotas, el alquiler queda como estaba
            modificar(alquiler);
            throw e;
        }
    }

    // ---------------------------------------------------------------------
    // Alquileres pagados por completo
    // ---------------------------------------------------------------------

    /** true si ya tiene cuotas pagadas y no le queda ninguna pendiente. */
    public boolean estaTotalmentePagado(String idAlquiler) {
        ServicioPlanificacionPago cuotas = new ServicioPlanificacionPago();
        return !cuotas.tieneCuotasPendientes(idAlquiler) && cuotas.tieneCuotasPagadas(idAlquiler);
    }

    /**
     * Un alquiler con todas sus cuotas pagadas no se elimina (ni en cascada al eliminar un cliente o un
     * propietario) hasta que llegue el día de su fecha final.
     */
    public boolean estaProtegido(Alquiler alquiler) {
        return alquiler.getFechaFinal().isAfter(LocalDate.now()) && estaTotalmentePagado(alquiler.getId());
    }

    /**
     * Elimina los alquileres totalmente pagados cuya fecha final ya llegó (hoy o antes).
     *
     * @return cuántos eliminó
     */
    public int limpiarFinalizados() throws IOException {
        int eliminados = 0;
        LocalDate hoy = LocalDate.now();
        for (Alquiler a : listar()) {
            if (!a.getFechaFinal().isAfter(hoy) && estaTotalmentePagado(a.getId())) {
                if (eliminar(a.getId())) {
                    eliminados++;
                }
            }
        }
        return eliminados;
    }

    /**
     * Genera el siguiente id: toma el número más alto que haya existido y le suma 1.
     * Cuenta los alquileres actuales, los eliminados (alquileres_eliminados.csv) y los alquileres
     * que todavía aparecen en archivos de cuotas, así un id eliminado no vuelve a usarse.
     */
    public String generarId() throws IOException {
        int maximo = 0;
        for (Alquiler alquiler : listar()) {
            maximo = Math.max(maximo, numeroDeId(alquiler.getId()));
        }
        maximo = Math.max(maximo, mayorNumeroEnArchivo(ARCHIVO_ELIMINADOS, 0));
        maximo = Math.max(maximo, mayorNumeroEnArchivo(modelo.PlanificacionPago.ARCHIVO, 1));
        maximo = Math.max(maximo, mayorNumeroEnArchivo(modelo.PlanificacionPago.ARCHIVO_REALIZADOS, 1));
        return String.format("%s%04d", PREFIJO, maximo + 1);
    }

    // ---------------------------------------------------------------------
    // Métodos privados
    // ---------------------------------------------------------------------

    /** true si el id aparece en alquileres_eliminados.csv (el alquiler existió y fue eliminado). */
    public static boolean estaEliminado(String id) {
        File f = new File(ARCHIVO_ELIMINADOS);
        if (id == null || !f.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().equalsIgnoreCase(id.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ARCHIVO_ELIMINADOS + ": " + e.getMessage());
        }
        return false;
    }

    private void liberarDepartamento(Alquiler eliminado, List<Alquiler> restantes) {
        for (Alquiler a : restantes) {
            if (a.getIdDepartamento().equalsIgnoreCase(eliminado.getIdDepartamento())
                    && a.getIdCliente() != null && !a.getIdCliente().trim().isEmpty()) {
                return; // el departamento sigue alquilado por otro alquiler con cliente
            }
        }
        new ServicioDepartamento().cambiarEstado(eliminado.getIdDepartamento(), Departamento.DISPONIBLE);
    }

    private void registrarIdEliminado(String id) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_ELIMINADOS, true))) {
            pw.println(id);
        }
    }

    private int numeroDeId(String id) {
        try {
            return Integer.parseInt(id.substring(PREFIJO.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
            return 0; // id con formato raro: se ignora
        }
    }

    /** Mayor número de id de alquiler que aparece en la columna indicada de un CSV (0 si no existe). */
    private int mayorNumeroEnArchivo(String ruta, int columna) {
        int mayor = 0;
        File f = new File(ruta);
        if (!f.exists()) {
            return 0;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(Alquiler.SEPARADOR);
                if (partes.length > columna) {
                    mayor = Math.max(mayor, numeroDeId(partes[columna].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ruta + ": " + e.getMessage());
        }
        return mayor;
    }

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
        if (alquiler.getIdCliente() == null) {
            alquiler.setIdCliente(""); // alquiler sin cliente: lo acepta un cliente más adelante
        } else if (!alquiler.getIdCliente().trim().isEmpty()) {
            validarTexto(alquiler.getIdCliente(), "El id del cliente");
        }
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
