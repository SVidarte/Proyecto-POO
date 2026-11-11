package servicio;

import modelo.Cliente;
import modelo.Persona;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioCliente implements ServicioCrud<Cliente> {
    private final String ARCHIVO = "clientes.csv";
    private List<Cliente> listaClientes;

    public ServicioCliente() {
        listaClientes = new ArrayList<>();
        cargarDesdeArchivo();
    }

    @Override
    public void guardar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isEmpty()) {
            cliente.setId(generarIdAutoIncremental());
        }
        listaClientes.add(cliente);
        sincronizarArchivo();
    }

    @Override
    public List<Cliente> listar() {
        return listaClientes;
    }

    @Override
    public Cliente buscar(String id) {
        for (Cliente c : listaClientes) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public boolean modificar(Cliente clienteModificado) {
        for (int i = 0; i < listaClientes.size(); i++) {
            if (listaClientes.get(i).getId().equalsIgnoreCase(clienteModificado.getId())) {
                listaClientes.set(i, clienteModificado);
                sincronizarArchivo();
                return true;
            }
        }
        return false;
    }

    /** Clientes cuyo DNI contiene el texto indicado. */
    public List<Cliente> buscarPorDni(String dni) {
        listaClientes.clear();
        cargarDesdeArchivo();
        List<Cliente> resultado = new ArrayList<>();
        for (Cliente c : listaClientes) {
            if (c.getDni().trim().contains(dni.trim())) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    /** Elimina el cliente. @return true si existía */
    @Override
    public boolean eliminar(String id) {
        cargarDesdeArchivo();
        boolean eliminado = listaClientes.removeIf(c -> c.getId().equalsIgnoreCase(id));
        if (eliminado) {
            sincronizarArchivo();
        }
        return eliminado;
    }

    /** Siguiente id (C0001, C0002, ...): nunca reutiliza un número que todavía aparece en alquiler.csv. */
    private String generarIdAutoIncremental() {
        int mayor = 0;
        for (Cliente c : listaClientes) {
            try {
                mayor = Math.max(mayor, Integer.parseInt(c.getId().substring(1)));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // id con formato raro: se ignora
            }
        }
        mayor = Math.max(mayor, UtilIds.mayorNumero("alquiler.csv", 1, "C"));
        return String.format("C%04d", mayor + 1);
    }

    private void cargarDesdeArchivo() {
        listaClientes.clear();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 8) {
                    Cliente c = new Cliente(
                        partes[0], partes[1], partes[2], partes[3], 
                        partes[4], partes[5], partes[6].charAt(0), partes[7]
                    );
                    listaClientes.add(c);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer clientes.csv: " + e.getMessage());
        }
    }

    private void sincronizarArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO))) {
            for (Persona persona : listaClientes) {
                pw.println(persona.toCSV()); // cada clase hija decide qué campos escribe
            }
        } catch (IOException e) {
            System.err.println("Error al escribir clientes.csv: " + e.getMessage());
        }
    }
}
