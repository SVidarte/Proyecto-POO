package servicio;

import modelo.Alquiler;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eliminaciones en cascada de propietarios y clientes, y consultas del administrador.
 * Los alquileres con todas sus cuotas pagadas no se eliminan hasta el día de su fecha final.
 */
public class ServicioAdministracion {

    /** Resultado de una eliminación en cascada. */
    public static class Resumen {
        public int alquileresEliminados;
        public int alquileresConservados;   // pagados por completo, siguen hasta su fecha final
        public int departamentosEliminados;
    }

    private final ServicioAlquiler servicioAlquiler;

    public ServicioAdministracion() throws IOException {
        servicioAlquiler = new ServicioAlquiler();
    }

    /** Número de clientes distintos que tienen un alquiler en algún departamento del propietario. */
    public int contarClientesDePropietario(String idPropietario) throws IOException {
        Set<String> clientes = new HashSet<>();
        for (Alquiler a : servicioAlquiler.listarPorPropietario(idPropietario)) {
            if (a.getIdCliente() != null && !a.getIdCliente().trim().isEmpty()) {
                clientes.add(a.getIdCliente().trim().toUpperCase());
            }
        }
        return clientes.size();
    }

    /**
     * Elimina al propietario: sus datos, su cuenta, sus departamentos y sus alquileres (los ids de esos
     * alquileres quedan en alquileres_eliminados.csv y sus cuotas pendientes se borran).
     */
    public Resumen eliminarPropietario(String idPropietario) throws IOException {
        Resumen resumen = new Resumen();
        eliminarAlquileres(servicioAlquiler.listarPorPropietario(idPropietario), resumen);
        resumen.departamentosEliminados = new ServicioDepartamento().eliminarPorPropietario(idPropietario);
        new ServicioPropietario().eliminar(idPropietario);
        ServicioCuenta.propietarios().eliminarPorPerfil(idPropietario);
        return resumen;
    }

    /**
     * Elimina al cliente y todos sus alquileres (ids a alquileres_eliminados.csv; sus cuotas pendientes se
     * borran sin pasar a planificacion_pagos_realizados.csv). Su cuenta se conserva sin datos: la próxima vez
     * que inicie sesión tendrá que registrarlos otra vez.
     */
    public Resumen eliminarCliente(String idCliente) throws IOException {
        Resumen resumen = new Resumen();
        eliminarAlquileres(servicioAlquiler.listarPorCliente(idCliente), resumen);
        new ServicioCliente().eliminar(idCliente);
        ServicioCuenta.clientes().desvincularPerfil(idCliente);
        return resumen;
    }

    private void eliminarAlquileres(List<Alquiler> alquileres, Resumen resumen) throws IOException {
        for (Alquiler a : alquileres) {
            if (servicioAlquiler.estaProtegido(a)) {
                resumen.alquileresConservados++;
            } else if (servicioAlquiler.eliminar(a.getId())) {
                resumen.alquileresEliminados++;
            }
        }
    }
}
