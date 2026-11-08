package modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;

/**
 * Clase abstracta que representa un alquiler de departamento.
 */
public abstract class Alquiler {

    /** Separador de campos usado en los archivos CSV. */
    public static final String SEPARADOR = ";";

    /**
     * Formato de fecha dd/MM/yyyy.
     * Al leer acepta el día y el mes con 1 o 2 dígitos (7/11/2026 o 07/11/2026);
     * al escribir siempre usa 2 dígitos (07/11/2026), así el CSV mantiene un solo formato.
     * Es estricto con las fechas inexistentes (rechaza 31/02/2026) y exige el año con 4 dígitos.
     */
    public static final DateTimeFormatter FORMATO_FECHA = new DateTimeFormatterBuilder()
            .parseLenient()
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('/')
            .parseStrict()
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

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
