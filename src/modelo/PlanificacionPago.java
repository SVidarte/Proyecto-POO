package modelo;

import java.io.File;
import java.io.IOException;

public class PlanificacionPago {
    public static final String ARCHIVO = "planificaciones_pago.csv";
    public static final String ARCHIVO_REALIZADOS = "planificacion_pagos_realizados.csv";

    private String id;
    private String idAlquiler;
    private String fechaPago;
    private String monto;
    private String monto_pagado;

    public PlanificacionPago() {}

    public PlanificacionPago(String id, String idAlquiler, String fechaPago, String monto, String monto_pagado) {
        this.id = id;
        this.idAlquiler = idAlquiler;
        this.fechaPago = fechaPago;
        this.monto = monto;
        this.monto_pagado = monto_pagado;
    }

    /**
     * Crea automáticamente planificaciones_pago.csv y planificacion_pagos_realizados.csv (vacíos)
     * si todavía no existen. No llevan encabezado porque ServicioPlanificacionPago lee todas
     * las líneas de 5 columnas como datos.
     */
    public static void crearArchivoSiNoExiste() {
        crearSiNoExiste(ARCHIVO);
        crearSiNoExiste(ARCHIVO_REALIZADOS);
    }

    private static void crearSiNoExiste(String ruta) {
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            try {
                archivo.createNewFile();
            } catch (IOException e) {
                System.err.println("Error al crear " + ruta + ": " + e.getMessage());
            }
        }
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdAlquiler() { return idAlquiler; }
    public void setIdAlquiler(String idAlquiler) { this.idAlquiler = idAlquiler; }

    public String getFechaPago() { return fechaPago; }
    public void setFechaPago(String fechaPago) { this.fechaPago = fechaPago; }

    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }

    public String getMonto_pagado() { return monto_pagado; }
    public void setMonto_pagado(String monto_pagado) { this.monto_pagado = monto_pagado; }

    /**
     * Verificador: true si monto_pagado y monto son iguales (comparados en centavos),
     * false en caso contrario o si alguno no es un número válido.
     * No se guarda en el CSV: se calcula cada vez que se consulta.
     */
    public boolean isVerificador() {
        try {
            double total = Double.parseDouble(monto);
            double pagado = Double.parseDouble(monto_pagado);
            return Math.round(total * 100) == Math.round(pagado * 100);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    public double calcularSaldoPendiente() {
        try {
            double total = Double.parseDouble(monto);
            double pagado = Double.parseDouble(monto_pagado);
            return total - pagado;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
