package vista;

import modelo.Alquiler;
import modelo.PlanificacionPago;
import servicio.ServicioAlquiler;
import servicio.ServicioPlanificacionPago;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Vista para generar y consultar las cuotas (planificaciones de pago) de un alquiler.
 */
public class VistaPlanificacionPago extends JFrame {

    private final ServicioPlanificacionPago servicio = new ServicioPlanificacionPago();
    private final String idCliente; // null = sin restricción; con un cliente, solo ve las cuotas de sus alquileres

    private JTextField txtIdAlquiler;
    private DefaultTableModel modeloTabla;

    public VistaPlanificacionPago() {
        this(null);
    }

    public VistaPlanificacionPago(String idCliente) {
        this.idCliente = idCliente;
        setTitle(idCliente == null ? "Planificación de Pagos" : "Mi Planificación de Pagos");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        txtIdAlquiler = new JTextField(10);
        JButton btnGenerar = new JButton("Generar cuotas");
        JButton btnFiltrar = new JButton("Ver cuotas del alquiler");
        JButton btnPagadas = new JButton("Ver cuotas ya pagadas");
        JButton btnTodos = new JButton("Listar todo");

        btnGenerar.addActionListener(e -> generarCuotas());
        btnFiltrar.addActionListener(e -> filtrar());
        btnPagadas.addActionListener(e -> verPagadas());
        btnTodos.addActionListener(e -> cargarTabla(cuotasVisibles()));

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelSuperior.add(new JLabel("ID alquiler:"));
        panelSuperior.add(txtIdAlquiler);
        if (idCliente == null) { // las cuotas de un alquiler se generan solas cuando el cliente lo acepta
            panelSuperior.add(btnGenerar);
        }
        panelSuperior.add(btnFiltrar);
        panelSuperior.add(btnPagadas);
        panelSuperior.add(btnTodos);
        add(panelSuperior, BorderLayout.NORTH);

        modeloTabla = new DefaultTableModel(new String[]{
                "ID", "ID alquiler", "Fecha de pago", "Monto", "Monto pagado", "Saldo pendiente", "Verificador"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modeloTabla);
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        cargarTabla(cuotasVisibles());
    }

    private void generarCuotas() {
        String id = txtIdAlquiler.getText().trim();
        try {
            List<PlanificacionPago> cuotas = servicio.generarCuotas(id);
            JOptionPane.showMessageDialog(this,
                    "Se generaron " + cuotas.size() + " cuotas para el alquiler " + id.toUpperCase() + ".");
            cargarTabla(servicio.listarPorAlquiler(id));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al acceder al archivo: " + ex.getMessage());
        }
    }

    private void filtrar() {
        String id = txtIdAlquiler.getText().trim();
        if (id.isEmpty()) {
            mostrarError("Ingrese el id del alquiler.");
            return;
        }
        if (!esAlquilerVisible(id)) {
            cargarTabla(java.util.Collections.<PlanificacionPago>emptyList());
            mostrarError("Ese alquiler no existe o no es suyo.");
            return;
        }
        if (!servicio.alquilerExiste(id)) {
            cargarTabla(java.util.Collections.<PlanificacionPago>emptyList());
            if (servicio.alquilerEliminado(id)) {
                JOptionPane.showMessageDialog(this, "El alquiler " + id.toUpperCase() + " fue eliminado y ya no tiene "
                        + "cuotas pendientes.\nUse \"Ver cuotas ya pagadas\" para ver las que se habían pagado.");
            } else {
                mostrarError("El alquiler " + id.toUpperCase() + " no existe.");
            }
            return;
        }
        List<PlanificacionPago> cuotas = servicio.listarPorAlquiler(id);
        cargarTabla(cuotas);
        if (cuotas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ese alquiler no tiene cuotas pendientes.");
        }
    }

    private void verPagadas() {
        String id = txtIdAlquiler.getText().trim();
        if (id.isEmpty()) {
            mostrarError("Ingrese el id del alquiler.");
            return;
        }
        if (!esAlquilerVisible(id)) {
            cargarTabla(java.util.Collections.<PlanificacionPago>emptyList());
            mostrarError("Ese alquiler no existe o no es suyo.");
            return;
        }
        boolean existe = servicio.alquilerExiste(id);
        boolean eliminado = servicio.alquilerEliminado(id);
        if (!existe && !eliminado) {
            cargarTabla(java.util.Collections.<PlanificacionPago>emptyList());
            mostrarError("El alquiler " + id.toUpperCase() + " no existe.");
            return;
        }
        List<PlanificacionPago> pagadas = servicio.listarPagadasPorAlquiler(id);
        cargarTabla(pagadas);
        if (pagadas.isEmpty()) {
            JOptionPane.showMessageDialog(this, eliminado
                    ? "El alquiler " + id.toUpperCase() + " fue eliminado y no tenía cuotas pagadas."
                    : "Ese alquiler todavía no tiene cuotas pagadas.");
        } else if (eliminado) {
            JOptionPane.showMessageDialog(this, "El alquiler " + id.toUpperCase()
                    + " fue eliminado. Se muestran las cuotas que ya estaban pagadas.");
        }
    }

    private void cargarTabla(List<PlanificacionPago> lista) {
        modeloTabla.setRowCount(0);
        for (PlanificacionPago p : lista) {
            modeloTabla.addRow(new Object[]{
                    p.getId(),
                    p.getIdAlquiler(),
                    p.getFechaPago(),
                    p.getMonto(),
                    p.getMonto_pagado(),
                    String.format(Locale.US, "%.2f", p.calcularSaldoPendiente()),
                    p.isVerificador()
            });
        }
    }

    /** Ids (en mayúsculas) de los alquileres del cliente. */
    private java.util.Set<String> misAlquileres() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        try {
            for (Alquiler a : new ServicioAlquiler().listarPorCliente(idCliente)) {
                ids.add(a.getId().toUpperCase());
            }
        } catch (IOException e) {
            mostrarError("No se pudo leer alquiler.csv: " + e.getMessage());
        }
        return ids;
    }

    private boolean esAlquilerVisible(String id) {
        return idCliente == null || misAlquileres().contains(id.trim().toUpperCase());
    }

    /** Todas las cuotas pendientes, o solo las de los alquileres del cliente. */
    private List<PlanificacionPago> cuotasVisibles() {
        return (idCliente == null) ? servicio.listar() : servicio.listarPorAlquileres(misAlquileres());
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VistaPlanificacionPago().setVisible(true));
    }
}
