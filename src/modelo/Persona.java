package modelo;

public abstract class Persona {
    protected String id;
    protected String dni;
    protected String nombres;
    protected String apellidos;
    protected String email;
    protected String celular;
    protected char sexo; // 'M' o 'F'

    public Persona() {}

    public Persona(String id, String dni, String nombres, String apellidos, String email, String celular, char sexo) {
        this.id = id;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.celular = celular;
        this.sexo = sexo;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public char getSexo() { return sexo; }
    public void setSexo(char sexo) { this.sexo = sexo; }

    /**
     * Línea que se guarda en el archivo CSV. Cada tipo de persona define qué campos guarda
     * (polimorfismo: quien la llama solo conoce a Persona).
     */
    public abstract String toCSV();
}