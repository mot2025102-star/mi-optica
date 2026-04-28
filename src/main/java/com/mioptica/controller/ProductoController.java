package com.mioptica.controller;
 
import com.mioptica.model.*;
import com.mioptica.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {
 
    private final ProductoService productoService;
 
    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR','CONTADOR','BODEGUERO','OPTOMETRISTA')")
    public String lista(
            @RequestParam(defaultValue = "")       String  q,
            @RequestParam(defaultValue = "0")      Integer cat,
            @RequestParam(defaultValue = "0")      Integer mar,
            @RequestParam(defaultValue = "activo") String  est,
            @RequestParam(defaultValue = "tabla")  String  vista,
            Model model) {
 
        var productos = q.isBlank() ? productoService.listarTodos()
                                     : productoService.buscar(q);
 
        if (cat > 0) { int c = cat; productos = productos.stream().filter(p -> p.getCategoria() != null && p.getCategoria().getIdCategoria().equals(c)).toList(); }
        if (mar > 0) { int m = mar; productos = productos.stream().filter(p -> p.getMarca()     != null && p.getMarca().getIdMarca().equals(m)).toList(); }
        if ("activo".equals(est))        productos = productos.stream().filter(Producto::getActivo).toList();
        else if ("inactivo".equals(est)) productos = productos.stream().filter(p -> !p.getActivo()).toList();
 
        long activos   = productos.stream().filter(Producto::getActivo).count();
        long inactivos = productos.size() - activos;
 
        // ── stockMap: stock total por producto ────────────────────
        var stockMap = new HashMap<Integer, BigDecimal>();
        productos.forEach(p -> stockMap.put(p.getIdProducto(), productoService.stockTotal(p.getIdProducto())));
 
        // ── precioMap: precio de venta por producto ───────────────
        var precioMap = new HashMap<Integer, BigDecimal>();
        productos.forEach(p -> {
            BigDecimal pv = productoService.precioVenta(p.getIdProducto());
            if (pv != null) precioMap.put(p.getIdProducto(), pv);
        });
 
        model.addAttribute("productos",   productos);
        model.addAttribute("stockMap",    stockMap);
        model.addAttribute("precioMap",   precioMap);   // ← NUEVO
        model.addAttribute("categorias",  productoService.listarCategorias());
        model.addAttribute("marcas",      productoService.listarMarcas());
        model.addAttribute("activos",     activos);
        model.addAttribute("inactivos",   inactivos);
        model.addAttribute("q",           q);
        model.addAttribute("cat",         cat);
        model.addAttribute("mar",         mar);
        model.addAttribute("est",         est);
        model.addAttribute("vista",       vista);
        model.addAttribute("activePage",  "productos");
        return "productos/lista";
    }
 
    // ─── FORMULARIO NUEVO ─────────────────────────────────────────
    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public String nuevo(Model model) {
        model.addAttribute("producto",     new Producto());
        model.addAttribute("categorias",   productoService.listarCategorias());
        model.addAttribute("marcas",       productoService.listarMarcas());
        model.addAttribute("sucursales",   productoService.listarSucursales());
        model.addAttribute("tiposLente",   productoService.listarTiposLente());
        model.addAttribute("materiales",   productoService.listarMateriales());
        model.addAttribute("tratamientos", productoService.listarTratamientos());
        model.addAttribute("editando",     false);
        model.addAttribute("activePage",   "productos");
        return "productos/formulario";
    }
 
    // ─── FORMULARIO EDITAR ────────────────────────────────────────
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return productoService.findById(id).map(p -> {
            model.addAttribute("producto",     p);
            model.addAttribute("categorias",   productoService.listarCategorias());
            model.addAttribute("marcas",       productoService.listarMarcas());
            model.addAttribute("sucursales",   productoService.listarSucursales());
            model.addAttribute("tiposLente",   productoService.listarTiposLente());
            model.addAttribute("materiales",   productoService.listarMateriales());
            model.addAttribute("tratamientos", productoService.listarTratamientos());
            model.addAttribute("editando",     true);
            model.addAttribute("activePage",   "productos");
            return "productos/formulario";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensajeError", "Producto no encontrado.");
            return "redirect:/productos";
        });
    }
 
    // ─── ELIMINAR ─────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            productoService.eliminar(id);
            ra.addFlashAttribute("mensajeOk", "Producto eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/productos";
    }
 
    // ─── GUARDAR ──────────────────────────────────────────────────
    @PostMapping("/guardar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public String guardar(
            @Valid @ModelAttribute("producto") Producto producto,
            BindingResult result,
            @RequestParam Map<String, String> allParams,
            Model model,
            RedirectAttributes ra) {
 
        if (result.hasErrors()) {
            model.addAttribute("categorias",   productoService.listarCategorias());
            model.addAttribute("marcas",       productoService.listarMarcas());
            model.addAttribute("sucursales",   productoService.listarSucursales());
            model.addAttribute("tiposLente",   productoService.listarTiposLente());
            model.addAttribute("materiales",   productoService.listarMateriales());
            model.addAttribute("tratamientos", productoService.listarTratamientos());
            model.addAttribute("editando",     producto.getIdProducto() != null);
            model.addAttribute("activePage",   "productos");
            return "productos/formulario";
        }
 
        try {
            boolean esNuevo = (producto.getIdProducto() == null);
            productoService.guardar(producto, allParams);
            ra.addFlashAttribute("mensajeOk",
                    esNuevo ? "Producto creado con stock inicial registrado."
                            : "Producto actualizado correctamente.");
        } catch (Exception e) {
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("categorias",   productoService.listarCategorias());
            model.addAttribute("marcas",       productoService.listarMarcas());
            model.addAttribute("sucursales",   productoService.listarSucursales());
            model.addAttribute("tiposLente",   productoService.listarTiposLente());
            model.addAttribute("materiales",   productoService.listarMateriales());
            model.addAttribute("tratamientos", productoService.listarTratamientos());
            model.addAttribute("editando",     producto.getIdProducto() != null);
            model.addAttribute("activePage",   "productos");
            return "productos/formulario";
        }
 
        return "redirect:/productos";
    }
 
    // ─── TOGGLE ACTIVO ────────────────────────────────────────────
    @PostMapping("/toggle/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            productoService.toggleActivo(id);
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/productos";
    }
 
    // ─── REST API ─────────────────────────────────────────────────
    @PostMapping("/api/categoria")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> guardarCategoria(@RequestParam String nombre) {
        try {
            Categoria cat = productoService.guardarCategoria(nombre);
            return ResponseEntity.ok(Map.of("ok", true, "id", cat.getIdCategoria(), "nombre", cat.getNombre()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
 
    @PostMapping("/api/marca")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> guardarMarca(@RequestParam String nombre) {
        try {
            Marca m = productoService.guardarMarca(nombre);
            return ResponseEntity.ok(Map.of("ok", true, "id", m.getIdMarca(), "nombre", m.getNombre()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
 
    @GetMapping("/api/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarApi(@RequestParam(defaultValue = "") String q) {
        return productoService.buscar(q).stream().limit(15).map(p ->
                Map.<String, Object>of("id", p.getIdProducto(), "codigo", p.getCodigo(), "detalle", p.getDetalle())
        ).toList();
    }
 
    // ─── API precio de lente por combinación ──────────────────────
    @GetMapping("/api/precio-lente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> precioLente(
            @RequestParam Integer idTipo,
            @RequestParam Integer idMaterial,
            @RequestParam Integer idTratamiento) {
        return productoService.consultarPrecio(idTipo, idMaterial, idTratamiento)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "ok", true,
                        "costo", p.getCosto(),
                        "precioVenta", p.getPrecioVenta())))
                .orElse(ResponseEntity.ok(Map.of("ok", false)));
    }
}