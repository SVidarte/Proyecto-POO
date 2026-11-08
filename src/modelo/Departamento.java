package modelo;
public class Departamento implements Mantenible {
    private String id_departamento;
    private int piso;
    private String numero_departamento;
    private String direccion;
    private double precio_mensual;
    private String estado;

    public Departamento(String id_departamento, int piso, String numero_departamento, String direccion, double precio_mensual, String estado) {
        setId_departamento(id_departamento);
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
        if(id_departamento!=null && !id_departamento.trim().isEmpty())
        this.id_departamento = id_departamento.trim().toUpperCase();
        else
            throw new IllegalArgumentException("El departamento no puede estar vacio");
    }

    public int getPiso() {
        return piso;
    }

    public void setPiso(int piso) {
        if(piso>=1 && piso<=5)
        this.piso = piso;
        else
            throw new IllegalArgumentException("Piso inexistente");
    }

    public String getNumero_departamento() {
        return numero_departamento;
    }

    public void setNumero_departamento(String numero_departamento) {
        if(numero_departamento!=null && !numero_departamento.trim().isEmpty())
        this.numero_departamento = numero_departamento.trim().toUpperCase();
        else
            throw new IllegalArgumentException("El numero de departamento no puede estar vacio");
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        if(direccion!=null && !direccion.trim().isEmpty())
        this.direccion = direccion.trim();
        else
            throw new IllegalArgumentException("La direccion no puede estar vacia");
    }

    
    public double getPrecio_mensual() {
        return precio_mensual;
    }

    public void setPrecio_mensual(double precio_mensual) {
        if(precio_mensual<0){
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        else
        this.precio_mensual = precio_mensual;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        if(estado!=null &&(estado.toUpperCase().equals("DISPONIBLE") || estado.toUpperCase().equals("OCUPADO"))){
            this.estado=estado.toUpperCase();
        }
        
        else
            throw new IllegalArgumentException("Estado invalido");
    }
    @Override
    public void actualizar_estado(){
        if(estado.equals("DISPONIBLE")){
            setEstado("OCUPADO");
        }
        else{
            setEstado("DISPONIBLE");
        }
    }
}
