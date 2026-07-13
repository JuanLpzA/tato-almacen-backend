package com.tato.almacen.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Inicializa Firebase (para notificaciones push del modulo Almacen).
 * Soporta DOS formas de dar las credenciales, en este orden de prioridad:
 *
 * 1) firebase.credentials.json -- el CONTENIDO del JSON directamente
 *    (pensado para Railway/produccion: se pega el JSON completo como
 *    variable de entorno, ya que esas plataformas no tienen un
 *    filesystem persistente donde "subir" un archivo).
 * 2) firebase.credentials.path -- ruta a un archivo en disco (pensado
 *    para desarrollo local).
 *
 * Si ninguna esta configurada, NO rompe el arranque -- solo desactiva el
 * envio de notificaciones (NotificacionPushService lo detecta y no hace nada).
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.json:}")
    private String credencialesJson;

    @Value("${firebase.credentials.path:}")
    private String credencialesPath;

    @PostConstruct
    public void inicializar() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (InputStream serviceAccount = obtenerStreamCredenciales()) {
            if (serviceAccount == null) {
                log.warn("Firebase no esta configurado (ni firebase.credentials.json ni " +
                        "firebase.credentials.path). Las notificaciones push quedan desactivadas.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase inicializado correctamente (notificaciones push activas)");

        } catch (IOException e) {
            log.warn("No se pudo inicializar Firebase: {}. Las notificaciones push quedan desactivadas.",
                    e.getMessage());
        }
    }

    private InputStream obtenerStreamCredenciales() throws IOException {
        if (credencialesJson != null && !credencialesJson.isBlank()) {
            return new ByteArrayInputStream(credencialesJson.getBytes(StandardCharsets.UTF_8));
        }
        if (credencialesPath != null && !credencialesPath.isBlank()) {
            try {
                return new FileInputStream(credencialesPath);
            } catch (IOException e) {
                return null; // archivo no existe, se maneja como "no configurado"
            }
        }
        return null;
    }
}
