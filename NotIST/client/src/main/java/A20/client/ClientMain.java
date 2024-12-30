package A20.client;

import A20.*;
import A20.util.*;
import io.grpc.ManagedChannel;
import io.grpc.netty.*;
import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.google.gson.*;

public class ClientMain {
	private static final String SPACE = " ";
	private static final String LOGIN = "login";                // login (user alr exists)
	private static final String SIGNUP = "signup";              // signup (user is new)
	private static final String LOGOUT = "logout";              // logout
    private static final String EXIT = "exit";                  // exit the client app

	private static final String NEW_NOTE = "nnote";             // create's a note
    private static final String ADD_NOTE = "anote";             // sends a note already created locally
	private static final String READ_NOTE = "rnote";            // read a note
    private static final String EDIT_NOTE = "enote";            // edit a note
	private static final String SEE_NOTES = "snotes";           // show the notes and note's ids that the user has access
	private static final String MY_NOTES = "mnotes";            // show the notes created by the user
	private static final String HELP = "help";                  // show how to use each command
    private final String host = "127.0.0.1";
    private final String port = "50052";

	private static final String NOT_LOGGEDIN = "You are not logged in. Type \"help\" to see all the available commands and it's usage.";
    private static final String ALR_LOGGEDIN = "You are already logged in. Type \"help\" to see all the available commands and it's usage.";
	private static final String FORMAT_ERROR = "Invalid command or format.\nTry \"help\" to see all the available commands and it's usage.";
	private static final String USER_NOT_EXIST = "ERROR: Your username/user doesn't exist.";
    private static final String SQL_ERROR = "ERROR: Server SQL error.";
    private static final String JSON_ERROR = "ERROR: Server JSON error."; 
    private static final String UNKNOWN = "Unknown error.";
    
    NotISTGrpc.NotISTBlockingStub stub;
    ManagedChannel channel;
    KeyGeneratorForHMAC gen;

    private String username;
    private boolean loggedin;

    public static void main(String[] args) {
		// Main loop
        ClientMain clientMain = new ClientMain();
		clientMain.main_loop();
	}

    // Auxiliar functions

