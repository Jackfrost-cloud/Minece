package com.minece.spotifyoverlay.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class LocalCallbackServer {
    private HttpServer server;

    public void start(String redirectUri, Consumer<String> onCode, Runnable onError) throws IOException {
        stop();

        URI uri = URI.create(redirectUri);
        int port = uri.getPort() == -1 ? 8888 : uri.getPort();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/callback", exchange -> handleCallback(exchange, onCode, onError));
        server.setExecutor(null);
        server.start();
    }

    private void handleCallback(HttpExchange exchange, Consumer<String> onCode, Runnable onError) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String responseBody = "<html><body><h1>Spotify login complete.</h1><p>You can close this window and return to Minecraft.</p></body></html>";
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

        try {
            if (query != null && query.contains("code=")) {
                String code = query.split("code=")[1].split("&")[0];
                onCode.accept(code);
            } else {
                onError.run();
            }
        } finally {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
            stop();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
