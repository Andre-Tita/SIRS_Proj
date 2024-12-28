package A20.server;

import io.grpc.stub.StreamObserver;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.*;

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
        System.out.println("Received nnote, username: " + request.getUsername() + " | note: " + request.getNote());

        try {
            // Check if user exists and is logged in
            User user = userDAO.getUserByUsername(request.getUsername());
            
            if (user == null) { 
                // User doesn't exist
                NNoteResponse response = NNoteResponse.newBuilder().setAck(1).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                System.out.println("User exists !\n");

                // Parse the JSON note content
                Gson gson = new Gson();
                JsonObject noteJson = gson.fromJson(request.getNote(), JsonObject.class);
                
                // Check if the note already exists
                String title = noteJson.get("title").getAsString();
                Note existingNote = noteDAO.getNoteByTitle(title);
                
                if (existingNote != null) {
                    // Note already exists
                    NNoteResponse response = NNoteResponse.newBuilder().setAck(2).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } else {
                    // Extract fields from note JSON
                    String content = noteJson.get("note").getAsString();
                    JsonArray viewers = noteJson.getAsJsonArray("viewers");
                    JsonArray editors = noteJson.getAsJsonArray("editors");

                    // Add the new note to the database
                    Note newNote = new Note(title, content, user.getUserId());

                    noteDAO.addNote(newNote);
                    System.out.println("Note added !\n");
                    Note note = noteDAO.getNoteByTitle(title);
                    System.out.println("Got the note from db: ");
                    System.out.println(note);
                    
                    // Add viewer permissions
                    for (JsonElement other_user : viewers) {
                        try {
                            // if it cant add the user prob the user isn't registered
                            int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                            noteDAO.grantAccessNote(other_user_id, note.getNoteId(), user.getUserId(), "VIEWER");

                        } catch (SQLException e) {
                            e.printStackTrace();
                            NNoteResponse response = NNoteResponse.newBuilder().setAck(-1).build();         // Failure
                            responseObserver.onNext(response);
                            return;
                        }
                    }

                    // Add editor permissions 
                    for (JsonElement other_user : editors) {
                        try {
                            int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                            noteDAO.grantAccessNote(other_user_id, note.getNoteId(), user.getUserId(), "EDITOR");
                        
                        } catch (SQLException e) {
                            e.printStackTrace();
                            NNoteResponse response = NNoteResponse.newBuilder().setAck(-1).build();         // Failure
                            responseObserver.onNext(response);
                            return;
                        }
                    }

                    NNoteResponse response = NNoteResponse.newBuilder().setAck(0).build();      // Success
                    responseObserver.onNext(response);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            NNoteResponse response = NNoteResponse.newBuilder().setAck(-1).build();         // Failure
            responseObserver.onNext(response);

        } catch (JsonSyntaxException e) {
            System.out.println("Invalid JSON format: " + e.getMessage());
            NNoteResponse response = NNoteResponse.newBuilder().setAck(-2).build();         // Failure
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
    public void rnote(RNoteRequest request, StreamObserver<RNoteResponse> responseObserver) {

        try {
            // Checks if user exists
            User user = userDAO.getUserByUsername(request.getUsername());
            if (user == null) {
                RNoteResponse response = RNoteResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
            } else {
                // Checks if note exists
                Note note = noteDAO.getNoteByTitleAndVersion(request.getTitle(), request.getVersion());
                System.out.println("Note: " + note + "\n");
                if (note == null) {
                    RNoteResponse response = RNoteResponse.newBuilder().setAck(2).build(); // Failure
                    responseObserver .onNext(response);
                } else {
                    // Checks if the user has permission to view the note
                    if (noteDAO.hasAccess(user.getUserId(), note.getTitle(), "VIEWER") || noteDAO.hasAccess(user.getUserId(), note.getTitle(), "EDITOR")) {
                        System.out.println("User has access!\n");
                        List<User> viewers = userDAO.getUsersByUserIds(noteDAO.getNoteViewers(note.getNoteId()));
                        List<User> editors = userDAO.getUsersByUserIds(noteDAO.getNoteEditors(note.getNoteId()));

                        for (User u : viewers) 
                            note.addViewer(user);

                        for (User u : editors)
                            note.addEditor(user);

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
    public void enoteP1(ENotePhase1Request request, StreamObserver<ENotePhase1Response> responseObserver) {
        System.out.println("Received a phase 1 edit note.");
        try {
            // Checks if user exists
            User user = userDAO.getUserByUsername(request.getUsername());
            if (user == null) {
                ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
            } else {
                // Checks if note exists
                Note note = noteDAO.getNoteByTitle(request.getTitle());
                if (note == null) {
                    ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(2).build(); // Failure
                    responseObserver .onNext(response);
                } else {
                    // Checks if the user has permission to edit the note
                    if (noteDAO.hasAccess(user.getUserId(), note.getTitle(), "EDITOR")) {
                        List<User> viewers = userDAO.getUsersByUserIds(noteDAO.getNoteViewers(note.getNoteId()));
                        List<User> editors = userDAO.getUsersByUserIds(noteDAO.getNoteEditors(note.getNoteId()));

                        for (User u : viewers) 
                            note.addViewer(u);

                        for (User u : editors)
                            note.addEditor(u);

                        ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(0).setNote((note.toJSON((userDAO.getUserByUserId(note.getOwnerId())).getUsername())).toString()).build(); // Success
                        responseObserver .onNext(response);
                    } else {
                        ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(3).build(); // Failure
                        responseObserver .onNext(response);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } 
        responseObserver.onCompleted(); 
    }

    @Override
    public void enoteP2(ENotePhase2Request request, StreamObserver<ENotePhase2Response> responseObserver) {
        System.out.println("Received a phase 2 edit note. With note: " + request.getNote());
        try {
            // Convert the JSON.toString() to a Note
            Gson gson = new Gson();
            JsonObject noteJson = gson.fromJson(request.getNote(), JsonObject.class);
            JsonObject owner = noteJson.getAsJsonObject("owner");
            System.out.println(owner);
            Note note = new Note(
                noteJson.get("id").getAsInt(),
                noteJson.get("title").getAsString(),
                noteJson.get("note").getAsString(),
                noteJson.get("data_created").getAsString(),
                noteJson.get("last_modified_by").getAsInt(),
                (noteJson.get("version").getAsInt()) + 1,
                (noteJson.getAsJsonObject("owner")).get("id").getAsInt()
            );
            
            // Remove all the accesses user's have to the note to then add them (in case of an update)
            noteDAO.removeAccesses(note.getNoteId());

            JsonArray viewers = noteJson.getAsJsonArray("viewers");
            JsonArray editors = noteJson.getAsJsonArray("editors");

            // Add viewer permissions
            for (JsonElement other_user : viewers) {
                try {
                    // if it cant add the user prob the user isn't registered
                    int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                    noteDAO.grantAccessNote(other_user_id, note.getNoteId(), note.getOwnerId(), "VIEWER");

                } catch (SQLException e) {
                    e.printStackTrace();
                    ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(-1).build();         // Failure
                    responseObserver.onNext(response);
                    return;
                }
            }

            // Add editor permissions 
            for (JsonElement other_user : editors) {
                try {
                    int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                    noteDAO.grantAccessNote(other_user_id, note.getNoteId(), note.getOwnerId(), "EDITOR");
                
                } catch (SQLException e) {
                    e.printStackTrace();
                    ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(-1).build();         // Failure
                    responseObserver.onNext(response);
                    return;
                }
            }

            noteDAO.insertNote(note);

            ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(0).build();         // Success
            responseObserver.onNext(response);

        } catch (SQLException e) {
            e.printStackTrace();
            ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(-1).build();         // Failure
            responseObserver.onNext(response);

        } catch (JsonSyntaxException e) {
            System.out.println("Invalid JSON format: " + e.getMessage());
            ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(-2).build();         // Failure
            responseObserver.onNext(response);

        }

        responseObserver.onCompleted();
    }
}