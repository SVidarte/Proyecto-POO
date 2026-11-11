package vista;

import modelo.Cliente;
import servicio.ServicioCliente;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class VistaCliente extends JFrame {
    private ServicioCliente servicio;

    private JTextField txtId;
    private JTextField txtDni;
    private JTextField txtNombres;
    private JTextField txtApellidos;
    private JTextField txtEmail;
    private JTextField txtCelular;
    private JComboBox<String> cmbSexo;
    private JTextField txtReferencia;

    private JTable tablaClientes;
    private DefaultTableModel modeloTabla;

    private JButton btnGuardar;
    private JButton btnModificar;
    private JButton btnLimpiar;

    public VistaCliente() {
        servicio = new ServicioCliente();
        inicializarComponentes();
        cargarTabla();
    }

    private void inicializarComponentes() {
        setTitle("Gestión de Clientes");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        // Etiquetas y Campos
        JLabel lblId = new JLabel("ID:");
        lblId.setBounds(20, 20, 80, 25);
        add(lblId);

        txtId = new JTextField();
        txtId.setBounds(100, 20, 150, 25);
        txtId.setEditable(false); // Es autoincrementable generado por el servicio
        add(txtId);

        JLabel lblDni = new JLabel("DNI:");
        lblDni.setBounds(20, 55, 80, 25);
        add(lblDni);

        txtDni = new JTextField();
        FiltroNumerico.aplicar(txtDni, 8);
        txtDni.setBounds(100, 55, 150, 25);
        add(txtDni);

        JLabel lblNombres = new JLabel("Nombres:");
        lblNombres.setBounds(20, 90, 80, 25);
        add(lblNombres);

        txtNombres = new JTextField();
        txtNombres.setBounds(100, 90, 150, 25);
        add(txtNombres);

        JLabel lblApellidos = new JLabel("Apellidos:");
        lblApellidos.setBounds(20, 125, 80, 25);
        add(lblApellidos);

        txtApellidos = new JTextField();
        txtApellidos.setBounds(100, 125, 150, 25);
        add(txtApellidos);

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setBounds(380, 20, 90, 25);
        add(lblEmail);

        txtEmail = new JTextField();
        txtEmail.setBounds(480, 20, 200, 25);
        add(txtEmail);

        JLabel lblCelular = new JLabel("Celular:");
        lblCelular.setBounds(380, 55, 90, 25);
        add(lblCelular);

        txtCelular = new JTextField();
        FiltroNumerico.aplicarCelular(txtCelular, 9);
        txtCelular.setBounds(480, 55, 200, 25);
        add(txtCelular);

        JLabel lblSexo = new JLabel("Sexo:");
        lblSexo.setBounds(380, 90, 90, 25);
        add(lblSexo);

        cmbSexo = new JComboBox<>(new String[]{"Masculino", "Femenino"});
        cmbSexo.setBounds(480, 90, 200, 25);
        add(cmbSexo);

        JLabel lblReferencia = new JLabel("Referencia:");
        lblReferencia.setBounds(380, 125, 90, 25);
        add(lblReferencia);

        txtReferencia = new JTextField();
        txtReferencia.setBounds(480, 125, 200, 25);
        add(txtReferencia);

        // Botones
        btnGuardar = new JButton("Guardar");
        btnGuardar.setBounds(100, 175, 110, 30);
        add(btnGuardar);

        btnModificar = new JButton("Modificar");
        btnModificar.setBounds(230, 175, 110, 30);
        add(btnModificar);

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setBounds(360, 175, 110, 30);
        add(btnLimpiar);

        // Tabla
        modeloTabla = new DefaultTableModel(new String[]{"ID", "DNI", "Nombres", "Apellidos", "Email", "Celular", "Sexo", "Referencia"}, 0);
        tablaClientes = new JTable(modeloTabla);
        JScrollPane scrollPane = new JScrollPane(tablaClientes);
        scrollPane.setBounds(20, 225, 690, 210);
        add(scrollPane);

        // Eventos de Botones
        btnGuardar.addActionListener(e -> guardarCliente());
        btnModificar.addActionListener(e -> modificarCliente());
        btnLimpiar.addActionListener(e -> limpiarCampos());

        // Evento de selección en la tabla
        tablaClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaClientes.getSelectedRow();
                if (fila >= 0) {
                    txtId.setText(modeloTabla.getValueAt(fila, 0).toString());
                    txtDni.setText(modeloTabla.getValueAt(fila, 1).toString());
                    txtNombres.setText(modeloTabla.getValueAt(fila, 2).toString());
                    txtApellidos.setText(modeloTabla.getValueAt(fila, 3).toString());
                    txtEmail.setText(modeloTabla.getValueAt(fila, 4).toString());
                    txtCelular.setText(modeloTabla.getValueAt(fila, 5).toString());
                    
                    char sexo = modeloTabla.getValueAt(fila, 6).toString().charAt(0);
                    cmbSexo.setSelectedItem(sexo == 'M' ? "Masculino" : "Femenino");
                    
                    txtReferencia.setText(modeloTabla.getValueAt(fila, 7).toString());
                }
            }
        });
    }

    private void guardarCliente() {
        try {
            char sexo = cmbSexo.getSelectedItem().toString().equals("Masculino") ? 'M' : 'F';
            Cliente cliente = new Cliente(
                null, // ID se autogenera en el servicio
                txtDni.getText(),
                txtNombres.getText(),
                txtApellidos.getText(),
                txtEmail.getText(),
                txtCelular.getText(),
                sexo,
                txtReferencia.getText()
            );
            servicio.guardar(cliente);
            cargarTabla();
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Cliente guardado exitosamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarCliente() {
        String id = txtId.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente de la tabla para modificar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            char sexo = cmbSexo.getSelectedItem().toString().equals("Masculino") ? 'M' : 'F';
            Cliente cliente = new Cliente(
                id,
                txtDni.getText(),
                txtNombres.getText(),
                txtApellidos.getText(),
                txtEmail.getText(),
                txtCelular.getText(),
                sexo,
                txtReferencia.getText()
            );
            boolean modificado = servicio.modificar(cliente);
            if (modificado) {
                cargarTabla();
                limpiarCampos();
                JOptionPane.showMessageDialog(this, "Cliente modificado correctamente.");
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el cliente.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al modificar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtDni.setText("");
        txtNombres.setText("");
        txtApellidos.setText("");
        txtEmail.setText("");
        txtCelular.setText("");
        cmbSexo.setSelectedIndex(0);
        txtReferencia.setText("");
        tablaClientes.clearSelection();
    }

    private void cargarTabla() {
        modeloTabla.setRowCount(0);
        List<Cliente> lista = servicio.listar();
        for (Cliente c : lista) {
            modeloTabla.addRow(new Object[]{
                c.getId(), c.getDni(), c.getNombres(), c.getApellidos(),
                c.getEmail(), c.getCelular(), c.getSexo(), c.getReferencia()
            });
        }
    }
}