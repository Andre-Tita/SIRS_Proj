package A20.client;

import A20.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Scanner;

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
	private static final String DEFAULT = "Invalid command or format.\nTry \"help\" to see all the available commands and it's usage.";
	NotISTGrpc.NotISTBlockingStub stub;
    ManagedChannel channel;

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

	public void main_loop() {
        this.initComms();

        Scanner scanner = new Scanner(System.in);
        Boolean exit = false;
        Boolean loggedin = false;

        System.out.println("Welcome to NotIST !\nPlease singup/login before we start.\nType \"help\" to see each command usage.");

        while(!exit) {
            System.out.print("-> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            switch (split[0]) {

                case LOGIN:
                    if (split.length == 3) {
                        if(this.login(split[1], split[2])) {
                            System.out.println("Success, logged in.");
                            loggedin = true;
                        }
                        else { debug("Error, username or password invalid."); }
                    } else { debug(DEFAULT); }
                    break;

                case SIGNUP:
                    if (split.length == 3) {
                        if(this.signup(split[1], split[2])) {
                            System.out.println("Success, signed up.");
                            loggedin = true;
                        }
                        else { debug("Error, username already in use."); }
                    } else { debug(DEFAULT); }
                    break;

                case LOGOUT:
                    if (loggedin) {
                        if (split.length == 1) {
                            if(this.logout()) {
                                System.out.println("Logged out with success. Leaving...");
                                loggedin = false;
                            } else { debug("Unexpected error."); }
                        } else { debug(DEFAULT); }
                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case EXIT:
                    if(loggedin)
                        this.logout();
                    exit = true;
                    break;
				
				// #TODO
                case NEW_NOTE:
                    if(loggedin) {

                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case READ_NOTE:
                    if(loggedin) {

                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case SEE_NOTES:
                    if(loggedin) {

                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case MY_NOTES:
                    if(loggedin) {

                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case GRANT_ACCESS:
                    if(loggedin) {

                    } else { debug(NOT_LOGGEDIN); }
                    break;

                case HELP:
                    if (loggedin) {
                        // #TODO
                        System.out.println("Available commands:\n" +
                        "LOGOUT: logout\n" +
                        "NEW NOTE: nnote\n" +
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
                    debug(DEFAULT);
                    break;
            }
        }

        this.endComms();
    }

	public Boolean login(String username, String password) {
        LoginRequest request = LoginRequest.newBuilder().setUsername(username).setPassword(password).build();
        LoginResponse response = stub.login(request);
        return response.getAck() != 0;
    }

    public Boolean signup(String username, String new_password) {
        SignUpRequest request = SignUpRequest.newBuilder().setUsername(username).setPassword(new_password).build();
        SignUpResponse response = stub.signup(request);
        return response.getAck() != 0;
    }

    public Boolean logout() {
        LogoutRequest request = LogoutRequest.newBuilder().build();
        LogoutResponse response = stub.logout(request);
        return response.getAck() != 0;
    }

    // #TODO
    public Boolean nnote() {
        return true;
    }

    public Boolean rnote(String title) {
        return true;
    }

    public Boolean snotes() {
        return true;
    }

    public Boolean mnotes() {
        return true;
    }

    public Boolean gaccess() {
        return true;
    }
}
