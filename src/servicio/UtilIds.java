package servicio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/** Apoyo para generar ids que nunca reutilizan un número que todavía aparece en otro archivo. */
final class UtilIds {

    private UtilIds() {
    }

    /**
     * Mayor número de id con ese prefijo que aparece en la columna indicada de un CSV
     * (por ejemplo, la columna idCliente de alquiler.csv). Devuelve 0 si el archivo no existe.
     */
    static int mayorNumero(String ruta, int columna, String prefijo) {
        int mayor = 0;
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            return 0;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";", -1);
                if (partes.length > columna) {
                    String valor = partes[columna].trim();
                    if (valor.startsWith(prefijo)) {
                        try {
                            mayor = Math.max(mayor, Integer.parseInt(valor.substring(prefijo.length())));
                        } catch (NumberFormatException e) {
                            // no es un id (encabezado u otro texto): se ignora
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ruta + ": " + e.getMessage());
        }
        return mayor;
    }
}
