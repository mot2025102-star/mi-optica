package com.mioptica.repository;

import com.mioptica.model.FichaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FichaClinicaRepository extends JpaRepository<FichaClinica, Integer> {

    // Fichas con saldo pendiente > 0
    @Query("SELECT f FROM FichaClinica f " +
           "JOIN FETCH f.cliente " +
           "JOIN FETCH f.sucursal " +
           "WHERE f.saldo > 0 " +
           "ORDER BY f.saldo DESC, f.fecha ASC")
    List<FichaClinica> findConSaldoPendiente();

    // Fichas con saldo pendiente de una sucursal específica
    @Query("SELECT f FROM FichaClinica f " +
           "JOIN FETCH f.cliente " +
           "JOIN FETCH f.sucursal " +
           "WHERE f.saldo > 0 AND f.sucursal.idSucursal = :idSuc " +
           "ORDER BY f.saldo DESC, f.fecha ASC")
    List<FichaClinica> findConSaldoPendienteBySucursal(@Param("idSuc") Integer idSuc);

    // Entregas pendientes (no entregadas y con fecha de entrega asignada)
    @Query("SELECT f FROM FichaClinica f " +
           "JOIN FETCH f.cliente " +
           "JOIN FETCH f.sucursal " +
           "WHERE (f.estadoEntrega IS NULL OR f.estadoEntrega <> 'Entregado') " +
           "AND f.fechaEntrega IS NOT NULL " +
           "ORDER BY f.fechaEntrega ASC")
    List<FichaClinica> findEntregasPendientes();

    // Entregas pendientes por sucursal
    @Query("SELECT f FROM FichaClinica f " +
           "JOIN FETCH f.cliente " +
           "JOIN FETCH f.sucursal " +
           "WHERE (f.estadoEntrega IS NULL OR f.estadoEntrega <> 'Entregado') " +
           "AND f.fechaEntrega IS NOT NULL " +
           "AND f.sucursal.idSucursal = :idSuc " +
           "ORDER BY f.fechaEntrega ASC")
    List<FichaClinica> findEntregasPendientesBySucursal(@Param("idSuc") Integer idSuc);

    // Historial de fichas de un cliente
    List<FichaClinica> findByClienteIdClienteOrderByFechaDesc(Integer idCliente);
}
