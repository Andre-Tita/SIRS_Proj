package A20.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class KeyGeneratorForRSA {
    PrivateKey privateKey;
    PublicKey publicKey;

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024); // 1024-bit RSA key

        return keyPairGenerator.generateKeyPair();
    }

    // Helper method to save a key to a file
    public static void saveKeyToFile(java.security.Key key, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(key.getEncoded());
        fos.close();
    }
    
    // Function to encrypt the keys with the public Key
    public String encryptWithPublicKey(String data, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Function to decrypt the keys with the public key
    public String decryptWithPublicKey(String encryptedData, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }


    // Function to encrypt the keys with the private Key
    public String encryptWithPrivateKey(String data, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // Function to decrypt the keys with the private key
    public String decryptWithPrivateKey(String encryptedData, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

}
