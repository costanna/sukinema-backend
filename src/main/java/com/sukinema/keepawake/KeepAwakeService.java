package com.sukinema.keepawake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Keep-awake estilo woke-dyno para el plan gratuito de Render.
 *
 * <p>Render duerme el servicio tras ~15 min sin peticiones ENTRANTES. Este
 * componente pide la URL pública del backend cada N milisegundos: la petición
 * sale del contenedor y vuelve a entrar por el balanceador, así que cuenta
 * como tráfico y el servicio no se duerme (adiós al cold start de ~1 min).
 *
 * <p>Solo existe cuando {@code keep-awake.enabled=true} (apagado por defecto)
 * y necesita {@code keep-awake.url} con la URL pública (nunca localhost). Sin
 * claves ni servicios extra. Ojo al consumo: despierto 24/7 gasta ~720 h/mes
 * de las 750 h gratuitas; vale para un solo servicio.
 */
@Component
@ConditionalOnProperty(name = "keep-awake.enabled", havingValue = "true")
public class KeepAwakeService {

    private static final Logger log = LoggerFactory.getLogger(KeepAwakeService.class);

    private final RestTemplate rest;
    private final String url;

    public KeepAwakeService(@Value("${keep-awake.url:}") String url) {
        this.url = url == null ? "" : url.strip();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        this.rest = new RestTemplate(factory);
    }

    /** Primer ping al arrancar: no espera al primer intervalo. */
    @EventListener(ContextRefreshedEvent.class)
    public void pingOnStartup() {
        ping();
    }

    @Scheduled(fixedRateString = "${keep-awake.interval-ms:600000}")
    public void pingOnSchedule() {
        ping();
    }

    void ping() {
        if (url.isEmpty()) {
            log.warn("Keep-awake activado pero sin keep-awake.url: no hay nada que pedir");
            return;
        }
        try {
            rest.getForEntity(url, String.class);
            log.info("Keep-awake ping a {}", url);
        } catch (RestClientException | IllegalArgumentException e) {
            // Sin red, DNS caído, Render redesplegando...: se reintenta al siguiente ciclo.
            log.debug("Keep-awake ping fallido ({}): {}", url, e.toString());
        }
    }
}
