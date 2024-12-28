package A20.server;

import A20.*;
import A20.server.repository.UserDAO;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class NotISTMain {
    public static void main(String[] args) throws IOException, InterruptedException{
        final int port = 50052;
        final BindableService impl = new NotISTServiceImpl();

        File certFile = new File("src/main/java/A20/server/server.crt");
        File keyFile = new File("src/main/java/A20/server/server.key");

        //Server server = ServerBuilder.forPort(port).addService(impl).build();
        Server server = NettyServerBuilder.forPort(port).addService(impl)
                        .sslContext(GrpcSslContexts.forServer(certFile, keyFile)
                        .build())
                        .build();

        server.start();

        UserDAO userDAO = new UserDAO();
        try {
            userDAO.logoutAllUsers();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Server started with TLS on port " + port);

        server.awaitTermination();
    }
}
