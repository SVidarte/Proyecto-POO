package servicio;

import modelo.PlanificacionPago;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioPlanificacionPago {
    private final String ARCHIVO = "planificaciones_pago.csv";
    private List<PlanificacionPago> listaPlanificaciones;

    public ServicioPlanificacionPago() {
        listaPlanificaciones = new ArrayList<>();
        cargarDesdeArchivo();
    }

    public void guardar(PlanificacionPago planificacion) {
        if (planificacion.getId() == null || planificacion.getId().isEmpty()) {
            planificacion.setId(generarIdAutoIncremental());
        }
        listaPlanificaciones.add(planificacion);
        sincronizarArchivo();
    }

    public List<PlanificacionPago> listar() {
        return listaPlanificaciones;
    }

    public PlanificacionPago buscar(String id) {
        for (PlanificacionPago p : listaPlanificaciones) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    public boolean modificar(PlanificacionPago planificacionModificada) {
        for (int i = 0; i < listaPlanificaciones.size(); i++) {
            if (listaPlanificaciones.get(i).getId().equalsIgnoreCase(planificacionModificada.getId())) {
                listaPlanificaciones.set(i, planificacionModificada);
                sincronizarArchivo();
                return true;
            }
        }
        return false;
    }

    private String generarIdAutoIncremental() {
        if (listaPlanificaciones.isEmpty()) {
            return "PL0001";
        }
        String ultimoId = listaPlanificaciones.get(listaPlanificaciones.size() - 1).getId();
        int numero = Integer.parseInt(ultimoId.substring(2)) + 1;
        return String.format("PL%04d", numero);
    }

    private void cargarDesdeArchivo() {
        listaPlanificaciones.clear();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 5) {
                    PlanificacionPago p = new PlanificacionPago(
                        partes[0], partes[1], partes[2], partes[3], partes[4]
                    );
                    listaPlanificaciones.add(p);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer planificaciones_pago.csv: " + e.getMessage());
        }
    }

    private void sincronizarArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO))) {
            for (PlanificacionPago p : listaPlanificaciones) {
                pw.println(p.getId() + ";" + 
                           p.getIdAlquiler() + ";" + 
                           p.getFechaPago() + ";" + 
                           p.getMonto() + ";" + 
                           p.getMonto_pagado());
            }
        } catch (IOException e) {
            System.err.println("Error al escribir planificaciones_pago.csv: " + e.getMessage());
        }
    }
}