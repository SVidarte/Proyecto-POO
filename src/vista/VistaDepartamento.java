package vista;

import modelo.Departamento;
import modelo.Propietario;
import servicio.ServicioDepartamento;
import servicio.ServicioPropietario;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VistaDepartamento extends JFrame {
    private ServicioDepartamento servicio;
    private JTextField txtId;
    private JTextField txtPiso;
    private JTextField txtNumeroDepartamento;
    private JTextField txtDireccion;
    private JTextField txtPrecio;
    private JTextField txtBuscarDireccion;
    private JComboBox<String> cmbEstado;
    private JComboBox<String> cmbPropietario;
    private JTable tablaDepartamentos;
    private DefaultTableModel modeloTabla;
    private JButton btnGuardar;
    private JButton btnEliminar;
    private JButton btnBuscar;
    private JButton btnMostrarTodos;
    private JButton btnLimpiar;
    private JButton btnModificar;

    public VistaDepartamento(){
        servicio = new ServicioDepartamento();
        inicializarComponentes();
        cargarPropietarios();
        cargarTabla();
    }
    private void inicializarComponentes(){
        setTitle("Gestión de Departamentos");
        setSize(850, 640);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);
        JLabel lblId = new JLabel("ID:");
        JLabel lblPiso = new JLabel("Piso:");
        JLabel lblNumero = new JLabel("Número:");
        JLabel lblDireccion = new JLabel("Dirección:");
        JLabel lblPrecio = new JLabel("Precio mensual:");
        JLabel lblEstado = new JLabel("Estado:");
        JLabel lblPropietario = new JLabel("Propietario:");
        txtId = new JTextField();
        txtId.setEditable(false); // lo genera el servicio: DP0001, DP0002, ...
        txtPiso = new JTextField();
        txtNumeroDepartamento = new JTextField();
        txtDireccion = new JTextField();
        txtPrecio = new JTextField();
        cmbEstado = new JComboBox<>(new String[]{Departamento.DISPONIBLE, Departamento.OCUPADO});
        cmbPropietario = new JComboBox<>();
        lblId.setBounds(30, 30, 100, 25);
        txtId.setBounds(140, 30, 180, 25);

        lblPiso.setBounds(350, 30, 100, 25);
        txtPiso.setBounds(460, 30, 100, 25);

        lblNumero.setBounds(30, 70, 100, 25);
        txtNumeroDepartamento.setBounds(140, 70, 180, 25);

        lblDireccion.setBounds(350, 70, 100, 25);
        txtDireccion.setBounds(460, 70, 300, 25);

        lblPrecio.setBounds(30, 110, 110, 25);
        txtPrecio.setBounds(140, 110, 180, 25);

        lblEstado.setBounds(350, 110, 100, 25);
        cmbEstado.setBounds(460, 110, 150, 25);

        lblPropietario.setBounds(30, 150, 100, 25);
        cmbPropietario.setBounds(140, 150, 300, 25);
        add(lblId);
        add(txtId);
        add(lblPiso);
        add(txtPiso);
        add(lblNumero);
        add(txtNumeroDepartamento);
        add(lblDireccion);
        add(txtDireccion);
        add(lblPrecio);
        add(txtPrecio);
        add(lblEstado);
        add(cmbEstado);
        add(lblPropietario);
        add(cmbPropietario);

        btnGuardar = new JButton("Guardar");
        btnEliminar = new JButton("Eliminar");
        btnLimpiar = new JButton("Limpiar");
        btnModificar = new JButton("Modificar");

        btnGuardar.setBounds(30, 200, 120, 30);
        btnEliminar.setBounds(170, 200, 120, 30);
        btnLimpiar.setBounds(310, 200, 120, 30);
        btnModificar.setBounds(580, 200 , 120, 30);

        add(btnGuardar);
        add(btnEliminar);
        add(btnLimpiar);
        add(btnModificar);
        btnGuardar.addActionListener(e -> guardarDepartamento());
        btnEliminar.addActionListener(e -> eliminarDepartamento());
        btnLimpiar.addActionListener(e -> limpiarCampos());
        btnModificar.addActionListener(e -> modificarDepartamento());

        JLabel lblBuscar = new JLabel("Buscar dirección:");
        lblBuscar.setBounds(30, 255, 120, 25);

        txtBuscarDireccion = new JTextField();
        txtBuscarDireccion.setBounds(150, 255, 250, 25);
        btnBuscar = new JButton("Buscar");
        btnBuscar.setBounds(420, 255, 100, 25);
        btnMostrarTodos = new JButton("Mostrar todos");
        btnMostrarTodos.setBounds(530, 255, 130, 25);
        add(lblBuscar);
        add(txtBuscarDireccion);
        add(btnBuscar);
        add(btnMostrarTodos);
        btnBuscar.addActionListener(e -> buscarDepartamento());
        btnMostrarTodos.addActionListener(e -> mostrarTodos());
        modeloTabla = new DefaultTableModel(
                new Object[]{"ID", "ID Propietario", "Piso", "Número", "Dirección", "Precio", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tablaDepartamentos = new JTable(modeloTabla);

        tablaDepartamentos.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
           int fila = tablaDepartamentos.getSelectedRow();

           if (fila != -1) {
              txtId.setText(modeloTabla.getValueAt(fila, 0).toString());
              seleccionarPropietario(modeloTabla.getValueAt(fila, 1).toString());
              txtPiso.setText(modeloTabla.getValueAt(fila, 2).toString());
              txtNumeroDepartamento.setText(modeloTabla.getValueAt(fila, 3).toString());
              txtDireccion.setText(modeloTabla.getValueAt(fila, 4).toString());
              txtPrecio.setText(modeloTabla.getValueAt(fila, 5).toString());
              cmbEstado.setSelectedItem(modeloTabla.getValueAt(fila, 6).toString());
            }
        }
    });

        // Si se registra un propietario en otra ventana, la lista se actualiza al volver a esta
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                cargarPropietarios();
            }
        });

        JScrollPane scroll = new JScrollPane(tablaDepartamentos);
        scroll.setBounds(30, 300, 770, 250);

        add(scroll);
    }

    /** Llena el combo con los propietarios registrados ("P0001 - Nombres Apellidos"), conservando la selección. */
    private void cargarPropietarios() {
        String seleccionado = idPropietarioSeleccionado();
        cmbPropietario.removeAllItems();
        for (Propietario p : new ServicioPropietario().listar()) {
            cmbPropietario.addItem(p.getId() + " - " + p.getNombres() + " " + p.getApellidos());
        }
        if (seleccionado != null) {
            seleccionarPropietario(seleccionado);
        }
    }

    /** Devuelve el id del propietario elegido en el combo, o null si no hay ninguno. */
    private String idPropietarioSeleccionado() {
        Object item = cmbPropietario.getSelectedItem();
        if (item == null) {
            return null;
        }
        String texto = item.toString();
        int corte = texto.indexOf(" - ");
        return corte >= 0 ? texto.substring(0, corte) : texto;
    }

    private void seleccionarPropietario(String idPropietario) {
        for (int i = 0; i < cmbPropietario.getItemCount(); i++) {
            if (cmbPropietario.getItemAt(i).startsWith(idPropietario + " - ")) {
                cmbPropietario.setSelectedIndex(i);
                return;
            }
        }
    }

    private void agregarFila(Departamento d) {
        modeloTabla.addRow(new Object[]{d.getId_departamento(), d.getId_propietario(), d.getPiso(),
            d.getNumero_departamento(), d.getDireccion(), d.getPrecio_mensual(), d.getEstado()});
    }

    private void cargarTabla(){
        modeloTabla.setRowCount(0);
        List<Departamento> lista = servicio.listarDepartamento();
        for(Departamento d : lista){
            agregarFila(d);
        }
    }

    private void guardarDepartamento() {
    try {
        String idPropietario = idPropietarioSeleccionado();
        if (idPropietario == null) {
            JOptionPane.showMessageDialog(this,
                "No hay propietarios registrados. Registre un propietario primero.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int piso = Integer.parseInt(txtPiso.getText().trim());
        String numero = txtNumeroDepartamento.getText();
        String direccion = txtDireccion.getText();
        double precio = Double.parseDouble(txtPrecio.getText().trim().replace(',', '.'));
        String estado = cmbEstado.getSelectedItem().toString();

        // El id se envía null: lo genera ServicioDepartamento (DP0001, DP0002, ...)
        Departamento d = new Departamento(null, idPropietario, piso, numero, direccion, precio, estado);

        String idGenerado = servicio.agregarDepartamento(d);

        cargarTabla();
        limpiarCampos();
        JOptionPane.showMessageDialog(this,
              "Departamento guardado con ID " + idGenerado + ".");

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this,
            "El piso y el precio deben ser valores numéricos.",
            "Error", JOptionPane.ERROR_MESSAGE);

    } catch (IllegalArgumentException | IllegalStateException e) {
        JOptionPane.showMessageDialog(this,
            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
    private void limpiarCampos() {
    txtId.setText("");
    txtPiso.setText("");
    txtNumeroDepartamento.setText("");
    txtDireccion.setText("");
    txtPrecio.setText("");
    cmbEstado.setSelectedIndex(0);
    tablaDepartamentos.clearSelection();
}

    private void eliminarDepartamento() {

    int filaSeleccionada = tablaDepartamentos.getSelectedRow();

    if (filaSeleccionada == -1) {
        JOptionPane.showMessageDialog(this,
                "Seleccione un departamento de la tabla.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        return;
    }

    int respuesta = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de eliminar este departamento?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION
    );

    if (respuesta == JOptionPane.YES_OPTION) {

        String id = modeloTabla.getValueAt(filaSeleccionada, 0).toString();

        try {
            if (servicio.eliminarDepartamento(id)) {
                JOptionPane.showMessageDialog(this, "Departamento eliminado correctamente.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "El departamento no existe.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this,
                    e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        cargarTabla();
        limpiarCampos();
    }
}
    private void buscarDepartamento(){
        String direccion = txtBuscarDireccion.getText().trim();

        if (direccion.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "Ingrese una dirección para buscar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        return;
     }

    List<Departamento> lista = servicio.buscarPorDireccion(direccion);

    modeloTabla.setRowCount(0);

    for (Departamento d : lista) {
        agregarFila(d);
     }

    if (lista.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "No se encontraron departamentos en esa dirección.");
       }
    }
    private void modificarDepartamento() {

       try {

           String id = txtId.getText().trim();

           if (id.isEmpty()) {

               JOptionPane.showMessageDialog(this,
                    "Seleccione un departamento de la tabla.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String idPropietario = idPropietarioSeleccionado();
            if (idPropietario == null) {
                JOptionPane.showMessageDialog(this,
                        "No hay propietarios registrados. Registre un propietario primero.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int piso = Integer.parseInt(txtPiso.getText().trim());
            String numero = txtNumeroDepartamento.getText();
            String direccion = txtDireccion.getText();
            double precio = Double.parseDouble(txtPrecio.getText().trim().replace(',', '.'));
            String estado = cmbEstado.getSelectedItem().toString();

            Departamento d = new Departamento(id, idPropietario, piso, numero, direccion, precio, estado);

            boolean modificado = servicio.modificarDepartamento(d);

           if (modificado) {
               cargarTabla();
               limpiarCampos();

               JOptionPane.showMessageDialog(this,
                      "Departamento modificado correctamente.");
           } else {

               JOptionPane.showMessageDialog(this,

                       "No se encontró el departamento.",
                       "Aviso", JOptionPane.WARNING_MESSAGE);
           }

      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this,
                "El piso y el precio deben ser valores numéricos.",
                "Error", JOptionPane.ERROR_MESSAGE);
    } catch (IllegalArgumentException | IllegalStateException e) {
        JOptionPane.showMessageDialog(this,
                e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
    private void mostrarTodos() {
    txtBuscarDireccion.setText("");
    cargarTabla();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VistaDepartamento().setVisible(true));
    }
}
