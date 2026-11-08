package modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Pago {

    // Formato de fecha del CSV y de la vista. Acepta 7/11/2026 o 07/11/2026 al leer,
    // siempre escribe 07/11/2026, y rechaza fechas inexistentes (31/02/2026).
    public static final DateTimeFormatter FORMATO_FECHA = Alquiler.FORMATO_FECHA;

    public static final String TRANSFERENCIA = "T";
    public static final String EFECTIVO = "E";

    private String id;
    private String idPlanificacionPago;
    private double monto;
    private LocalDate fecha;
    private String metodo; // "T" = Transferencia, "E" = Efectivo

    public Pago() {}

    public Pago(String id, String idPlanificacionPago, double monto, LocalDate fecha, String metodo) {
        this.id = id;
        this.idPlanificacionPago = idPlanificacionPago;
        this.monto = monto;
        this.fecha = fecha;
        this.metodo = metodo;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdPlanificacionPago() { return idPlanificacionPago; }
    public void setIdPlanificacionPago(String idPlanificacionPago) { this.idPlanificacionPago = idPlanificacionPago; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
}
