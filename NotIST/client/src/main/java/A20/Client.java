package NotIST.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import javax.sound.midi.SysexMessage;
import pt.tecnico.ttt.*;
import pt.tecnico.ttt.PlayResult;

import java.util.Scanner;

public class Client {
	/** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		System.err.println(debugMessage);
	}

	/** The main method is the starting point for the program. */
	public static void main(String[] args) {
		final String host = "localhost";
		final int port = "50052";
		final String target = host + ":" + port;
		debug("Target: " + target);

		// Channel is the abstraction to connect to a service endpoint.
		// Let us use plaintext communication because we do not have certificates.
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		// It is up to the client to determine whether to block the call.
		// Here we create a blocking stub, but an async stub,
		// or an async stub with Future are always possible.
		TTTGrpc.TTTBlockingStub stub = TTTGrpc.newBlockingStub(channel);

		playGame(stub);

		// A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
	}

	private static void playGame(TTTGrpc.TTTBlockingStub stub) {

		int player = 0; /* Player number - 0 or 1 */
		int go = 0; /* Square selection number for turn */
		int row = 0; /* Row index for a square */
		int column = 0; /* Column index for a square */
		int winner = -1; /* The winning player */
		PlayResult play_res;

		/*
		 * Using try with scanner - ensures the resource is closed in the end, even if
		 * there are exceptions.
		 */
		try (Scanner scanner = new Scanner(System.in)) {

			/* The main game loop. The game continues for up to 9 turns, */
			/* as long as there is no winner. */

			System.out.printf(
					"Type 1 for playing mode or type 0 for waitForWinnerMode: ");
			Integer mode = scanner.nextInt();

			if(mode == 1) {
				// Playing mode
				do {
				/* Get valid player square selection. */
					do {
						/* Print current board. */
						debug("Call currentBoard");
						System.out.println(stub.currentBoard(CurrentBoardRequest.getDefaultInstance()).getBoard());

						System.out.printf(
								"%nPlayer %d, please enter the number of the square "
										+ "where you want to place your %c (or 0 to refresh the board): ",
								player, (player == 1) ? 'X' : 'O');
						go = scanner.nextInt();
						debug("go = " + go);

						if (go == 0) {
							play_res = PlayResult.UNKNOWN;
							continue;
						}

						/* Get row index of board. */
						row = --go / 3;
						/* Get column index of board. */
						column = go % 3;
						debug("row = " + row + ", column = " + column);

						// TODO call play and set the proper play result
						PlayRequest request = PlayRequest.newBuilder().setRow(row).setColumn(column).setPlayer(player).build();
						play_res = null;
						try	{
							play_res = stub.play(request).getPlayResult();
							if (play_res != PlayResult.SUCCESS) {
								displayResult(play_res);
							}
						} catch (StatusRuntimeException e) {
							System.out.println("Caught exception with description: " +
									e.getStatus().getDescription()); // The same exception description provided in the server side
						}
					} while (play_res != PlayResult.SUCCESS);

					// TODO call check winner and set the winning player.
					CheckWinnerRequest request = CheckWinnerRequest.newBuilder().build();
					CheckWinnerResponse response = stub.checkWinner(request);
					winner = response.getResult();
					/* Select next player. */
					player = (player + 1) % 2;
					debug("player " + player);

				} while (winner == -1);

			} else {
				//waitForWinner mode
				System.out.println("Waiting for winner...");
				WaitForWinnerResponse response = stub.waitForWinner(WaitForWinnerRequest.getDefaultInstance()); //blocking call
				winner = response.getResult();
			}
			/* Game is over so display the final board. */
			debug("Call currentBoard");
			System.out.println(stub.currentBoard(CurrentBoardRequest.getDefaultInstance()).getBoard());

			/* Display result message. */
			if (winner == 2) {
				System.out.println();
				System.out.println("How boring, it is a draw");
			} else {
				System.out.println();
				System.out.println("Congratulations, player " + winner + ", YOU ARE THE WINNER!");
			}
		}
	}

	/** Helper method to display result as text. */
	private static void displayResult(PlayResult play_res) {
		switch (play_res) {
		case OUT_OF_BOUNDS:
			System.out.print("Position outside board.");
			break;
		case SQUARE_TAKEN:
			System.out.print("Square already taken.");
			break;
		case WRONG_TURN:
			System.out.print("Not your turn.");
			break;
		case GAME_FINISHED:
			System.out.print("Game has finished.");
			break;
		default:
			System.out.println("Unexpected result: " + play_res.toString());
			break;
		}
		System.out.println(" Try again...");
	}

}
