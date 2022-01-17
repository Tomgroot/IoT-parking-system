package nl.parkinglot;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class CustomHttpServer {

    private HttpServer httpServer;

    public CustomHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(8000), 0);
            httpServer.createContext("/reserve", new ReserveHttpHandler());
            httpServer.setExecutor(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        this.httpServer.start();
    }
}