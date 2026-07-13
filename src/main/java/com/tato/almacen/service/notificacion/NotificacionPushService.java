package com.tato.almacen.service.notificacion;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Envuelve el envio de push notifications. Si Firebase no esta configurado
 * (ver FirebaseConfig), simplemente no hace nada -- el resto del modulo
 * Almacen sigue funcionando igual (el pedido se crea, solo no llega el push).
 */
@Service
public class NotificacionPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPushService.class);

    public void enviarATokens(List<String> tokens, String titulo, String cuerpo, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase no esta configurado, se omite el envio de notificacion: {}", titulo);
            return;
        }

        try {
            MulticastMessage mensaje = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(cuerpo)
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .addAllTokens(tokens)
                    .build();

            FirebaseMessaging.getInstance().sendEachForMulticast(mensaje);
        } catch (FirebaseMessagingException e) {
            log.error("Error enviando notificacion push: {}", e.getMessage());
        }
    }
}
