package com.mioptica.service;

import com.mioptica.model.Proveedor;
import com.mioptica.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAll();
    }

    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByActivoTrueOrderByNombreAsc();
    }

    public Optional<Proveedor> findById(Integer id) {
        return proveedorRepository.findById(id);
    }

    public List<Proveedor> buscar(String q) {
        if (q == null || q.isBlank()) {
            return listarActivos();
        }
        return proveedorRepository.searchByQuery(q);
    }

    public Proveedor guardar(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public void toggleActivo(Integer id) {
        proveedorRepository.findById(id).ifPresent(p -> {
            p.setActivo(!p.getActivo());
            proveedorRepository.save(p);
        });
    }

    public void eliminar(Integer id) {
        proveedorRepository.deleteById(id);
    }
}
