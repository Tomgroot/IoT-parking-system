package nl.parkinglot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class ReserveHttpHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        URI uri = t.getRequestURI();
        String response = "Test";
        t.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}