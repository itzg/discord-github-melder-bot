package me.itzg.melderbot.config;

import java.util.Map;
import org.springframework.boot.autoconfigure.security.reactive.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class WebSecurityConfig {

    private final Environment springEnv;

    public WebSecurityConfig(Environment springEnv) {
        this.springEnv = springEnv;
    }

    /**
     * @return an empty user details service to disable user auto-creation
     */
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService() {
        return new MapReactiveUserDetailsService(Map.of());
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf().disable();

        http.authorizeExchange(spec -> {
            spec.matchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
            if (springEnv.acceptsProfiles(Profiles.of("dev"))) {
                spec.pathMatchers("/dev/**").permitAll();
            }
            spec.pathMatchers("/").permitAll();
            spec.pathMatchers("/webhook/**").permitAll();
            spec.anyExchange().authenticated();
        });

        http.oauth2Client(Customizer.withDefaults());
        http.oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}
