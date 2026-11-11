package vista;

import modelo.Cliente;
import servicio.ServicioCliente;
import servicio.ValidadorPersona;

import javax.swing.*;
import java.awt.*;

/**
 * Formulario que se muestra la primera vez que un cliente inicia sesión, para registrar sus datos.
 * El id (C0001, C0002, ...) lo genera ServicioCliente.
 */
public class DialogoPerfilCliente extends JDialog {

    private String idCreado = null;
    private final Cliente existente; // null = primer registro; con datos = "Mis datos" (modificar)

    private final JTextField txtDni = new JTextField();
    private final JTextField txtNombres = new JTextField();
    private final JTextField txtApellidos = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtCelular = new JTextField();
    private final JComboBox<String> cmbSexo = new JComboBox<>(new String[]{"Masculino", "Femenino"});
    private final JComboBox<String> cmbReferencia = new JComboBox<>(new String[]{"Redes sociales", "Amigos/Familiares"});

    public DialogoPerfilCliente(Frame padre) {
        this(padre, null);
    }

    public DialogoPerfilCliente(Frame padre, Cliente existente) {
        super(padre, existente == null ? "Registre sus datos de cliente" : "Mis datos", true);
        this.existente = existente;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // DNI: solo 8 números. Celular: solo 9 números.
        FiltroNumerico.aplicar(txtDni, ValidadorPersona.LONGITUD_DNI);
        FiltroNumerico.aplicarCelular(txtCelular, ValidadorPersona.LONGITUD_CELULAR);

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
        agregar(formulario, "Referencia:", cmbReferencia);

        if (existente != null) {
            txtDni.setText(existente.getDni());
            txtNombres.setText(existente.getNombres());
            txtApellidos.setText(existente.getApellidos());
            txtEmail.setText(existente.getEmail());
            txtCelular.setText(existente.getCelular());
            cmbSexo.setSelectedItem(existente.getSexo() == 'M' ? "Masculino" : "Femenino");
            cmbReferencia.setSelectedItem(existente.getReferencia());
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

    private void agregar(JPanel panel, String etiqueta, JComponent campo) {
        panel.add(new JLabel(etiqueta));
        panel.add(campo);
    }

    private void guardar() {
        String[] datos = {txtDni.getText(), txtNombres.getText(), txtApellidos.getText(),
            txtEmail.getText(), txtCelular.getText()};
        for (String valor : datos) {
            if (valor.trim().isEmpty()) {
                error("Complete todos los datos.");
                return;
            }
            if (valor.contains(";")) {
                error("Los datos no pueden contener el carácter \";\".");
                return;
            }
        }
        if (txtDni.getText().trim().length() != ValidadorPersona.LONGITUD_DNI) {
            error("El DNI debe tener exactamente " + ValidadorPersona.LONGITUD_DNI + " dígitos.");
            return;
        }
        if (txtCelular.getText().trim().length() != ValidadorPersona.LONGITUD_CELULAR) {
            error("El celular debe tener exactamente " + ValidadorPersona.LONGITUD_CELULAR + " dígitos.");
            return;
        }
        try {
            char sexo = "Masculino".equals(cmbSexo.getSelectedItem()) ? 'M' : 'F';
            Cliente cliente = new Cliente(existente == null ? null : existente.getId(), txtDni.getText().trim(), txtNombres.getText().trim(),
                    txtApellidos.getText().trim(), txtEmail.getText().trim(), txtCelular.getText().trim(), sexo,
                    cmbReferencia.getSelectedItem().toString());
            if (existente == null) {
                new ServicioCliente().guardar(cliente);
                JOptionPane.showMessageDialog(this, "Datos guardados. Su id es " + cliente.getId() + ".");
            } else {
                new ServicioCliente().modificar(cliente);
                JOptionPane.showMessageDialog(this, "Sus datos se actualizaron.");
            }
            idCreado = cliente.getId();
            dispose();
        } catch (RuntimeException ex) {
            error("No se pudo guardar: " + ex.getMessage());
        }
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Id del cliente creado, o null si se canceló. */
    public String getIdCreado() {
        return idCreado;
    }
}
