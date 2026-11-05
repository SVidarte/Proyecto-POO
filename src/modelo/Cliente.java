package modelo;

public class Cliente extends Persona {
    private String referencia;

    public Cliente() {
        super();
    }

    public Cliente(String id, String dni, String nombres, String apellidos, String email, String celular, char sexo, String referencia) {
        super(id, dni, nombres, apellidos, email, celular, sexo);
        this.referencia = referencia;
    }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}