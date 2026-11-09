package vista;

import modelo.Cliente;
import servicio.ServicioCliente;

import javax.swing.*;
import java.awt.*;

/** Opciones del cliente: Pagos, Planificación de Pagos, Alquileres Disponibles y Alquileres Actuales. */
public class VistaMenuCliente extends JFrame {

    public VistaMenuCliente(String idCliente) {
        setTitle("Cliente");
        setSize(420, 360);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        Cliente c = new ServicioCliente().buscar(idCliente);
        String nombre = (c != null) ? c.getNombres() + " " + c.getApellidos() : idCliente;
        JLabel lblBienvenida = new JLabel("Cliente: " + nombre + " (" + idCliente + ")", SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("Arial", Font.BOLD, 14));
        lblBienvenida.setBounds(20, 20, 370, 30);
        add(lblBienvenida);

        JButton btnPagos = crearBoton("Pagos", 70);
        JButton btnPlanificacion = crearBoton("Planificación de Pagos", 130);
        JButton btnDisponibles = crearBoton("Alquileres Disponibles", 190);
        JButton btnActuales = crearBoton("Alquileres Actuales", 250);

        btnPagos.addActionListener(e -> new VistaPago(idCliente).setVisible(true));
        btnPlanificacion.addActionListener(e -> new VistaPlanificacionPago(idCliente).setVisible(true));
        btnDisponibles.addActionListener(e ->
                new VistaAlquileresCliente(idCliente, VistaAlquileresCliente.Modo.DISPONIBLES).setVisible(true));
        btnActuales.addActionListener(e ->
                new VistaAlquileresCliente(idCliente, VistaAlquileresCliente.Modo.ACTUALES).setVisible(true));
    }

    private JButton crearBoton(String texto, int y) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("Arial", Font.PLAIN, 14));
        boton.setBounds(100, y, 220, 40);
        add(boton);
        return boton;
    }
}
