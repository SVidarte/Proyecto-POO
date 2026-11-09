package vista;

import javax.swing.table.DefaultTableModel;

/** Modelo de tabla de solo lectura (las celdas no se pueden editar). */
final class ModeloTabla extends DefaultTableModel {

    ModeloTabla(String[] columnas) {
        super(columnas, 0);
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }
}
