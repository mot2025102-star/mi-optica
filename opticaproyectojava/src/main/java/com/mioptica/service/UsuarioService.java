package com.mioptica.service;

import com.mioptica.dto.UsuarioRequest;
import com.mioptica.model.Rol;
import com.mioptica.model.Sucursal;
import com.mioptica.model.Usuario;
import com.mioptica.repository.RolRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository  usuarioRepo;
    private final RolRepository      rolRepo;
    private final SucursalRepository sucursalRepo;
    private final PasswordEncoder    passwordEncoder; // BCrypt inyectado desde SecurityConfig

    // ─── Listas ───────────────────────────────────────────────────
    public List<Usuario>  listarTodos()    { return usuarioRepo.findAllByOrderByActivoDescNombreCompletoAsc(); }
    public List<Rol>      listarRoles()    { return rolRepo.findAll(); }
    public List<Sucursal> listarSucursales(){ return sucursalRepo.findByActivoTrue(); }

    public Optional<Usuario> findById(Integer id) { return usuarioRepo.findById(id); }

    // ─── Convertir Request → Entidad ──────────────────────────────
    private Usuario toEntidad(UsuarioRequest req) throws Exception {
        Rol      rol      = rolRepo.findById(req.getIdRol())
                .orElseThrow(() -> new Exception("Rol no encontrado."));
        Sucursal sucursal = sucursalRepo.findById(req.getIdSucursal())
                .orElseThrow(() -> new Exception("Sucursal no encontrada."));

        Usuario usuario = req.esNuevo() ? new Usuario()
                : usuarioRepo.findById(req.getIdUsuario())
                    .orElseThrow(() -> new Exception("Usuario no encontrado."));

        usuario.setNombreCompleto(req.getNombreCompleto().trim());
        usuario.setUsername(req.getUsername().trim().toLowerCase());
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setPuesto(req.getPuesto());
        usuario.setActivo(req.getActivo() != null ? req.getActivo() : true);

        return usuario;
    }

    // ─── Guardar usuario nuevo ────────────────────────────────────
    @Transactional
    public Usuario crear(UsuarioRequest req) throws Exception {
        // Validar username único
        if (usuarioRepo.findByUsername(req.getUsername().trim().toLowerCase()).isPresent()) {
            throw new Exception("El username \"" + req.getUsername() + "\" ya está en uso.");
        }

        // Al crear, la contraseña es obligatoria
        if (!req.tienePassword()) {
            throw new Exception("Debes establecer una contraseña.");
        }
        if (!req.passwordsCoinciden()) {
            throw new Exception("Las contraseñas no coinciden.");
        }
        if (req.getPassword().length() < 6) {
            throw new Exception("La contraseña debe tener al menos 6 caracteres.");
        }

        Usuario usuario = toEntidad(req);
        usuario.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        return usuarioRepo.save(usuario);
    }

    // ─── Actualizar usuario existente ─────────────────────────────
    @Transactional
    public Usuario actualizar(UsuarioRequest req) throws Exception {
        // Validar username único (excepto el propio)
        usuarioRepo.findByUsername(req.getUsername().trim().toLowerCase())
                .ifPresent(existente -> {
                    if (!existente.getIdUsuario().equals(req.getIdUsuario())) {
                        throw new RuntimeException("El username \"" + req.getUsername() + "\" ya está en uso.");
                    }
                });

        Usuario usuario = toEntidad(req);

        // Solo cambiar contraseña si vino algo en el campo
        if (req.tienePassword()) {
            if (!req.passwordsCoinciden()) {
                throw new Exception("Las contraseñas no coinciden.");
            }
            if (req.getPassword().length() < 6) {
                throw new Exception("La contraseña debe tener al menos 6 caracteres.");
            }
            usuario.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        // Si el campo vino vacío, el passwordHash ya está seteado desde toEntidad() con el original

        return usuarioRepo.save(usuario);
    }

    // ─── Toggle activo ────────────────────────────────────────────
    @Transactional
    public void toggleActivo(Integer id, Integer idAdmin) throws Exception {
        if (id.equals(idAdmin)) {
            throw new Exception("No puedes desactivarte a ti mismo.");
        }
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new Exception("Usuario no encontrado."));
        u.setActivo(!u.getActivo());
        usuarioRepo.save(u);
    }

    // ─── Stats para la vista ──────────────────────────────────────
    public long countActivos() {
        return usuarioRepo.findAllByOrderByActivoDescNombreCompletoAsc()
                .stream().filter(Usuario::getActivo).count();
    }
}
