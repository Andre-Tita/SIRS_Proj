package A20.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class CommandLineInterface {

    private static final int GCM_IV_SIZE = 12; // 12 bytes IV for AES-GCM
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag

    public void printHelp() {
        System.out.println("Usage :");
        System.out.println("  protect <input-file> <privateKey-file> <hmacKey-file> <output-file>");
        System.out.println("  check <input-file> <hmacKey-file>");
        System.out.println("  unprotect <input-file> <privatekey-file> <hmackey-file> <output-file>");
        System.out.println(" ");
        System.out.println("Explainations :");
        System.out.println("  protect : add security to the document by encrypting its content with the AES-GMC mechanism and add a hmac signature ");
        System.out.println("  check : verify the security of the document and the authenticity by checking at the hmac signature ");
        System.out.println("  unprotect : remove security of the document by decrypting it");
    }

    public void protect(String inputFile, String privateKeyFile, String hmacKeyFile, String outputFile) {
        try {
            // Step 1: Load the plaintext JSON
            String plaintextJson = new String(Files.readAllBytes(Paths.get(inputFile)), "UTF-8");
            JsonObject originalJson = JsonParser.parseString(plaintextJson).getAsJsonObject();

            // Step 2: Extract the "note" field
            String noteContent = originalJson.get("note").getAsString();

            // Step 3: Load the AES secret key
            byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // Step 4: Encrypt the "note" field
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv); // Generate a random IV

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encryptedNoteBytes = cipher.doFinal(noteContent.getBytes("UTF-8"));
            String encryptedNote = Base64.getEncoder().encodeToString(encryptedNoteBytes);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);

            // Step 5: Replace "note" field with encrypted data and add IV
            originalJson.addProperty("note", encryptedNote);
            originalJson.addProperty("iv", ivBase64);

            // Step 6: Compute the HMAC for the JSON document
            // Load the HMAC secret key
            byte[] hmacKeyBytes = Files.readAllBytes(Paths.get(hmacKeyFile));
            SecretKey hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");

            // Create the Mac instance and initialize it with the HMAC key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);

            // Convert the JSON (excluding HMAC) to a string for HMAC calculation
            String jsonWithoutHmac = originalJson.toString();
            byte[] hmacBytes = mac.doFinal(jsonWithoutHmac.getBytes("UTF-8"));
            
            // Base64-encode the HMAC and add it to the JSON
            String hmacBase64 = Base64.getEncoder().encodeToString(hmacBytes);
            originalJson.addProperty("hmac", hmacBase64);

            // Step 7: Save the modified JSON to a file
            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(originalJson, fileWriter);
            }

            System.out.println("Document encrypted and written to: " + outputFile);

        } catch (Exception e) {
            System.err.println("Error during encryption: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void check(String inputFile, String hmacKeyFile) {
        try {
            // Step 1: Load the encrypted JSON
            String encryptedJson = new String(Files.readAllBytes(Paths.get(inputFile)), "UTF-8");
            JsonObject jsonObject = JsonParser.parseString(encryptedJson).getAsJsonObject();

            // Step 2: Check required fields are present
            if (!jsonObject.has("note")) {
                System.out.println("Error: The 'note' field is missing.");
                return;
            }

            if (!jsonObject.has("iv")) {
                System.out.println("Error: The 'iv' field is missing.");
                return;
            }

            if (!jsonObject.has("hmac")) {
                System.out.println("Error: The 'hmac' field is missing.");
                return;
            }

            // Step 3: Validate Base64 encoding of 'note' and 'iv'
            String encryptedNote = jsonObject.get("note").getAsString();
            String ivBase64 = jsonObject.get("iv").getAsString();

            try {
                Base64.getDecoder().decode(encryptedNote);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: The 'note' field is not valid Base64-encoded data.");
                return;
            }

            try {
                Base64.getDecoder().decode(ivBase64);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: The 'iv' field is not valid Base64-encoded data.");
                return;
            }

            // Step 4: Extract the HMAC from the JSON
            String providedHmac = jsonObject.get("hmac").getAsString();

            // Step 5: Remove the HMAC field from the JSON object
            jsonObject.remove("hmac");

            // Step 6: Serialize JSON without HMAC
            String jsonWithoutHmac = jsonObject.toString();

            // Step 7: Load the HMAC key
            byte[] hmacKeyBytes = Files.readAllBytes(Paths.get(hmacKeyFile));
            SecretKey hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");

            // Step 8: Compute the HMAC for the JSON
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] computedHmacBytes = mac.doFinal(jsonWithoutHmac.getBytes("UTF-8"));
            String computedHmacBase64 = Base64.getEncoder().encodeToString(computedHmacBytes);

            // Step 9: Compare the received HMAC with the computed HMAC
            if (!providedHmac.equals(computedHmacBase64)) {
                throw new SecurityException("HMAC validation failed. The file may have been tampered with.");
            }

            // Step 10: Print verification success
            System.out.println("The file is properly formatted, contains encrypted data, and its integrity is verified.");
            System.out.println("  - Encrypted note field is Base64-encoded.");
            System.out.println("  - IV field is Base64-encoded.");
            System.out.println("  - HMAC signature matches.");

        } catch (Exception e) {
            System.err.println("Error during check operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unprotect(String inputFile, String keyFile, String hmacKeyFile, String outputFile) {
        try {
            // Step 1: Load the encrypted JSON
            String encryptedJson = new String(Files.readAllBytes(Paths.get(inputFile)), "UTF-8");
            JsonObject encryptedObject = JsonParser.parseString(encryptedJson).getAsJsonObject();

            // Step 2: Extract the HMAC field
            if (!encryptedObject.has("hmac")) {
                throw new IllegalArgumentException("The JSON file does not contain an HMAC field.");
            }
            String receivedHmacBase64 = encryptedObject.get("hmac").getAsString();

            // Step 3: Remove the "hmac" field for decryption
            encryptedObject.remove("hmac");

            // Step 4: Extract the IV and encrypted "note" field
            String ivBase64 = encryptedObject.get("iv").getAsString();
            String encryptedNote = encryptedObject.get("note").getAsString();

            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] encryptedNoteBytes = Base64.getDecoder().decode(encryptedNote);

            // Step 5: Load the AES secret key
            byte[] keyBytes = Files.readAllBytes(Paths.get(keyFile));
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // Step 6: Decrypt the "note" field
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decryptedNoteBytes = cipher.doFinal(encryptedNoteBytes);
            String decryptedNote = new String(decryptedNoteBytes, "UTF-8");

            // Step 7: Replace the encrypted "note" field with decrypted content
            encryptedObject.addProperty("note", decryptedNote);
            encryptedObject.remove("iv"); // Remove IV from output

            // Step 8: Save the modified JSON to a file
            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(encryptedObject, fileWriter);
            }

            System.out.println("Document decrypted and written to: " + outputFile);

        } catch (SecurityException e) {
            System.err.println("Integrity check failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during decryption: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CommandLineInterface cli = new CommandLineInterface();

        if (args.length < 2) {
            cli.printHelp();
            return;
        }

        String command = args[0];

        try {
            switch (command.toLowerCase()) {
                case "protect":
                    if (args.length != 5) {
                        System.out.println("You are missing arguments, please read again the following instructions.");
                        System.out.println(""); 
                        cli.printHelp();
                        break;
                    }
                    cli.protect(args[1], args[2], args[3], args[4]);
                    break;
                case "check":
                    if (args.length != 3) {
                        System.out.println("You are missing arguments, please read again the following instructions.");
                        System.out.println(""); 
                        cli.printHelp();
                        break;
                    }
                    cli.check(args[1], args[2]);
                    break;
                case "unprotect":
                    if (args.length != 5) {
                        System.out.println("You are missing arguments, please read again the following instructions.");
                        System.out.println("");
                        cli.printHelp();
                        break;
                    }
                    cli.unprotect(args[1], args[2], args[3], args[4]);
                    break;
                default:
                    cli.printHelp();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}