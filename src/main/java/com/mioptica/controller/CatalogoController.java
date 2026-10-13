package com.mioptica.controller;

import com.mioptica.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/productos/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/{tipo}/lista")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listar(@PathVariable String tipo) {
        return ResponseEntity.ok(catalogoService.listar(tipo));
    }

    @PostMapping("/{tipo}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> crear(@PathVariable String tipo, @RequestParam String nombre) {
        try {
            return ResponseEntity.ok(catalogoService.crear(tipo, nombre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{tipo}/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable String tipo, @PathVariable Integer id, @RequestParam String nombre) {
        try {
            return ResponseEntity.ok(catalogoService.actualizar(tipo, id, nombre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tipo}/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable String tipo, @PathVariable Integer id) {
        return ResponseEntity.ok(catalogoService.eliminar(tipo, id));
    }
}
