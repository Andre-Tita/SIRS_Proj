package A20.client;

import A20.*;
import A20.util.*;
import io.grpc.ManagedChannel;
import io.grpc.netty.*;
import io.netty.handler.ssl.SslContext;

import java.io.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.*;

public class ClientMain {
	// User commands
    private static final String SPACE = " ";
	private static final String LOGIN = "login";                // login (user alr exists)
	private static final String SIGNUP = "signup";              // signup (user is new)
	private static final String LOGOUT = "logout";              // logout
    private static final String EXIT = "exit";                  // exit the client app

    // Notes commands
    private static final String NEW_NOTE = "nnote";             // create's a note
    private static final String ADD_NOTE = "anote";             // sends a note already created locally
	private static final String READ_NOTE = "rnote";            // read a note
    private static final String EDIT_NOTE = "enote";            // edit a note
	private static final String SEE_NOTES = "snotes";           // show the notes and note's ids that the user has access
	private static final String MY_NOTES = "mnotes";            // show the notes created by the user
	private static final String HELP = "help";                  // show how to use each command
    private final String host = "192.168.1.1";
    private final String port = "50052";
    
    // Error messages
	private static final String NOT_LOGGEDIN = "You are not logged in. Type \"help\" to see all the available commands and it's usage.";
    private static final String ALR_LOGGEDIN = "You are already logged in. Type \"help\" to see all the available commands and it's usage.";
	private static final String FORMAT_ERROR = "Invalid command or format.\nTry \"help\" to see all the available commands and it's usage.";
	private static final String USER_NOT_EXIST = "ERROR: Your username/user doesn't exist.";
    private static final String SQL_ERROR = "ERROR: Server SQL error.";
    private static final String JSON_ERROR = "ERROR: Server JSON error."; 
    private static final String UNKNOWN = "Unknown error.";

    // Paths
    private static final String PRIVATE_KEY_PATH = "src/main/java/A20/client/keys/";
    private static final String tmpPath = "src/main/java/A20/client/editable.json";
    private static final String secretKeyPath = "src/main/java/A20/client/keys/secret.key";
    private static final String HMACPath = "src/main/java/A20/client/keys/hmac.key";

    // Communication channels
    private NotISTGrpc.NotISTBlockingStub stub;
    private ManagedChannel channel;

    // Security
    private KeyGeneratorForHMAC genHMAC = new KeyGeneratorForHMAC();
    private KeyGeneratorForAES genAES = new KeyGeneratorForAES();
    private KeyGeneratorForRSA genRSA = new KeyGeneratorForRSA();
    private CommandLineInterface ci = new CommandLineInterface();

    // Auxiliar variables
    private String username;
    private boolean loggedin;
    private PrivateKey privKey;

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

    // Function to start the communication (TLS)
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

