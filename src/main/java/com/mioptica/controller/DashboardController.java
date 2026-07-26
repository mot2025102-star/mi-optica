package com.mioptica.controller;

import com.mioptica.model.Inventario;
import com.mioptica.repository.*;
import com.mioptica.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioRepository      usuarioRepo;
    private final ClienteRepository      clienteRepo;
    private final ProductoRepository     productoRepo;
    private final InventarioRepository   inventarioRepo;
    private final FichaClinicaRepository fichaRepo;
    private final VentaService           ventaService;

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        Integer idSuc = usuario.getSucursal() != null
                ? usuario.getSucursal().getIdSucursal() : null;

        model.addAttribute("usuario",    usuario);
        model.addAttribute("activePage", "dashboard");

        String rol = ud.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().toUpperCase())
                .orElse("");

        // ── OPTOMETRISTA ──────────────────────────────────────────
        if (rol.contains("OPTOMETRISTA")) {
            var todasFichas = fichaRepo.findAll().stream()
                    .filter(f -> f.getOptometrista().getIdUsuario()
                            .equals(usuario.getIdUsuario()))
                    .toList();

            long pendientes = todasFichas.stream()
                    .filter(f -> !"Entregado".equals(f.getEstadoEntrega())).count();
            long conSaldo = todasFichas.stream()
                    .filter(f -> f.getSaldo() != null &&
                            f.getSaldo().compareTo(BigDecimal.ZERO) > 0).count();
            var proximasEntregas = todasFichas.stream()
                    .filter(f -> !"Entregado".equals(f.getEstadoEntrega())
                            && f.getFechaEntrega() != null)
                    .sorted(java.util.Comparator.comparing(
                            com.mioptica.model.FichaClinica::getFechaEntrega))
                    .limit(5).toList();
            var proximosAniversarios = proximosAniversarioCliente(todasFichas);

            model.addAttribute("totalFichas",     todasFichas.size());
            model.addAttribute("pendientes",       pendientes);
            model.addAttribute("conSaldo",         conSaldo);
            model.addAttribute("proximasEntregas", proximasEntregas);
            model.addAttribute("proximosAniversarios", proximosAniversarios);
            return "dashboard/optometrista";
        }

        // ── VENDEDOR ──────────────────────────────────────────────
        if (rol.contains("VENDEDOR")) {
            var ventasHoy  = ventaService.totalHoy(idSuc);
            var countHoy   = ventaService.countHoy(idSuc);
            var stockBajo  = stockBajoSucursal(idSuc);

            model.addAttribute("ventasHoy",  ventasHoy);
            model.addAttribute("countHoy",   countHoy);
            model.addAttribute("stockBajo",  stockBajo);
            model.addAttribute("sucursal",   usuario.getSucursal() != null
                    ? usuario.getSucursal().getNombre() : "Sin sucursal");
            return "dashboard/vendedor";
        }

        // ── BODEGUERO ─────────────────────────────────────────────
        if (rol.contains("BODEGUERO")) {
            var stockBajo = stockBajoSucursal(idSuc);
            var todoInventario = idSuc != null
                    ? inventarioRepo.findBySucursalIdSucursal(idSuc)
                    : inventarioRepo.findTodosConProductoActivo();

            long totalProductos = todoInventario.size();
            long sinStock = todoInventario.stream()
                    .filter(i -> i.getExistencia().compareTo(BigDecimal.ZERO) <= 0)
                    .count();

            model.addAttribute("stockBajo",      stockBajo);
            model.addAttribute("totalProductos", totalProductos);
            model.addAttribute("sinStock",       sinStock);
            model.addAttribute("sucursal",       usuario.getSucursal() != null
                    ? usuario.getSucursal().getNombre() : "Todas");
            return "dashboard/bodeguero";
        }

        // ── CONTADOR ──────────────────────────────────────────────
        if (rol.contains("CONTADOR")) {
            var ventasHoy = ventaService.totalHoy(null);
            var countHoy  = ventaService.countHoy(null);

            model.addAttribute("ventasHoy",      ventasHoy);
            model.addAttribute("countHoy",       countHoy);
            model.addAttribute("totalClientes",  clienteRepo.count());
            model.addAttribute("totalProductos", productoRepo.count());
            return "dashboard/contador";
        }

        // ── ADMINISTRADOR (default) ───────────────────────────────
        List<Inventario> stockBajo = inventarioRepo.findTodosConProductoActivo()
                .stream()
                .filter(i -> i.getExistencia().compareTo(BigDecimal.valueOf(2)) <= 0)
                .limit(10).toList();

        var todasFichasAdmin = fichaRepo.findAll();
        var proximosAniversariosAdmin = proximosAniversarioCliente(todasFichasAdmin);

        model.addAttribute("totalClientes",  clienteRepo.count());
        model.addAttribute("totalProductos", productoRepo.count());
        model.addAttribute("ventasHoy",      ventaService.totalHoy(idSuc));
        model.addAttribute("pedidosHoy",     ventaService.countHoy(idSuc));
        model.addAttribute("stockBajo",      stockBajo);
        model.addAttribute("proximosAniversarios", proximosAniversariosAdmin);
        return "dashboard/admin";
    }

    // ── Helper ────────────────────────────────────────────────────
    private List<Inventario> stockBajoSucursal(Integer idSuc) {
        var base = idSuc != null
                ? inventarioRepo.findBySucursalIdSucursal(idSuc)
                : inventarioRepo.findTodosConProductoActivo();
        return base.stream()
                .filter(i -> i.getExistencia().compareTo(BigDecimal.valueOf(2)) <= 0)
                .limit(10).toList();
    }

    /**
     * Punto 9: de la última ficha de cada cliente, retorna las que están a
     * 30 días o menos de cumplir 1 año desde esa consulta (o ya lo cumplieron
     * y aún no han vuelto). Solo se considera la ficha más reciente por
     * cliente para no alertar de un aniversario ya cubierto por una consulta
     * posterior.
     */
    private List<com.mioptica.model.FichaClinica> proximosAniversarioCliente(
            List<com.mioptica.model.FichaClinica> fichas) {
        return fichas.stream()
                .collect(java.util.stream.Collectors.toMap(
                        f -> f.getCliente().getIdCliente(),
                        f -> f,
                        (f1, f2) -> f1.getFecha().isAfter(f2.getFecha()) ? f1 : f2))
                .values().stream()
                .filter(com.mioptica.model.FichaClinica::isProximaACumplirAnio)
                .sorted(java.util.Comparator.comparingLong(
                        com.mioptica.model.FichaClinica::getDiasParaAniversario))
                .limit(10)
                .toList();
    }
}