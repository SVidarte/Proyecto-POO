package vista;

import javax.swing.*;
import java.awt.*;

public class MenuPrincipal extends JFrame {

    public MenuPrincipal() {
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        setTitle("Sistema de Gestión - Menú Principal");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        // Título de bienvenida
        JLabel lblTitulo = new JLabel("Menú Principal", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitulo.setBounds(50, 30, 280, 30);
        add(lblTitulo);

        // Botón para Clientes
        JButton btnClientes = new JButton("Gestión de Clientes");
        btnClientes.setFont(new Font("Arial", Font.PLAIN, 14));
        btnClientes.setBounds(80, 90, 220, 40);
        add(btnClientes);

        // Botón para Propietarios
        JButton btnPropietarios = new JButton("Gestión de Propietarios");
        btnPropietarios.setFont(new Font("Arial", Font.PLAIN, 14));
        btnPropietarios.setBounds(80, 150, 220, 40);
        add(btnPropietarios);

        // Eventos de los botones para abrir las ventanas correspondientes
        btnClientes.addActionListener(e -> {
            java.awt.EventQueue.invokeLater(() -> {
                new VistaCliente().setVisible(true);
            });
        });

        btnPropietarios.addActionListener(e -> {
            java.awt.EventQueue.invokeLater(() -> {
                new VistaPropietario().setVisible(true);
            });
        });
    }

    public static void main(String[] args) {
        // Lanzar la aplicación desde el menú principal
        java.awt.EventQueue.invokeLater(() -> {
            new MenuPrincipal().setVisible(true);
        });
    }
}