    // Function to end the communication
    private void endComms() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }
    
    // Load the current user private key
    private void loadPrivateKey() {
        try {
            // Attempt to load the private key from the file
            FileInputStream privFis = new FileInputStream(PRIVATE_KEY_PATH + this.username + "_privateKey.key");
            byte[] privEncoded = new byte[privFis.available()];
            privFis.read(privEncoded);
            privFis.close();

            // Convert the byte array into a PrivateKey object
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
            KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
            PrivateKey priv = keyFacPriv.generatePrivate(privSpec);
            this.privKey = priv;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("Error loading private key: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Function to write a key to a file
    public void writeKeyToFile(String encryptedKey, String filePath) throws IOException {
        try {
            Files.write(Paths.get(filePath), encryptedKey.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to delete a file
    private static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.delete()) {
            return;
        } else { System.err.println("Failed to delete the file: " + filePath); }
    }

    // Function to create a "mini-note" (with the fields the user can edit)
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

    // Function to edit a note
    private void edit_note(String noteToString, String secretKey, String hmacKey) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject noteJson = JsonParser.parseString(noteToString).getAsJsonObject();
        JsonObject noteRead = new JsonObject();

        // Receive the keys and decrypt them
        try {
            String decryptedSecretKey = genRSA.decryptWithPrivateKey(secretKey, this.privKey);
            String decryptedHmacKey = genRSA.decryptWithPrivateKey(hmacKey, this.privKey);
            
            // Write them to a file (so we can use the ci)
            try {
                writeKeyToFile(decryptedSecretKey, secretKeyPath);
                writeKeyToFile(decryptedHmacKey, HMACPath);
                
            } catch (IOException e) {
                System.err.println("Error writing keys to file: " + e.getMessage());
                deleteFile(secretKeyPath);
                deleteFile(HMACPath);
                return;
            }

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            deleteFile(tmpPath);
            return;
        }

        // Write JSON to a temporary file
        try (FileWriter fileWriter = new FileWriter(tmpPath)) {
            gson.toJson(noteJson, fileWriter);
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            deleteFile(secretKeyPath);
            deleteFile(HMACPath);
            return;
        }

        // Check if the note has been tempered with
        ci.check(tmpPath, HMACPath);

        // Unprotect the note
        ci.unprotect(tmpPath, secretKeyPath, HMACPath, tmpPath);

        // We can delete the HMAC file (since we will need to generate one for the "new" note)
        deleteFile(HMACPath);

        // Read the note again (now unprotected)
        try  (FileReader fileReader = new FileReader(tmpPath)) {
            noteJson = gson.fromJson(fileReader, JsonObject.class);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            return;
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON content: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            return;
        }

        // Note is saved on noteJson, delete to write the "mini-note" on it
        deleteFile(tmpPath);

        // Write a mini note to a temporary file 
        try (FileWriter fileWriter = new FileWriter(tmpPath)) {
            gson.toJson(createMiniNote(noteJson,
            this.username.equals((noteJson.getAsJsonObject("owner")).get("username").getAsString())),
            fileWriter);
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            deleteFile(secretKeyPath);
            return;
        }

        // Try to open a text editor to edit the note
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
                    break;
                }
                Thread.sleep(1000); // Check every second
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error opening editor: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            return;
        }

        // Read the changes made to the note (unprotected) and change the original note
        try  (FileReader fileReader = new FileReader(tmpPath)) {
            // Validate JSON structure
            noteRead = gson.fromJson(fileReader, JsonObject.class);

            noteJson.addProperty("note", noteRead.get("note").getAsString());

            // Owner can change the viewers and editors
            if (this.username.equals((noteJson.getAsJsonObject("owner")).get("username").getAsString())) {
                noteJson.add("editors", noteRead.getAsJsonArray("editors"));
                noteJson.add("viewers", noteRead.getAsJsonArray("viewers"));
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            return;
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON content: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            return;
        }

        // Got the changes stored, delete the file to write/create it again
        deleteFile(tmpPath);

        // Write the note to the temporary file to protect it
        try (FileWriter fileWriter = new FileWriter(tmpPath)) {
            gson.toJson(noteJson, fileWriter);
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            deleteFile(secretKeyPath);
            return;
        }

        // Generate a new HMAC for the "new" note
        genHMAC.generateHMAC(HMACPath);

        // Protect the note again
        ci.protect(tmpPath, secretKeyPath, HMACPath, tmpPath);

        // Read the note (protected)
        try  (FileReader fileReader = new FileReader(tmpPath)) {
            noteJson = gson.fromJson(fileReader, JsonObject.class);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            deleteFile(HMACPath);
            return;
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON content: " + e.getMessage());
            deleteFile(tmpPath);
            deleteFile(secretKeyPath);
            deleteFile(HMACPath);
            return;
        }

        // Encrypt the keys and send them alongisde the note
        try {
            // Read AES key and HMAC key as Strings
            String newSecretKey = new String(Files.readAllBytes(Paths.get(secretKeyPath)), StandardCharsets.UTF_8);
            String newHmacKey = new String(Files.readAllBytes(Paths.get(HMACPath)), StandardCharsets.UTF_8);

            // Encrypt the AES and HMAC keys using the private RSA key
            String encryptedSecretKey = genRSA.encryptWithPrivateKey(newSecretKey, this.privKey);
            String encryptedHMACKey = genRSA.encryptWithPrivateKey(newHmacKey, this.privKey);

            ENotePhase2Request request = ENotePhase2Request.newBuilder()
            .setUsername(this.username)
            .setNote(noteJson.toString())
            .setSecretKey(encryptedSecretKey)
            .setHmacKey(encryptedHMACKey)
            .build();
            ENotePhase2Response response = stub.enoteP2(request);

            switch (response.getAck()) {
                case 0:
                    System.out.println("Note edited with success.");
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
        } catch (GeneralSecurityException e) {
            System.err.println("Error on the end of edit_note: ");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        
        deleteFile(tmpPath);
        deleteFile(secretKeyPath);
        deleteFile(HMACPath);
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
                loadPrivateKey();
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
        try {
            // Generate RSA key pair
            KeyPair keyPair = genRSA.generateKeyPair();

            // Extract private and public keys
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Now send the signup request to the server
            SignUpRequest request = SignUpRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(new_password)
                    .setPubKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()))
                    .build();
            SignUpResponse response = stub.signup(request);

            // Handle the server response
            switch (response.getAck()) {
                case 0:
                    System.out.println("User registered successfully.");
                    // Save the private key to a file
                    genRSA.saveKeyToFile(privateKey, PRIVATE_KEY_PATH + username + "_privateKey.key");
                    this.username = username;
                    this.loggedin = true;
                    loadPrivateKey();
                    return;
    
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
    
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Error during RSA key generation or saving private key: " + e.getMessage());
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
                this.privKey = null;
                break;
            
            case 1:
                debug(USER_NOT_EXIST);
                break;

            case 2:
                debug("ERROR: User is not logged in.");
                break;

            case -1:
                debug(SQL_ERROR);
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    private void nnote () {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject noteJson = new JsonObject();

        // Fields with empty values
        noteJson.addProperty("title", "");
        noteJson.addProperty("note", "");

        // Editors array is empty
        JsonArray editors = new JsonArray();
        noteJson.add("editors", editors);

        // Viewers array is empty
        JsonArray viewers = new JsonArray();
        noteJson.add("viewers", viewers);

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
            deleteFile(tmpPath);
            return;
        }

        // Reads the produced note to check if has the fields
        try  (FileReader fileReader = new FileReader(tmpPath)) {
            JsonObject noteRead = gson.fromJson(fileReader, JsonObject.class);
            if (!noteRead.has("title") || !noteRead.has("note")) {
                System.err.println("Error: 'title' and 'note' fields must be filled.");
                deleteFile(tmpPath);
                return;
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            deleteFile(tmpPath);
            return;
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON content: " + e.getMessage());
            deleteFile(tmpPath);
            return;
        }

        // Generate the SecretKey and the HMACKey
        genAES.generateAESKey(secretKeyPath);
        genHMAC.generateHMAC(HMACPath);

        // Protect the note with the generated keys
        ci.protect(tmpPath, secretKeyPath, HMACPath, tmpPath);

        // Read the note encrypted
        try  (FileReader fileReader = new FileReader(tmpPath)) {
            JsonObject noteRead = gson.fromJson(fileReader, JsonObject.class);

            // Read AES key and HMAC key as Strings
            String secretKey = new String(Files.readAllBytes(Paths.get(secretKeyPath)), StandardCharsets.UTF_8);
            String hmacKey = new String(Files.readAllBytes(Paths.get(HMACPath)), StandardCharsets.UTF_8);

            // Encrypt the AES and HMAC keys using the private RSA key
            String encryptedSecretKey = genRSA.encryptWithPrivateKey(secretKey, this.privKey);
            String encryptedHMACKey = genRSA.encryptWithPrivateKey(hmacKey, this.privKey);

            // Send the note to the server
            NNoteRequest request = NNoteRequest
            .newBuilder()
            .setUsername(this.username)
            .setNote(noteRead.toString())
            .setSecretKey(encryptedSecretKey)
            .setHmacKey(encryptedHMACKey)
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
                    debug("ERROR: A Note with that title already exists.");
                    break;

                case -1:
                    debug(SQL_ERROR);
                    break;
            
                default:
                    debug(UNKNOWN);
                    break;
            }
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Error handling file or encryption: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON content: " + e.getMessage());
        }

        deleteFile(tmpPath);
        deleteFile(secretKeyPath);
        deleteFile(HMACPath);
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
                // Write on a temporary file the note received from the server (encrypted)
                try (FileWriter fileWriter = new FileWriter(tmpPath)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    gson.toJson(noteJson, fileWriter);
                } catch (IOException e) {
                    System.err.println("Error creating file: " + e.getMessage());
                    return;
                }
                
                // Receive the keys and decrypt them
                try {
                    String decryptedSecretKey = genRSA.decryptWithPrivateKey(response.getSecretKey(), this.privKey);
                    String decryptedHmacKey = genRSA.decryptWithPrivateKey(response.getHmacKey(), this.privKey);
                    
                    // Write them to a file (so we can use the ci)
                    try {
                        writeKeyToFile(decryptedSecretKey, secretKeyPath);
                        writeKeyToFile(decryptedHmacKey, HMACPath);
                        
                    } catch (IOException e) {
                        System.err.println("Error writing keys to file: " + e.getMessage());
                        deleteFile(tmpPath);
                        deleteFile(secretKeyPath);
                        deleteFile(HMACPath);
                        return;
                    }

                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    deleteFile(tmpPath);
                    return;
                }

                // Verify the integrity
                ci.check(tmpPath, HMACPath);

                // Unprotect the note
                ci.unprotect(tmpPath, secretKeyPath, HMACPath, tmpPath);

                try (FileReader fileReader = new FileReader(tmpPath)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject noteRead = gson.fromJson(fileReader, JsonObject.class);
                    System.out.println(gson.toJson(noteRead));     
                } catch (IOException e) {
                    System.err.println("Error reading file: " + e.getMessage());
                }

                deleteFile(tmpPath);
                deleteFile(secretKeyPath);
                deleteFile(HMACPath);
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
            
            case -2:
                debug("ERROR: Error encrypting or decrypting...");
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    private void enote(String title) {
        ENotePhase1Request request = ENotePhase1Request.newBuilder().setUsername(this.username).setTitle(title).build();
        ENotePhase1Response response = stub.enoteP1(request);
        switch (response.getAck()) {
            case 0:
                edit_note(response.getNote(), response.getSecretKey(), response.getHmacKey());
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
            
            case -2:
                debug("ERROR: Error encrypting or decrypting...");
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    // main loop
    private void main_loop() {
        this.initComms();

        File directory = new File(PRIVATE_KEY_PATH);

        // Check if the directory exists
        if (!directory.exists()) {
            directory.mkdirs();
        }

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
                        "NEW NOTE: nnote\n" +
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