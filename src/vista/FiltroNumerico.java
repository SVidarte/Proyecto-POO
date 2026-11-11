package vista;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro para JTextField: solo deja escribir (o pegar) dígitos del 0 al 9 y limita la cantidad
 * de caracteres. Se usa en DNI (8 dígitos) y Celular (9 dígitos).
 */
public class FiltroNumerico extends DocumentFilter {

    private final int maximo;
    private final boolean quitarCodigoPais; // true = al pegar, "+51" / "0051" no cuenta como dígitos

    public FiltroNumerico(int maximo, boolean quitarCodigoPais) {
        this.maximo = maximo;
        this.quitarCodigoPais = quitarCodigoPais;
    }

    /** Aplica el filtro a un campo de texto (para el DNI). */
    public static void aplicar(JTextField campo, int maximo) {
        aplicar(campo, maximo, false);
    }

    /** Para el celular: igual que el anterior, pero ignora el código de país "+51" al pegar un número. */
    public static void aplicarCelular(JTextField campo, int maximo) {
        aplicar(campo, maximo, true);
    }

    private static void aplicar(JTextField campo, int maximo, boolean quitarCodigoPais) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroNumerico(maximo, quitarCodigoPais));
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, texto, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int longitud, String texto, AttributeSet attr) throws BadLocationException {
        if (texto == null) {
            texto = "";
        }
        // Número pegado con código de país: "+51 987-654-321" o "(+51) 987654321" -> "987-654-321"
        if (quitarCodigoPais) {
            texto = texto.trim().replaceFirst("^\\(?(\\+|00)51\\)?[\\s.-]*", "");
        }
        // Se descarta todo lo que no sea 0-9 (Character.isDigit aceptaría otros alfabetos)
        StringBuilder soloDigitos = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c >= '0' && c <= '9') {
                soloDigitos.append(c);
            }
        }
        // Espacio que queda disponible una vez quitado el texto que se reemplaza
        int disponible = maximo - (fb.getDocument().getLength() - longitud);
        if (disponible < soloDigitos.length()) {
            soloDigitos.setLength(Math.max(disponible, 0));
        }
        if (soloDigitos.length() == 0 && texto.length() > 0) {
            return; // se escribió algo que no es válido: no se hace nada
        }
        super.replace(fb, offset, longitud, soloDigitos.toString(), attr);
    }
}
