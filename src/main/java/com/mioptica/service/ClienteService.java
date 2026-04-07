package com.mioptica.service;

import com.mioptica.model.Cliente;
import com.mioptica.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepo;

    // ─── Listar ───────────────────────────────────────────────────
    public List<Cliente> listarTodos() {
        return clienteRepo.findAllByOrderByActivoDescNombreAsc();
    }

    public List<Cliente> listarActivos() {
        return clienteRepo.findByActivoTrueOrderByNombreAsc();
    }

    public List<Cliente> buscar(String q) {
        if (q == null || q.isBlank()) return listarActivos();
        return clienteRepo.buscar(q.trim());
    }

    // ─── Obtener uno ──────────────────────────────────────────────
    public Optional<Cliente> findById(Integer id) {
        return clienteRepo.findById(id);
    }

    // ─── Guardar / actualizar ─────────────────────────────────────
    @Transactional
    public Cliente guardar(Cliente cliente) throws Exception {
        if (cliente.getDpi() != null && !cliente.getDpi().isBlank()) {
            Optional<Cliente> existente = clienteRepo.findByDpi(cliente.getDpi());
            if (existente.isPresent() && !existente.get().getIdCliente().equals(cliente.getIdCliente())) {
                throw new Exception("Ya existe un cliente con ese DPI: " + cliente.getDpi());
            }
        }
        return clienteRepo.save(cliente);
    }

    // ─── Toggle activo/inactivo ───────────────────────────────────
    @Transactional
    public void toggleActivo(Integer id) throws Exception {
        Cliente c = clienteRepo.findById(id)
                .orElseThrow(() -> new Exception("Cliente no encontrado"));
        c.setActivo(!c.getActivo());
        clienteRepo.save(c);
    }

    // ─── Eliminar ─────────────────────────────────────────────────
    @Transactional
    public void eliminar(Integer id) throws Exception {
        Cliente c = clienteRepo.findById(id)
                .orElseThrow(() -> new Exception("Cliente no encontrado."));
        clienteRepo.delete(c);
    }

    // ─── Stats ────────────────────────────────────────────────────
    public long countActivos() {
        return clienteRepo.findByActivoTrueOrderByNombreAsc().size();
    }
}