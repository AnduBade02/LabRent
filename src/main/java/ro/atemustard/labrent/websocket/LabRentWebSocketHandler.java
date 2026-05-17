package ro.atemustard.labrent.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LabRentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LabRentWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();

    public LabRentWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionLocks.put(session, new Object());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionLocks.remove(session);
    }

    public void broadcast(LabRentRealtimeEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize realtime event", e);
            return;
        }

        TextMessage message = new TextMessage(payload);
        sessionLocks.forEach((session, lock) -> sendIfVisible(session, lock, event, message));
    }

    private void sendIfVisible(WebSocketSession session,
                               Object lock,
                               LabRentRealtimeEvent event,
                               TextMessage message) {
        if (!session.isOpen() || !canReceive(session, event)) {
            return;
        }

        try {
            synchronized (lock) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.debug("Dropping websocket session after send failure", e);
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException closeError) {
                log.debug("Could not close websocket session", closeError);
            }
            sessionLocks.remove(session);
        }
    }

    private boolean canReceive(WebSocketSession session, LabRentRealtimeEvent event) {
        Object role = session.getAttributes().get("role");
        if ("ADMIN".equals(role)) {
            return true;
        }

        Object username = session.getAttributes().get("username");
        return username != null && username.equals(event.username());
    }
}
