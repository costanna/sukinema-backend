package com.sukinema.keepawake;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Keep-awake estilo woke-dyno: sin hilos programados ni HTTP real fuera de
 * un servidor local efímero. El @Scheduled no se dispara aquí (no hay
 * @EnableScheduling en este contexto), solo el ping de arranque.
 */
class KeepAwakeServiceTests {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(KeepAwakeService.class);

    private static HttpServer serverReturning(AtomicInteger hits, int status) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/health", exchange -> {
            hits.incrementAndGet();
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;
    }

    @Test
    void beanAbsentByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(KeepAwakeService.class));
    }

    @Test
    void beanPresentAndPingsOnStartupWhenEnabled() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = serverReturning(hits, 200);
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/health";
            runner.withPropertyValues("keep-awake.enabled=true", "keep-awake.url=" + url)
                    .run(context -> {
                        assertThat(context).hasSingleBean(KeepAwakeService.class);
                        assertThat(hits.get()).isEqualTo(1);
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void pingFailureIsSwallowed() {
        KeepAwakeService service = new KeepAwakeService("http://localhost:9/health");
        assertThatNoException().isThrownBy(service::pingOnSchedule);
    }

    @Test
    void blankUrlDoesNothing() {
        KeepAwakeService service = new KeepAwakeService("   ");
        assertThatNoException().isThrownBy(service::pingOnSchedule);
    }
}
