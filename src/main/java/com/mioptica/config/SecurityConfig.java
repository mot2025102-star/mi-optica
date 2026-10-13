package com.mioptica.config;

import com.mioptica.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner generadorDeHash(PasswordEncoder encoder,
                                             com.mioptica.repository.UsuarioRepository usuarioRepository,
                                             com.mioptica.repository.RolRepository rolRepository,
                                             com.mioptica.repository.SucursalRepository sucursalRepository) {
        return args -> {
            if (usuarioRepository.count() == 0) {
                System.out.println("No se encontraron usuarios. Creando datos iniciales...");

                // 1. Crear Roles si no existen
                if (rolRepository.count() == 0) {
                    java.util.List<String> roles = java.util.List.of("ADMINISTRADOR", "VENDEDOR", "BODEGUERO", "OPTOMETRISTA", "CONTADOR");
                    for (String nombreRol : roles) {
                        com.mioptica.model.Rol rol = new com.mioptica.model.Rol();
                        rol.setNombre(nombreRol);
                        rolRepository.save(rol);
                    }
                }

                // 2. Crear una sucursal por defecto
                com.mioptica.model.Sucursal sucursal = sucursalRepository.findById(1).orElse(null);
                if (sucursal == null) {
                    sucursal = new com.mioptica.model.Sucursal();
                    sucursal.setNombre("Central");
                    sucursal.setDireccion("Dirección Principal");
                    sucursal.setTelefono("00000000");
                    sucursal.setActivo(true);
                    sucursal = sucursalRepository.save(sucursal);
                }

                // 3. Crear el usuario Administrador por defecto
                com.mioptica.model.Usuario admin = new com.mioptica.model.Usuario();
                admin.setNombreCompleto("Administrador del Sistema");
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("admin123")); // Contraseña por defecto
                admin.setRol(rolRepository.findById(1).orElseThrow()); // ID 1 es ADMINISTRADOR
                admin.setSucursal(sucursal);
                admin.setPuesto("Gerente");
                admin.setActivo(true);
                
                usuarioRepository.save(admin);
                System.out.println("=================================================");
                System.out.println("Usuario Creado -> Username: admin | Password: admin123");
                System.out.println("=================================================");
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(List.of(authProvider));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        /*
         * CSRF con cookie.
         * El token se fuerza a cargar en AuthController.login() (GET /login)
         * para que la cookie XSRF-TOKEN esté presente antes del primer submit.
         */
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(requestHandler)
            )
            .headers(headers -> headers
                .cacheControl(cache -> {})
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/logo/**", "/favicon.ico").permitAll()
                .requestMatchers("/login", "/login-error").permitAll()
                .requestMatchers("/ordenes-lab/**").hasAnyRole("OPTOMETRISTA", "VENDEDOR", "ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler())
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        };
    }
}