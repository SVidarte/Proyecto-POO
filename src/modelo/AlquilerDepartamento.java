package modelo;

import java.time.LocalDate;

/**
 * Alquiler concreto de un departamento. Es la clase que se guarda en alquiler.csv,
 * porque Alquiler es abstracta y no se puede instanciar directamente.
 */
public class AlquilerDepartamento extends Alquiler {

    public AlquilerDepartamento(String id, String idCliente, String idPropietario, String idDepartamento,
                                LocalDate fechaInicial, LocalDate fechaFinal, double montoPagar) {
        super(id, idCliente, idPropietario, idDepartamento, fechaInicial, fechaFinal, montoPagar);
    }

    @Override
    public String toCSV() {
        return getId() + SEPARADOR
                + getIdCliente() + SEPARADOR
                + getIdPropietario() + SEPARADOR
                + getIdDepartamento() + SEPARADOR
                + getFechaInicial().format(FORMATO_FECHA) + SEPARADOR
                + getFechaFinal().format(FORMATO_FECHA) + SEPARADOR
                + getMontoPagar();
    }
}
