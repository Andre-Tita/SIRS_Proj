package A20.server;

import io.grpc.stub.StreamObserver;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import A20.*;
import A20.server.model.*;
import A20.server.repository.*;

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

                // Checks if user is already logged in
                if (userDAO.isUserLoggedIn(user.getUserId())) {
                    LoginResponse response = LoginResponse.newBuilder().setAck(2).build(); // Failure
                    responseObserver.onNext(response);

                } else {

                    userDAO.login(user.getUserId());
                    LoginResponse response = LoginResponse.newBuilder().setAck(0).setUserId(user.getUserId()).build(); // Success
                    responseObserver.onNext(response);
                }
            
            } else {
                LoginResponse response = LoginResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            LoginResponse response = LoginResponse.newBuilder().setAck(-1).build(); // Error
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
                User newUser = new User(request.getUsername(), request.getPassword(), request.getPubKey()); // #Replace with appropriate public key
                userDAO.addUser(newUser);
                SignUpResponse response = SignUpResponse.newBuilder().setAck(0).setUserId((userDAO.getUserByUsername(request.getUsername())).getUserId()).build(); // Success
                responseObserver.onNext(response);

            } else {
                // User already exists
                SignUpResponse response = SignUpResponse.newBuilder().setAck(1).build(); // Failure
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
        System.out.println("Received a logout request!");

        try {
            // Checks if user exists
            User user = userDAO.getUserByUsername(request.getUsername());
            if (user == null) {
                LogoutResponse response = LogoutResponse.newBuilder().setAck(1).build();    // Failure
                responseObserver.onNext(response);
            
            } else {

                // Checks if user is logged in
                if (userDAO.isUserLoggedIn(user.getUserId())) {
                    
                    userDAO.logout(user.getUserId());
                    LogoutResponse response = LogoutResponse.newBuilder().setAck(0).build();    // Success
                    responseObserver.onNext(response);   
                } else {

                    LogoutResponse response = LogoutResponse.newBuilder().setAck(2).build();    // Failure
                    responseObserver.onNext(response);  
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            LogoutResponse response = LogoutResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 

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
                NNoteResponse response = NNoteResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
                
            } else {
                // Check if the note already exists
                Note existingNote = noteDAO.getNoteByTitle(request.getTitle());

                if (existingNote == null) {
                    // Add new note
                    Note newNote = new Note(user.getUserId(), request.getTitle(), request.getContent());
                    System.out.println(newNote);
                    noteDAO.addNote(newNote);
                    NNoteResponse response = NNoteResponse.newBuilder().setAck(0).build(); // Success
                    responseObserver.onNext(response);

                } else {
                    // Note already exists
                    NNoteResponse response = NNoteResponse.newBuilder().setAck(2).build(); // Failure
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
                MNoteResponse response = MNoteResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
                
            } else {
                MNoteResponse response = MNoteResponse.newBuilder().setAck(0).addAllNoteTitles(noteDAO.getNotesTitleByUserId(user.getUserId())).build(); // Success
                responseObserver.onNext(response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            MNoteResponse response = MNoteResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 

        responseObserver.onCompleted(); 
    }

    @Override
    public void rnote(RNoteRequest request, StreamObserver<RNoteResponse> responseObserver) {

        try {
            // Checks if user exists
            User user = userDAO.getUserByUsername(request.getUsername());
            if (user == null) {
                RNoteResponse response = RNoteResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
            } else {
                // Checks if note exists
                Note note = noteDAO.getNoteByTitle(request.getTitle());
                if (note == null) {
                    RNoteResponse response = RNoteResponse.newBuilder().setAck(2).build(); // Failure
                    responseObserver .onNext(response);
                } else {
                    // Checks if the user has permission to view the note
                    if (noteDAO.canAccessNote(user.getUserId(), note.getNoteId())) {
                        RNoteResponse response = RNoteResponse.newBuilder().setAck(0).setContent(note.getContent()).build(); // Success
                        responseObserver .onNext(response);
                    } else {
                        RNoteResponse response = RNoteResponse.newBuilder().setAck(3).build(); // Failure
                        responseObserver .onNext(response);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            RNoteResponse response = RNoteResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 
        responseObserver.onCompleted(); 
    }

    @Override
    public void snotes(SNotesRequest request, StreamObserver<SNotesResponse> responseObserver) {
        try {
            // Checks if user exists
            User user = userDAO.getUserByUsername(request.getUsername());

            if (user == null) {
                // User doesn't exist
                SNotesResponse response = SNotesResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);

            } else {

                SNotesResponse response = SNotesResponse.newBuilder().setAck(0).addAllNoteTitles(noteDAO.getUsersAccessNotes(user.getUserId())).build(); // Success
                responseObserver.onNext(response);
            }
  
        } catch (SQLException e) {
            e.printStackTrace();
            SNotesResponse response = SNotesResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 
        responseObserver.onCompleted(); 
    }

    @Override
    public void gaccess(GAccessRequest request, StreamObserver<GAccessResponse> responseObserver) {
        
        try {
            // Checks if user requesting exists
            User my_user = userDAO.getUserByUsername(request.getUsername());

            if (my_user == null) {
                // My user doesn't exist
                GAccessResponse response = GAccessResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);

            } else {

                // Checks if note exists
                Note note = noteDAO.getNoteByTitle(request.getTitle());
                if (note == null) {
                    GAccessResponse response = GAccessResponse.newBuilder().setAck(2).build(); // Failure
                        responseObserver.onNext(response);

                } else {

                    // Checks if my user is owner of the note
                    if(!noteDAO.isOwner(my_user.getUserId(), request.getTitle())) {
                        GAccessResponse response = GAccessResponse.newBuilder().setAck(3).build(); // Failure
                        responseObserver.onNext(response);
                    
                    } else {

                        // Checks if other user exists
                        User other_user = userDAO.getUserByUsername(request.getOtherUsername());
                        if (other_user == null) {
                            GAccessResponse response = GAccessResponse.newBuilder().setAck(4).build(); // Failure
                            responseObserver.onNext(response);
                        
                        } else {
                            noteDAO.grantAccessNote(other_user.getUserId(), note.getNoteId(), my_user.getUserId());
                            GAccessResponse response = GAccessResponse.newBuilder().setAck(0).build(); // Success
                            responseObserver.onNext(response);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            GAccessResponse response = GAccessResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 
        responseObserver.onCompleted(); 
    }
}
