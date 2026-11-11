package servicio;

import modelo.DatosInvalidosException;
import modelo.Persona;
import modelo.Propietario;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioPropietario implements ServicioCrud<Propietario> {
    private final String ARCHIVO = "propietarios.csv";
    private List<Propietario> listaPropietarios;

    // El método de pago de un propietario solo puede ser uno de estos dos
    private static final String[] METODOS_PAGO = {"Transferencia", "Efectivo"};

    public ServicioPropietario() {
        listaPropietarios = new ArrayList<>();
        cargarDesdeArchivo();
    }

    @Override
    public void guardar(Propietario propietario) {
        ValidadorPersona.validar(propietario);
        propietario.setMetodoPagoPreferido(validarMetodoPago(propietario.getMetodoPagoPreferido()));
        if (propietario.getId() == null || propietario.getId().isEmpty()) {
            propietario.setId(generarIdAutoIncremental());
        }
        listaPropietarios.add(propietario);
        sincronizarArchivo();
    }

    @Override
    public List<Propietario> listar() {
        return listaPropietarios;
    }

    @Override
    public Propietario buscar(String id) {
        for (Propietario p : listaPropietarios) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean modificar(Propietario propietarioModificado) {
        ValidadorPersona.validar(propietarioModificado);
        propietarioModificado.setMetodoPagoPreferido(validarMetodoPago(propietarioModificado.getMetodoPagoPreferido()));
        for (int i = 0; i < listaPropietarios.size(); i++) {
            if (listaPropietarios.get(i).getId().equalsIgnoreCase(propietarioModificado.getId())) {
                listaPropietarios.set(i, propietarioModificado);
                sincronizarArchivo();
                return true;
            }
        }
        return false;
    }

    /** Devuelve "Transferencia" o "Efectivo" (sin importar mayúsculas); cualquier otro valor es un error. */
    private String validarMetodoPago(String metodo) {
        if (metodo != null) {
            for (String valido : METODOS_PAGO) {
                if (valido.equalsIgnoreCase(metodo.trim())) {
                    return valido;
                }
            }
        }
        throw new DatosInvalidosException("El método de pago debe ser Transferencia o Efectivo.");
    }

    /** Propietarios cuyo DNI contiene el texto indicado. */
    public List<Propietario> buscarPorDni(String dni) {
        cargarDesdeArchivo();
        List<Propietario> resultado = new ArrayList<>();
        for (Propietario p : listaPropietarios) {
            if (p.getDni().trim().contains(dni.trim())) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /** Elimina el propietario. @return true si existía */
    @Override
    public boolean eliminar(String id) {
        cargarDesdeArchivo();
        boolean eliminado = listaPropietarios.removeIf(p -> p.getId().equalsIgnoreCase(id));
        if (eliminado) {
            sincronizarArchivo();
        }
        return eliminado;
    }

    /** Siguiente id (P0001, P0002, ...): nunca reutiliza un número que todavía aparece en otros archivos. */
    private String generarIdAutoIncremental() {
        int mayor = 0;
        for (Propietario p : listaPropietarios) {
            try {
                mayor = Math.max(mayor, Integer.parseInt(p.getId().substring(1)));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // id con formato raro: se ignora
            }
        }
        mayor = Math.max(mayor, UtilIds.mayorNumero("alquiler.csv", 2, "P"));
        mayor = Math.max(mayor, UtilIds.mayorNumero("departamentos.csv", 1, "P"));
        return String.format("P%04d", mayor + 1);
    }

    private void cargarDesdeArchivo() {
        listaPropietarios.clear();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 10) {
                    Propietario p = new Propietario(
                        partes[0], partes[1], partes[2], partes[3], 
                        partes[4], partes[5], partes[6].charAt(0), 
                        partes[7], partes[8], partes[9]
                    );
                    listaPropietarios.add(p);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer propietarios.csv: " + e.getMessage());
        }
    }

    private void sincronizarArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO))) {
            for (Persona persona : listaPropietarios) {
                pw.println(persona.toCSV()); // cada clase hija decide qué campos escribe
            }
        } catch (IOException e) {
            System.err.println("Error al escribir propietarios.csv: " + e.getMessage());
        }
    }
}