    /** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		System.err.println(debugMessage);
	}

    private void initComms() {
        final String target = host + ":" + port;
        debug("Target: " + target);

        try {
            // Load the server certificate
            File certFile = new File("src/main/java/A20/client/server.crt");
            SslContext sslContext = GrpcSslContexts.forClient()
                    .trustManager(certFile)
                    .build();

            // Create a channel with TLS enabled
            channel = NettyChannelBuilder.forTarget(target)
                    .sslContext(sslContext)
                    .build();

            stub = NotISTGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize TLS for client.");
        }
    }

    private void endComms() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }
    
    // Function to delete the file
    private static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("File deleted: " + filePath);
        } else {
            System.err.println("Failed to delete the file: " + filePath);
        }
    }

    // Function to create a "mini-note"
    private JsonObject createMiniNote(JsonObject note, boolean o) {
        JsonObject miniNote = new JsonObject();
        miniNote.addProperty("title", note.get("title").getAsString());
        miniNote.addProperty("note", note.get("note").getAsString());

        if (o) {
            miniNote.add("editors", note.getAsJsonArray("editors"));
            miniNote.add("viewers", note.getAsJsonArray("viewers"));
        }

        return miniNote;
    }

    // Function to edit the note
    private void edit_note(String noteToString) {
        String tmpPath = "src/main/java/A20/client/editable.json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject noteJson = JsonParser.parseString(noteToString).getAsJsonObject();

        // Write JSON to a temporary file
        try (FileWriter fileWriter = new FileWriter(tmpPath)) {
            gson.toJson(createMiniNote(noteJson,
            this.username.equals((noteJson.getAsJsonObject("owner")).get("username").getAsString())),
            fileWriter);
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            return;
        }

        // Try to open a text editor
        try {
            // Open file in the default text editor
            System.out.println("Opening: " + noteJson.get("title").getAsString());
            ProcessBuilder processBuilder = new ProcessBuilder("code", tmpPath);   // Code must have auto-save disabled !
            processBuilder.start();

            // Wait for the editor to save
            File editableFile = new File(tmpPath);
            long lastModified = editableFile.lastModified();

            System.out.println("Waiting for edits...");

            while (true) {
                if (editableFile.lastModified() > lastModified) {
                    System.out.println("File has been modified.");
                    break;
                }
                Thread.sleep(1000); // Check every second
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error opening editor: " + e.getMessage());
            return;
        }

        try  (FileReader fileReader = new FileReader(tmpPath)) {
            // Validate JSON structure
            JsonObject noteRead = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("New read: \n" + noteRead.toString());

            noteJson.addProperty("note", noteRead.get("note").getAsString());
            
            // Owner can change the viewers and editors
            if (this.username.equals((noteJson.getAsJsonObject("owner")).get("username").getAsString())) {
                noteJson.add("editors", noteRead.getAsJsonArray("editors"));
                noteJson.add("viewers", noteRead.getAsJsonArray("viewers"));
            }

            ENotePhase2Request request = ENotePhase2Request.newBuilder().setUsername(this.username).setNote(noteJson.toString()).build();
            ENotePhase2Response response = stub.enoteP2(request);

            switch (response.getAck()) {
                case 0:
                    System.out.println("Note edited with success.");
                    break;
                
                case -1:
                    debug(SQL_ERROR);
    
                case -2:
                    debug(JSON_ERROR);
    
                default:
                    debug(UNKNOWN);
                    break;
            }
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        } catch (JsonSyntaxException e) {
            System.out.println("Invalid JSON content: " + e.getMessage());
            return;
        }

        deleteFile(tmpPath);
    }

    // Main functions
	private void login(String username, String password) {
        LoginRequest request = LoginRequest.newBuilder().setUsername(username).setPassword(password).build();
        LoginResponse response = stub.login(request);
        switch (response.getAck()) {
            case 0:
                System.out.println("Logged in with success.");
                this.username = username;
                this.loggedin = true;
                break;

            case 1:
                debug(USER_NOT_EXIST);
                break;

            case 2:
                debug("ERROR: User already logged in.");
                break;
        
            case -1:
                debug(SQL_ERROR);
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    private void signup(String username, String new_password) {
        SignUpRequest request = SignUpRequest.newBuilder().setUsername(username).setPassword(new_password).build();
        SignUpResponse response = stub.signup(request);
        switch (response.getAck()) {
            case 0:
                System.out.println("User registered with success.");
                this.username = username;
                this.loggedin = true;
                break;

            case 1:
                debug("User already exists.");
                break;

            case -1:
                debug(SQL_ERROR);
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    private void logout() {
        LogoutRequest request = LogoutRequest.newBuilder().setUsername(this.username).build();
        LogoutResponse response = stub.logout(request);
        switch (response.getAck()) {
            case 0:
                System.out.println("Logged out with success.");
                this.username = null;
                this.loggedin = false;
                break;
            
            case 1:
                debug(USER_NOT_EXIST);
                break;

            case 2:
                debug("ERROR: User is not logged in.");
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }
    
    private void anote(String filename) {
        String filepath = "src/main/java/A20/client/";
        
        try  (FileReader fileReader = new FileReader(filepath + filename)) {
            
            // Validate JSON structure
            Gson gson = new Gson();
            JsonObject noteJson = gson.fromJson(fileReader, JsonObject.class);
    
            // Verify required fields exist
            if (!noteJson.has("title") || !noteJson.has("note")) {
                System.err.println("Invalid JSON structure. Required fields: title, note.");
                return;
            }

            System.out.println(noteJson.toString());

            NNoteRequest request = NNoteRequest.newBuilder()
            .setUsername(this.username)
            .setNote(noteJson.toString()) // Sending JSON as string
            .build();

            NNoteResponse response = stub.nnote(request);
            
            switch (response.getAck()) {
                case 0:
                    System.out.println("Note created with success.");
                    break;
            
                case 1:
                    debug(USER_NOT_EXIST);
                    break;

                case 2:
                    debug("A Note with that title already exists.");
                    break;
                
                case -1:
                    debug(SQL_ERROR);
                    break;
                
                case -2:
                    debug(JSON_ERROR);
                    break;
                
                default:
                    debug(UNKNOWN);
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        } catch (JsonSyntaxException e) {
            System.out.println("Invalid JSON content: " + e.getMessage());
            return;
        }
    }

    private void nnote () {
        JsonObject noteJson = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Fields with empty values
        noteJson.addProperty("title", "");
        noteJson.addProperty("note", "");

        // Editors array is empty
        JsonArray editors = new JsonArray();
        noteJson.add("editors", editors);

        // Viewers array is empty
        JsonArray viewers = new JsonArray();
        noteJson.add("viewers", viewers);

        String tmpPath = "src/main/java/A20/client/editable.json";

        // Writes and creates a temporary file for the user to fill
        try (FileWriter fileWriter = new FileWriter(tmpPath)) {
            gson.toJson(noteJson, fileWriter);
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            return;
        }

        System.out.println("Fill the given fields.");

        // Code to open vscode to create the note
        try {
            // Open file in the default text editor
            ProcessBuilder processBuilder = new ProcessBuilder("code", tmpPath);   // Code must have auto-save disabled !
            processBuilder.start();

            // Wait for the editor to close
            File editableFile = new File(tmpPath);
            long lastModified = editableFile.lastModified();

            while (true) {
                if (editableFile.lastModified() > lastModified) {
                    System.out.println("File has been modified.");
                    break;
                }
                Thread.sleep(1000); // Check every second
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error opening editor: " + e.getMessage());
            return;
        }

        try  (FileReader fileReader = new FileReader(tmpPath)) {
            JsonObject noteRead = gson.fromJson(fileReader, JsonObject.class);
            System.out.println("New read: \n" + noteRead.toString());

            String title = noteRead.has("title") ? noteRead.get("title").getAsString() : "";
            String note = noteRead.has("note") ? noteRead.get("note").getAsString() : "";

            if (title.isEmpty() || note.isEmpty()) {
                System.out.println("Error: 'title' and 'note' fields must be filled.");
                deleteFile(tmpPath);
                return;
            }

            // Send the note to the server
            NNoteRequest request = NNoteRequest.newBuilder().setUsername(this.username).setNote(noteRead.toString()).build();
            NNoteResponse response = stub.nnote(request);
            switch (response.getAck()) {
                case 0:
                    System.out.println("Note created with success.");

                    //Create another file named with the title and with the note itself
                    noteJson = JsonParser.parseString(response.getNote()).getAsJsonObject();
                    String newFilePath = "src/main/java/A20/client/" + noteJson.get("title").getAsString() + ".json";

                    // Copy the original file to the new file
                    try (FileWriter fileWriter = new FileWriter(newFilePath)) {
                        gson.toJson(noteJson, fileWriter);
                    } catch (IOException e) {
                        System.err.println("Error copying file: " + e.getMessage());
                    }
                    break;
                
                case 1:
                    debug(USER_NOT_EXIST);
                    break;

                case 2:
                    debug("ERROR: A Note with that title already exists.");
                    break;

                case -1:
                    debug(SQL_ERROR);
                    break;
            
                default:
                    debug(UNKNOWN);
                    break;
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        } catch (JsonSyntaxException e) {
            System.out.println("Invalid JSON content: " + e.getMessage());
            return;
        }

        deleteFile(tmpPath);

    }

    private void mnotes() {
        List<String> my_notes = new ArrayList<>();
        MNoteRequest request = MNoteRequest.newBuilder().setUsername(this.username).build();
        MNoteResponse response = stub.mnote(request);
        switch (response.getAck()) {
            case 0:
                my_notes = response.getNoteTitlesList();
                System.out.println("Your notes:\n" + my_notes);
                break;
                
            case 1:
                debug(USER_NOT_EXIST);
                break;
                            
            case -1:
                    debug(SQL_ERROR);
                    break;

            default:
                debug(UNKNOWN);
                break;

        }
    }

    private void snotes() {
        List<String> availableNotes = new ArrayList<>();
        SNotesRequest request = SNotesRequest.newBuilder().setUsername(this.username).build();
        SNotesResponse response = stub.snotes(request);
        switch (response.getAck()) {
            case 0:
                availableNotes = response.getNoteTitlesList();
                System.out.println("Notes you have access to:");
                for(String note: availableNotes) {
                    System.out.println(note);
                }
                break;
            case 1:
                debug(USER_NOT_EXIST);
                break;

            case -1:
                debug(SQL_ERROR);
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    private void rnote(String title, int version) {
        RNoteRequest request = RNoteRequest.newBuilder().setUsername(this.username).setTitle(title).setVersion(version).build();
        RNoteResponse response = stub.rnote(request);
        switch (response.getAck()) {
            case 0:
                JsonObject noteJson = JsonParser.parseString(response.getNote()).getAsJsonObject();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                System.out.println(gson.toJson(noteJson));                
                break;
            
            case 1:
                debug(USER_NOT_EXIST);
                break;

            case 2:
                debug("ERROR: A note with that title doesn't exists.");
                break;

            case 3:
                debug("ERROR: You don't have access to that note.");
                break;

            case -1:
                debug(SQL_ERROR);
                break;

            default:
                break;
        }
    }

    private void enote(String title) {
        ENotePhase1Request request = ENotePhase1Request.newBuilder().setUsername(this.username).setTitle(title).build();
        ENotePhase1Response response = stub.enoteP1(request);
        switch (response.getAck()) {
            case 0:
                edit_note(response.getNote());
                break;
        
            case 1:
                debug(USER_NOT_EXIST);
                break;

            case 2:
                debug("ERROR: A note with that title doesn't exists.");
                break;

            case 3:
                debug("ERROR: You don't have access to that note.");
                break;
            
            case 4:
                debug("ERROR: The note is being accessed by other user.");
                break;

            case -1:
                debug(SQL_ERROR);
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    // main loop
    private void main_loop() {
        this.initComms();

        Scanner scanner = new Scanner(System.in);
        Boolean exit = false;

        System.out.println("Welcome to NotIST !\nPlease singup/login before we start.\nType \"help\" to see each command usage.");

        while(!exit) {
            System.out.print("-> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            switch (split[0]) {

                case LOGIN:
                    if (split.length == 3) {
                        if (this.loggedin) {
                            debug(ALR_LOGGEDIN);
                            break;
                        } this.login(split[1], split[2]);
                    } else { debug(FORMAT_ERROR); }
                    break;

                case SIGNUP:
                    if (split.length == 3) {
                        if (this.loggedin) {
                            debug(ALR_LOGGEDIN);
                            break;
                        } this.signup(split[1], split[2]);
                    } else { debug(FORMAT_ERROR); }
                    break;

                case LOGOUT:
                    if (this.loggedin) {
                        if (split.length == 1) {
                            this.logout();
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case ADD_NOTE:
                    if(this.loggedin) {
                        if (split.length == 2) {
                            this.anote(split[1]);
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;
                
                case NEW_NOTE:
                    if(this.loggedin) {
                        if (split.length == 1) {
                            this.nnote();
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;
                    

                case MY_NOTES:
                    if(this.loggedin) {
                        if (split.length == 1) {
                            this.mnotes();
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;
                
                case EXIT:
                    if(this.loggedin)
                        this.logout();
                    exit = true;
                    System.out.println("Exiting NotIST app...");
                    break;
                
                case READ_NOTE:
                    if(this.loggedin) {
                        if (split.length == 3) {
                            this.rnote(split[1], Integer.parseInt(split[2]));
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case EDIT_NOTE:
                    if (this.loggedin) {
                        if (split.length == 2) {
                            this.enote(split[1]);
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case SEE_NOTES:
                    if(this.loggedin) {
                        if (split.length == 1) {
                            this.snotes();
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case HELP:
                    if (this.loggedin) {
                        System.out.println("Available commands:\n" +
                        "LOGOUT: logout\n" +
                        "ADD NOTE: anote [filepath]\n" +
                        "NEW NOTE: nnote" +
                        "READ NOTE: rnote [title] [version]\n" +
                        "EDIT NOTE: enote [title]\n" +
                        "SEE NOTES: snotes\n" +
                        "MY NOTES: mnotes\n" +
                        "GRANT ACCESS: gaccess [other_username] [note_title] [user_role] -> (can be VIEWER or EDITOR)\n"+
                        "EXIT: exit\n");
                        break;
                    } else { 
                        System.out.println("LOGIN: login [username] [password]\n" +
                        "SIGNUP: signup [username] [password]\n" +
                        "EXIT: exit\n");
                        break;
                    }

                default :
                    debug(FORMAT_ERROR);
                    break;
            }
        }

        scanner.close();
        this.endComms();
    }
}