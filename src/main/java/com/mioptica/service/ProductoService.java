package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository          productoRepo;
    private final CategoriaRepository         categoriaRepo;
    private final MarcaRepository             marcaRepo;
    private final InventarioRepository        inventarioRepo;
    private final SucursalRepository          sucursalRepo;
    private final Tipo_lenteRepository        tipoLenteRepo;
    private final Material_lenteRepository    materialLenteRepo;
    private final Tratamiento_lenteRepository tratamientoLenteRepo;
    private final Precio_lenteRepository      precioLenteRepo;
    private final KardexRepository            kardexRepo;      // ── NUEVO
    private final UsuarioRepository           usuarioRepo;     // ── NUEVO

    // ─── Listas ───────────────────────────────────────────────────
    public List<Producto>          listarTodos()          { return productoRepo.findAll(); }
    public List<Producto>          listarActivos()        { return productoRepo.findByActivoTrueOrderByDetalleAsc(); }
    public List<Producto>          buscar(String q)       { return productoRepo.buscar(q); }
    public List<Categoria>         listarCategorias()     { return categoriaRepo.findAllByOrderByNombreAsc(); }
    public List<Marca>             listarMarcas()         { return marcaRepo.findAllByOrderByNombreAsc(); }
    public List<Sucursal>          listarSucursales()     { return sucursalRepo.findByActivoTrue(); }
    public List<Tipo_lente>        listarTiposLente()     { return tipoLenteRepo.findAllByOrderByNombreAsc(); }
    public List<Material_lente>    listarMateriales()     { return materialLenteRepo.findAllByOrderByNombreAsc(); }
    public List<Tratamiento_lente> listarTratamientos()   { return tratamientoLenteRepo.findAllByOrderByNombreAsc(); }
    public List<Precio_lente>      listarPreciosLente()   { return precioLenteRepo.findAllByOrderByTipoNombreAscMaterialNombreAsc(); }

    // ─── Obtener uno ──────────────────────────────────────────────
    public Optional<Producto> findById(Integer id) { return productoRepo.findById(id); }

    // ─── Consultar precio automático por combinación ──────────────
    public Optional<Precio_lente> consultarPrecio(Integer idTipo, Integer idMaterial, Integer idTratamiento) {
        return precioLenteRepo.findByCombinacion(idTipo, idMaterial, idTratamiento);
    }

    // ─── Guardar producto (sin stock inicial) ─────────────────────
    @Transactional
    public Producto guardar(Producto producto) throws Exception {
        return guardar(producto, new HashMap<>());
    }

    // ─── Guardar producto (con stock inicial por sucursal) ────────
    @Transactional
    public Producto guardar(Producto producto, Map<String, String> stockParams) throws Exception {

        // ── Resolver entidades relacionadas desde BD ──────────────
        if (producto.getCategoria() != null && producto.getCategoria().getIdCategoria() != null) {
            producto.setCategoria(categoriaRepo.findById(producto.getCategoria().getIdCategoria()).orElse(null));
        } else {
            producto.setCategoria(null);
        }
        if (producto.getMarca() != null && producto.getMarca().getIdMarca() != null) {
            producto.setMarca(marcaRepo.findById(producto.getMarca().getIdMarca()).orElse(null));
        } else {
            producto.setMarca(null);
        }
        if (producto.getTipoLente() != null && producto.getTipoLente().getIdTipo() != null) {
            producto.setTipoLente(tipoLenteRepo.findById(producto.getTipoLente().getIdTipo()).orElse(null));
        } else {
            producto.setTipoLente(null);
        }
        if (producto.getMaterialLente() != null && producto.getMaterialLente().getIdMaterial() != null) {
            producto.setMaterialLente(materialLenteRepo.findById(producto.getMaterialLente().getIdMaterial()).orElse(null));
        } else {
            producto.setMaterialLente(null);
        }
        if (producto.getTratamientoLente() != null && producto.getTratamientoLente().getIdTratamiento() != null) {
            producto.setTratamientoLente(tratamientoLenteRepo.findById(producto.getTratamientoLente().getIdTratamiento()).orElse(null));
        } else {
            producto.setTratamientoLente(null);
        }

        // ── Validar código duplicado ──────────────────────────────
        Optional<Producto> existente = productoRepo.findByCodigo(producto.getCodigo());
        if (existente.isPresent()
                && !existente.get().getIdProducto().equals(producto.getIdProducto())) {
            throw new Exception("Ya existe un producto con el código: " + producto.getCodigo());
        }

        boolean esNuevo = (producto.getIdProducto() == null);
        Producto guardado = productoRepo.save(producto);

        // ── Obtener usuario actual para kardex ────────────────────
        Usuario usuarioActual = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails ud) {
                usuarioActual = usuarioRepo.findByUsername(ud.getUsername()).orElse(null);
            }
        } catch (Exception ignored) {}

        if (esNuevo) {
            for (Sucursal suc : sucursalRepo.findByActivoTrue()) {
                String sid = String.valueOf(suc.getIdSucursal());

                String activa = stockParams.getOrDefault("sucActiva_" + sid, "0");
                if (!"1".equals(activa)) continue;

                BigDecimal existencia = parseBD(stockParams.get("existencia_" + sid));
                BigDecimal costo      = parseBD(stockParams.get("costo_"      + sid));
                BigDecimal precio     = parseBD(stockParams.get("precio_"     + sid));
                // ── Guardar en inventario ─────────────────────────
                boolean yaExiste = inventarioRepo
                        .findByProductoAndSucursal(guardado, suc).isPresent();
                if (!yaExiste) {
                    Inventario inv = new Inventario();
                    inv.setProducto(guardado);
                    inv.setSucursal(suc);
                    inv.setExistencia(existencia);
                    inv.setCosto(costo);
                    inv.setPrecioVenta(precio);
                    inventarioRepo.save(inv);
                }

                // ── NUEVO: Guardar en kardex ──────────────────────
                if (existencia.compareTo(BigDecimal.ZERO) > 0) {
                    Kardex k = new Kardex();
                    k.setProducto(guardado);
                    k.setSucursal(suc);
                    k.setTipoMovimiento("Entrada");
                    k.setReferencia("Stock inicial");
                    k.setFecha(LocalDate.now());
                    k.setCantidad(existencia);
                    k.setPrecioUnitario(costo);
                    k.setPrecioVenta(precio);
                    k.setExistenciaAnterior(BigDecimal.ZERO);
                    k.setExistenciaNueva(existencia);
                    k.setObservacion("Ingreso inicial al crear producto");
                    k.setUsuario(usuarioActual);
                    kardexRepo.save(k);
                }
            }
        }

        return guardado;
    }

    private BigDecimal parseBD(String val) {
        try { return val != null && !val.isBlank() ? new BigDecimal(val) : BigDecimal.ZERO; }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    // ─── Toggle activo/inactivo ───────────────────────────────────
    @Transactional
    public void toggleActivo(Integer id) throws Exception {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new Exception("Producto no encontrado."));
        p.setActivo(!p.getActivo());
        productoRepo.save(p);
    }

    // ─── Eliminar producto ────────────────────────────────────────
    @Transactional
    public void eliminar(Integer id) throws Exception {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new Exception("Producto no encontrado."));
        inventarioRepo.findAll().stream()
                .filter(i -> i.getProducto().getIdProducto().equals(id))
                .forEach(inventarioRepo::delete);
        kardexRepo.findByProducto(id)
                .forEach(kardexRepo::delete);   // ── NUEVO: limpiar kardex al eliminar
        productoRepo.delete(p);
    }

    // ─── Guardar categoría ────────────────────────────────────────
    @Transactional
    public Categoria guardarCategoria(String nombre) {
        return categoriaRepo.save(new Categoria() {{ setNombre(nombre.trim()); }});
    }

    // ─── Guardar marca ────────────────────────────────────────────
    @Transactional
    public Marca guardarMarca(String nombre) {
        return marcaRepo.save(new Marca() {{ setNombre(nombre.trim()); }});
    }

    // ─── Guardar tipo de lente ────────────────────────────────────
    @Transactional
    public Tipo_lente guardarTipoLente(String nombre) throws Exception {
        Tipo_lente t = new Tipo_lente();
        t.setNombre(nombre.trim());
        return tipoLenteRepo.save(t);
    }

    // ─── Guardar material ─────────────────────────────────────────
    @Transactional
    public Material_lente guardarMaterial(String nombre) throws Exception {
        Material_lente m = new Material_lente();
        m.setNombre(nombre.trim());
        return materialLenteRepo.save(m);
    }

    // ─── Guardar tratamiento ──────────────────────────────────────
    @Transactional
    public Tratamiento_lente guardarTratamiento(String nombre) throws Exception {
        Tratamiento_lente t = new Tratamiento_lente();
        t.setNombre(nombre.trim());
        return tratamientoLenteRepo.save(t);
    }

    // ─── Guardar / actualizar precio de lente ─────────────────────
    @Transactional
    public Precio_lente guardarPrecioLente(Integer idTipo, Integer idMaterial,
                                           Integer idTratamiento,
                                           BigDecimal costo, BigDecimal precioVenta) throws Exception {
        Tipo_lente        tipo        = tipoLenteRepo.findById(idTipo)
                .orElseThrow(() -> new Exception("Tipo de lente no encontrado."));
        Material_lente    material    = materialLenteRepo.findById(idMaterial)
                .orElseThrow(() -> new Exception("Material no encontrado."));
        Tratamiento_lente tratamiento = tratamientoLenteRepo.findById(idTratamiento)
                .orElseThrow(() -> new Exception("Tratamiento no encontrado."));

        Precio_lente precio = precioLenteRepo
                .findByCombinacion(idTipo, idMaterial, idTratamiento)
                .orElse(new Precio_lente());

        precio.setTipo(tipo);
        precio.setMaterial(material);
        precio.setTratamiento(tratamiento);
        precio.setCosto(costo);
        precio.setPrecioVenta(precioVenta);
        return precioLenteRepo.save(precio);
    }

    // ─── Stock total por producto ─────────────────────────────────
    public BigDecimal stockTotal(Integer idProducto) {
        return inventarioRepo.findAll().stream()
                .filter(i -> i.getProducto().getIdProducto().equals(idProducto))
                .map(Inventario::getExistencia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}