package servicio;

import java.util.List;

/**
 * Contrato de las operaciones CRUD que ofrece un servicio de persistencia.
 *
 * @param <T> tipo de entidad que administra el servicio
 */
public interface ServicioCrud<T> {

    void guardar(T elemento);

    List<T> listar();

    T buscar(String id);

    /** @return true si encontró el elemento y lo modificó */
    boolean modificar(T elemento);

    /** @return true si encontró el elemento y lo eliminó */
    boolean eliminar(String id);
}
