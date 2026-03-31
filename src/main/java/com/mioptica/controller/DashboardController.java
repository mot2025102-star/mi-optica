package com.mioptica.controller;

import com.mioptica.model.Inventario;
import com.mioptica.repository.ClienteRepository;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.repository.ProductoRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.VentaService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
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
    private final VentaService        ventaService;

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        Integer idSuc = usuario.getSucursal() != null
                ? usuario.getSucursal().getIdSucursal() : null;

        // Productos con stock bajo (existencia <= 2)
        List<Inventario> stockBajo = inventarioRepo.findTodosConProductoActivo()
                .stream()
                .filter(i -> i.getExistencia().compareTo(BigDecimal.valueOf(2)) <= 0)
                .limit(10)
                .toList();

        model.addAttribute("usuario",        usuario);
        model.addAttribute("totalClientes",  clienteRepo.count());
        model.addAttribute("totalProductos", productoRepo.count());
        model.addAttribute("ventasHoy",      ventaService.totalHoy(idSuc));
        model.addAttribute("pedidosHoy",     ventaService.countHoy(idSuc));
        model.addAttribute("stockBajo",      stockBajo);
        model.addAttribute("activePage",     "dashboard");

        return "dashboard/admin";
    }
}
