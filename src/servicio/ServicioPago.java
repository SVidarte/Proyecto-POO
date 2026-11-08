package servicio;

import modelo.Pago;
import modelo.PlanificacionPago;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServicioPago {

    private static final String ARCHIVO = "pago.csv";
    private static final String ENCABEZADO = "id;id_planificacion_pago;monto;fecha;metodo";

    private final List<Pago> listaPagos = new ArrayList<>();
    private final ServicioPlanificacionPago servicioPlanificacion = new ServicioPlanificacionPago();

    public ServicioPago() {
        crearArchivoSiNoExiste();
        cargarDesdeArchivo();
    }

    // ---------------------------------------------------------------
    // Operaciones principales
    // ---------------------------------------------------------------

    /** Guarda un pago nuevo, le asigna su id y suma el monto a la planificación. */
    public void guardar(Pago pago) {
        validarCampos(pago);
        PlanificacionPago plan = obtenerPlanificacion(pago.getIdPlanificacionPago());

        pago.setId(generarId());
        listaPagos.add(pago);
        ajustarMontoPagado(plan, pago.getMonto());
        sincronizarArchivo();
    }

    public List<Pago> listar() {
        return new ArrayList<>(listaPagos);
    }

    /** Busca un pago por su id exacto. Devuelve null si no existe. */
    public Pago buscarPorId(String id) {
        for (Pago p : listaPagos) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    /** Busca pagos cuyo id o cuyo id_planificacion_pago coincida con el texto. */
    public List<Pago> buscar(String texto) {
        List<Pago> resultado = new ArrayList<>();
        String t = texto.trim();
        for (Pago p : listaPagos) {
            if (p.getId().equalsIgnoreCase(t) || p.getIdPlanificacionPago().equalsIgnoreCase(t)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Modifica un pago existente (el id no cambia) y corrige el monto_pagado
     * de la planificación (o planificaciones) afectada(s).
     * Devuelve false si el id no existe.
     */
    public boolean modificar(Pago nuevo) {
        validarCampos(nuevo);
        Pago viejo = buscarPorId(nuevo.getId());
        if (viejo == null) {
            return false;
        }
        PlanificacionPago planNueva = obtenerPlanificacion(nuevo.getIdPlanificacionPago());

        if (viejo.getIdPlanificacionPago().equalsIgnoreCase(nuevo.getIdPlanificacionPago())) {
            double diferencia = nuevo.getMonto() - viejo.getMonto();
            if (diferencia != 0) {
                ajustarMontoPagado(planNueva, diferencia);
            }
        } else {
            PlanificacionPago planVieja = servicioPlanificacion.buscar(viejo.getIdPlanificacionPago());
            if (planVieja != null) {
                ajustarMontoPagado(planVieja, -viejo.getMonto());
            }
            ajustarMontoPagado(planNueva, nuevo.getMonto());
        }

        int indice = listaPagos.indexOf(viejo);
        listaPagos.set(indice, nuevo);
        sincronizarArchivo();
        return true;
    }

    /** Elimina un pago y resta su monto de la planificación. Devuelve false si no existe. */
    public boolean eliminar(String id) {
        Pago pago = buscarPorId(id);
        if (pago == null) {
            return false;
        }
        PlanificacionPago plan = servicioPlanificacion.buscar(pago.getIdPlanificacionPago());
        if (plan != null) {
            ajustarMontoPagado(plan, -pago.getMonto());
        }
        listaPagos.remove(pago);
        sincronizarArchivo();
        return true;
    }

    // ---------------------------------------------------------------
    // Validaciones y apoyo
    // ---------------------------------------------------------------

    private void validarCampos(Pago pago) {
        if (pago.getIdPlanificacionPago() == null || pago.getIdPlanificacionPago().trim().isEmpty()) {
            throw new IllegalArgumentException("El id de la planificación de pago es obligatorio.");
        }
        if (pago.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        }
        if (pago.getFecha() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        if (!Pago.TRANSFERENCIA.equals(pago.getMetodo()) && !Pago.EFECTIVO.equals(pago.getMetodo())) {
            throw new IllegalArgumentException("El método de pago debe ser Transferencia o Efectivo.");
        }
    }

    private PlanificacionPago obtenerPlanificacion(String idPlanificacion) {
        PlanificacionPago plan = servicioPlanificacion.buscar(idPlanificacion.trim());
        if (plan == null) {
            throw new IllegalArgumentException(
                    "La planificación de pago \"" + idPlanificacion + "\" no existe o ya fue pagada por completo.");
        }
        return plan;
    }

    /** Suma (o resta, si delta es negativo) al monto_pagado y guarda la planificación. */
    private void ajustarMontoPagado(PlanificacionPago planAnterior, double delta) {
        // Se vuelve a leer la planificación para trabajar con su valor más reciente
        PlanificacionPago plan = servicioPlanificacion.buscar(planAnterior.getId());
        if (plan == null) {
            return;
        }
        double actual;
        try {
            actual = Double.parseDouble(plan.getMonto_pagado());
        } catch (NumberFormatException | NullPointerException e) {
            actual = 0.0;
        }
        double nuevoValor = actual + delta;
        if (nuevoValor < 0) {
            nuevoValor = 0;
        }
        plan.setMonto_pagado(String.format(Locale.US, "%.2f", nuevoValor));
        servicioPlanificacion.modificar(plan);
    }

    /** Toma el número más alto existente y suma 1: PA0001, PA0002, ... */
    private String generarId() {
        int mayor = 0;
        for (Pago p : listaPagos) {
            try {
                int numero = Integer.parseInt(p.getId().substring(2));
                if (numero > mayor) {
                    mayor = numero;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // id con formato inesperado: se ignora
            }
        }
        return String.format("PA%04d", mayor + 1);
    }

    // ---------------------------------------------------------------
    // Archivo CSV
    // ---------------------------------------------------------------

    private void crearArchivoSiNoExiste() {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.println(ENCABEZADO);
            } catch (IOException e) {
                System.err.println("Error al crear " + ARCHIVO + ": " + e.getMessage());
            }
        }
    }

    private void cargarDesdeArchivo() {
        listaPagos.clear();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length != 5 || partes[0].equalsIgnoreCase("id")) {
                    continue; // salta el encabezado y líneas incompletas
                }
                try {
                    Pago p = new Pago(
                            partes[0],
                            partes[1],
                            Double.parseDouble(partes[2]),
                            LocalDate.parse(partes[3], Pago.FORMATO_FECHA),
                            partes[4]
                    );
                    listaPagos.add(p);
                } catch (NumberFormatException | DateTimeParseException e) {
                    System.err.println("Línea ignorada en " + ARCHIVO + ": " + linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ARCHIVO + ": " + e.getMessage());
        }
    }

    private void sincronizarArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO))) {
            pw.println(ENCABEZADO);
            for (Pago p : listaPagos) {
                pw.println(p.getId() + ";"
                        + p.getIdPlanificacionPago() + ";"
                        + String.format(Locale.US, "%.2f", p.getMonto()) + ";"
                        + p.getFecha().format(Pago.FORMATO_FECHA) + ";"
                        + p.getMetodo());
            }
        } catch (IOException e) {
            System.err.println("Error al escribir " + ARCHIVO + ": " + e.getMessage());
        }
    }
}
