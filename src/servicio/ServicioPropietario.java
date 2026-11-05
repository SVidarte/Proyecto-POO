package servicio;

import modelo.Propietario;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioPropietario {
    private final String ARCHIVO = "propietarios.csv";
    private List<Propietario> listaPropietarios;

    public ServicioPropietario() {
        listaPropietarios = new ArrayList<>();
        cargarDesdeArchivo();
    }

    public void guardar(Propietario propietario) {
        if (propietario.getId() == null || propietario.getId().isEmpty()) {
            propietario.setId(generarIdAutoIncremental());
        }
        listaPropietarios.add(propietario);
        sincronizarArchivo();
    }

    public List<Propietario> listar() {
        return listaPropietarios;
    }

    public Propietario buscar(String id) {
        for (Propietario p : listaPropietarios) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    public boolean modificar(Propietario propietarioModificado) {
        for (int i = 0; i < listaPropietarios.size(); i++) {
            if (listaPropietarios.get(i).getId().equalsIgnoreCase(propietarioModificado.getId())) {
                listaPropietarios.set(i, propietarioModificado);
                sincronizarArchivo();
                return true;
            }
        }
        return false;
    }

    private String generarIdAutoIncremental() {
        if (listaPropietarios.isEmpty()) {
            return "P0001";
        }
        String ultimoId = listaPropietarios.get(listaPropietarios.size() - 1).getId();
        int numero = Integer.parseInt(ultimoId.substring(1)) + 1;
        return String.format("P%04d", numero);
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
            for (Propietario p : listaPropietarios) {
                pw.println(p.getId() + ";" + 
                           p.getDni() + ";" + 
                           p.getNombres() + ";" + 
                           p.getApellidos() + ";" + 
                           p.getEmail() + ";" + 
                           p.getCelular() + ";" + 
                           p.getSexo() + ";" + 
                           p.getMetodoPagoPreferido() + ";" + 
                           p.getNumeroCuenta() + ";" + 
                           p.getBanco());
            }
        } catch (IOException e) {
            System.err.println("Error al escribir propietarios.csv: " + e.getMessage());
        }
    }
}