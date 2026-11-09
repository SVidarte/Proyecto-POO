package vista;

import modelo.Alquiler;
import modelo.Departamento;
import modelo.Propietario;
import servicio.ServicioAlquiler;
import servicio.ServicioDepartamento;
import servicio.ServicioPropietario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Alquileres del cliente. En modo DISPONIBLES muestra los alquileres sin cliente cuyo departamento está
 * Disponible y permite aceptarlos; en modo ACTUALES muestra los alquileres que ya son del cliente.
 */
public class VistaAlquileresCliente extends JFrame {

    public enum Modo { DISPONIBLES, ACTUALES }

    private final String idCliente;
    private final Modo modo;
    private List<Alquiler> alquileres = new ArrayList<>();

    private final ModeloTabla modeloTabla = new ModeloTabla(new String[]{
        "ID", "Propietario", "Departamento", "Dirección", "Fecha inicial", "Fecha final", "Monto a pagar"});
    private final JTable tabla = new JTable(modeloTabla);

    public VistaAlquileresCliente(String idCliente, Modo modo) {
        this.idCliente = idCliente;
        this.modo = modo;

        setTitle(modo == Modo.DISPONIBLES ? "Alquileres Disponibles" : "Alquileres Actuales");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        if (modo == Modo.DISPONIBLES) {
            JButton btnAceptar = new JButton("Aceptar alquiler");
            btnAceptar.addActionListener(e -> aceptar());
            sur.add(btnAceptar);
        }
        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> cargarTabla());
        sur.add(btnActualizar);
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
        try {
            ServicioAlquiler servicio = new ServicioAlquiler();
            alquileres = (modo == Modo.DISPONIBLES) ? servicio.listarDisponibles() : servicio.listarPorCliente(idCliente);
        } catch (IOException e) {
            error("No se pudo leer alquiler.csv: " + e.getMessage());
            return;
        }
        ServicioPropietario servicioPropietario = new ServicioPropietario();
        List<Departamento> departamentos = new ServicioDepartamento().listarDepartamento();

        modeloTabla.setRowCount(0);
        for (Alquiler a : alquileres) {
            Propietario p = servicioPropietario.buscar(a.getIdPropietario());
            String propietario = (p != null) ? p.getNombres() + " " + p.getApellidos() : a.getIdPropietario();
            String numero = a.getIdDepartamento();
            String direccion = "(departamento eliminado)";
            for (Departamento d : departamentos) {
                if (d.getId_departamento().equalsIgnoreCase(a.getIdDepartamento())) {
                    numero = d.getId_departamento() + " - Dpto " + d.getNumero_departamento();
                    direccion = d.getDireccion();
                    break;
                }
            }
            modeloTabla.addRow(new Object[]{a.getId(), propietario, numero, direccion,
                a.getFechaInicial().format(Alquiler.FORMATO_FECHA), a.getFechaFinal().format(Alquiler.FORMATO_FECHA),
                String.format(Locale.US, "%.2f", a.getMontoPagar())});
        }
    }

    private void aceptar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= alquileres.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione un alquiler de la tabla.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Alquiler a = alquileres.get(fila);
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Aceptar el alquiler " + a.getId() + "?\nSe generarán sus cuotas mensuales.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int cuotas = new ServicioAlquiler().aceptarAlquiler(a.getId(), idCliente);
            JOptionPane.showMessageDialog(this, "Alquiler " + a.getId() + " aceptado. Se generaron " + cuotas + " cuotas.");
        } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
            error(ex.getMessage());
        }
        cargarTabla();
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
