package modelo;

public class Departamento implements Mantenible {

    public static final String DISPONIBLE = "Disponible";
    public static final String OCUPADO = "Ocupado";

    private String id_departamento;   // lo genera ServicioDepartamento: DP0001, DP0002, ...
    private String id_propietario;    // id del Propietario al que pertenece el departamento (P0001, ...)
    private int piso;
    private String numero_departamento;
    private String direccion;
    private double precio_mensual;
    private String estado;

    /**
     * @param id_departamento puede ser null o vacío al crear un departamento nuevo:
     *                        ServicioDepartamento le asigna el id automáticamente.
     */
    public Departamento(String id_departamento, String id_propietario, int piso, String numero_departamento,
                        String direccion, double precio_mensual, String estado) {
        if (id_departamento != null && !id_departamento.trim().isEmpty()) {
            setId_departamento(id_departamento);
        }
        setId_propietario(id_propietario);
        setPiso(piso);
        setNumero_departamento(numero_departamento);
        setDireccion(direccion);
        setPrecio_mensual(precio_mensual);
        setEstado(estado);
    }

    public String getId_departamento() {
        return id_departamento;
    }

    public void setId_departamento(String id_departamento) {
        if (id_departamento != null && !id_departamento.trim().isEmpty())
            this.id_departamento = id_departamento.trim().toUpperCase();
        else
            throw new DatosInvalidosException("El departamento no puede estar vacio");
    }

    public String getId_propietario() {
        return id_propietario;
    }

    public void setId_propietario(String id_propietario) {
        if (id_propietario != null && !id_propietario.trim().isEmpty())
            this.id_propietario = id_propietario.trim().toUpperCase();
        else
            throw new DatosInvalidosException("El departamento debe pertenecer a un propietario");
    }

    public int getPiso() {
        return piso;
    }

    public void setPiso(int piso) {
        if (piso >= 1 && piso <= 50)
            this.piso = piso;
        else
            throw new DatosInvalidosException("Piso inexistente");
    }

    public String getNumero_departamento() {
        return numero_departamento;
    }

    public void setNumero_departamento(String numero_departamento) {
        if (numero_departamento == null || numero_departamento.trim().isEmpty())
            throw new DatosInvalidosException("El numero de departamento no puede estar vacio");
        if (numero_departamento.contains(";"))
            throw new DatosInvalidosException("El numero de departamento no puede contener el caracter \";\"");
        this.numero_departamento = numero_departamento.trim().toUpperCase();
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        if (direccion == null || direccion.trim().isEmpty())
            throw new DatosInvalidosException("La direccion no puede estar vacia");
        if (direccion.contains(";"))
            throw new DatosInvalidosException("La direccion no puede contener el caracter \";\"");
        this.direccion = direccion.trim();
    }

    public double getPrecio_mensual() {
        return precio_mensual;
    }

    public void setPrecio_mensual(double precio_mensual) {
        if (precio_mensual < 0) {
            throw new DatosInvalidosException("El precio no puede ser negativo");
        } else
            this.precio_mensual = precio_mensual;
    }

    public String getEstado() {
        return estado;
    }

    /** Acepta "disponible" u "ocupado" en cualquier combinación de mayúsculas y lo guarda como Disponible / Ocupado. */
    public void setEstado(String estado) {
        if (estado != null && estado.trim().equalsIgnoreCase(DISPONIBLE)) {
            this.estado = DISPONIBLE;
        } else if (estado != null && estado.trim().equalsIgnoreCase(OCUPADO)) {
            this.estado = OCUPADO;
        } else
            throw new DatosInvalidosException("Estado invalido");
    }

    @Override
    public void actualizar_estado() {
        if (estado.equals(DISPONIBLE)) {
            setEstado(OCUPADO);
        } else {
            setEstado(DISPONIBLE);
        }
    }
}
