package A20.server;

import io.grpc.stub.StreamObserver;
import java.sql.SQLException;
import java.util.List;
import java.io.*;
import java.util.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

import com.google.gson.*;

import A20.*;
import A20.util.*;
import A20.server.model.*;
import A20.server.repository.*;
import A20.util.CommandLineInterface;


public class NotISTServiceImpl extends NotISTGrpc.NotISTImplBase {
    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    private KeyGeneratorForRSA genRSA = new KeyGeneratorForRSA(); 
    private CommandLineInterface ci = new CommandLineInterface();

    // User Operations
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        // Debug purposes
        System.out.println("Received login request from username: " + request.getUsername());

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
        System.out.println("Received signup request from username: " + request.getUsername());

        try {
            // Check if the user already exists
            User existingUser = userDAO.getUserByUsername(request.getUsername());

            if (existingUser == null) {
                // Add new user
                User newUser = new User(request.getUsername(), request.getPassword(), request.getPubKey());
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
        System.out.println("Received a logout from : " + request.getUsername());

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
        System.out.println("Received nnote request from username: " + request.getUsername());

        try {
            // Check if user exists and is logged in
            User user = userDAO.getUserByUsername(request.getUsername());
            
            if (user == null) {
                // User doesn't exist
                NNoteResponse response = NNoteResponse.newBuilder().setAck(1).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } else {
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
                    // Add the new note to the database
                    Note newNote = new Note(title,
                                noteJson.get("note").getAsString(),
                                user.getUserId(),
                                noteJson.get("hmac").getAsString(),
                                noteJson.get("iv").getAsString());
                    noteDAO.addNote(newNote, request.getSecretKey(), request.getHmacKey());
                    Note note = noteDAO.getNoteByTitle(title);

                    JsonArray viewers = noteJson.getAsJsonArray("viewers");
                    JsonArray editors = noteJson.getAsJsonArray("editors");

                    // Add viewer permissions
                    for (JsonElement other_user : viewers) {
                        // if it cant add the user prob the user isn't registered
                        int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                        noteDAO.grantAccessNote(other_user_id, note.getNoteId(), user.getUserId(), "VIEWER");
                    }

                    // Add editor permissions 
                    for (JsonElement other_user : editors) {
                        // if it cant add the user prob the user isn't registered
                        int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                        noteDAO.grantAccessNote(other_user_id, note.getNoteId(), user.getUserId(), "EDITOR");
                    }

                    List<User> viewers_list = userDAO.getUsersByUserIds(noteDAO.getNoteViewers(note.getNoteId()));
                    List<User> editors_list = userDAO.getUsersByUserIds(noteDAO.getNoteEditors(note.getNoteId()));

                    for (User u : viewers_list)
                        note.addViewer(u);

                    for (User u : editors_list)
                        note.addEditor(u);

                    NNoteResponse response = NNoteResponse.newBuilder().setAck(0).build();      // Success
                    responseObserver.onNext(response);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            NNoteResponse response = NNoteResponse.newBuilder().setAck(-1).build();         // Failure
            responseObserver.onNext(response);

        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON format: " + e.getMessage());
            NNoteResponse response = NNoteResponse.newBuilder().setAck(-2).build();         // Failure
            responseObserver.onNext(response);

        }

        responseObserver.onCompleted();
    }

    @Override
    public void mnote(MNoteRequest request, StreamObserver<MNoteResponse> responseObserver) {
        System.out.println("Received a mnote request from: " + request.getUsername());
        
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
        System.out.println("Received a snotes request from: " + request.getUsername());
        
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
        System.out.println("Received a rnote request from: " + request.getUsername());
        
        try {
            // Checks if user exists
            User user = userDAO.getUserByUsername(request.getUsername());
            if (user == null) {
                RNoteResponse response = RNoteResponse.newBuilder().setAck(1).build(); // Failure
                responseObserver.onNext(response);
            } else {
                // Checks if note exists
                Note note = noteDAO.getNoteByTitleAndVersion(request.getTitle(), request.getVersion());
                if (note == null) {
                    RNoteResponse response = RNoteResponse.newBuilder().setAck(2).build(); // Failure
                    responseObserver.onNext(response);
                } else {
                    // Checks if the user has permission to view the note
                    if (noteDAO.hasAccess(user.getUserId(), note.getTitle(), "VIEWER") || noteDAO.hasAccess(user.getUserId(), note.getTitle(), "EDITOR")) {
                        List<User> viewers = userDAO.getUsersByUserIds(noteDAO.getNoteViewers(note.getNoteId()));
                        List<User> editors = userDAO.getUsersByUserIds(noteDAO.getNoteEditors(note.getNoteId()));

                        for (User u : viewers)
                            note.addViewer(u);

                        for (User u : editors)
                            note.addEditor(u);

                        // Note -> Json
                        JsonObject noteJson = note.toJSON(userDAO.getUserByUserId(note.getOwnerId()).getUsername());

                        // Get the last user public key
                        String pubKey = userDAO.getUserPubKey(note.getLastModifiedBy());
                        PublicKey publicKey = loadPublicKey(pubKey);

                        // Get the note keys
                        String decryptedSecretKey = genRSA.decryptWithPublicKey(noteDAO.getNoteSecretKey(note.getNoteId(), note.getVersion()), publicKey);
                        String decryptedHmacKey = genRSA.decryptWithPublicKey(noteDAO.getNoteHmacKey(note.getNoteId(), note.getVersion()), publicKey);

                        // Encrypt the note key's again
                        pubKey = userDAO.getUserPubKey(user.getUserId());
                        publicKey = loadPublicKey(pubKey);

                        // Encrypt the key's
                        String encryptedSecretKey = genRSA.encryptWithPublicKey(decryptedSecretKey, publicKey);
                        String encryptedHmacKey = genRSA.encryptWithPublicKey(decryptedHmacKey, publicKey);

                        // JSON -> String
                        RNoteResponse response = RNoteResponse.newBuilder()
                        .setAck(0)
                        .setNote(noteJson.toString())
                        .setSecretKey(encryptedSecretKey)
                        .setHmacKey(encryptedHmacKey)
                        .build(); // Success
                        responseObserver.onNext(response);
                    } else {
                        RNoteResponse response = RNoteResponse.newBuilder().setAck(3).build(); // Failure
                        responseObserver.onNext(response);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            RNoteResponse response = RNoteResponse.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            RNoteResponse response = RNoteResponse.newBuilder().setAck(-2).build(); // Error
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void enoteP1(ENotePhase1Request request, StreamObserver<ENotePhase1Response> responseObserver) {
        System.out.println("Received a phase 1 edit note from: " + request.getUsername());

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
                    responseObserver.onNext(response);
                } else {
                    // Checks if the note is locked for other person to write on it
                    if (noteDAO.isLocked(note.getNoteId())) {
                        ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(4).build(); // Failure
                        responseObserver.onNext(response);
                    } else {
                        // Checks if the user has permission to edit the note
                        if (noteDAO.hasAccess(user.getUserId(), note.getTitle(), "EDITOR")) {
                            // Locks the note so no more users can edit it
                            noteDAO.lockNote(note.getNoteId(), true);

                            List<User> viewers = userDAO.getUsersByUserIds(noteDAO.getNoteViewers(note.getNoteId()));
                            List<User> editors = userDAO.getUsersByUserIds(noteDAO.getNoteEditors(note.getNoteId()));

                            for (User u : viewers) 
                                note.addViewer(u);

                            for (User u : editors)
                                note.addEditor(u);
                            
                            // Note -> Json
                            JsonObject noteJson = note.toJSON((userDAO.getUserByUserId(note.getOwnerId())).getUsername());

                            // Get the last user public key
                            String pubKey = userDAO.getUserPubKey(note.getLastModifiedBy());
                            PublicKey publicKey = loadPublicKey(pubKey);

                            // Get the note keys
                            String decryptedSecretKey = genRSA.decryptWithPublicKey(noteDAO.getNoteSecretKey(note.getNoteId(), note.getVersion()), publicKey);
                            String decryptedHmacKey = genRSA.decryptWithPublicKey(noteDAO.getNoteHmacKey(note.getNoteId(), note.getVersion()), publicKey);

                            // Encrypt the note key's again
                            pubKey = userDAO.getUserPubKey(user.getUserId());
                            publicKey = loadPublicKey(pubKey);

                            // Encrypt the key's
                            String encryptedSecretKey = genRSA.encryptWithPublicKey(decryptedSecretKey, publicKey);
                            String encryptedHmacKey = genRSA.encryptWithPublicKey(decryptedHmacKey, publicKey);

                            // Json -> String
                            ENotePhase1Response response = ENotePhase1Response.newBuilder()
                            .setAck(0)
                            .setNote(noteJson.toString())
                            .setSecretKey(encryptedSecretKey)
                            .setHmacKey(encryptedHmacKey)
                            .build(); // Success
                            responseObserver.onNext(response);
                        } else {
                            ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(3).build(); // Failure
                            responseObserver.onNext(response);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(-1).build(); // Error
            responseObserver.onNext(response);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            ENotePhase1Response response = ENotePhase1Response.newBuilder().setAck(-2).build(); // Error
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted(); 
    }

    @Override
    public void enoteP2(ENotePhase2Request request, StreamObserver<ENotePhase2Response> responseObserver) {
        System.out.println("Received a phase 2 edit note from: " + request.getUsername());
        
        try {
            // Convert the JSON.toString() to a Note
            Gson gson = new Gson();
            JsonObject noteJson = gson.fromJson(request.getNote(), JsonObject.class);
            JsonObject owner = noteJson.getAsJsonObject("owner");
            Note note = new Note(
                noteJson.get("id").getAsInt(),
                noteJson.get("title").getAsString(),
                noteJson.get("note").getAsString(),
                noteJson.get("data_created").getAsString(),
                userDAO.getUserByUsername(request.getUsername()).getUserId(),
                (noteJson.get("version").getAsInt()) + 1,
                (noteJson.getAsJsonObject("owner")).get("id").getAsInt(),
                noteJson.get("hmac").getAsString(),
                noteJson.get("iv").getAsString()
            );
            
            if (request.getUsername().equals((noteJson.getAsJsonObject("owner")).get("username").getAsString())) {
                // Remove all the accesses user's have to the note to then add them (in case of an update)
                noteDAO.removeAccesses(note.getNoteId());

                // Add viewer permissions
                for (JsonElement other_user : noteJson.getAsJsonArray("viewers")) {
                    // If it cant add the user prob the user isn't registered
                    int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                    noteDAO.grantAccessNote(other_user_id, note.getNoteId(), note.getOwnerId(), "VIEWER");
                }

                // Add editor permissions
                for (JsonElement other_user : noteJson.getAsJsonArray("editors")) {
                    // If it cant add the user prob the user isn't registered
                    int other_user_id = other_user.getAsJsonObject().get("id").getAsInt();
                    noteDAO.grantAccessNote(other_user_id, note.getNoteId(), note.getOwnerId(), "EDITOR");
                }
            }

            noteDAO.insertNote(note, request.getSecretKey(), request.getHmacKey());
            noteDAO.lockNote(note.getNoteId(), false);

            ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(0).build();         // Success
            responseObserver.onNext(response);

        } catch (SQLException e) {
            e.printStackTrace();
            ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(-1).build();         // Failure
            responseObserver.onNext(response);

        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON format: " + e.getMessage());
            ENotePhase2Response response = ENotePhase2Response.newBuilder().setAck(-2).build();         // Failure
            responseObserver.onNext(response);

        }

        responseObserver.onCompleted();
    }
    
    // Auxiliar function
    private PublicKey loadPublicKey(String publicKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Decode the Base64-encoded public key string
        byte[] pubEncoded = Base64.getDecoder().decode(publicKeyString);

        // Convert the byte array into a PublicKey object
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(pubSpec);
    }
}