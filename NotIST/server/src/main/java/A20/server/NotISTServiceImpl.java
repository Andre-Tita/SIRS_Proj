package A20.server;

import io.grpc.stub.StreamObserver;

import A20.*;
import A20.NotISTGrpc;
import A20.server.model.*;
import A20.server.repository.*;
import A20.server.util.*;

public class NotISTServiceImpl extends NotISTGrpc.NotISTImplBase {
    // #TODO: Add the database connector.

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        // #TODO Verify if the user exists.
        System.out.println("Received username: " + request.getUsername() + " | and password: " + request.getPassword());
        
        LoginResponse response = LoginResponse.newBuilder().setAck(1).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void signup(SignUpRequest request, StreamObserver<SignUpResponse> responseObserver) {
        // #TODO Verify if the user alr exists.
        System.out.println("Received username: " + request.getUsername() + " | and password: " + request.getPassword());

        SignUpResponse response = SignUpResponse.newBuilder().setAck(1).build();
        responseObserver.onNext(response);
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
