package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TrasladoService {

    private final InventarioRepository inventarioRepo;
    private final KardexRepository kardexRepo;
    private final ProductoRepository productoRepo;
    private final SucursalRepository sucursalRepo;
    private final UsuarioRepository usuarioRepo;
    private final TrasladoRepository trasladoRepo;
    private final DetalleTrasladoRepository detalleTrasladoRepo;

    @Transactional
    public void realizarTraslado(
            Integer idProducto,
            Integer idSucursalOrigen,
            Integer idSucursalDestino,
            BigDecimal cantidad,
            String referencia,
            String observacion,
            String username
    ) throws Exception {

        if (idProducto == null || idProducto <= 0) throw new Exception("Selecciona un producto.");
        if (idSucursalOrigen == null || idSucursalOrigen <= 0) throw new Exception("Selecciona la sucursal origen.");
        if (idSucursalDestino == null || idSucursalDestino <= 0) throw new Exception("Selecciona la sucursal destino.");
        if (idSucursalOrigen.equals(idSucursalDestino)) throw new Exception("La sucursal destino no puede ser la misma que el origen.");
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("La cantidad debe ser mayor a 0.");

        Usuario usuario = usuarioRepo.findByUsername(username).orElseThrow(() -> new Exception("Usuario no encontrado."));
        Producto producto = productoRepo.findById(idProducto).orElseThrow(() -> new Exception("Producto no encontrado."));
        Sucursal sucOrigen = sucursalRepo.findById(idSucursalOrigen).orElseThrow(() -> new Exception("Sucursal origen no encontrada."));
        Sucursal sucDestino = sucursalRepo.findById(idSucursalDestino).orElseThrow(() -> new Exception("Sucursal destino no encontrada."));

        Inventario invOrigen = inventarioRepo.findByProductoAndSucursal(producto, sucOrigen)
                .orElseThrow(() -> new Exception("No hay inventario para ese producto en la sucursal origen."));

        BigDecimal existenciaOrigenAntes = invOrigen.getExistencia() != null ? invOrigen.getExistencia() : BigDecimal.ZERO;
        if (cantidad.compareTo(existenciaOrigenAntes) > 0) {
            throw new Exception("La cantidad no puede ser mayor al stock disponible (" + existenciaOrigenAntes + ").");
        }

        Inventario invDestino = inventarioRepo.findByProductoAndSucursal(producto, sucDestino)
                .orElseGet(() -> {
                    Inventario nuevo = new Inventario();
                    nuevo.setProducto(producto);
                    nuevo.setSucursal(sucDestino);
                    nuevo.setExistencia(BigDecimal.ZERO);
                    nuevo.setCosto(invOrigen.getCosto());
                    nuevo.setPrecioVenta(invOrigen.getPrecioVenta());
                    return nuevo;
                });

        BigDecimal existenciaDestinoAntes = invDestino.getExistencia() != null ? invDestino.getExistencia() : BigDecimal.ZERO;

        BigDecimal existenciaOrigenNueva = existenciaOrigenAntes.subtract(cantidad);
        BigDecimal existenciaDestinoNueva = existenciaDestinoAntes.add(cantidad);

        // Paso A: descuenta origen
        invOrigen.setExistencia(existenciaOrigenNueva);
        inventarioRepo.save(invOrigen);

        // Paso B: suma destino
        invDestino.setExistencia(existenciaDestinoNueva);
        inventarioRepo.save(invDestino);

        // Persistimos registro de traslado (cabecera + detalle)
        Traslado t = new Traslado();
        t.setNumeroRegistro(referencia != null && !referencia.isBlank() ? referencia.trim() : null);
        t.setSucursalOrigen(sucOrigen);
        t.setSucursalDestino(sucDestino);
        t.setUsuario(usuario);
        t.setFecha(LocalDate.now());
        t.setNota(observacion);
        t.setEstado("Completado");
        trasladoRepo.save(t);

        DetalleTraslado dt = new DetalleTraslado();
        dt.setTraslado(t);
        dt.setProducto(producto);
        dt.setCantidad(cantidad);
        detalleTrasladoRepo.save(dt);

        String ref = (referencia != null && !referencia.isBlank()) ? referencia.trim() : null;
        LocalDate hoy = LocalDate.now();

        // Kardex: Salida (Traslado_Salida) en origen
        Kardex kSalida = new Kardex();
        kSalida.setProducto(producto);
        kSalida.setSucursal(sucOrigen);
        kSalida.setTipoMovimiento("Traslado_Salida");
        kSalida.setReferencia(ref);
        kSalida.setFecha(hoy);
        kSalida.setCantidad(BigDecimal.ZERO);
        kSalida.setPrecioUnitario(invOrigen.getCosto() != null ? invOrigen.getCosto() : BigDecimal.ZERO);
        kSalida.setPrecioVenta(invOrigen.getPrecioVenta() != null ? invOrigen.getPrecioVenta() : BigDecimal.ZERO);
        kSalida.setEgreso(cantidad);
        kSalida.setFechaEgreso(hoy);
        kSalida.setExistenciaAnterior(existenciaOrigenAntes);
        kSalida.setExistenciaNueva(existenciaOrigenNueva);
        kSalida.setObservacion("Traslado enviado a " + sucDestino.getNombre() + (observacion != null && !observacion.isBlank() ? ". " + observacion : ""));
        kSalida.setUsuario(usuario);
        kardexRepo.save(kSalida);

        // Kardex: Entrada (Traslado_Entrada) en destino
        Kardex kEntrada = new Kardex();
        kEntrada.setProducto(producto);
        kEntrada.setSucursal(sucDestino);
        kEntrada.setTipoMovimiento("Traslado_Entrada");
        kEntrada.setReferencia(ref);
        kEntrada.setFecha(hoy);
        kEntrada.setCantidad(cantidad);
        kEntrada.setPrecioUnitario(invDestino.getCosto() != null ? invDestino.getCosto() : BigDecimal.ZERO);
        kEntrada.setPrecioVenta(invDestino.getPrecioVenta() != null ? invDestino.getPrecioVenta() : BigDecimal.ZERO);
        kEntrada.setEgreso(BigDecimal.ZERO);
        kEntrada.setExistenciaAnterior(existenciaDestinoAntes);
        kEntrada.setExistenciaNueva(existenciaDestinoNueva);
        kEntrada.setObservacion("Traslado recibido de " + sucOrigen.getNombre() + (observacion != null && !observacion.isBlank() ? ". " + observacion : ""));
        kEntrada.setUsuario(usuario);
        kardexRepo.save(kEntrada);
    }
}

