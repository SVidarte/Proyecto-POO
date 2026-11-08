package servicio;
import modelo.Departamento;
import java.io.*;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class ServicioDepartamento {
    private final String archivo = "departamentos.txt";
    public boolean agregarDepartamento(Departamento d) {
    try {
        FileReader fr = new FileReader(archivo);
        BufferedReader br = new BufferedReader(fr);

        String linea;
        boolean existe = false;

        while ((linea = br.readLine()) != null) {
            if (linea.trim().isEmpty()) {
                continue;
            }

            String[] datos = linea.split(",");

            if (datos.length >= 6 &&
                datos[0].trim().equalsIgnoreCase(d.getId_departamento())) {
                existe = true;
                break;
            }
        }

        br.close();

        if (existe) {
            return false;
        }

        FileWriter fw = new FileWriter(archivo, true);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(d.getId_departamento() + ","
                + d.getPiso() + ","
                + d.getNumero_departamento() + ","
                + d.getDireccion() + ","
                + d.getPrecio_mensual() + ","
                + d.getEstado());

        bw.newLine();
        bw.close();

        return true;

    } catch (IOException e) {
        throw new IllegalStateException(
                "Error al guardar el departamento: " + e.getMessage());
    }
}
    public void eliminarDepartamento(String id){
        try{
            FileReader fr = new FileReader(archivo);
            BufferedReader br = new BufferedReader(fr);
            String linea;
            boolean encontrada = false;
            List<String> departamentos = new ArrayList<>();
            while((linea = br.readLine())!=null){
                String[]datos = linea.split(",");
                if(!datos[0].trim().equalsIgnoreCase(id)){
                    departamentos.add(linea);
                }
                else{
                    encontrada = true;
                }
            }
            br.close();
            if(encontrada){
                FileWriter fw = new FileWriter(archivo);
                BufferedWriter bw = new BufferedWriter(fw);
                for(String departamento : departamentos){
                    bw.write(departamento);
                    bw.newLine();
                }
                bw.close();
                fw.close();
            }
            else{
                System.out.println("El departamento no existe");
            }
        }catch(IOException e){
            System.out.println("Error al leer el archivo: " + e.getMessage());
        }
    }
    public List<Departamento> listarDepartamento(){
        List<Departamento> departamentos = new ArrayList<>();
        try{
           FileReader fr = new FileReader(archivo); 
           BufferedReader br = new BufferedReader(fr);
           String linea;
           while((linea=br.readLine())!=null){
               String[]datos = linea.split(",");
               int piso = Integer.parseInt(datos[1]);
               double precio = Double.parseDouble(datos[4]);
               Departamento h = new Departamento(datos[0],piso,datos[2],datos[3],precio,datos[5]);
               departamentos.add(h);
           }
           br.close();
        }catch(NumberFormatException e){
            System.out.println("Error: Hay numeros errados en el archivo");
        }catch(IOException e){
            System.out.println("Error al listar departamentos: " + e.getMessage());
        }
        return departamentos;
    }
    public List<Departamento> buscarPorDireccion(String direccion){
        List<Departamento> departamentos = new ArrayList<>();
        try{
            FileReader fr = new FileReader(archivo); 
            BufferedReader br = new BufferedReader(fr);
            String linea;
            while((linea=br.readLine())!=null){
                String[]datos = linea.split(",");
                if(datos[3].replaceAll("\\s+", "").equalsIgnoreCase(direccion.replaceAll("\\s+", ""))){
                    int piso = Integer.parseInt(datos[1]);
                    double precio = Double.parseDouble(datos[4]);
                    Departamento h = new Departamento(datos[0],piso,datos[2],datos[3],precio,datos[5]);
                    departamentos.add(h);
                }
            }
            br.close();
        }catch(NumberFormatException e){
            System.out.println("Error: Hay datos numeros invalidos en el archivo");
        }catch(IOException e){
            System.out.println("Error al buscar departamentos: " + e.getMessage());
        }
        return departamentos;
    }
    public boolean modificarDepartamento(Departamento d){
        List<String> departamentos = new ArrayList<>();
        boolean encontrado = false;
        try{
            FileReader fr = new FileReader(archivo);
            BufferedReader br = new BufferedReader(fr);

            String linea;
            while((linea=br.readLine())!=null){
                if(linea.trim().isEmpty()){
                    continue;
                }
                String[]datos=linea.split(",");
                if (datos.length >= 6 && datos[0].trim().equalsIgnoreCase(d.getId_departamento())){
                    linea = d.getId_departamento() + "," + d.getPiso() + "," + d.getNumero_departamento() + "," + d.getDireccion() + "," + d.getPrecio_mensual() + "," + d.getEstado();
                    encontrado = true;
                }
                departamentos.add(linea);
            }
            br.close();
            if(encontrado){
                FileWriter fw = new FileWriter(archivo);
                BufferedWriter bw = new BufferedWriter(fw);
                for(String departamento : departamentos){
                    bw.write(departamento);
                    bw.newLine();
                }
                bw.close();
            }
        }catch (IOException e){
            System.out.println("Error al modificar el departamento: " + e.getMessage());
            return false;
        }
        return encontrado;
    }
}


