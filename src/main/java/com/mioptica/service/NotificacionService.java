package com.mioptica.service;

import com.mioptica.model.Inventario;
import com.mioptica.model.Notificacion;
import com.mioptica.model.Venta;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.repository.NotificacionRepository;
import com.mioptica.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepo;
    private final InventarioRepository inventarioRepo;
    private final VentaRepository ventaRepo;

    @Scheduled(fixedRate = 300_000) // cada 5 minutos
    public void revisarReglas() {
        revisarStock();
        revisarEntregas();
        revisarCobros();
    }

    private void revisarStock() {
        for (Inventario inv : inventarioRepo.findAgotadosGlobal()) {
            crearSiNoExiste("STOCK", "CRITICO",
                    "Stock agotado",
                    inv.getProducto().getDetalle() + " se quedó en 0 unidades",
                    "/inventario");
        }
        for (Inventario inv : inventarioRepo.findBajosGlobal()) {
            crearSiNoExiste("STOCK", "ATENCION",
                    "Stock bajo",
                    inv.getProducto().getDetalle() + " tiene solo " + inv.getExistencia() + " unidades",
                    "/inventario");
        }
    }

    private void revisarEntregas() {
        LocalDate manana = LocalDate.now().plusDays(1);
        for (Venta v : ventaRepo.findEntregasPendientes()) {
            if (v.getFechaEntrega() != null && (v.getFechaEntrega().isEqual(manana) || v.getFechaEntrega().isBefore(manana))) {
                crearSiNoExiste("ENTREGA", "INFO",
                        "Entrega próxima o atrasada",
                        "Los lentes de " + (v.getCliente() != null ? v.getCliente().getNombre() : "Cliente Anónimo") + " se entregan pronto",
                        "/ventas/ver/" + v.getIdVenta());
            }
        }
    }

    private void revisarCobros() {
        List<Venta> vencidas = ventaRepo.findConSaldoPendiente();
        if (!vencidas.isEmpty()) {
            crearSiNoExiste("COBRO", "ATENCION",
                    "Cuentas pendientes",
                    vencidas.size() + " clientes tienen saldo pendiente",
                    "/saldos");
        }
    }

    private void crearSiNoExiste(String tipo, String sev, String titulo, String msg, String url) {
        if (!notificacionRepo.existsByTipoAndTituloAndMensajeAndLeidaFalse(tipo, titulo, msg)) {
            notificacionRepo.save(new Notificacion(tipo, sev, titulo, msg, url));
        }
    }
}
