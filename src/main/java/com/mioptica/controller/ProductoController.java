package com.mioptica.controller;
 
import com.mioptica.model.*;
import com.mioptica.service.ProductoService;
import com.mioptica.service.ProveedorService;
import com.mioptica.service.InventarioService;
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
    private final ProveedorService proveedorService;
    private final InventarioService inventarioService;
 
    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR','CONTADOR','BODEGUERO','OPTOMETRISTA')")
    public String lista(
            @RequestParam(defaultValue = "")       String  q,
            @RequestParam(defaultValue = "")       String  tipo,
            @RequestParam(defaultValue = "0")      Integer mar,
            @RequestParam(defaultValue = "activo") String  est,
            @RequestParam(defaultValue = "tabla")  String  vista,
            @RequestParam(defaultValue = "0")      Integer suc,
            @RequestParam(defaultValue = "0")      Integer fMarca,
            @RequestParam(defaultValue = "")       String  fModelo,
            @RequestParam(defaultValue = "")       String  fMaterial,
            @RequestParam(defaultValue = "0")      Integer fTipoFiltro,
            @RequestParam(defaultValue = "")       String  fColor,
            @RequestParam(defaultValue = "")       String  fForma,
            @RequestParam(defaultValue = "")       String  fGenero,
            @RequestParam(defaultValue = "0")      Integer fTratamiento,
            @RequestParam(defaultValue = "")       String  fRango,
            @RequestParam(defaultValue = "0")      Integer fProveedor,
            Model model) {

        var productos = q.isBlank() ? productoService.listarTodos()
                                    : productoService.buscar(q);

        // Filtro Categoria principal
        if (!tipo.isBlank()) {
            productos = productos.stream()
                .filter(p -> tipo.equalsIgnoreCase(p.getTipoProducto()))
                .toList();
        }
        
        // Filtro estatus
        if ("activo".equals(est))        productos = productos.stream().filter(Producto::getActivo).toList();
        else if ("inactivo".equals(est)) productos = productos.stream().filter(p -> !p.getActivo()).toList();

        // Filtros dinámicos
        if (fMarca > 0) {
            productos = productos.stream().filter(p -> p.getMarca() != null && p.getMarca().getIdMarca().equals(fMarca)).toList();
        } else if (mar > 0) {
            productos = productos.stream().filter(p -> p.getMarca() != null && p.getMarca().getIdMarca().equals(mar)).toList();
        }
        if (!fModelo.isBlank()) {
            productos = productos.stream().filter(p -> p.getCodigoFabrica() != null && p.getCodigoFabrica().toLowerCase().contains(fModelo.toLowerCase())).toList();
        }
        if (!fMaterial.isBlank()) {
            productos = productos.stream().filter(p -> 
                (p.getMaterialLente() != null && String.valueOf(p.getMaterialLente().getIdMaterial()).equals(fMaterial)) ||
                (p.getMaterialArmazon() != null && p.getMaterialArmazon().equalsIgnoreCase(fMaterial))
            ).toList();
        }
        if (fTipoFiltro > 0) {
            productos = productos.stream().filter(p -> p.getTipoLente() != null && p.getTipoLente().getIdTipo().equals(fTipoFiltro)).toList();
        }
        if (!fColor.isBlank()) {
            productos = productos.stream().filter(p -> fColor.equalsIgnoreCase(p.getColor())).toList();
        }
        if (!fForma.isBlank()) {
            productos = productos.stream().filter(p -> fForma.equalsIgnoreCase(p.getFormaArmazon())).toList();
        }
        if (!fGenero.isBlank()) {
            productos = productos.stream().filter(p -> fGenero.equalsIgnoreCase(p.getSegmentoArmazon())).toList();
        }
        if (fTratamiento > 0) {
            productos = productos.stream().filter(p -> p.getTratamientoLente() != null && p.getTratamientoLente().getIdTratamiento().equals(fTratamiento)).toList();
        }
        if (!fRango.isBlank()) {
            productos = productos.stream().filter(p -> fRango.equalsIgnoreCase(p.getGamaArmazon())).toList();
        }
        if (fProveedor > 0) {
            productos = productos.stream().filter(p -> p.getProveedor() != null && p.getProveedor().getIdProveedor().equals(fProveedor)).toList();
        }

        long activos   = productos.stream().filter(Producto::getActivo).count();
        long inactivos = productos.size() - activos;

        var stockMap    = new HashMap<Integer, BigDecimal>();
        var precioMap   = new HashMap<Integer, BigDecimal>();
        var costoMap    = new HashMap<Integer, BigDecimal>();
        var utilidadMap = new HashMap<Integer, BigDecimal>();

        productos.forEach(p -> {
            if (suc > 0) {
                var inv = inventarioService.buscarPorProductoYSucursal(p.getIdProducto(), suc);
                if (inv != null) {
                    stockMap.put(p.getIdProducto(), inv.getExistencia());
                    precioMap.put(p.getIdProducto(), inv.getPrecioVenta());
                    costoMap.put(p.getIdProducto(), inv.getCosto());
                    // Inventario no tiene campo utilidad; calcular desde costo y precio
                    if (inv.getCosto() != null && inv.getCosto().compareTo(BigDecimal.ZERO) > 0 && inv.getPrecioVenta() != null) {
                        BigDecimal util = inv.getPrecioVenta().subtract(inv.getCosto())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(inv.getCosto(), 2, java.math.RoundingMode.HALF_UP);
                        utilidadMap.put(p.getIdProducto(), util);
                    } else {
                        utilidadMap.put(p.getIdProducto(), BigDecimal.ZERO);
                    }
                } else {
                    stockMap.put(p.getIdProducto(), BigDecimal.ZERO);
                    precioMap.put(p.getIdProducto(), BigDecimal.ZERO);
                    costoMap.put(p.getIdProducto(), BigDecimal.ZERO);
                    utilidadMap.put(p.getIdProducto(), BigDecimal.ZERO);
                }
            } else {
                stockMap.put(p.getIdProducto(), productoService.stockTotal(p.getIdProducto()));
                BigDecimal pv = productoService.precioVenta(p.getIdProducto());
                if (pv != null) precioMap.put(p.getIdProducto(), pv);
                costoMap.put(p.getIdProducto(), BigDecimal.ZERO);
                utilidadMap.put(p.getIdProducto(), BigDecimal.ZERO);
            }
        });

        model.addAttribute("productos",  productos);
        model.addAttribute("stockMap",   stockMap);
        model.addAttribute("precioMap",  precioMap);
        model.addAttribute("costoMap",   costoMap);
        model.addAttribute("utilidadMap",utilidadMap);
        model.addAttribute("marcas",     productoService.listarMarcas());
        model.addAttribute("sucursales", productoService.listarSucursales());
        model.addAttribute("tiposLente", productoService.listarTiposLente());
        model.addAttribute("materiales", productoService.listarMateriales());
        model.addAttribute("tratamientos", productoService.listarTratamientos());
        model.addAttribute("proveedores", proveedorService.listarActivos());
        
        model.addAttribute("activos",    activos);
        model.addAttribute("inactivos",  inactivos);
        model.addAttribute("q",          q);
        model.addAttribute("tipo",       tipo);
        model.addAttribute("mar",        mar);
        model.addAttribute("est",        est);
        model.addAttribute("vista",      vista);
        model.addAttribute("suc",        suc);
        
        // Pass filter values back to view
        model.addAttribute("fMarca", fMarca);
        model.addAttribute("fModelo", fModelo);
        model.addAttribute("fMaterial", fMaterial);
        model.addAttribute("fTipoFiltro", fTipoFiltro);
        model.addAttribute("fColor", fColor);
        model.addAttribute("fForma", fForma);
        model.addAttribute("fGenero", fGenero);
        model.addAttribute("fTratamiento", fTratamiento);
        model.addAttribute("fRango", fRango);
        model.addAttribute("fProveedor", fProveedor);

        model.addAttribute("activePage", "productos");
        return "productos/lista";
    }
 
    // ─── FORMULARIO NUEVO ─────────────────────────────────────────
    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public String nuevo(Model model) {
        model.addAttribute("producto",     new Producto());
        model.addAttribute("marcas",       productoService.listarMarcas());
        model.addAttribute("sucursales",   productoService.listarSucursales());
        model.addAttribute("tiposLente",   productoService.listarTiposLente());
        model.addAttribute("materiales",   productoService.listarMateriales());
        model.addAttribute("tratamientos", productoService.listarTratamientos());
        model.addAttribute("proveedores",  proveedorService.listarActivos());
        model.addAttribute("editando",     false);
        model.addAttribute("activePage",   "productos");
        model.addAttribute("proximoId",    productoService.findMaxId() + 1);
        return "productos/formulario";
    }
 
    // ─── FORMULARIO EDITAR ────────────────────────────────────────
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return productoService.findById(id).map(p -> {
            var inventarios = inventarioService.listarTodo().stream()
                    .filter(i -> i.getProducto().getIdProducto().equals(p.getIdProducto()))
                    .toList();
            var invMap = new HashMap<Integer, Inventario>();
            inventarios.forEach(i -> invMap.put(i.getSucursal().getIdSucursal(), i));

            model.addAttribute("producto",     p);
            model.addAttribute("marcas",       productoService.listarMarcas());
            model.addAttribute("sucursales",   productoService.listarSucursales());
            model.addAttribute("tiposLente",   productoService.listarTiposLente());
            model.addAttribute("materiales",   productoService.listarMateriales());
            model.addAttribute("tratamientos", productoService.listarTratamientos());
            model.addAttribute("proveedores",  proveedorService.listarActivos());
            model.addAttribute("invMap",       invMap);
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
            model.addAttribute("marcas",       productoService.listarMarcas());
            model.addAttribute("sucursales",   productoService.listarSucursales());
            model.addAttribute("tiposLente",   productoService.listarTiposLente());
            model.addAttribute("materiales",   productoService.listarMateriales());
            model.addAttribute("tratamientos", productoService.listarTratamientos());
            model.addAttribute("proveedores",  proveedorService.listarActivos());
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
            model.addAttribute("marcas",       productoService.listarMarcas());
            model.addAttribute("sucursales",   productoService.listarSucursales());
            model.addAttribute("tiposLente",   productoService.listarTiposLente());
            model.addAttribute("materiales",   productoService.listarMateriales());
            model.addAttribute("tratamientos", productoService.listarTratamientos());
            model.addAttribute("proveedores",  proveedorService.listarActivos());
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

    @PostMapping("/api/tratamiento")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> guardarTratamiento(@RequestParam String nombre) {
        try {
            Tratamiento_lente t = productoService.guardarTratamiento(nombre);
            return ResponseEntity.ok(Map.of("ok", true, "id", t.getIdTratamiento(), "nombre", t.getNombre()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/api/tratamiento/lista")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarTratamientosApi() {
        List<Map<String, Object>> lista = productoService.listarTratamientos().stream()
                .map(t -> Map.<String, Object>of("id", t.getIdTratamiento(), "nombre", t.getNombre()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/api/tratamiento/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> actualizarTratamiento(@PathVariable Integer id, @RequestParam String nombre) {
        try {
            Tratamiento_lente t = productoService.actualizarTratamiento(id, nombre);
            return ResponseEntity.ok(Map.of("ok", true, "id", t.getIdTratamiento(), "nombre", t.getNombre()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/tratamiento/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> eliminarTratamiento(@PathVariable Integer id) {
        try {
            productoService.eliminarTratamiento(id);
            return ResponseEntity.ok(Map.of("ok", true));
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

    // ─── ACTUALIZACIÓN MASIVA DE PRECIOS ─────────────────────────
    public static class PrecioUpdateRequest {
        public Integer idProducto;
        public BigDecimal costo;
        public BigDecimal utilidad;
        public BigDecimal precioVenta;
    }

    public static class MasivoRequest {
        public Integer sucursalId;
        public List<PrecioUpdateRequest> updates;
    }

    @PostMapping("/actualizar-precios-masivo")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizarPreciosMasivo(@RequestBody MasivoRequest request) {
        try {
            inventarioService.actualizarPreciosMasivo(request.sucursalId, request.updates);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}