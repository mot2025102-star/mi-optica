package com.mioptica.controller;
 
import com.mioptica.model.Inventario;
import com.mioptica.model.Kardex;
import com.mioptica.repository.ProductoRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.InventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
 
@Controller
@RequiredArgsConstructor
public class InventarioController {
 
    private final InventarioService  inventarioService;
    private final UsuarioRepository  usuarioRepo;
    private final ProductoRepository productoRepo;
    private final SucursalRepository sucursalRepo;
 
    @GetMapping("/inventario")
    public String inventario(
            @RequestParam(defaultValue = "0") Integer pid,
            @RequestParam(defaultValue = "")  String  est,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {
 
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        Integer idSuc   = usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null;
 
        List<Inventario> todos = esAdmin
                ? inventarioService.listarTodo()
                : inventarioService.listarPorSucursal(idSuc);
 
        // ✅ Stats calculados del TOTAL antes de cualquier filtro
        long sinStock  = todos.stream().filter(i -> i.getExistencia().compareTo(BigDecimal.ZERO) == 0).count();
        long stockBajo = todos.stream().filter(i -> i.getExistencia().compareTo(BigDecimal.ZERO) > 0
                          && i.getExistencia().compareTo(BigDecimal.valueOf(2)) <= 0).count();
        long stockOk   = todos.stream().filter(i -> i.getExistencia().compareTo(BigDecimal.valueOf(2)) > 0).count();
 
        // Filtros aplicados DESPUÉS
        List<Inventario> items = todos;
        if ("sin_stock".equals(est)) {
            items = todos.stream()
                    .filter(i -> i.getExistencia().compareTo(BigDecimal.ZERO) == 0).toList();
        } else if ("bajo".equals(est)) {
            items = todos.stream()
                    .filter(i -> i.getExistencia().compareTo(BigDecimal.ZERO) > 0
                              && i.getExistencia().compareTo(BigDecimal.valueOf(2)) <= 0).toList();
        } else if ("ok".equals(est)) {
            items = todos.stream()
                    .filter(i -> i.getExistencia().compareTo(BigDecimal.valueOf(2)) > 0).toList();
        }
 
        if (pid > 0) {
            int pidFinal = pid;
            items = items.stream()
                    .filter(i -> i.getProducto().getIdProducto().equals(pidFinal)).toList();
        }
 
        model.addAttribute("items",      items);
        model.addAttribute("sinStock",   sinStock);
        model.addAttribute("stockBajo",  stockBajo);
        model.addAttribute("stockOk",    stockOk);
        model.addAttribute("esAdmin",    esAdmin);
        model.addAttribute("est",        est);
        model.addAttribute("pid",        pid);
        model.addAttribute("activePage", "inventario");
        return "inventario/lista";
    }
 
    @PostMapping("/inventario/ajustar")
    public String ajustar(
            @RequestParam Integer    idProducto,
            @RequestParam Integer    idSucursal,
            @RequestParam BigDecimal existencia,
            @RequestParam(required = false) BigDecimal costo,
            @RequestParam(required = false) BigDecimal precio,
            @RequestParam(defaultValue = "") String motivo,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
 
        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            inventarioService.ajustarStock(
                    idProducto, idSucursal, existencia, costo, precio,
                    motivo, usuario.getIdUsuario());
            ra.addFlashAttribute("mensajeOk", "Stock ajustado y registrado en Kardex.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/inventario";
    }
 
    @PostMapping("/inventario/eliminar/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String eliminarInventario(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            inventarioService.eliminarInventario(id);
            ra.addFlashAttribute("mensajeOk", "Registro de inventario eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/inventario";
    }
 
    @GetMapping("/inventario/kardex/{idProd}")
    @ResponseBody
    public List<Map<String, Object>> kardexProducto(@PathVariable Integer idProd) {
        return inventarioService.kardexPorProducto(idProd).stream()
                .limit(15)
                .map(k -> Map.<String, Object>of(
                        "fecha",       k.getFecha().toString(),
                        "tipo",        k.getTipoMovimiento(),
                        "ref",         k.getReferencia() != null ? k.getReferencia() : "—",
                        "cant",        k.getCantidad(),
                        "egreso",      k.getEgreso(),
                        "fechaEgreso", k.getFechaEgreso() != null ? k.getFechaEgreso().toString() : "—",
                        "saldo",       k.getExistenciaNueva(),
                        "sucursal",    k.getSucursal().getNombre()
                ))
                .toList();
    }
 
    @GetMapping("/kardex")
    public String kardex(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String buscar,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {
 
        if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
        if (ff == null) ff = LocalDate.now();
 
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        Integer idSuc   = usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null;
 
        List<Kardex> movimientos = inventarioService.listarKardex(fi, ff, idSuc, esAdmin);
 
        if (!tipo.isBlank()) {
            String tipoFinal = tipo;
            movimientos = movimientos.stream()
                    .filter(k -> k.getTipoMovimiento().equalsIgnoreCase(tipoFinal)).toList();
        }
        if (!buscar.isBlank()) {
            String b = buscar.toLowerCase();
            movimientos = movimientos.stream()
                    .filter(k -> k.getProducto().getCodigo().toLowerCase().contains(b)
                              || k.getProducto().getDetalle().toLowerCase().contains(b)).toList();
        }
 
        var totalEntradas = movimientos.stream()
                .filter(k -> k.getTipoMovimiento().startsWith("Entrada") || k.getTipoMovimiento().equals("Traslado_Entrada"))
                .map(Kardex::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalSalidas = movimientos.stream()
                .filter(k -> k.getTipoMovimiento().startsWith("Salida") || k.getTipoMovimiento().equals("Traslado_Salida"))
                .map(Kardex::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalAjustes = movimientos.stream().filter(k -> k.getTipoMovimiento().equals("Ajuste")).count();
 
        model.addAttribute("movimientos",   movimientos);
        model.addAttribute("totalEntradas", totalEntradas);
        model.addAttribute("totalSalidas",  totalSalidas);
        model.addAttribute("totalAjustes",  totalAjustes);
        model.addAttribute("fi",            fi);
        model.addAttribute("ff",            ff);
        model.addAttribute("tipo",          tipo);
        model.addAttribute("buscar",        buscar);
        model.addAttribute("esAdmin",       esAdmin);
        model.addAttribute("productos",     productoRepo.findByActivoTrueOrderByDetalleAsc());
        model.addAttribute("sucursales",    sucursalRepo.findByActivoTrue());
        model.addAttribute("activePage",    "kardex");
        return "inventario/kardex";
    }
 
    @PostMapping("/kardex/agregar")
    public String agregarIngreso(
            @RequestParam Integer    idProducto,
            @RequestParam Integer    idSucursal,
            @RequestParam BigDecimal cantidad,
            @RequestParam BigDecimal precioCompra,
            @RequestParam BigDecimal precioVenta,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaIngreso,
            @RequestParam(defaultValue = "") String observacion,
            @RequestParam(defaultValue = "") String referencia,  // ← NUEVO
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
 
        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            inventarioService.agregarIngreso(idProducto, idSucursal, cantidad,
                    precioCompra, precioVenta, fechaIngreso, observacion, referencia, usuario.getIdUsuario());
            ra.addFlashAttribute("mensajeOk", "Ingreso registrado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/kardex";
    }
 
    @PostMapping("/kardex/egreso")
    public String registrarEgreso(
            @RequestParam Integer    idProducto,
            @RequestParam Integer    idSucursal,
            @RequestParam BigDecimal egreso,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEgreso,
            @RequestParam(defaultValue = "") String referencia,  // ← NUEVO
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
 
        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            inventarioService.registrarEgreso(idProducto, idSucursal, egreso, fechaEgreso, referencia, usuario.getIdUsuario());
            ra.addFlashAttribute("mensajeOk", "Egreso registrado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/kardex";
    }
 
    @PostMapping("/kardex/editar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CONTADOR')")
    public String editarKardex(
            @RequestParam Integer    idKardex,
            @RequestParam String     tipoMovimiento,
            @RequestParam BigDecimal cantidad,
            @RequestParam BigDecimal precioUnitario,
            @RequestParam BigDecimal precioVenta,
            @RequestParam BigDecimal egreso,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) String referencia,  // ← NUEVO
            RedirectAttributes ra) {
 
        try {
            inventarioService.editarKardex(idKardex, tipoMovimiento, cantidad,
                    precioUnitario, precioVenta, egreso, observacion, referencia);
            ra.addFlashAttribute("mensajeOk", "Movimiento actualizado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/kardex";
    }
 
    @PostMapping("/kardex/eliminar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CONTADOR')")
    public String eliminarKardex(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            inventarioService.eliminarKardex(id);
            ra.addFlashAttribute("mensajeOk", "Movimiento eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/kardex";
    }
}