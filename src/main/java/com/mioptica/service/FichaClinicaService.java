package com.mioptica.service;

import com.mioptica.model.FichaClinica;
import com.mioptica.repository.FichaClinicaRepository;
import com.mioptica.repository.ClienteRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FichaClinicaService {

    private final FichaClinicaRepository fichaRepo;
    private final ClienteRepository      clienteRepo;
    private final SucursalRepository     sucursalRepo;
    private final UsuarioRepository      usuarioRepo;

    // ─── Listar todas ─────────────────────────────────────────────
    public List<FichaClinica> listarTodas() {
        return fichaRepo.findAll();
    }

    public List<FichaClinica> listarPorCliente(Integer idCliente) {
        return fichaRepo.findByClienteIdClienteOrderByFechaDesc(idCliente);
    }

    // ─── Obtener una ──────────────────────────────────────────────
    public Optional<FichaClinica> findById(Integer id) {
        return fichaRepo.findById(id);
    }

    // ─── Guardar / actualizar ─────────────────────────────────────
    @Transactional
    public FichaClinica guardar(FichaClinica ficha) {
        // Calcular saldo automáticamente
        if (ficha.getTotal() != null && ficha.getAbono() != null) {
            ficha.setSaldo(ficha.getTotal().subtract(ficha.getAbono()));
        } else if (ficha.getTotal() != null) {
            ficha.setSaldo(ficha.getTotal());
        }
        return fichaRepo.save(ficha);
    }

    // ─── Eliminar ─────────────────────────────────────────────────
    @Transactional
    public void eliminar(Integer id) throws Exception {
        FichaClinica f = fichaRepo.findById(id)
                .orElseThrow(() -> new Exception("Ficha no encontrada."));
        fichaRepo.delete(f);
    }
}
