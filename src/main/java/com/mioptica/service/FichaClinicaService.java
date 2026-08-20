package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FichaClinicaService {

    private final FichaClinicaRepository     fichaRepo;
    private final ClienteRepository          clienteRepo;
    private final SucursalRepository         sucursalRepo;
    private final UsuarioRepository          usuarioRepo;
    private final OrdenLaboratorioRepository ordenRepo;  // ← NUEVO

    public List<FichaClinica> listarTodas() {
        return fichaRepo.findAll();
    }

    public List<FichaClinica> listarPorCliente(Integer idCliente) {
        return fichaRepo.findByClienteIdClienteOrderByFechaDesc(idCliente);
    }

    public Optional<FichaClinica> findById(Integer id) {
        return fichaRepo.findById(id);
    }

    @Transactional
    public FichaClinica guardar(FichaClinica ficha) {
        // Calcular saldo automáticamente
        if (ficha.getTotal() != null && ficha.getAbono() != null) {
            ficha.setSaldo(ficha.getTotal().subtract(ficha.getAbono()));
        } else if (ficha.getTotal() != null) {
            ficha.setSaldo(ficha.getTotal());
        }

        boolean esNueva = (ficha.getIdFicha() == null);
        FichaClinica saved = fichaRepo.save(ficha);

        // ── Auto-generar Orden de Laboratorio solo si es nueva ───
        // (Desactivado: ahora se genera automáticamente desde Venta)
        // if (esNueva) {
        //     generarOrdenDesdeFicha(saved);
        // }


        return saved;
    }

    @Transactional
    public void eliminar(Integer id) throws Exception {
        FichaClinica f = fichaRepo.findById(id)
                .orElseThrow(() -> new Exception("Ficha no encontrada."));
        fichaRepo.delete(f);
    }

    // ── Helper privado ────────────────────────────────────────────
    private void generarOrdenDesdeFicha(FichaClinica ficha) {
        try {
            int next = ordenRepo.findMaxId().orElse(0) + 1;

            OrdenLaboratorio orden = new OrdenLaboratorio();
            orden.setNumeroOrden(String.format("OL-%06d", next));
            orden.setSucursal(ficha.getSucursal());
            orden.setUsuario(ficha.getOptometrista());
            orden.setCliente(ficha.getCliente());
            orden.setFechaEmision(ficha.getFecha());
            orden.setFechaEntregaEstimada(ficha.getFechaEntrega());
            orden.setEstado("Pendiente");

            // Origen
            orden.setOrigen("FICHA_INTERNA");
            orden.setIdFicha(ficha.getIdFicha());
            orden.setNotaOrigen("Generada automáticamente desde Ficha #" + ficha.getIdFicha());

            // ── Graduación a fabricar ──────────────────────────────
            // Prioridad: RX Final (receta definitiva) → RX Subjetivo → Lensometría (lo que traía el paciente)
            // para no enviar al laboratorio la graduación anterior del paciente por error.
            if (tieneValor(ficha.getRxOdEsfera()) || tieneValor(ficha.getRxOiEsfera())) {
                orden.setOdEsfera(ficha.getRxOdEsfera());
                orden.setOdCilindro(ficha.getRxOdCilindro());
                orden.setOdEje(ficha.getRxOdEje());
                orden.setOdAdd(ficha.getRxOdAdicion());
                orden.setOiEsfera(ficha.getRxOiEsfera());
                orden.setOiCilindro(ficha.getRxOiCilindro());
                orden.setOiEje(ficha.getRxOiEje());
                orden.setOiAdd(ficha.getRxOiAdicion());
                orden.setOdAltura(bd(ficha.getRxOdAltura()));
                orden.setOiAltura(bd(ficha.getRxOiAltura()));
            } else if (tieneValor(ficha.getSubOdEsfera()) || tieneValor(ficha.getSubOiEsfera())) {
                orden.setOdEsfera(ficha.getSubOdEsfera());
                orden.setOdCilindro(ficha.getSubOdCilindro());
                orden.setOdEje(ficha.getSubOdEje());
                orden.setOdAdd(ficha.getSubOdAdicion());
                orden.setOiEsfera(ficha.getSubOiEsfera());
                orden.setOiCilindro(ficha.getSubOiCilindro());
                orden.setOiEje(ficha.getSubOiEje());
                orden.setOiAdd(ficha.getSubOiAdicion());
            } else {
                // Última opción: graduación de los lentes que traía el paciente
                orden.setOdEsfera(bd(ficha.getOdEsfera()));
                orden.setOdCilindro(bd(ficha.getOdCilindro()));
                orden.setOdEje(ficha.getOdEje() != null ? ficha.getOdEje().toString() : null);
                orden.setOdAdd(bd(ficha.getOdAdicion()));
                orden.setOiEsfera(bd(ficha.getOiEsfera()));
                orden.setOiCilindro(bd(ficha.getOiCilindro()));
                orden.setOiEje(ficha.getOiEje() != null ? ficha.getOiEje().toString() : null);
                orden.setOiAdd(bd(ficha.getOiAdicion()));
            }

            // ── D.I.P. / N.D.P. ─────────────────────────────────────
            String dip = bd(ficha.getRxDip());
            orden.setOdDip(dip);
            orden.setOiDip(dip);
            orden.setOdNdpod(bd(ficha.getRxNdpOd()));
            orden.setOiNdpoi(bd(ficha.getRxNdpOi()));

            // ── Medidas de armazón / dispensación ───────────────────
            orden.setVertex(ficha.getVertex());
            orden.setPantoscopico(ficha.getPantoscopico());
            orden.setPanoramico(ficha.getPanoramico());

            // Observaciones: armazón + detalle de lentes de la ficha
            StringBuilder obs = new StringBuilder();
            if (ficha.getArmazon() != null && !ficha.getArmazon().isBlank()) {
                obs.append("Armazón: ").append(ficha.getArmazon());
            }
            if (ficha.getDetalleLentes() != null && !ficha.getDetalleLentes().isBlank()) {
                if (obs.length() > 0) obs.append(" — ");
                obs.append(ficha.getDetalleLentes());
            }
            if (obs.length() > 0) {
                orden.setObservaciones(obs.toString());
            }

            ordenRepo.save(orden);

        } catch (Exception e) {
            // Log pero no romper el flujo de guardado de la ficha
            System.err.println("⚠️ No se pudo generar orden de lab desde ficha: " + e.getMessage());
        }
    }

    private String bd(BigDecimal val) {
        return val != null ? val.toPlainString() : null;
    }

    /** true si el String tiene contenido real (no null, no vacío/blank). */
    private boolean tieneValor(String s) {
        return s != null && !s.isBlank();
    }
}