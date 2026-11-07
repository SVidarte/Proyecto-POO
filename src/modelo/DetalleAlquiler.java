package modelo;

/**
 * Detalle de un alquiler. Hereda los datos del alquiler al que pertenece
 * y agrega su propio identificador (idDetalle) y la referencia (idAlquiler).
 */
public class DetalleAlquiler extends Alquiler {

    private String idDetalle;
    private String idAlquiler;

    /**
     * Crea el detalle copiando los datos del alquiler al que pertenece.
     */
    public DetalleAlquiler(String idDetalle, Alquiler alquiler) {
        super(alquiler.getId(), alquiler.getIdCliente(), alquiler.getIdPropietario(),
                alquiler.getIdDepartamento(), alquiler.getFechaInicial(), alquiler.getFechaFinal(),
                alquiler.getMontoPagar());
        this.idDetalle = idDetalle;
        this.idAlquiler = alquiler.getId();
    }

    public String getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(String idDetalle) {
        this.idDetalle = idDetalle;
    }

    public String getIdAlquiler() {
        return idAlquiler;
    }

    public void setIdAlquiler(String idAlquiler) {
        this.idAlquiler = idAlquiler;
    }

    /** En detalle_alquiler.csv solo se guardan idDetalle e idAlquiler. */
    @Override
    public String toCSV() {
        return idDetalle + SEPARADOR + idAlquiler;
    }

    @Override
    public String toString() {
        return idDetalle;
    }
}
