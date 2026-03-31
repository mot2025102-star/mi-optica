package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository    productoRepo;
    private final CategoriaRepository   categoriaRepo;
    private final MarcaRepository       marcaRepo;
    private final InventarioRepository  inventarioRepo;
    private final SucursalRepository    sucursalRepo;

    // ─── Listas ───────────────────────────────────────────────────
    public List<Producto>  listarTodos()      { return productoRepo.findAll(); }
    public List<Producto>  listarActivos()    { return productoRepo.findByActivoTrueOrderByDetalleAsc(); }
    public List<Producto>  buscar(String q)   { return productoRepo.buscar(q); }
    public List<Categoria> listarCategorias() { return categoriaRepo.findAllByOrderByNombreAsc(); }
    public List<Marca>     listarMarcas()     { return marcaRepo.findAllByOrderByNombreAsc(); }
    public List<Sucursal>  listarSucursales() { return sucursalRepo.findByActivoTrue(); }

    // ─── Obtener uno ──────────────────────────────────────────────
    public Optional<Producto> findById(Integer id) { return productoRepo.findById(id); }

    // ─── Guardar producto (sin stock inicial) ─────────────────────
    @Transactional
    public Producto guardar(Producto producto) throws Exception {
        return guardar(producto, new HashMap<>());
    }

    // ─── Guardar producto (con stock inicial por sucursal) ────────
    @Transactional
    public Producto guardar(Producto producto, Map<String, String> stockParams) throws Exception {
        Optional<Producto> existente = productoRepo.findByCodigo(producto.getCodigo());
        if (existente.isPresent()
                && !existente.get().getIdProducto().equals(producto.getIdProducto())) {
            throw new Exception("Ya existe un producto con el código: " + producto.getCodigo());
        }

        boolean esNuevo = (producto.getIdProducto() == null);
        Producto guardado = productoRepo.save(producto);

        if (esNuevo) {
            for (Sucursal suc : sucursalRepo.findByActivoTrue()) {
                String sid = String.valueOf(suc.getIdSucursal());

                // ✅ Solo crear si el usuario incluyó esa sucursal (checkbox marcado)
                if (!stockParams.containsKey("existencia_" + sid)) continue;

                boolean yaExiste = inventarioRepo
                        .findByProductoAndSucursal(guardado, suc).isPresent();
                if (!yaExiste) {
                    BigDecimal existencia = parseBD(stockParams.get("existencia_" + sid));
                    BigDecimal costo      = parseBD(stockParams.get("costo_"      + sid));
                    BigDecimal precio     = parseBD(stockParams.get("precio_"     + sid));

                    Inventario inv = new Inventario();
                    inv.setProducto(guardado);
                    inv.setSucursal(suc);
                    inv.setExistencia(existencia);
                    inv.setCosto(costo);
                    inv.setPrecioVenta(precio);
                    inventarioRepo.save(inv);
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

    // ─── Eliminar producto completamente ─────────────────────────
    @Transactional
    public void eliminar(Integer id) throws Exception {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new Exception("Producto no encontrado."));
        inventarioRepo.findAll().stream()
                .filter(i -> i.getProducto().getIdProducto().equals(id))
                .forEach(inventarioRepo::delete);
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

    // ─── Stock total por producto (suma ambas sucursales) ─────────
    public BigDecimal stockTotal(Integer idProducto) {
        return inventarioRepo.findAll().stream()
                .filter(i -> i.getProducto().getIdProducto().equals(idProducto))
                .map(Inventario::getExistencia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
