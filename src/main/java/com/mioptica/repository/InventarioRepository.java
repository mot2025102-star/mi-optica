package com.mioptica.repository;

import com.mioptica.model.Inventario;
import com.mioptica.model.Producto;
import com.mioptica.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    Optional<Inventario> findByProductoAndSucursal(Producto producto, Sucursal sucursal);

    List<Inventario> findBySucursal(Sucursal sucursal);

    List<Inventario> findBySucursalIdSucursal(Integer idSucursal);
    /*List<Inventario>: Le indica al programa que el resultado que esperamos obtener será una lista con varios registros del inventario.
    findBy: Es una palabra clave. Cuando Spring Boot lee esto, automáticamente entiende: "Ah, quieres que haga un SELECT * FROM en la base de datos".
    SucursalIdSucursal: Aquí es donde ocurre la magia de Spring. El programa lee estas palabras juntas y traza la ruta: "Ve a la tabla de Inventario, busca la relación con la tabla Sucursal, y filtra los resultados usando la columna idSucursal".
    (Integer idSucursal): Es el número real que le vas a pasar. Por ejemplo, si le envías un 5, buscará todo lo de la sucursal 5. */

    @Query("SELECT i FROM Inventario i WHERE i.producto.activo = true ORDER BY i.existencia ASC")
    List<Inventario> findTodosConProductoActivo();

    @Query("SELECT i FROM Inventario i WHERE i.sucursal.idSucursal = :idSucursal AND i.existencia > 0 AND i.producto.activo = true ORDER BY i.producto.detalle ASC")
    List<Inventario> findConStockBySucursal(@Param("idSucursal") Integer idSucursal);

    @Query("SELECT i FROM Inventario i WHERE i.sucursal.idSucursal = :idSucursal AND i.existencia <= 2 AND i.producto.activo = true")
    List<Inventario> findStockBajoBySucursal(@Param("idSucursal") Integer idSucursal);
}
