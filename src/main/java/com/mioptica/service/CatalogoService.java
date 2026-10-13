package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoService {
    private final ColorRepository colorRepo;
    private final MarcaRepository marcaRepo;
    private final MaterialArmazonRepository matArmRepo;
    private final FamiliaArmazonRepository famArmRepo;
    private final FormaArmazonRepository formaArmRepo;
    private final Tipo_lenteRepository tipoLenteRepo;
    private final Material_lenteRepository matLenteRepo;
    private final Tratamiento_lenteRepository tratLenteRepo;
    private final ClasificacionLenteRepository clasifLenteRepo;

    public List<Map<String, Object>> listar(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "color" -> colorRepo.findAll().stream().map(c -> Map.<String, Object>of("id", c.getIdColor(), "nombre", c.getNombre())).toList();
            case "marca" -> marcaRepo.findAll().stream().map(m -> Map.<String, Object>of("id", m.getIdMarca(), "nombre", m.getNombre())).toList();
            case "material-armazon" -> matArmRepo.findAll().stream().map(m -> Map.<String, Object>of("id", m.getIdMaterialArmazon(), "nombre", m.getNombre())).toList();
            case "familia-armazon" -> famArmRepo.findAll().stream().map(f -> Map.<String, Object>of("id", f.getIdFamiliaArmazon(), "nombre", f.getNombre())).toList();
            case "forma-armazon" -> formaArmRepo.findAll().stream().map(f -> Map.<String, Object>of("id", f.getIdFormaArmazon(), "nombre", f.getNombre())).toList();
            case "categoria-lente" -> tipoLenteRepo.findAll().stream().map(t -> Map.<String, Object>of("id", t.getIdTipo(), "nombre", t.getNombre())).toList();
            case "familia-lente" -> matLenteRepo.findAll().stream().map(m -> Map.<String, Object>of("id", m.getIdMaterial(), "nombre", m.getNombre())).toList();
            case "tratamiento-lente" -> tratLenteRepo.findAll().stream().map(t -> Map.<String, Object>of("id", t.getIdTratamiento(), "nombre", t.getNombre())).toList();
            case "clasificacion-lente" -> clasifLenteRepo.findAll().stream().map(c -> Map.<String, Object>of("id", c.getIdClasificacion(), "nombre", c.getNombre())).toList();
            default -> throw new IllegalArgumentException("Tipo de catálogo no válido: " + tipo);
        };
    }

    public Map<String, Object> crear(String tipo, String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        Map<String, Object> res = new HashMap<>();
        switch (tipo.toLowerCase()) {
            case "color" -> { Color c = new Color(); c.setNombre(nombre); c = colorRepo.save(c); res.put("id", c.getIdColor()); res.put("nombre", c.getNombre()); }
            case "marca" -> { Marca m = new Marca(); m.setNombre(nombre); m = marcaRepo.save(m); res.put("id", m.getIdMarca()); res.put("nombre", m.getNombre()); }
            case "material-armazon" -> { MaterialArmazon m = new MaterialArmazon(); m.setNombre(nombre); m = matArmRepo.save(m); res.put("id", m.getIdMaterialArmazon()); res.put("nombre", m.getNombre()); }
            case "familia-armazon" -> { FamiliaArmazon f = new FamiliaArmazon(); f.setNombre(nombre); f = famArmRepo.save(f); res.put("id", f.getIdFamiliaArmazon()); res.put("nombre", f.getNombre()); }
            case "forma-armazon" -> { FormaArmazon f = new FormaArmazon(); f.setNombre(nombre); f = formaArmRepo.save(f); res.put("id", f.getIdFormaArmazon()); res.put("nombre", f.getNombre()); }
            case "categoria-lente" -> { Tipo_lente t = new Tipo_lente(); t.setNombre(nombre); t = tipoLenteRepo.save(t); res.put("id", t.getIdTipo()); res.put("nombre", t.getNombre()); }
            case "familia-lente" -> { Material_lente m = new Material_lente(); m.setNombre(nombre); m = matLenteRepo.save(m); res.put("id", m.getIdMaterial()); res.put("nombre", m.getNombre()); }
            case "tratamiento-lente" -> { Tratamiento_lente t = new Tratamiento_lente(); t.setNombre(nombre); t = tratLenteRepo.save(t); res.put("id", t.getIdTratamiento()); res.put("nombre", t.getNombre()); }
            case "clasificacion-lente" -> { ClasificacionLente c = new ClasificacionLente(); c.setNombre(nombre); c = clasifLenteRepo.save(c); res.put("id", c.getIdClasificacion()); res.put("nombre", c.getNombre()); }
            default -> throw new IllegalArgumentException("Tipo de catálogo no válido: " + tipo);
        }
        res.put("ok", true);
        return res;
    }

    public Map<String, Object> actualizar(String tipo, Integer id, String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        Map<String, Object> res = new HashMap<>();
        switch (tipo.toLowerCase()) {
            case "color" -> { Color c = colorRepo.findById(id).orElseThrow(); c.setNombre(nombre); colorRepo.save(c); }
            case "marca" -> { Marca m = marcaRepo.findById(id).orElseThrow(); m.setNombre(nombre); marcaRepo.save(m); }
            case "material-armazon" -> { MaterialArmazon m = matArmRepo.findById(id).orElseThrow(); m.setNombre(nombre); matArmRepo.save(m); }
            case "familia-armazon" -> { FamiliaArmazon f = famArmRepo.findById(id).orElseThrow(); f.setNombre(nombre); famArmRepo.save(f); }
            case "forma-armazon" -> { FormaArmazon f = formaArmRepo.findById(id).orElseThrow(); f.setNombre(nombre); formaArmRepo.save(f); }
            case "categoria-lente" -> { Tipo_lente t = tipoLenteRepo.findById(id).orElseThrow(); t.setNombre(nombre); tipoLenteRepo.save(t); }
            case "familia-lente" -> { Material_lente m = matLenteRepo.findById(id).orElseThrow(); m.setNombre(nombre); matLenteRepo.save(m); }
            case "tratamiento-lente" -> { Tratamiento_lente t = tratLenteRepo.findById(id).orElseThrow(); t.setNombre(nombre); tratLenteRepo.save(t); }
            case "clasificacion-lente" -> { ClasificacionLente c = clasifLenteRepo.findById(id).orElseThrow(); c.setNombre(nombre); clasifLenteRepo.save(c); }
            default -> throw new IllegalArgumentException("Tipo de catálogo no válido: " + tipo);
        }
        res.put("ok", true);
        res.put("nombre", nombre);
        return res;
    }

    public Map<String, Object> eliminar(String tipo, Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            switch (tipo.toLowerCase()) {
                case "color" -> colorRepo.deleteById(id);
                case "marca" -> marcaRepo.deleteById(id);
                case "material-armazon" -> matArmRepo.deleteById(id);
                case "familia-armazon" -> famArmRepo.deleteById(id);
                case "forma-armazon" -> formaArmRepo.deleteById(id);
                case "categoria-lente" -> tipoLenteRepo.deleteById(id);
                case "familia-lente" -> matLenteRepo.deleteById(id);
                case "tratamiento-lente" -> tratLenteRepo.deleteById(id);
                case "clasificacion-lente" -> clasifLenteRepo.deleteById(id);
                default -> throw new IllegalArgumentException("Tipo de catálogo no válido: " + tipo);
            }
            res.put("ok", true);
        } catch (Exception e) {
            res.put("error", "No se puede eliminar porque está en uso por uno o más productos.");
            res.put("ok", false);
        }
        return res;
    }
}
