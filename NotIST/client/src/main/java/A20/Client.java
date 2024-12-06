package NotIST.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import javax.sound.midi.SysexMessage;

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

		// # TTTGrpc.TTTBlockingStub stub = TTTGrpc.newBlockingStub(channel);

		// #TODO

		// A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
	}
}
