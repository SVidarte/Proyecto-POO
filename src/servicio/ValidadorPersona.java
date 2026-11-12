package servicio;

import modelo.DatosInvalidosException;
import modelo.Persona;

/**
 * Reglas de DNI y Celular. Se valida al guardar/modificar (no al leer el CSV, para no romper
 * los registros que ya estaban guardados).
 */
public class ValidadorPersona {

    public static final int LONGITUD_DNI = 8;
    public static final int LONGITUD_CELULAR = 9;

    private ValidadorPersona() {}

    public static void validar(Persona persona) {
        validarNumero(persona.getDni(), "DNI", LONGITUD_DNI);
        validarNumero(persona.getCelular(), "Celular", LONGITUD_CELULAR);
    }

    /** Debe tener exactamente la cantidad indicada de dígitos del 0 al 9. */
    public static void validarNumero(String valor, String campo, int longitud) {
        if (valor == null || valor.length() != longitud) {
            throw new DatosInvalidosException("El " + campo + " debe tener exactamente " + longitud + " dígitos.");
        }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c < '0' || c > '9') {
                throw new DatosInvalidosException("El " + campo + " solo puede contener números del 0 al 9.");
            }
        }
    }
}

