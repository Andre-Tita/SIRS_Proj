package A20.server;

import A20.*;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class NotISTMain {
    public static void main(String[] args) throws IOException, InterruptedException{
        final int port = 50052;
		final BindableService impl = new NotISTServiceImpl();

        Server server = ServerBuilder.forPort(port).addService(impl).build();

        server.start();
        System.out.println("Server started.");

        server.awaitTermination();
    }
}
