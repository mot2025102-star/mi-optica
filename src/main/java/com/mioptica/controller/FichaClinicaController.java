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
import org.springframework.http.ResponseEntity;

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

        model.addAttribute("fichas",              fichas);
        model.addAttribute("q",                   q);
        model.addAttribute("esAdmin",             esAdmin);
        model.addAttribute("entregasPendientes",  entregasPendientes);
        model.addAttribute("conSaldo",            conSaldo);
        model.addAttribute("activePage",          "fichas");
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
        ficha.setFechaSiguienteConsulta(LocalDate.now().plusYears(1));
        ficha.setOptometrista(usuario);

        if (idCliente != null) {
            clienteRepo.findById(idCliente).ifPresent(ficha::setCliente);
        }

        model.addAttribute("ficha",      ficha);
        model.addAttribute("clientes",   clienteRepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("sucursales", sucursalRepo.findByActivoTrue());
        model.addAttribute("optometras", usuarioRepo.findAll());
        model.addAttribute("editando",   false);
        model.addAttribute("activePage", "fichas");
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
            // ── RX SUBJETIVO ──────────────────────────────────────
            ficha.setSubOdEsfera(params.getOrDefault("sub_odEsfera",   ""));
            ficha.setSubOdCilindro(params.getOrDefault("sub_odCilindro",""));
            ficha.setSubOdEje(params.getOrDefault("sub_odEje",         ""));
            ficha.setSubOdAdicion(params.getOrDefault("sub_odAdicion", ""));
            ficha.setSubOiEsfera(params.getOrDefault("sub_oiEsfera",   ""));
            ficha.setSubOiCilindro(params.getOrDefault("sub_oiCilindro",""));
            ficha.setSubOiEje(params.getOrDefault("sub_oiEje",         ""));
            ficha.setSubOiAdicion(params.getOrDefault("sub_oiAdicion", ""));

            // ── RX FINAL ──────────────────────────────────────────
            ficha.setRxOdEsfera(params.getOrDefault("rx_odEsfera",     ""));
            ficha.setRxOdCilindro(params.getOrDefault("rx_odCilindro", ""));
            ficha.setRxOdEje(params.getOrDefault("rx_odEje",           ""));
            ficha.setRxOdAdicion(params.getOrDefault("rx_odAdicion",   ""));
            ficha.setRxOiEsfera(params.getOrDefault("rx_oiEsfera",     ""));
            ficha.setRxOiCilindro(params.getOrDefault("rx_oiCilindro", ""));
            ficha.setRxOiEje(params.getOrDefault("rx_oiEje",           ""));
            ficha.setRxOiAdicion(params.getOrDefault("rx_oiAdicion",   ""));

            parseBigDecimal(params.get("rx_odAltura")).ifPresent(ficha::setRxOdAltura);
            parseBigDecimal(params.get("rx_oiAltura")).ifPresent(ficha::setRxOiAltura);
            parseBigDecimal(params.get("rx_dip")).ifPresent(ficha::setRxDip);
            parseBigDecimal(params.get("rx_ndpOd")).ifPresent(ficha::setRxNdpOd);
            parseBigDecimal(params.get("rx_ndpOi")).ifPresent(ficha::setRxNdpOi);

            // ── SUGERENCIA DE MATERIALES ──────────────────────────
            ficha.setSugTipoLente(params.getOrDefault("sug_tipoLente",     ""));
            ficha.setSugMaterialLente(params.getOrDefault("sug_materialLente",""));
            ficha.setSugColor(params.getOrDefault("sug_color",             ""));
            ficha.setSugObservaciones(params.getOrDefault("sug_observaciones",""));

            // Tratamientos sugeridos → guardar como lista separada por comas
            String[] tratamientos = {"antireflejo","bloqueador_luz_azul","filtro_solar",
                "fotocromatico","polarizado","endurecido","hidrofobico","uv"};
            StringBuilder sugTrat = new StringBuilder();
            for (String t : tratamientos) {
                if (params.containsKey("sug_trat_" + t)) {
                    if (sugTrat.length() > 0) sugTrat.append(",");
                    sugTrat.append(t);
                }
            }
            ficha.setSugTratamientos(sugTrat.toString());

            // ── HISTORIA CLÍNICA (JSON) ───────────────────────────
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
                "inflamacion_palpebral","conjuntiva_hiperemica","pupilas_anomalas",
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
            json.append("\"medicamentos\":\"").append(esc(params.get("hc_medicamentos"))).append("\",");

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
            json.append("\"antfam_otros\":\"").append(esc(params.get("hc_antfam_otros"))).append("\",");

            // Historia visual
            json.append("\"usa_lentes\":\"").append(esc(params.get("hc_usa_lentes"))).append("\",");
            json.append("\"ultimo_examen\":\"").append(esc(params.get("hc_ultimo_examen"))).append("\",");
            json.append("\"satisfaccion\":\"").append(esc(params.get("hc_satisfaccion"))).append("\",");
            json.append("\"tiempo_uso\":\"").append(esc(params.get("hc_tiempo_uso"))).append("\",");

            // Tipos de lente usados
            json.append("\"tipos_lente_usados\":[");
            String[] tiposLente = {"monofocal","bifocal","progresivo","lectura"};
            first = true;
            for (String t : tiposLente) {
                if (params.containsKey("hc_tipo_lente_" + t)) {
                    if (!first) json.append(",");
                    json.append("\"").append(t).append("\"");
                    first = false;
                }
            }
            json.append("],");

            // Antecedentes visuales
            json.append("\"antecedentes_visuales\":[");
            String[] antVis = {"miopia","hipermetropia","astigmatismo","presbicia",
                "ambliopia","estrabismo_vis","cirugias_oculares"};
            first = true;
            for (String s : antVis) {
                if (params.containsKey("hc_antvis_" + s)) {
                    if (!first) json.append(",");
                    json.append("\"").append(s).append("\"");
                    first = false;
                }
            }
            json.append("],");

            // Hábitos visuales
            json.append("\"horas_pantalla\":\"").append(esc(params.get("hc_horas_pantalla"))).append("\",");
            json.append("\"distancia_trabajo\":\"").append(esc(params.get("hc_distancia_trabajo"))).append("\",");
            json.append("\"iluminacion\":\"").append(esc(params.get("hc_iluminacion"))).append("\",");

            // Actividades frecuentes
            json.append("\"actividades\":[");
            String[] actividades = {"lectura","computadora","celular","conduccion","manualidades"};
            first = true;
            for (String a : actividades) {
                if (params.containsKey("hc_actividad_" + a)) {
                    if (!first) json.append(",");
                    json.append("\"").append(a).append("\"");
                    first = false;
                }
            }
            json.append("],");

            // Pruebas preliminares
            json.append("\"cover_test\":\"").append(esc(params.get("hc_cover_test"))).append("\",");
            json.append("\"ojo_dominante\":\"").append(esc(params.get("hc_ojo_dominante"))).append("\",");
            json.append("\"motilidad\":\"").append(esc(params.get("hc_motilidad"))).append("\",");
            json.append("\"pio_od\":\"").append(esc(params.get("hc_pio_od"))).append("\",");
            json.append("\"pio_oi\":\"").append(esc(params.get("hc_pio_oi"))).append("\",");

            // Pruebas preliminares adicionales (punto 10)
            json.append("\"coverte_test\":\"").append(esc(params.get("hc_coverte_test"))).append("\",");
            json.append("\"test_ishinara\":\"").append(esc(params.get("hc_test_ishinara"))).append("\",");
            json.append("\"estereopsis\":\"").append(esc(params.get("hc_estereopsis"))).append("\",");
            json.append("\"campo_visual\":\"").append(esc(params.get("hc_campo_visual"))).append("\",");
            json.append("\"motricidad_ocular\":\"").append(esc(params.get("hc_motricidad_ocular"))).append("\",");
            json.append("\"prueba_ambulatoria_tolera\":").append(params.containsKey("hc_prueba_ambulatoria_tolera") ? "true" : "false").append(",");
            json.append("\"pruebas_preliminares_observaciones\":\"").append(esc(params.get("hc_pruebas_preliminares_observaciones"))).append("\"");

            json.append("}");
            ficha.setHistoriaClinica(json.toString());

            fichaService.guardar(ficha);
            ra.addFlashAttribute("mensajeOk",
                ficha.getIdFicha() == null
                    ? "Ficha clínica creada correctamente."
                    : "Ficha clínica actualizada correctamente.");

        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al guardar: " + e.getMessage());
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

    // ─── API: última ficha clínica de un cliente (Rx Final) ──────
    @GetMapping("/api/ultima-rx")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> ultimaRx(
            @RequestParam Integer idCliente) {

        java.util.Map<String, String> rx = new java.util.LinkedHashMap<>();

        // ── Edad del cliente (viene de Clientes, no se duplica el dato) ──
        clienteRepo.findById(idCliente).ifPresent(c -> {
            Integer edad = c.getEdad() != null ? c.getEdad() : c.getEdadCalculada();
            rx.put("edad", edad != null ? edad.toString() : "");
        });

        List<FichaClinica> fichas =
            fichaService.listarPorCliente(idCliente);

        if (fichas.isEmpty()) {
            return ResponseEntity.ok(rx);
        }

        FichaClinica f = fichas.get(0); // la más reciente

        rx.put("rxOdEsfera",   f.getRxOdEsfera()   != null ? f.getRxOdEsfera()   : "");
        rx.put("rxOdCilindro", f.getRxOdCilindro() != null ? f.getRxOdCilindro() : "");
        rx.put("rxOdEje",      f.getRxOdEje()      != null ? f.getRxOdEje()      : "");
        rx.put("rxOdAdicion",  f.getRxOdAdicion()  != null ? f.getRxOdAdicion()  : "");
        rx.put("rxOiEsfera",   f.getRxOiEsfera()   != null ? f.getRxOiEsfera()   : "");
        rx.put("rxOiCilindro", f.getRxOiCilindro() != null ? f.getRxOiCilindro() : "");
        rx.put("rxOiEje",      f.getRxOiEje()      != null ? f.getRxOiEje()      : "");
        rx.put("rxOiAdicion",  f.getRxOiAdicion()  != null ? f.getRxOiAdicion()  : "");
        rx.put("fechaFicha",   f.getFecha()        != null ? f.getFecha().toString() : "");

        return ResponseEntity.ok(rx);
    }

    // ─── Helpers privados ─────────────────────────────────────────

    /** Escapa comillas para no romper el JSON manual */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    /** Parsea BigDecimal de forma segura, retorna empty si es null o vacío */
    private java.util.Optional<BigDecimal> parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(new BigDecimal(s));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}