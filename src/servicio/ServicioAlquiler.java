package servicio;

import modelo.DatosInvalidosException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            "id;idCliente;idPropietario;idDepartamento;fechaInicial;fechaFinal;montoPagar;estado";
    private static final String PREFIJO = "A";
    // Un alquiler eliminado no se borra del archivo: queda con estado Eliminado, así su id nunca se reutiliza.
    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_ELIMINADO = "Eliminado";
    private static final int COLUMNA_ESTADO = 7;
    // archivo que se usaba antes para guardar los ids eliminados (ya no se usa)
    private static final String ARCHIVO_ELIMINADOS_ANTERIOR = "alquileres_eliminados.csv";

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
        importarEliminadosAnteriores();
    }

    /** Lee el archivo y devuelve los alquileres activos (los eliminados no se incluyen). */
    public List<Alquiler> listar() throws IOException {
        List<Alquiler> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("id" + Alquiler.SEPARADOR)) {
                    continue; // línea vacía o cabecera
                }
                if (esEliminada(linea)) {
                    continue; // alquiler eliminado: solo se conserva su id
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
            pw.println(alquiler.toCSV() + Alquiler.SEPARADOR + ESTADO_ACTIVO);
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
     * Disponible. Queda en alquiler.csv con estado Eliminado para
     * que su id nunca vuelva a usarse, y sus cuotas pendientes se eliminan de planificaciones_pago.csv
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
        List<String> eliminados = lineasEliminadas();
        eliminados.add(objetivo.toCSV() + Alquiler.SEPARADOR + ESTADO_ELIMINADO); // queda registrado con estado Eliminado
        escribirTodo(lista, eliminados);
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
            throw new DatosInvalidosException("El alquiler " + idAlquiler + " no existe.");
        }
        if (alquiler.getIdCliente() != null && !alquiler.getIdCliente().trim().isEmpty()) {
            throw new DatosInvalidosException("Este alquiler ya fue aceptado por otro cliente.");
        }
        ServicioDepartamento servicioDepartamento = new ServicioDepartamento();
        Departamento departamento = servicioDepartamento.buscarPorId(alquiler.getIdDepartamento());
        if (departamento == null || !Departamento.DISPONIBLE.equals(departamento.getEstado())) {
            throw new DatosInvalidosException("El departamento de este alquiler ya no está disponible.");
        }
        if (new ServicioCliente().buscar(idCliente) == null) {
            throw new DatosInvalidosException("El cliente " + idCliente + " no existe.");
        }

        alquiler.setIdCliente(idCliente);
        modificar(alquiler);
        try {
            int cuotas = new ServicioPlanificacionPago().generarCuotas(alquiler.getId()).size();
            servicioDepartamento.alternarEstado(departamento.getId_departamento()); // Disponible pasa a Ocupado
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
     * Cuenta los alquileres activos y los eliminados de alquiler.csv, y los que todavía aparecen en los
     * archivos de cuotas, así un id eliminado no vuelve a usarse.
     */
    public String generarId() throws IOException {
        int maximo = UtilIds.mayorNumero(archivo.getPath(), 0, PREFIJO);
        maximo = Math.max(maximo, UtilIds.mayorNumero(modelo.PlanificacionPago.ARCHIVO, 1, PREFIJO));
        maximo = Math.max(maximo, UtilIds.mayorNumero(modelo.PlanificacionPago.ARCHIVO_REALIZADOS, 1, PREFIJO));
        return String.format("%s%04d", PREFIJO, maximo + 1);
    }

    // ---------------------------------------------------------------------
    // Métodos privados
    // ---------------------------------------------------------------------

    /** true si el id corresponde a un alquiler eliminado (fila con estado Eliminado en alquiler.csv). */
    public static boolean estaEliminado(String id) {
        File f = new File(ARCHIVO);
        if (id == null || !f.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] p = linea.split(Alquiler.SEPARADOR, -1);
                if (p.length > COLUMNA_ESTADO && p[0].trim().equalsIgnoreCase(id.trim()) && esEliminada(linea)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ARCHIVO + ": " + e.getMessage());
        }
        return false;
    }

    /** Si el departamento ya no tiene otro alquiler con cliente, vuelve a quedar Disponible. */
    private void liberarDepartamento(Alquiler eliminado, List<Alquiler> restantes) {
        for (Alquiler a : restantes) {
            if (a.getIdDepartamento().equalsIgnoreCase(eliminado.getIdDepartamento())
                    && a.getIdCliente() != null && !a.getIdCliente().trim().isEmpty()) {
                return; // el departamento sigue alquilado por otro alquiler con cliente
            }
        }
        ServicioDepartamento servicioDepartamento = new ServicioDepartamento();
        Departamento departamento = servicioDepartamento.buscarPorId(eliminado.getIdDepartamento());
        if (departamento != null && Departamento.OCUPADO.equals(departamento.getEstado())) {
            servicioDepartamento.alternarEstado(departamento.getId_departamento()); // Ocupado pasa a Disponible
        }
    }

    private static boolean esEliminada(String linea) {
        String[] p = linea.split(Alquiler.SEPARADOR, -1);
        return p.length > COLUMNA_ESTADO && ESTADO_ELIMINADO.equalsIgnoreCase(p[COLUMNA_ESTADO].trim());
    }

    /** Líneas de alquileres eliminados tal como están en el archivo. */
    private List<String> lineasEliminadas() throws IOException {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.trim().isEmpty() && esEliminada(linea)) {
                    lineas.add(linea);
                }
            }
        }
        return lineas;
    }

    /**
     * Antes los ids eliminados se guardaban en alquileres_eliminados.csv. Si ese archivo todavía existe,
     * sus ids se pasan a alquiler.csv como filas con estado Eliminado (solo la primera vez; luego se puede borrar).
     */
    private void importarEliminadosAnteriores() throws IOException {
        File anterior = new File(ARCHIVO_ELIMINADOS_ANTERIOR);
        if (!anterior.exists()) {
            return;
        }
        Set<String> conocidos = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                conocidos.add(linea.split(Alquiler.SEPARADOR, -1)[0].trim().toUpperCase());
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(anterior));
                PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
            String id;
            while ((id = br.readLine()) != null) {
                id = id.trim();
                if (!id.isEmpty() && conocidos.add(id.toUpperCase())) {
                    pw.println(id + ";;;;;;;" + ESTADO_ELIMINADO);
                }
            }
        }
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

    /** Guarda la lista de alquileres activos conservando las filas de los eliminados. */
    private void reescribir(List<Alquiler> lista) throws IOException {
        escribirTodo(lista, lineasEliminadas());
    }

    private void escribirTodo(List<Alquiler> activos, List<String> eliminados) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println(CABECERA);
            for (Alquiler alquiler : activos) {
                pw.println(alquiler.toCSV() + Alquiler.SEPARADOR + ESTADO_ACTIVO);
            }
            for (String linea : eliminados) {
                pw.println(linea);
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
            throw new DatosInvalidosException("Las fechas son obligatorias.");
        }
        if (alquiler.getFechaFinal().isBefore(alquiler.getFechaInicial())) {
            throw new DatosInvalidosException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        if (alquiler.getMontoPagar() < 0) {
            throw new DatosInvalidosException("El monto a pagar no puede ser negativo.");
        }
    }

    private void validarTexto(String valor, String nombre) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new DatosInvalidosException(nombre + " es obligatorio.");
        }
        if (valor.contains(Alquiler.SEPARADOR)) {
            throw new DatosInvalidosException(nombre + " no puede contener el carácter \";\".");
        }
    }
}
