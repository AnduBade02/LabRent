package ro.atemustard.labrent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 * ==========================================================================
 * SECURITY CONFIG — Configurare temporara (dezactivata)
 * ==========================================================================
 *
 * Spring Security blocheaza TOTUL by default — daca nu configurezi nimic,
 * fiecare request HTTP primeste 401 Unauthorized.
 *
 * Deocamdata (saptamana 1-2) nu avem nevoie de securitate.
 * Vrem doar sa testam ca entitatile se creeaza corect in baza de date.
 *
 * Ce face aceasta configurare:
 *   - Permite TOATE request-urile fara autentificare (permitAll)
 *   - Dezactiveaza CSRF (Cross-Site Request Forgery protection)
 *     CSRF e important in productie, dar ne incurca la testare.
 *
 * In saptamana 3-4, vom inlocui aceasta configurare cu:
 *   - JWT authentication (token-based)
 *   - Roluri (STUDENT, PROFESSOR, ADMIN) cu permisiuni diferite
 *   - CSRF activat pentru formularele Thymeleaf
 *
 * CE INSEAMNA ANNOTATIONS:
 *   @Configuration → Spune lui Spring: "aceasta clasa contine configurari"
 *   @EnableWebSecurity → Activeaza Spring Security cu configurarea noastra custom
 *   @Bean → Spune lui Spring: "metoda asta returneaza un obiect pe care sa-l gestionezi tu"
 *           Spring il creaza O SINGURA DATA si il refoloseste peste tot (Singleton pattern).
 *
 * ==========================================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()       // Permite totul — TEMPORAR!
            )
            .csrf(csrf -> csrf.disable());      // Dezactiveaza CSRF — TEMPORAR!

        return http.build();
    }
}
