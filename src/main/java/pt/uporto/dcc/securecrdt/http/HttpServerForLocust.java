package pt.uporto.dcc.securecrdt.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

import com.sun.net.httpserver.HttpServer;
import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.client.Client;
import pt.uporto.dcc.securecrdt.messages.states.PlayerState;


public class HttpServerForLocust {
    int serverPort = 8000;

    public HttpServerForLocust(Client client) {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.createContext("/api/update", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {

                Map<String, String> params = splitQuery(exchange.getRequestURI().getRawQuery());
                String operation = params.getOrDefault("op", "") + " ";
                String value = params.getOrDefault("value", null);

                if (value != null && !params.isEmpty()) {
                    try {
                        client.getCrdtClient().update("update " + operation + value);
                    } catch (InvalidSecretValue e) {
                        throw new RuntimeException(e);
                    }
                }
                String respText = "Done\n";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.createContext("/api/query", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {

                Map<String, String> params = splitQuery(exchange.getRequestURI().getRawQuery());
                String operation = params.getOrDefault("op", "") + " ";
                String value = params.getOrDefault("value", "");

                int currentValues;
                try {
                    currentValues = client.getCrdtClient().query("query " + operation + value);
                } catch (InterruptedException | InvalidSecretValue e) {
                    throw new RuntimeException(e);
                }
                String respText = currentValues + "\n";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.createContext("/api/propagate", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                PlayerState[] state;
                try {
                    state = client.getCrdtClient().propagate();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                String respText = Arrays.toString(state) + "\n";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.createContext("/api/merge", (exchange -> {

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    client.getCrdtClient().merge();
                } catch (InvalidSecretValue e) {
                    throw new RuntimeException(e);
                }
                String respText = "Done\n";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.createContext("/api/exit", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String respText = "Exiting.\n";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
            System.exit(0);
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public static Map<String, String> splitQuery(String query) {

        if (query == null || "".equals(query)) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();

        for (String param : query.split("&")) {
            String[] splitParam = param.split("=");
            map.put(splitParam[0], splitParam[1]);
        }
        return map;
    }

}
