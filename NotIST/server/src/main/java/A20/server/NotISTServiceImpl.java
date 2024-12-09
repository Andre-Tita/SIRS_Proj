package A20.server;

import io.grpc.stub.StreamObserver;

import java.sql.SQLException;

import A20.*;
import A20.server.model.*;
import A20.server.repository.UserDAO;           // #Change "UserDAO" -> "*"
import A20.server.util.*;

public class NotISTServiceImpl extends NotISTGrpc.NotISTImplBase {
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        // Debug purposes #delete
        System.out.println("Received username: " + request.getUsername() + " | and password: " + request.getPassword());

        try {
            // Check if the user exists in the database
            User user = userDAO.getUserByUsername(request.getUsername());
            
            if (user != null && user.getPassword().equals(request.getPassword())) { // #Use hashed password comparison here.
                LoginResponse response = LoginResponse.newBuilder().setAck(1).build(); // Success
                responseObserver.onNext(response);
            
            } else {
                LoginResponse response = LoginResponse.newBuilder().setAck(0).build(); // Failure
                responseObserver.onNext(response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            LoginResponse response = LoginResponse.newBuilder().setAck(0).build(); // Error
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }


    @Override
    public void signup(SignUpRequest request, StreamObserver<SignUpResponse> responseObserver) {
        System.out.println("Received username: " + request.getUsername() + " | and password: " + request.getPassword());

        try {
            // Check if the user already exists
            User existingUser = userDAO.getUserByUsername(request.getUsername());

            if (existingUser == null) {
                // Add new user
                User newUser = new User(0, request.getUsername(), request.getPassword(), ""); // #Replace with appropriate public key
                userDAO.addUser(newUser);
                SignUpResponse response = SignUpResponse.newBuilder().setAck(1).build(); // Success
                responseObserver.onNext(response);

            } else {
                // User already exists
                SignUpResponse response = SignUpResponse.newBuilder().setAck(0).build(); // Failure
                responseObserver.onNext(response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            SignUpResponse response = SignUpResponse.newBuilder().setAck(0).build(); // Error
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        // #TODO Verify if the user is logged in.
        System.out.println("Received a logout request!");

        LogoutResponse response = LogoutResponse.newBuilder().setAck(1).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
