package vista;

import modelo.Propietario;
import servicio.ServicioPropietario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class VistaPropietario extends JFrame {
    private ServicioPropietario servicio;

    private JTextField txtId;
    private JTextField txtDni;
    private JTextField txtNombres;
    private JTextField txtApellidos;
    private JTextField txtEmail;
    private JTextField txtCelular;
    private JComboBox<String> cmbSexo;
    private JTextField txtMetodoPago;
    private JTextField txtNumeroCuenta;
    private JTextField txtBanco;

    private JTable tablaPropietarios;
    private DefaultTableModel modeloTabla;

    private JButton btnGuardar;
    private JButton btnModificar;
    private JButton btnLimpiar;

    public VistaPropietario() {
        servicio = new ServicioPropietario();
        inicializarComponentes();
        cargarTabla();
    }

    private void inicializarComponentes() {
        setTitle("Gestión de Propietarios");
        setSize(780, 530);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        // Etiquetas y Campos
        JLabel lblId = new JLabel("ID:");
        lblId.setBounds(20, 20, 120, 25);
        add(lblId);

        txtId = new JTextField();
        txtId.setBounds(150, 20, 150, 25);
        txtId.setEditable(false);
        add(txtId);

        JLabel lblDni = new JLabel("DNI:");
        lblDni.setBounds(20, 55, 120, 25);
        add(lblDni);

        txtDni = new JTextField();
        txtDni.setBounds(150, 55, 150, 25);
        add(txtDni);

        JLabel lblNombres = new JLabel("Nombres:");
        lblNombres.setBounds(20, 90, 120, 25);
        add(lblNombres);

        txtNombres = new JTextField();
        txtNombres.setBounds(150, 90, 150, 25);
        add(txtNombres);

        JLabel lblApellidos = new JLabel("Apellidos:");
        lblApellidos.setBounds(20, 125, 120, 25);
        add(lblApellidos);

        txtApellidos = new JTextField();
        txtApellidos.setBounds(150, 125, 150, 25);
        add(txtApellidos);

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setBounds(400, 20, 140, 25);
        add(lblEmail);

        txtEmail = new JTextField();
        txtEmail.setBounds(540, 20, 200, 25);
        add(txtEmail);

        JLabel lblCelular = new JLabel("Celular:");
        lblCelular.setBounds(400, 55, 140, 25);
        add(lblCelular);

        txtCelular = new JTextField();
        txtCelular.setBounds(540, 55, 200, 25);
        add(txtCelular);

        JLabel lblSexo = new JLabel("Sexo:");
        lblSexo.setBounds(400, 90, 140, 25);
        add(lblSexo);

        cmbSexo = new JComboBox<>(new String[]{"Masculino", "Femenino"});
        cmbSexo.setBounds(540, 90, 200, 25);
        add(cmbSexo);

        JLabel lblMetodoPago = new JLabel("Método Pago:");
        lblMetodoPago.setBounds(400, 125, 140, 25);
        add(lblMetodoPago);

        txtMetodoPago = new JTextField();
        txtMetodoPago.setBounds(540, 125, 200, 25);
        add(txtMetodoPago);

        JLabel lblNumCuenta = new JLabel("Nro. Cuenta:");
        lblNumCuenta.setBounds(400, 160, 140, 25);
        add(lblNumCuenta);

        txtNumeroCuenta = new JTextField();
        txtNumeroCuenta.setBounds(540, 160, 200, 25);
        add(txtNumeroCuenta);

        JLabel lblBanco = new JLabel("Banco:");
        lblBanco.setBounds(20, 160, 120, 25);
        add(lblBanco);

        txtBanco = new JTextField();
        txtBanco.setBounds(150, 160, 150, 25);
        add(txtBanco);

        // Botones
        btnGuardar = new JButton("Guardar");
        btnGuardar.setBounds(150, 205, 110, 30);
        add(btnGuardar);

        btnModificar = new JButton("Modificar");
        btnModificar.setBounds(280, 205, 110, 30);
        add(btnModificar);

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setBounds(410, 205, 110, 30);
        add(btnLimpiar);

        // Tabla
        modeloTabla = new DefaultTableModel(new String[]{"ID", "DNI", "Nombres", "Apellidos", "Email", "Celular", "Sexo", "Método Pago", "Nro. Cuenta", "Banco"}, 0);
        tablaPropietarios = new JTable(modeloTabla);
        JScrollPane scrollPane = new JScrollPane(tablaPropietarios);
        scrollPane.setBounds(20, 255, 720, 215);
        add(scrollPane);

        // Eventos
        btnGuardar.addActionListener(e -> guardarPropietario());
        btnModificar.addActionListener(e -> modificarPropietario());
        btnLimpiar.addActionListener(e -> limpiarCampos());

        tablaPropietarios.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaPropietarios.getSelectedRow();
                if (fila >= 0) {
                    txtId.setText(modeloTabla.getValueAt(fila, 0).toString());
                    txtDni.setText(modeloTabla.getValueAt(fila, 1).toString());
                    txtNombres.setText(modeloTabla.getValueAt(fila, 2).toString());
                    txtApellidos.setText(modeloTabla.getValueAt(fila, 3).toString());
                    txtEmail.setText(modeloTabla.getValueAt(fila, 4).toString());
                    txtCelular.setText(modeloTabla.getValueAt(fila, 5).toString());
                    
                    char sexo = modeloTabla.getValueAt(fila, 6).toString().charAt(0);
                    cmbSexo.setSelectedItem(sexo == 'M' ? "Masculino" : "Femenino");
                    
                    txtMetodoPago.setText(modeloTabla.getValueAt(fila, 7).toString());
                    txtNumeroCuenta.setText(modeloTabla.getValueAt(fila, 8).toString());
                    txtBanco.setText(modeloTabla.getValueAt(fila, 9).toString());
                }
            }
        });
    }

    private void guardarPropietario() {
        try {
            char sexo = cmbSexo.getSelectedItem().toString().equals("Masculino") ? 'M' : 'F';
            Propietario propietario = new Propietario(
                null,
                txtDni.getText(),
                txtNombres.getText(),
                txtApellidos.getText(),
                txtEmail.getText(),
                txtCelular.getText(),
                sexo,
                txtMetodoPago.getText(),
                txtNumeroCuenta.getText(),
                txtBanco.getText()
            );
            servicio.guardar(propietario);
            cargarTabla();
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Propietario guardado exitosamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarPropietario() {
        String id = txtId.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un propietario de la tabla para modificar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            char sexo = cmbSexo.getSelectedItem().toString().equals("Masculino") ? 'M' : 'F';
            Propietario propietario = new Propietario(
                id,
                txtDni.getText(),
                txtNombres.getText(),
                txtApellidos.getText(),
                txtEmail.getText(),
                txtCelular.getText(),
                sexo,
                txtMetodoPago.getText(),
                txtNumeroCuenta.getText(),
                txtBanco.getText()
            );
            boolean modificado = servicio.modificar(propietario);
            if (modificado) {
                cargarTabla();
                limpiarCampos();
                JOptionPane.showMessageDialog(this, "Propietario modificado correctamente.");
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el propietario.", "Error", JOptionPane.ERROR_MESSAGE);
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
        txtMetodoPago.setText("");
        txtNumeroCuenta.setText("");
        txtBanco.setText("");
        tablaPropietarios.clearSelection();
    }

    private void cargarTabla() {
        modeloTabla.setRowCount(0);
        List<Propietario> lista = servicio.listar();
        for (Propietario p : lista) {
            modeloTabla.addRow(new Object[]{
                p.getId(), p.getDni(), p.getNombres(), p.getApellidos(),
                p.getEmail(), p.getCelular(), p.getSexo(), 
                p.getMetodoPagoPreferido(), p.getNumeroCuenta(), p.getBanco()
            });
        }
    }
}