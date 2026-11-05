package servicio;

import modelo.Cliente;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioCliente {
    private final String ARCHIVO = "clientes.csv";
    private List<Cliente> listaClientes;

    public ServicioCliente() {
        listaClientes = new ArrayList<>();
        cargarDesdeArchivo();
    }

    public void guardar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isEmpty()) {
            cliente.setId(generarIdAutoIncremental());
        }
        listaClientes.add(cliente);
        sincronizarArchivo();
    }

    public List<Cliente> listar() {
        return listaClientes;
    }

    public Cliente buscar(String id) {
        for (Cliente c : listaClientes) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

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

    private String generarIdAutoIncremental() {
        if (listaClientes.isEmpty()) {
            return "C0001";
        }
        String ultimoId = listaClientes.get(listaClientes.size() - 1).getId(); // Ej: C0005
        int numero = Integer.parseInt(ultimoId.substring(1)) + 1;
        return String.format("C%04d", numero);
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
            for (Cliente c : listaClientes) {
                pw.println(c.getId() + ";" + 
                           c.getDni() + ";" + 
                           c.getNombres() + ";" + 
                           c.getApellidos() + ";" + 
                           c.getEmail() + ";" + 
                           c.getCelular() + ";" + 
                           c.getSexo() + ";" + 
                           c.getReferencia());
            }
        } catch (IOException e) {
            System.err.println("Error al escribir clientes.csv: " + e.getMessage());
        }
    }
}
