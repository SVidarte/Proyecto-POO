package modelo;

public class Propietario extends Persona {
    private String metodoPagoPreferido;
    private String numeroCuenta;
    private String banco;

    public Propietario() {
        super();
    }

    public Propietario(String id, String dni, String nombres, String apellidos, String email, String celular, char sexo, 
                       String metodoPagoPreferido, String numeroCuenta, String banco) {
        super(id, dni, nombres, apellidos, email, celular, sexo);
        this.metodoPagoPreferido = metodoPagoPreferido;
        this.numeroCuenta = numeroCuenta;
        this.banco = banco;
    }

    public String getMetodoPagoPreferido() { return metodoPagoPreferido; }
    public void setMetodoPagoPreferido(String metodoPagoPreferido) { this.metodoPagoPreferido = metodoPagoPreferido; }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
}