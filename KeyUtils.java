// RSA in Java - Krzysztof Majewski - https://www.baeldung.com/java-rsa - Accessed 11.11.2023

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class KeyUtils {
    private static final String PUBLIC_KEY_STRING = "-public.key";
    private static final String PRIVATE_KEY_STRING = "-private.key";
    private static final String ALGORITHM = "RSA";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static void main(String[] args) {
        try {
            KeyPair keyPair = generateRSAKeyPair();
            String originalString = "This is something encrypted.";
            String encryptedString = encryptString(originalString, keyPair.getPublic());
            String decryptedString = decryptString(encryptedString, keyPair.getPrivate());

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("Original String:\n");
            stringBuilder.append(originalString);
            stringBuilder.append('\n');

            stringBuilder.append("Encrypted String:\n");
            stringBuilder.append(encryptedString);
            stringBuilder.append('\n');

            stringBuilder.append("Decrypted String:\n");
            stringBuilder.append(decryptedString);
            stringBuilder.append('\n');

            System.out.println(stringBuilder.toString());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private static String encryptString(String unencryptedString, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        byte[] unencryptedBytes = unencryptedString.getBytes(CHARSET);
        Cipher encryptCipher = Cipher.getInstance(ALGORITHM);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = encryptCipher.doFinal(unencryptedBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String decryptString(String encryptedString, PrivateKey privateKey)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);
        Cipher decryptCipher = Cipher.getInstance(ALGORITHM);
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, CHARSET);
    }

    private static void saveKey(int chatID, KeyPair keyPair) {
        try (FileOutputStream fos = new FileOutputStream(chatID + PUBLIC_KEY_STRING)) {
            fos.write(keyPair.getPublic().getEncoded());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        try (FileOutputStream fos = new FileOutputStream(chatID + PRIVATE_KEY_STRING)) {
            fos.write(keyPair.getPrivate().getEncoded());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    private static PublicKey readPublicKey(int chatID)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(readKey(chatID + PUBLIC_KEY_STRING));
    }

    private static PrivateKey readPrivateKey(int chatID)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(readKey(chatID + PRIVATE_KEY_STRING));
    }

    private static EncodedKeySpec readKey(String filePath) throws IOException {
        File keyFile = new File(filePath);
        byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
        return new X509EncodedKeySpec(keyBytes);
    }

    private static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}