package proyectoalquileres;

import vista.*;

public class ProyectoAlquileres {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            new VistaCliente().setVisible(true);
            new VistaPropietario().setVisible(true);
        });
    }
    
}
