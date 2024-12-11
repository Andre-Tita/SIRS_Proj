package A20.server;

import io.grpc.stub.StreamObserver;

import java.sql.SQLException;
import java.util.List;

import A20.*;
import A20.server.model.*;
import A20.server.repository.*;
import A20.server.util.*;

public class NotISTServiceImpl extends NotISTGrpc.NotISTImplBase {
    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    // User Operations
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        // Debug purposes #delete
        System.out.println("Received username: " + request.getUsername() + " | and password: " + request.getPassword());

        try {
            // Check if the user exists in the database
            User user = userDAO.getUserByUsernameAndPass(request.getUsername(), request.getPassword());
            
            if (user != null) {
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
                User newUser = new User(request.getUsername(), request.getPassword(), ""); // #Replace with appropriate public key
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
            SignUpResponse response = SignUpResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        // #TODO Verify if the user is logged in ???
        System.out.println("Received a logout request!");

        LogoutResponse response = LogoutResponse.newBuilder().setAck(1).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Notes Operations
    @Override
    public void nnote(NNoteRequest request, StreamObserver<NNoteResponse> responseObserver) {
        System.out.println("Received username: " + request.getUsername() + " | title: " + request.getTitle()
        + " | content: " + request.getContent());

        try {
            // Check if user exists and is logged in
            User user = userDAO.getUserByUsername(request.getUsername());

            if (user == null) { 
                // User doesn't exists
                NNoteResponse response = NNoteResponse.newBuilder().setAck(0).build(); // Failure
                responseObserver.onNext(response);
                
            } else {
                // Check if the note already exists
                Note existingNote = noteDAO.getNoteByTitle(request.getTitle());

                if (existingNote == null) {
                    // Add new note
                    Note newNote = new Note(user.getUserId(), request.getTitle(), request.getContent());
                    System.out.println(newNote);
                    noteDAO.addNote(newNote);
                    NNoteResponse response = NNoteResponse.newBuilder().setAck(1).build(); // Success
                    responseObserver.onNext(response);

                } else {
                    // Note already exists
                    NNoteResponse response = NNoteResponse.newBuilder().setAck(0).build(); // Failure
                    responseObserver.onNext(response);
                }

            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            NNoteResponse response = NNoteResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 

        responseObserver.onCompleted();
    }

    @Override
    public void mnote(MNoteRequest request, StreamObserver<MNoteResponse> responseObserver) {
        try {
            // Check if user exists
            User user = userDAO.getUserByUsername(request.getUsername());

            if (user == null) { 
                // User doesn't exists
                MNoteResponse response = MNoteResponse.newBuilder().setAck(0).build(); // Failure
                responseObserver.onNext(response);
                
            } else {
                List<Note> notes_list = noteDAO.getNotesByUserId(user.getUserId());
                MNoteResponse response = MNoteResponse.newBuilder().setAck(1).addAllNoteTitles(noteDAO.getNotesTtileByUserId(user.getUserId())).build(); // Success
                responseObserver.onNext(response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            MNoteResponse response = MNoteResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 

        responseObserver.onCompleted(); 
    }
}
