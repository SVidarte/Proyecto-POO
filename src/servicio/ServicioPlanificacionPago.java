package servicio;

import modelo.Alquiler;
import modelo.PlanificacionPago;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ServicioPlanificacionPago {
    private final String ARCHIVO = PlanificacionPago.ARCHIVO;
    private final String ARCHIVO_REALIZADOS = PlanificacionPago.ARCHIVO_REALIZADOS;
    private static final String PREFIJO = "PL";
    private List<PlanificacionPago> listaPlanificaciones;

    public ServicioPlanificacionPago() {
        PlanificacionPago.crearArchivoSiNoExiste();
        listaPlanificaciones = new ArrayList<>();
        cargarDesdeArchivo();
    }

    // Nota: cada operación vuelve a leer el CSV primero, porque otros servicios
    // (por ejemplo ServicioPago) también modifican monto_pagado en el mismo archivo.

    public void guardar(PlanificacionPago planificacion) {
        cargarDesdeArchivo();
        if (planificacion.getId() == null || planificacion.getId().isEmpty()) {
            planificacion.setId(generarIdAutoIncremental());
        }
        listaPlanificaciones.add(planificacion);
        sincronizarArchivo();
    }

    /**
     * Crea las cuotas mensuales de un alquiler: la primera en fechaInicial y luego una por mes
     * mientras la fecha no pase de fechaFinal. Cada cuota recibe el montoPagar del alquiler
     * (renta mensual) y empieza con monto_pagado en 0.00.
     *
     * @return las cuotas creadas
     * @throws IllegalArgumentException si el alquiler no existe o ya tiene cuotas
     */
    public List<PlanificacionPago> generarCuotas(String idAlquiler) throws IOException {
        if (idAlquiler == null || idAlquiler.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese el id del alquiler.");
        }
        cargarDesdeArchivo();

        Alquiler alquiler = new ServicioAlquiler().buscar(idAlquiler.trim());
        if (alquiler == null) {
            throw new IllegalArgumentException("El alquiler " + idAlquiler.trim() + " no existe.");
        }
        // Se revisan las cuotas pendientes y también las ya pagadas
        List<PlanificacionPago> existentes = new ArrayList<>(listaPlanificaciones);
        existentes.addAll(leerCuotas(ARCHIVO_REALIZADOS));
        for (PlanificacionPago p : existentes) {
            if (p.getIdAlquiler().equalsIgnoreCase(alquiler.getId())) {
                throw new IllegalArgumentException(
                        "El alquiler " + alquiler.getId() + " ya tiene cuotas generadas.");
            }
        }

        LocalDate inicio = alquiler.getFechaInicial();
        LocalDate fin = alquiler.getFechaFinal();
        String monto = String.format(Locale.US, "%.2f", alquiler.getMontoPagar());

        List<PlanificacionPago> nuevas = new ArrayList<>();
        int mes = 0;
        LocalDate fecha = inicio;
        while (!fecha.isAfter(fin)) {
            PlanificacionPago cuota = new PlanificacionPago(
                    generarIdAutoIncremental(),
                    alquiler.getId(),
                    fecha.format(Alquiler.FORMATO_FECHA),
                    monto,
                    "0.00");
            listaPlanificaciones.add(cuota);
            nuevas.add(cuota);
            mes++;
            fecha = inicio.plusMonths(mes); // siempre desde la fecha inicial: 31/01 -> 28/02 -> 31/03
        }
        sincronizarArchivo();
        return nuevas;
    }

    /** Cuotas pendientes de todos los alquileres. */
    public List<PlanificacionPago> listar() {
        cargarDesdeArchivo();
        return new ArrayList<>(listaPlanificaciones);
    }

    /** Cuotas pendientes de un alquiler. */
    public List<PlanificacionPago> listarPorAlquiler(String idAlquiler) {
        cargarDesdeArchivo();
        List<PlanificacionPago> resultado = new ArrayList<>();
        if (!alquilerExiste(idAlquiler)) {
            return resultado; // alquiler eliminado: no se muestra ninguna cuota
        }
        for (PlanificacionPago p : listaPlanificaciones) {
            if (p.getIdAlquiler().equalsIgnoreCase(idAlquiler)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Cuotas ya pagadas por completo de un alquiler (archivo planificacion_pagos_realizados.csv).
     * También funciona con alquileres eliminados: sus cuotas pagadas se conservan.
     */
    public List<PlanificacionPago> listarPagadasPorAlquiler(String idAlquiler) {
        cargarDesdeArchivo(); // por si hay cuotas saldadas que todavía no se han movido
        List<PlanificacionPago> resultado = new ArrayList<>();
        for (PlanificacionPago p : leerCuotas(ARCHIVO_REALIZADOS)) {
            if (p.getIdAlquiler().equalsIgnoreCase(idAlquiler)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public PlanificacionPago buscar(String id) {
        cargarDesdeArchivo();
        for (PlanificacionPago p : listaPlanificaciones) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    public boolean modificar(PlanificacionPago planificacionModificada) {
        cargarDesdeArchivo();
        for (int i = 0; i < listaPlanificaciones.size(); i++) {
            if (listaPlanificaciones.get(i).getId().equalsIgnoreCase(planificacionModificada.getId())) {
                if (estaSaldada(planificacionModificada)) {
                    // Verificador true: sale de planificaciones_pago.csv y pasa a planificacion_pagos_realizados.csv
                    listaPlanificaciones.remove(i);
                    archivarPagadas(Collections.singletonList(planificacionModificada));
                } else {
                    listaPlanificaciones.set(i, planificacionModificada);
                }
                sincronizarArchivo();
                return true;
            }
        }
        return false;
    }

    /**
     * Elimina las cuotas PENDIENTES de un alquiler (se usa cuando se elimina el alquiler).
     * No pasan a planificacion_pagos_realizados.csv, y las cuotas que ese alquiler ya había
     * pagado se conservan en ese archivo.
     *
     * @return cantidad de cuotas pendientes eliminadas
     */
    public int eliminarPorAlquiler(String idAlquiler) {
        cargarDesdeArchivo();
        int antes = listaPlanificaciones.size();
        listaPlanificaciones.removeIf(p -> p.getIdAlquiler().equalsIgnoreCase(idAlquiler));
        int eliminadas = antes - listaPlanificaciones.size();
        if (eliminadas > 0) {
            sincronizarArchivo();
        }
        return eliminadas;
    }

    /**
     * true si el alquiler existió y ya fue eliminado: figura en alquileres_eliminados.csv
     * (o, para eliminaciones anteriores a ese archivo, todavía conserva cuotas pagadas).
     */
    public boolean alquilerEliminado(String idAlquiler) {
        if (idAlquiler == null || alquilerExiste(idAlquiler)) {
            return false;
        }
        String id = idAlquiler.trim();
        if (ServicioAlquiler.estaEliminado(id)) {
            return true;
        }
        for (PlanificacionPago p : leerCuotas(ARCHIVO_REALIZADOS)) {
            if (p.getIdAlquiler().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /** true si el alquiler todavía existe en alquiler.csv (un alquiler eliminado ya no existe). */
    public boolean alquilerExiste(String idAlquiler) {
        try {
            return new ServicioAlquiler().buscar(idAlquiler.trim()) != null;
        } catch (IOException e) {
            System.err.println("Error al leer alquiler.csv: " + e.getMessage());
            return true; // si no se puede comprobar, no se ocultan datos
        }
    }

    /**
     * Toma el número más alto usado y suma 1: PL0001, PL0002, ...
     * Se revisan también pago.csv y planificacion_pagos_realizados.csv para no reutilizar
     * nunca el id de una cuota que ya salió de planificaciones_pago.csv.
     */
    private String generarIdAutoIncremental() {
        int mayor = 0;
        for (PlanificacionPago p : listaPlanificaciones) {
            mayor = Math.max(mayor, numeroDeId(p.getId()));
        }
        mayor = Math.max(mayor, mayorNumeroEnArchivo("pago.csv", 1));
        mayor = Math.max(mayor, mayorNumeroEnArchivo(ARCHIVO_REALIZADOS, 0));
        return String.format("%s%04d", PREFIJO, mayor + 1);
    }

    private int numeroDeId(String id) {
        try {
            return Integer.parseInt(id.substring(PREFIJO.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
            return 0; // encabezado o id con formato inesperado: se ignora
        }
    }

    private int mayorNumeroEnArchivo(String ruta, int columna) {
        int mayor = 0;
        File archivo = new File(ruta);
        if (!archivo.exists()) return 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length > columna) {
                    mayor = Math.max(mayor, numeroDeId(partes[columna]));
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ruta + ": " + e.getMessage());
        }
        return mayor;
    }

    /** Saldada = verificador true con algo realmente pagado (una cuota de monto 0 no cuenta). */
    private boolean estaSaldada(PlanificacionPago p) {
        try {
            return p.isVerificador() && Double.parseDouble(p.getMonto_pagado()) > 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    private void cargarDesdeArchivo() {
        listaPlanificaciones.clear();
        listaPlanificaciones.addAll(leerCuotas(ARCHIVO));

        // Regla: una cuota con verificador true (pagada por completo) sale de este CSV y se guarda en
        // planificacion_pagos_realizados.csv. Se aplica aquí para que también limpie cuotas
        // saldadas antes o editadas a mano.
        List<PlanificacionPago> saldadas = new ArrayList<>();
        Iterator<PlanificacionPago> it = listaPlanificaciones.iterator();
        while (it.hasNext()) {
            PlanificacionPago p = it.next();
            if (estaSaldada(p)) {
                saldadas.add(p);
                it.remove();
            }
        }
        if (!saldadas.isEmpty()) {
            archivarPagadas(saldadas);
            sincronizarArchivo();
        }
    }

    /** Lee un CSV de cuotas (5 columnas separadas por ";"). Si no existe, devuelve una lista vacía. */
    private List<PlanificacionPago> leerCuotas(String ruta) {
        List<PlanificacionPago> lista = new ArrayList<>();
        File archivo = new File(ruta);
        if (!archivo.exists()) return lista;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 5) {
                    lista.add(new PlanificacionPago(
                        partes[0], partes[1], partes[2], partes[3], partes[4]
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ruta + ": " + e.getMessage());
        }
        return lista;
    }

    /** Agrega cuotas pagadas a planificacion_pagos_realizados.csv (sin duplicar ids). */
    private void archivarPagadas(List<PlanificacionPago> pagadas) {
        PlanificacionPago.crearArchivoSiNoExiste();
        Set<String> yaArchivadas = new HashSet<>();
        for (PlanificacionPago p : leerCuotas(ARCHIVO_REALIZADOS)) {
            yaArchivadas.add(p.getId().toUpperCase());
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_REALIZADOS, true))) {
            for (PlanificacionPago p : pagadas) {
                if (yaArchivadas.add(p.getId().toUpperCase())) {
                    pw.println(aLinea(p));
                }
            }
        } catch (IOException e) {
            System.err.println("Error al escribir " + ARCHIVO_REALIZADOS + ": " + e.getMessage());
        }
    }

    private void sincronizarArchivo() {
        escribirCuotas(ARCHIVO, listaPlanificaciones);
    }

    private void escribirCuotas(String ruta, List<PlanificacionPago> cuotas) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta))) {
            for (PlanificacionPago p : cuotas) {
                pw.println(aLinea(p));
            }
        } catch (IOException e) {
            System.err.println("Error al escribir " + ruta + ": " + e.getMessage());
        }
    }

    private String aLinea(PlanificacionPago p) {
        return p.getId() + ";"
                + p.getIdAlquiler() + ";"
                + p.getFechaPago() + ";"
                + p.getMonto() + ";"
                + p.getMonto_pagado();
    }
}