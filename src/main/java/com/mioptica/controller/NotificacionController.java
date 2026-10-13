package com.mioptica.controller;

import com.mioptica.model.Notificacion;
import com.mioptica.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionRepository repo;

    @GetMapping
    public Map<String, Object> listar() {
        return Map.of(
            "noLeidas", repo.countByLeidaFalse(),
            "items", repo.findTop20ByOrderByCreadaDesc()
        );
    }

    @PostMapping("/{id}/leer")
    public void marcarLeida(@PathVariable Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setLeida(true);
            repo.save(n);
        });
    }

    @PostMapping("/leer-todas")
    public void marcarTodas() {
        repo.findAll().forEach(n -> {
            if (!n.isLeida()) {
                n.setLeida(true);
                repo.save(n);
            }
        });
    }
}
