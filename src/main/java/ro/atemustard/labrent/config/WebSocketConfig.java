package ro.atemustard.labrent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ro.atemustard.labrent.websocket.LabRentWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LabRentWebSocketHandler labRentWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor;

    public WebSocketConfig(LabRentWebSocketHandler labRentWebSocketHandler,
                           JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor) {
        this.labRentWebSocketHandler = labRentWebSocketHandler;
        this.jwtWebSocketHandshakeInterceptor = jwtWebSocketHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(labRentWebSocketHandler, "/ws/live")
                .addInterceptors(jwtWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
