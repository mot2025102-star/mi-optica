package com.mioptica.controller;

import com.mioptica.model.Inventario;
import com.mioptica.repository.ClienteRepository;
import com.mioptica.repository.FichaClinicaRepository;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.repository.ProductoRepository;
import com.mioptica.repository.UsuarioRepository;
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

    private final UsuarioRepository   usuarioRepo;
    private final ClienteRepository   clienteRepo;
    private final ProductoRepository  productoRepo;
    private final InventarioRepository inventarioRepo;
    private final FichaClinicaRepository fichaRepo;
    private final VentaService        ventaService;

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        Integer idSuc = usuario.getSucursal() != null
                ? usuario.getSucursal().getIdSucursal() : null;

        model.addAttribute("usuario",    usuario);
        model.addAttribute("activePage", "dashboard");

        // ── Dashboard Optometrista ────────────────────────────────
        boolean esOpto = ud.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().toUpperCase()
                        .contains("OPTOMETRISTA"));
                        System.out.println(">>> ROL DETECTADO: " + ud.getAuthorities() + " | esOpto=" + esOpto);

        if (esOpto) {
            // Fichas del optometrista logueado
            var todasFichas = fichaRepo.findAll().stream()
                    .filter(f -> f.getOptometrista().getIdUsuario()
                            .equals(usuario.getIdUsuario()))
                    .toList();

            long pendientes = todasFichas.stream()
                    .filter(f -> !"Entregado".equals(f.getEstadoEntrega()))
                    .count();

            long conSaldo = todasFichas.stream()
                    .filter(f -> f.getSaldo() != null &&
                            f.getSaldo().compareTo(java.math.BigDecimal.ZERO) > 0)
                    .count();

            // Próximas entregas (las 5 más urgentes)
            var proximasEntregas = todasFichas.stream()
                    .filter(f -> !"Entregado".equals(f.getEstadoEntrega())
                            && f.getFechaEntrega() != null)
                    .sorted(java.util.Comparator.comparing(
                            com.mioptica.model.FichaClinica::getFechaEntrega))
                    .limit(5)
                    .toList();

            model.addAttribute("totalFichas",      todasFichas.size());
            model.addAttribute("pendientes",        pendientes);
            model.addAttribute("conSaldo",          conSaldo);
            model.addAttribute("proximasEntregas",  proximasEntregas);

            return "dashboard/optometrista";
        }

        // ── Dashboard Admin (default) ─────────────────────────────
        List<Inventario> stockBajo = inventarioRepo.findTodosConProductoActivo()
                .stream()
                .filter(i -> i.getExistencia().compareTo(BigDecimal.valueOf(2)) <= 0)
                .limit(10)
                .toList();

        model.addAttribute("totalClientes",  clienteRepo.count());
        model.addAttribute("totalProductos", productoRepo.count());
        model.addAttribute("ventasHoy",      ventaService.totalHoy(idSuc));
        model.addAttribute("pedidosHoy",     ventaService.countHoy(idSuc));
        model.addAttribute("stockBajo",      stockBajo);

        return "dashboard/admin";
    }
}