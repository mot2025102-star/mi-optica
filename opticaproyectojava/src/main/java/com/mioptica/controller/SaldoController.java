package com.mioptica.controller;

import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.SaldoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SaldoController {

    private final SaldoService      saldoService;
    private final UsuarioRepository usuarioRepo;

    // ─── SALDOS PENDIENTES ────────────────────────────────────────
    @GetMapping("/saldos")
    public String saldos(
            @RequestParam(defaultValue = "saldos") String tab,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        Integer idSuc   = usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null;
        if (idSuc == null) esAdmin = true;

        var saldos    = saldoService.saldosPendientes(idSuc, esAdmin);
        var entregas  = saldoService.entregasPendientes(idSuc, esAdmin);

        var totalDeuda        = saldoService.totalSaldoPendiente(idSuc, esAdmin);
        long entregasVencidas = saldoService.entregasVencidas(idSuc, esAdmin);
        long entregasProximas = saldoService.entregasProximas7Dias(idSuc, esAdmin);

        model.addAttribute("saldos",          saldos);
        model.addAttribute("entregas",         entregas);
        model.addAttribute("totalDeuda",       totalDeuda);
        model.addAttribute("entregasVencidas", entregasVencidas);
        model.addAttribute("entregasProximas", entregasProximas);
        model.addAttribute("tab",              tab);
        model.addAttribute("esAdmin",          esAdmin);
        model.addAttribute("activePage",       "saldos");
        return "saldos/lista";
    }

    // ─── REGISTRAR PAGO (REST) ────────────────────────────────────
    @PostMapping("/saldos/pagar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pagar(
            @RequestParam Integer    idFicha,
            @RequestParam BigDecimal monto,
            @RequestParam(defaultValue = "Contado") String formaPago,
            @RequestParam(defaultValue = "")        String nota,
            @AuthenticationPrincipal UserDetails ud) {

        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            BigDecimal nuevoSaldo = saldoService.registrarPago(
                    idFicha, monto, formaPago, nota, usuario.getIdUsuario());

            return ResponseEntity.ok(Map.of(
                    "ok",         true,
                    "nuevoSaldo", nuevoSaldo,
                    "mensaje",    nuevoSaldo.compareTo(BigDecimal.ZERO) == 0
                                  ? "¡Saldo liquidado completamente! ✅"
                                  : "Pago registrado. Saldo restante: Q" + nuevoSaldo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    // ─── MARCAR COMO ENTREGADO ────────────────────────────────────
    @PostMapping("/saldos/entregar/{id}")
    public String entregar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            saldoService.marcarEntregado(id);
            ra.addFlashAttribute("mensajeOk", "Entrega marcada. Fecha registrada hoy.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/saldos?tab=entregas";
    }
}