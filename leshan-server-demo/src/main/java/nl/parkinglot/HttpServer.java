package nl.parkinglot;

public class HttpServer {
    public static void main(String[] args) throws Exception {
        CustomHttpServer httpServer = new CustomHttpServer();
        httpServer.start();
    }
}