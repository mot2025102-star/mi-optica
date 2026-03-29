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

    @Query("SELECT i FROM Inventario i WHERE i.producto.activo = true ORDER BY i.existencia ASC")
    List<Inventario> findTodosConProductoActivo();

    @Query("SELECT i FROM Inventario i WHERE i.sucursal.idSucursal = :idSucursal AND i.existencia > 0 AND i.producto.activo = true ORDER BY i.producto.detalle ASC")
    List<Inventario> findConStockBySucursal(@Param("idSucursal") Integer idSucursal);

    @Query("SELECT i FROM Inventario i WHERE i.sucursal.idSucursal = :idSucursal AND i.existencia <= 2 AND i.producto.activo = true")
    List<Inventario> findStockBajoBySucursal(@Param("idSucursal") Integer idSucursal);
}
