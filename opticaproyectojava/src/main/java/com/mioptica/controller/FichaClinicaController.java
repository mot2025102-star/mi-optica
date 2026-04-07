package com.mioptica.controller;

import com.mioptica.model.FichaClinica;
import com.mioptica.repository.ClienteRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.FichaClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/fichas")
@RequiredArgsConstructor
public class FichaClinicaController {

    private final FichaClinicaService fichaService;
    private final ClienteRepository   clienteRepo;
    private final SucursalRepository  sucursalRepo;
    private final UsuarioRepository   usuarioRepo;

    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    public String lista(
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        List<FichaClinica> fichas = fichaService.listarTodas();

        // Filtrar por nombre de cliente si hay búsqueda
        if (!q.isBlank()) {
            String qLower = q.toLowerCase();
            fichas = fichas.stream()
                    .filter(f -> f.getCliente().getNombre().toLowerCase().contains(qLower))
                    .toList();
        }
        long entregasPendientes = fichas.stream()
            .filter(f -> !"Entregado".equals(f.getEstadoEntrega()) && f.getFechaEntrega() != null)
            .count();
        long conSaldo = fichas.stream()
            .filter(f -> f.getSaldo() != null && f.getSaldo().compareTo(BigDecimal.ZERO) > 0)
            .count();

        model.addAttribute("fichas",     fichas);
        model.addAttribute("q",          q);
        model.addAttribute("esAdmin",    esAdmin);
        model.addAttribute("activePage", "fichas");
        return "fichas/lista";
    }

