package A20.server;

import A20.*;
import A20.server.repository.UserDAO;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.sql.SQLException;

public class NotISTMain {
    public static void main(String[] args) throws IOException, InterruptedException{
        final int port = 50052;
		final BindableService impl = new NotISTServiceImpl();

        Server server = ServerBuilder.forPort(port).addService(impl).build();

        server.start();
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.logoutAllUsers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Server started.");

        server.awaitTermination();
    }
}
