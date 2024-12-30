package A20.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.io.IOException;

public class KeyGeneratorForAES {
    // Function to generate the AES
    public static void generateAESKey(String outputFile) {
        try {
            // Initialize AES key generator
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // Use 128-bit AES key
            SecretKey secretKey = keyGen.generateKey();
    
            // Convert the AES key to Base64
            String aesKeyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());
    
            // Write the Base64-encoded key to the specified output file
            Files.write(Paths.get(outputFile), aesKeyBase64.getBytes());
    
            System.out.println("AES key generated and saved to: " + outputFile);
    
        } catch (Exception e) {
            System.err.println("Error generating AES key: " + e.getMessage());
            e.printStackTrace();
        }
    }
}