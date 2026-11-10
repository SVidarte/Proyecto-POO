package modelo;

/**
 * Excepción propia del sistema: se lanza cuando un dato no cumple las reglas del negocio
 * (campo vacío, monto negativo, usuario repetido, etc.). Hereda de IllegalArgumentException,
 * por lo que las pantallas que ya capturan esa excepción siguen funcionando igual.
 */
public class DatosInvalidosException extends IllegalArgumentException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
