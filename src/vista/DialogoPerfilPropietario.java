package vista;

import modelo.Propietario;
import servicio.ServicioPropietario;

import javax.swing.*;
import java.awt.*;

/**
 * Formulario que se muestra la primera vez que un propietario inicia sesión, para registrar sus datos.
 * El id (P0001, P0002, ...) lo genera ServicioPropietario. Con "Efectivo", banco y número de cuenta
 * quedan bloqueados (se guardan como "-").
 */
public class DialogoPerfilPropietario extends JDialog {

    private String idCreado = null;
    private final Propietario existente; // null = primer registro; con datos = "Mis datos" (modificar)

    private final JTextField txtDni = new JTextField();
    private final JTextField txtNombres = new JTextField();
    private final JTextField txtApellidos = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtCelular = new JTextField();
    private final JComboBox<String> cmbSexo = new JComboBox<>(new String[]{"Masculino", "Femenino"});
    private final JComboBox<String> cmbMetodoPago = new JComboBox<>(new String[]{"Transferencia", "Efectivo"});
    private final JTextField txtBanco = new JTextField();
    private final JTextField txtNumeroCuenta = new JTextField();

    public DialogoPerfilPropietario(Frame padre) {
        this(padre, null);
    }

    public DialogoPerfilPropietario(Frame padre, Propietario existente) {
        super(padre, existente == null ? "Registre sus datos de propietario" : "Mis datos", true);
        this.existente = existente;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTextField txtId = new JTextField(existente == null ? "(automático)" : existente.getId());
        txtId.setEditable(false);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        agregar(formulario, "ID:", txtId);
        agregar(formulario, "DNI:", txtDni);
        agregar(formulario, "Nombres:", txtNombres);
        agregar(formulario, "Apellidos:", txtApellidos);
        agregar(formulario, "Email:", txtEmail);
        agregar(formulario, "Celular:", txtCelular);
        agregar(formulario, "Sexo:", cmbSexo);
        agregar(formulario, "Método de pago:", cmbMetodoPago);
        agregar(formulario, "Banco:", txtBanco);
        agregar(formulario, "Nro. de cuenta:", txtNumeroCuenta);

        cmbMetodoPago.addActionListener(e -> actualizarCamposBanco());
        actualizarCamposBanco();
        if (existente != null) {
            cargarDatos(existente);
        }

        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        botones.add(btnGuardar);
        botones.add(btnCancelar);

        add(formulario, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setSize(Math.max(getWidth(), 420), getHeight());
        setLocationRelativeTo(padre);
    }

    /** Muestra los datos actuales para poder modificarlos. */
    private void cargarDatos(Propietario p) {
        txtDni.setText(p.getDni());
        txtNombres.setText(p.getNombres());
        txtApellidos.setText(p.getApellidos());
        txtEmail.setText(p.getEmail());
        txtCelular.setText(p.getCelular());
        cmbSexo.setSelectedItem(p.getSexo() == 'M' ? "Masculino" : "Femenino");
        cmbMetodoPago.setSelectedItem(p.getMetodoPagoPreferido()); // activa o bloquea banco y cuenta
        if ("Transferencia".equals(cmbMetodoPago.getSelectedItem())) {
            txtBanco.setText(p.getBanco());
            txtNumeroCuenta.setText(p.getNumeroCuenta());
        }
    }

    private void agregar(JPanel panel, String etiqueta, JComponent campo) {
        panel.add(new JLabel(etiqueta));
        panel.add(campo);
    }

    /** Banco y número de cuenta solo se pueden escribir con Transferencia. */
    private void actualizarCamposBanco() {
        boolean transferencia = "Transferencia".equals(cmbMetodoPago.getSelectedItem());
        txtBanco.setEnabled(transferencia);
        txtNumeroCuenta.setEnabled(transferencia);
        if (!transferencia) {
            txtBanco.setText("");
            txtNumeroCuenta.setText("");
        }
    }

    private void guardar() {
        String metodo = cmbMetodoPago.getSelectedItem().toString();
        boolean transferencia = "Transferencia".equals(metodo);

        String[] obligatorios = {txtDni.getText(), txtNombres.getText(), txtApellidos.getText(),
            txtEmail.getText(), txtCelular.getText()};
        for (String valor : obligatorios) {
            if (valor.trim().isEmpty()) {
                error("Complete todos los datos.");
                return;
            }
        }
        if (transferencia && (txtBanco.getText().trim().isEmpty() || txtNumeroCuenta.getText().trim().isEmpty())) {
            error("Con transferencia debe indicar el banco y el número de cuenta.");
            return;
        }
        String[] todos = {txtDni.getText(), txtNombres.getText(), txtApellidos.getText(), txtEmail.getText(),
            txtCelular.getText(), txtBanco.getText(), txtNumeroCuenta.getText()};
        for (String valor : todos) {
            if (valor.contains(";")) {
                error("Los datos no pueden contener el carácter \";\".");
                return;
            }
        }
        try {
            char sexo = "Masculino".equals(cmbSexo.getSelectedItem()) ? 'M' : 'F';
            Propietario propietario = new Propietario(existente == null ? null : existente.getId(), txtDni.getText().trim(), txtNombres.getText().trim(),
                    txtApellidos.getText().trim(), txtEmail.getText().trim(), txtCelular.getText().trim(), sexo,
                    metodo,
                    transferencia ? txtBanco.getText().trim() : "-",
                    transferencia ? txtNumeroCuenta.getText().trim() : "-");
            if (existente == null) {
                new ServicioPropietario().guardar(propietario);
                JOptionPane.showMessageDialog(this, "Datos guardados. Su id es " + propietario.getId() + ".");
            } else {
                new ServicioPropietario().modificar(propietario);
                JOptionPane.showMessageDialog(this, "Sus datos se actualizaron.");
            }
            idCreado = propietario.getId();
            dispose();
        } catch (RuntimeException ex) {
            error("No se pudo guardar: " + ex.getMessage());
        }
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Id del propietario creado, o null si se canceló. */
    public String getIdCreado() {
        return idCreado;
    }
}