    // ─── FORMULARIO NUEVA FICHA ───────────────────────────────────
    @GetMapping("/nueva")
    public String nueva(
            @RequestParam(required = false) Integer idCliente,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();

        FichaClinica ficha = new FichaClinica();
        ficha.setFecha(LocalDate.now());
        ficha.setOptometrista(usuario);

        // Si viene de un cliente específico, prellenar
        if (idCliente != null) {
            clienteRepo.findById(idCliente).ifPresent(ficha::setCliente);
        }

        model.addAttribute("ficha",       ficha);
        model.addAttribute("clientes",    clienteRepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("sucursales",  sucursalRepo.findByActivoTrue());
        model.addAttribute("optometras",  usuarioRepo.findAll());
        model.addAttribute("editando",    false);
        model.addAttribute("activePage",  "fichas");
        return "fichas/formulario";
    }

    // ─── FORMULARIO EDITAR ────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return fichaService.findById(id).map(f -> {
            model.addAttribute("ficha",      f);
            model.addAttribute("clientes",   clienteRepo.findByActivoTrueOrderByNombreAsc());
            model.addAttribute("sucursales", sucursalRepo.findByActivoTrue());
            model.addAttribute("optometras", usuarioRepo.findAll());
            model.addAttribute("editando",   true);
            model.addAttribute("activePage", "fichas");
            return "fichas/formulario";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensajeError", "Ficha no encontrada.");
            return "redirect:/fichas";
        });
    }

    // ─── GUARDAR ──────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("ficha") FichaClinica ficha,
            @RequestParam java.util.Map<String, String> params,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {

        try {
            // ── Construir JSON de historia clínica ────────────────
            StringBuilder json = new StringBuilder("{");

            // Síntomas
            json.append("\"sintomas\":[");
            String[] sintomas = {"dolor_cabeza","mareos","vision_borrosa_lejos","vision_borrosa_cerca",
                "dificultad_enfocar","vision_doble","cansancio_visual","fotofobia","halos_luz",
                "ojos_secos","lagrimeo","ardor","picazon","irritacion_ocular","dolor_ocular",
                "cuerpo_extrano","secrecion"};
            boolean first = true;
            for (String s : sintomas) {
                if (params.containsKey("hc_sint_" + s)) {
                    if (!first) json.append(",");
                    json.append("\"").append(s).append("\"");
                    first = false;
                }
            }
            json.append("],");

            // Signos
            json.append("\"signos\":[");
            String[] signos = {"entrecierra_ojos","parpadeo_excesivo","enrojecimiento",
                "inflamacion_palpebral","conjuntiva_hiperémica","pupilas_anomalas",
                "catarata_aparente","estrabismo_evidente","nistagmo"};
            first = true;
            for (String s : signos) {
                if (params.containsKey("hc_sign_" + s)) {
                    if (!first) json.append(",");
                    json.append("\"").append(s).append("\"");
                    first = false;
                }
            }
            json.append("],");

            // Antecedentes personales
            json.append("\"antecedentes_personales\":[");
            String[] antPer = {"diabetes","hipertension","tiroides","migranas","alergias",
                "autoinmunes","cirugias_previas"};
            first = true;
            for (String s : antPer) {
                if (params.containsKey("hc_antper_" + s)) {
                    if (!first) json.append(",");
                    json.append("\"").append(s).append("\"");
                    first = false;
                }
            }
            json.append("],");
            json.append("\"medicamentos\":\"").append(params.getOrDefault("hc_medicamentos","")).append("\",");

            // Antecedentes familiares
            json.append("\"antecedentes_familiares\":[");
            String[] antFam = {"glaucoma","cataratas","retinopatias","degeneracion_macular",
                "estrabismo_fam","ceguera"};
            first = true;
            for (String s : antFam) {
                if (params.containsKey("hc_antfam_" + s)) {
                    if (!first) json.append(",");
                    json.append("\"").append(s).append("\"");
                    first = false;
                }
            }
            json.append("],");
            json.append("\"antfam_otros\":\"").append(params.getOrDefault("hc_antfam_otros","")).append("\",");

            // Historia visual
            json.append("\"usa_lentes\":\"").append(params.getOrDefault("hc_usa_lentes","")).append("\",");
            json.append("\"ultimo_examen\":\"").append(params.getOrDefault("hc_ultimo_examen","")).append("\",");
            json.append("\"satisfaccion\":\"").append(params.getOrDefault("hc_satisfaccion","")).append("\",");
            json.append("\"tiempo_uso\":\"").append(params.getOrDefault("hc_tiempo_uso","")).append("\",");

            // Hábitos visuales
            json.append("\"horas_pantalla\":\"").append(params.getOrDefault("hc_horas_pantalla","")).append("\",");
            json.append("\"distancia_trabajo\":\"").append(params.getOrDefault("hc_distancia_trabajo","")).append("\",");
            json.append("\"iluminacion\":\"").append(params.getOrDefault("hc_iluminacion","")).append("\",");

            // Pruebas preliminares
            json.append("\"cover_test\":\"").append(params.getOrDefault("hc_cover_test","")).append("\",");
            json.append("\"ojo_dominante\":\"").append(params.getOrDefault("hc_ojo_dominante","")).append("\",");
            json.append("\"motilidad\":\"").append(params.getOrDefault("hc_motilidad","")).append("\",");
            json.append("\"pio_od\":\"").append(params.getOrDefault("hc_pio_od","")).append("\",");
            json.append("\"pio_oi\":\"").append(params.getOrDefault("hc_pio_oi","")).append("\",");

            // Graduación actual (receta final)
            json.append("\"rx\":{");
            json.append("\"od_esfera\":\"").append(params.getOrDefault("rx_odEsfera","")).append("\",");
            json.append("\"od_cilindro\":\"").append(params.getOrDefault("rx_odCilindro","")).append("\",");
            json.append("\"od_eje\":\"").append(params.getOrDefault("rx_odEje","")).append("\",");
            json.append("\"od_adicion\":\"").append(params.getOrDefault("rx_odAdicion","")).append("\",");
            json.append("\"od_altura\":\"").append(params.getOrDefault("rx_odAltura","")).append("\",");
            json.append("\"oi_esfera\":\"").append(params.getOrDefault("rx_oiEsfera","")).append("\",");
            json.append("\"oi_cilindro\":\"").append(params.getOrDefault("rx_oiCilindro","")).append("\",");
            json.append("\"oi_eje\":\"").append(params.getOrDefault("rx_oiEje","")).append("\",");
            json.append("\"oi_adicion\":\"").append(params.getOrDefault("rx_oiAdicion","")).append("\",");
            json.append("\"oi_altura\":\"").append(params.getOrDefault("rx_oiAltura","")).append("\",");
            json.append("\"dip\":\"").append(params.getOrDefault("rx_dip","")).append("\",");
            json.append("\"ndp_od\":\"").append(params.getOrDefault("rx_ndpOd","")).append("\",");
            json.append("\"ndp_oi\":\"").append(params.getOrDefault("rx_ndpOi","")).append("\"");
            json.append("}");

            json.append("}");

            ficha.setHistoriaClinica(json.toString());

            fichaService.guardar(ficha);
            ra.addFlashAttribute("mensajeOk",
                    ficha.getIdFicha() == null
                    ? "Ficha clínica creada correctamente."
                    : "Ficha clínica actualizada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/fichas";
    }

    // ─── ELIMINAR ─────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            fichaService.eliminar(id);
            ra.addFlashAttribute("mensajeOk", "Ficha eliminada.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/fichas";
    }
}