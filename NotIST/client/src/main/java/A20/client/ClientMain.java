package A20.client;

import A20.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.print.DocFlavor.STRING;

public class ClientMain {
	private static final String SPACE = " ";
	private static final String LOGIN = "login";                // login (user alr exists)
	private static final String SIGNUP = "signup";              // signup (user is new)
	private static final String LOGOUT = "logout";              // logout
    private static final String EXIT = "exit";                  // exit the client app
	private static final String NEW_NOTE = "nnote";             // create a note
	private static final String READ_NOTE = "rnote";            // read a note
	private static final String SEE_NOTES = "snotes";           // show the notes and note's ids that the user has access
	private static final String MY_NOTES = "mnotes";            // show the notes created by the user
	private static final String GRANT_ACCESS = "gaccess";       // grant access to another user to see the user notes
	private static final String HELP = "help";                  // show how to use each command
    private final String host = "localhost";
    private final String port = "50052";

	private static final String NOT_LOGGEDIN = "You are not logged in. Type \"help\" to see all the available commands and it's usage.";
    private static final String ALR_LOGGEDIN = "You are already logged in. Type \"help\" to see all the available commands and it's usage.";
	private static final String FORMAT_ERROR = "Invalid command or format.\nTry \"help\" to see all the available commands and it's usage.";
	private static final String USER_NOT_EXIST = "ERROR: Your username/user doesn't exist.";
    private static final String SQL_ERROR = "ERROR: Server SQL error.";
    private static final String UNKNOWN = "Unknown error.";
    
    NotISTGrpc.NotISTBlockingStub stub;
    ManagedChannel channel;

    private String username;
    private boolean loggedin;

    public static void main(String[] args) {
		// Main loop
        ClientMain clientMain = new ClientMain();
		clientMain.main_loop();
	}

    /** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		System.err.println(debugMessage);
	}

    private void initComms() {
        final String target = host + ":" + port;
		debug("Target: " + target);

		// #TODO: We use plaintext communication because we do not have certificates (change).
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
		stub = NotISTGrpc.newBlockingStub(channel);
    }

    private void endComms() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }
    
	public void login(String username, String password) {
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
        
            case -1:
                debug(SQL_ERROR);
                break;

            default:
                debug(UNKNOWN);
                break;
        }
    }

    public void signup(String username, String new_password) {
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

    public void logout() {
        LogoutRequest request = LogoutRequest.newBuilder().build();
        LogoutResponse response = stub.logout(request);
        switch (response.getAck()) {
            case 0:
                System.out.println("Logged out with success.");
                this.username = null;
                this.loggedin = false;
                break;
        
            default:
                debug(UNKNOWN);
                break;
        }
    }
    
    public void nnote(String title, String content) {
        NNoteRequest request = NNoteRequest.newBuilder().setUsername(this.username).setTitle(title).setContent(content).build();
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
            
            default:
                debug(UNKNOWN);
                break;
        }
    }

    public void mnotes() {
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

    public void rnote(String title) {
        RNoteRequest request = RNoteRequest.newBuilder().setUsername(this.username).setTitle(title).build();
        RNoteResponse response = stub.rnote(request);
        switch (response.getAck()) {
            case 0:
                System.out.println(title + "\n" + response.getContent() + "\n");                
                break;
            
            case 1:
                debug(USER_NOT_EXIST);

            case 2:
                debug("ERROR: A note with that title doesn't exists.");

            case 3:
                debug("ERROR: You don't have access to that note.");

            case -1:
                debug(SQL_ERROR);
                break;

            default:
                break;
        }
    }

    public void snotes() {
        List<String> availableNotes = new ArrayList<>();
        SNotesRequest request = SNotesRequest.newBuilder().setUsername(this.username).build();
        SNotesResponse response = stub.snotes(request);
        switch (response.getAck()) {
            case 0:
                availableNotes = response.getNoteTitlesList();
                System.out.println("Notes you have access to: " + availableNotes);
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

    public void gaccess(String other_username, String title) {
        GAccessRequest request = GAccessRequest.newBuilder().setUsername(this.username).setOtherUsername(other_username).setTitle(title).build();
        GAccessResponse response = stub.gaccess(request);
        switch (response.getAck()) {
            case 0:
                System.out.println("Viewer access granted to: " + other_username);
                break;

            case 1:
                debug(USER_NOT_EXIST);
                break;

            case 2:
                debug("ERROR: Note doesn't exist.");
                break;
            
            case 3:
                debug("ERROR: other_username doesn't represent an existant user.");
                break;

            case 4:
                debug("ERROR: You are not the owner of that note.");
                break;
            
            case -1:
                debug(SQL_ERROR);
                break;        
            
            default:
                debug(UNKNOWN);
                break;
        }
    }
    
    public void main_loop() {
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

                case NEW_NOTE:
                    if(this.loggedin) {
                        if (split.length == 3) {
                            this.nnote(split [1], split[2]);        // #CHANGE !!! 
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
                        if (split.length == 2) {
                            this.rnote(split[1]);
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

                case GRANT_ACCESS:
                    if(this.loggedin) {
                        if (split.length == 3) {
                            this.gaccess(split[1], split[2]);
                        } else { debug(FORMAT_ERROR); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case HELP:
                    if (this.loggedin) {
                        System.out.println("Available commands:\n" +
                        "LOGOUT: logout\n" +
                        "NEW NOTE: nnote [title] [content]\n" +
                        "READ NOTE: rnote [title]\n" +
                        "SEE NOTES: snotes\n" +
                        "MY NOTES: mnotes\n" +
                        "GRANT ACCESS: gaccess [other_username] [note_title]\n"+
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

        this.endComms();
    }
}