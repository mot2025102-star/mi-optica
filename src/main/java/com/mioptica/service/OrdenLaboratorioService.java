package com.mioptica.service;

import com.mioptica.dto.OrdenLabRequest;
import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenLaboratorioService {

    private final OrdenLaboratorioRepository ordenRepo;
    private final ClienteRepository          clienteRepo;
    private final SucursalRepository         sucursalRepo;
    private final UsuarioRepository          usuarioRepo;

    // ── Listar ────────────────────────────────────────────────────
    public List<OrdenLaboratorio> listar(LocalDate fi, LocalDate ff,
                                         Integer idSuc, boolean esAdmin) {
        if (esAdmin || idSuc == null)
            return ordenRepo.findByPeriodo(fi, ff);
        return ordenRepo.findByPeriodoYSucursal(fi, ff, idSuc);
    }

    public Optional<OrdenLaboratorio> findById(Integer id) {
        return ordenRepo.findById(id);
    }

    // ── Crear ─────────────────────────────────────────────────────
    @Transactional
    public OrdenLaboratorio crear(OrdenLabRequest req, Integer idSuc, Integer idUsuario) {

        OrdenLaboratorio orden = new OrdenLaboratorio();

        // Número correlativo OL-XXXXXX
        int next = ordenRepo.findMaxId().orElse(0) + 1;
        orden.setNumeroOrden(String.format("OL-%06d", next));

        orden.setSucursal(sucursalRepo.findById(idSuc)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada")));
        orden.setUsuario(usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")));

        if (req.getIdCliente() != null)
            orden.setCliente(clienteRepo.findById(req.getIdCliente()).orElse(null));

        orden.setFechaEmision(LocalDate.now());
        orden.setFechaEnvioLab(req.getFechaEnvioLab());
        orden.setFechaEntregaEstimada(req.getFechaEntregaEstimada());
        orden.setObservaciones(req.getObservaciones());
        orden.setEstado("Pendiente");

        // OD
        orden.setOdEsfera(req.getOdEsfera());     orden.setOdCilindro(req.getOdCilindro());
        orden.setOdEje(req.getOdEje());           orden.setOdAdd(req.getOdAdd());
        orden.setOdDip(req.getOdDip());           orden.setOdNdpod(req.getOdNdpod());
        orden.setOdNdpoi(req.getOdNdpoi());       orden.setOdAltura(req.getOdAltura());

        // OI
        orden.setOiEsfera(req.getOiEsfera());     orden.setOiCilindro(req.getOiCilindro());
        orden.setOiEje(req.getOiEje());           orden.setOiAdd(req.getOiAdd());
        orden.setOiDip(req.getOiDip());           orden.setOiNdpod(req.getOiNdpod());
        orden.setOiNdpoi(req.getOiNdpoi());       orden.setOiAltura(req.getOiAltura());

        // Medidas
        orden.setPantoscopico(req.getPantoscopico());
        orden.setVertex(req.getVertex());
        orden.setPanoramico(req.getPanoramico());

        // Productos
        if (req.getProductos() != null) {
            for (var p : req.getProductos()) {
                DetalleOrdenLab det = new DetalleOrdenLab();
                det.setOrden(orden);
                det.setCodigo(p.getCodigo());
                det.setCantidad(p.getCantidad());
                det.setDescripcion(p.getDescripcion());
                det.setMaterial(p.getMaterial());
                det.setTratamiento(p.getTratamiento());
                det.setColorTinte(p.getColorTinte());
                orden.getProductos().add(det);
            }
        }

        return ordenRepo.save(orden);
    }

    // ── Cambiar estado ────────────────────────────────────────────
    @Transactional
    public void cambiarEstado(Integer id, String nuevoEstado) {
        OrdenLaboratorio o = ordenRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        o.setEstado(nuevoEstado);
        ordenRepo.save(o);
    }

    @Transactional //eliminar para orden laboratorio controller
    public void eliminar(Integer id) {
        OrdenLaboratorio o = ordenRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        ordenRepo.delete(o);
    }
}
