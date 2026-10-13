package com.mioptica.repository;

import com.mioptica.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop20ByOrderByCreadaDesc();
    long countByLeidaFalse();
    boolean existsByTipoAndTituloAndMensajeAndLeidaFalse(String tipo, String titulo, String mensaje);
}
