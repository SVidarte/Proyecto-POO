package vista;

import modelo.Alquiler;
import modelo.PlanificacionPago;
import servicio.ServicioAlquiler;
import servicio.ServicioPlanificacionPago;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuotas pendientes de los alquileres del propietario. Mientras una cuota no tenga pagos, el propietario puede
 * cambiar su fecha de pago y su monto, o eliminarla.
 */
public class VistaCuotasPropietario extends JFrame {

    private final String idPropietario;
    private List<PlanificacionPago> cuotas = new ArrayList<>();

    private final ModeloTabla modeloTabla = new ModeloTabla(
            new String[]{"ID cuota", "ID alquiler", "ID cliente", "Fecha de pago", "Monto", "Monto pagado"});
    private final JTable tabla = new JTable(modeloTabla);
    private final JTextField txtFecha = new JTextField(9);
    private final JTextField txtMonto = new JTextField(9);

    public VistaCuotasPropietario(String idPropietario) {
        this.idPropietario = idPropietario;

        setTitle("Cuotas de mis alquileres");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarSeleccionEnFormulario();
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel datos = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        datos.add(new JLabel("Fecha de pago (dd/MM/yyyy):"));
        datos.add(txtFecha);
        datos.add(new JLabel("Monto:"));
        datos.add(txtMonto);
        datos.add(new JLabel("  (solo se puede cambiar o eliminar una cuota sin pagos)"));

        JButton btnModificar = new JButton("Modificar cuota");
        JButton btnEliminar = new JButton("Eliminar cuota");
        JButton btnActualizar = new JButton("Actualizar");
        btnModificar.addActionListener(e -> modificar());
        btnEliminar.addActionListener(e -> eliminar());
        btnActualizar.addActionListener(e -> cargarTabla());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        botones.add(btnModificar);
        botones.add(btnEliminar);
        botones.add(btnActualizar);
        JButton btnVolver = new JButton("Volver");
        btnVolver.addActionListener(e -> dispose());
        botones.add(btnVolver);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(datos, BorderLayout.NORTH);
        sur.add(botones, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                cargarTabla();
            }
        });
        cargarTabla();
    }

    private void cargarTabla() {
        Map<String, String> clienteDeCadaAlquiler = new HashMap<>();
        try {
            for (Alquiler a : new ServicioAlquiler().listarPorPropietario(idPropietario)) {
                clienteDeCadaAlquiler.put(a.getId().toUpperCase(), a.getIdCliente());
            }
        } catch (IOException e) {
            error("No se pudo leer alquiler.csv: " + e.getMessage());
            return;
        }
        cuotas = new ServicioPlanificacionPago().listarPorAlquileres(clienteDeCadaAlquiler.keySet());
        modeloTabla.setRowCount(0);
        for (PlanificacionPago p : cuotas) {
            modeloTabla.addRow(new Object[]{p.getId(), p.getIdAlquiler(),
                clienteDeCadaAlquiler.get(p.getIdAlquiler().toUpperCase()),
                p.getFechaPago(), p.getMonto(), p.getMonto_pagado()});
        }
    }

    private PlanificacionPago cuotaSeleccionada() {
        int fila = tabla.getSelectedRow();
        return (fila >= 0 && fila < cuotas.size()) ? cuotas.get(fila) : null;
    }

    private void mostrarSeleccionEnFormulario() {
        PlanificacionPago c = cuotaSeleccionada();
        if (c != null) {
            txtFecha.setText(c.getFechaPago());
            txtMonto.setText(c.getMonto());
        }
    }

    private void modificar() {
        PlanificacionPago c = cuotaSeleccionada();
        if (c == null) {
            error("Seleccione una cuota de la tabla.");
            return;
        }
        try {
            LocalDate fecha;
            try {
                fecha = LocalDate.parse(txtFecha.getText().trim(), Alquiler.FORMATO_FECHA);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("La fecha no es válida. Use el formato dd/MM/yyyy.");
            }
            double monto;
            try {
                monto = Double.parseDouble(txtMonto.getText().trim().replace(',', '.'));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El monto debe ser un número (ejemplo: 1500.50).");
            }
            new ServicioPlanificacionPago().modificarCuota(c.getId(), fecha, monto);
            JOptionPane.showMessageDialog(this, "Cuota " + c.getId() + " modificada.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(ex.getMessage());
        }
        cargarTabla();
    }

    private void eliminar() {
        PlanificacionPago c = cuotaSeleccionada();
        if (c == null) {
            error("Seleccione una cuota de la tabla.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this, "¿Eliminar la cuota " + c.getId() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            new ServicioPlanificacionPago().eliminarCuota(c.getId());
            JOptionPane.showMessageDialog(this, "Cuota eliminada.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(ex.getMessage());
        }
        cargarTabla();
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
