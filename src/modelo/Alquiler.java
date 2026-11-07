package modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Clase abstracta que representa un alquiler de departamento.
 */
public abstract class Alquiler {

    /** Separador de campos usado en los archivos CSV. */
    public static final String SEPARADOR = ";";

    /** Formato de fecha dd/MM/yyyy (estricto: rechaza fechas como 31/02/2026). */
    public static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private String id;
    private String idCliente;
    private String idPropietario;
    private String idDepartamento;
    private LocalDate fechaInicial;
    private LocalDate fechaFinal;
    private double montoPagar;

    public Alquiler(String id, String idCliente, String idPropietario, String idDepartamento,
                    LocalDate fechaInicial, LocalDate fechaFinal, double montoPagar) {
        this.id = id;
        this.idCliente = idCliente;
        this.idPropietario = idPropietario;
        this.idDepartamento = idDepartamento;
        this.fechaInicial = fechaInicial;
        this.fechaFinal = fechaFinal;
        this.montoPagar = montoPagar;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getIdPropietario() {
        return idPropietario;
    }

    public void setIdPropietario(String idPropietario) {
        this.idPropietario = idPropietario;
    }

    public String getIdDepartamento() {
        return idDepartamento;
    }

    public void setIdDepartamento(String idDepartamento) {
        this.idDepartamento = idDepartamento;
    }

    public LocalDate getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(LocalDate fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    public LocalDate getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(LocalDate fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public double getMontoPagar() {
        return montoPagar;
    }

    public void setMontoPagar(double montoPagar) {
        this.montoPagar = montoPagar;
    }

    /**
     * Convierte el objeto en una línea de texto lista para guardar en un CSV.
     * Cada clase hija define qué campos se guardan.
     */
    public abstract String toCSV();

    @Override
    public String toString() {
        return id;
    }
}
