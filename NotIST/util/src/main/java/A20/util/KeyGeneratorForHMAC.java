package A20.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class KeyGeneratorForHMAC {
    // Function to generate the HMAC
    public void generateHMAC(String outputFile) {
        try {
            // Step 1: Generate a new HMAC key
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256); // 256-bit key for HMAC
            SecretKey hmacKey = keyGen.generateKey();

            // Step 2: Convert the key to Base64
            String hmacKeyBase64 = Base64.getEncoder().encodeToString(hmacKey.getEncoded());

            // Step 3: Write the key to the specified output file
            Files.write(Paths.get(outputFile), hmacKeyBase64.getBytes());

            System.out.println("HMAC key generated.");

        } catch (Exception e) {
            System.err.println("Error generating HMAC key: " + e.getMessage());
            e.printStackTrace();
        }
    }   
}
