package modelo;

public class PlanificacionPago {
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