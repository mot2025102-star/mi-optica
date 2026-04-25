package com.mioptica.controller;

import com.mioptica.model.Inventario;
import com.mioptica.repository.ProductoRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.service.TrasladoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TrasladoController {

    private final UsuarioRepository usuarioRepo;
    private final ProductoRepository productoRepo;
    private final SucursalRepository sucursalRepo;
    private final InventarioRepository inventarioRepo;
    private final TrasladoService trasladoService;

    @GetMapping("/traslados")
    public String traslados(
            @RequestParam(defaultValue = "0") Integer pid,
            @RequestParam(defaultValue = "0") Integer ori,
            @AuthenticationPrincipal UserDetails ud,
            Model model
    ) {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("pid", pid);
        model.addAttribute("ori", ori);
        model.addAttribute("productos", productoRepo.findByActivoTrueOrderByDetalleAsc()); // usado como fallback
        model.addAttribute("sucursales", sucursalRepo.findByActivoTrue());
        model.addAttribute("activePage", "inventario");

        return "inventario/traslados";
    }

    @GetMapping("/api/traslados/productos")
    @ResponseBody
    public List<Map<String, Object>> productosConStock(@RequestParam int ori) {
        return inventarioRepo.findConStockBySucursal(ori).stream()
                .map((Inventario inv) -> Map.<String, Object>of(
                        "idProducto", inv.getProducto().getIdProducto(),
                        "label", inv.getProducto().getCodigo() + " — " + inv.getProducto().getDetalle(),
                        "existencia", inv.getExistencia()
                ))
                .toList();
    }

    @GetMapping("/api/traslados/existencia")
    @ResponseBody
    public Map<String, Object> existenciaProducto(@RequestParam int ori, @RequestParam int pid) throws Exception {
        var producto = productoRepo.findById(pid).orElseThrow(() -> new Exception("Producto no encontrado."));
        var suc = sucursalRepo.findById(ori).orElseThrow(() -> new Exception("Sucursal no encontrada."));
        var inv = inventarioRepo.findByProductoAndSucursal(producto, suc).orElse(null);
        BigDecimal ex = inv != null && inv.getExistencia() != null ? inv.getExistencia() : BigDecimal.ZERO;
        return Map.of("existencia", ex);
    }

    @PostMapping("/traslados/guardar")
    public String guardarTraslado(
            @RequestParam Integer idProducto,
            @RequestParam Integer idSucursalOrigen,
            @RequestParam Integer idSucursalDestino,
            @RequestParam BigDecimal cantidad,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) String observacion,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra
    ) {
        try {
            trasladoService.realizarTraslado(
                    idProducto,
                    idSucursalOrigen,
                    idSucursalDestino,
                    cantidad,
                    referencia,
                    observacion,
                    ud.getUsername()
            );
            ra.addFlashAttribute("mensajeOk", "Traslado realizado y Kardex actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/traslados";
    }
}

