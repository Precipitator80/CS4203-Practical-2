// RSA in Java - Krzysztof Majewski - https://www.baeldung.com/java-rsa - Accessed 11.11.2023

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyUtils {
    public static final String KEY_FOLDER_STRING = "Chat Keys/";
    public static final String PUBLIC_KEY_STRING = "-rsa-public-key.pem";
    public static final String PRIVATE_KEY_STRING = "-rsa-public-key.pem";
    public static final String AES_KEY_STRING = "-aes-key.pem";
    public static final String RSA = "RSA";
    public static final int RSA_SIZE = 2048;
    public static final String AES = "AES";
    public static final int AES_SIZE = 256;
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    // Maximum string length using RSA and UTF_8. 245 bytes / 4 bytes per char (max case) = 61.

    // How can I get the maximum byte array length from a string that has always the same length?
    // Manfred Radlwimmer
    // https://stackoverflow.com/questions/38072073/how-can-i-get-the-maximum-byte-array-length-from-a-string-that-has-always-the-sa
    // Accessed 12.11.2023

    public static void main(String[] args) {
        try {
            int chatID = 1;

            PublicKey rsaPublicKey;
            PrivateKey rsaPrivateKey;
            SecretKey aesKey;
            try {
                rsaPublicKey = readRSAPublicKey(chatID);
                rsaPrivateKey = readRSAPrivateKey(chatID);
                aesKey = readAESKey(chatID);
                System.out.println("Read keys successfully.");
            } catch (FileNotFoundException e) {
                KeyPair rsaKeyPair = generateRSAKeyPair();
                saveRSAKeyPair(chatID, rsaKeyPair);
                rsaPublicKey = rsaKeyPair.getPublic();
                rsaPrivateKey = rsaKeyPair.getPrivate();

                aesKey = generateAESKey();
                saveAESKey(chatID, aesKey);

                System.out.println("Generated keys successfully.");
            }

            String originalString = "This is the sixth message!";
            String encryptedString = encryptString(originalString, aesKey, AES);
            String encryptedKey = encryptKey(aesKey, rsaPublicKey, RSA);

            SecretKey decryptedKey = decryptKey(encryptedKey, AES, rsaPrivateKey, RSA);
            String decryptedString = decryptString(encryptedString, aesKey, AES);

            System.out.println("Original String:\n" + originalString);
            System.out.println("Encrypted String:\n" + encryptedString);
            System.out.println("Decrypted String:\n" + decryptedString);
            System.out.println("Encrypted Key:\n" + encryptedKey);
            System.out.println("Decrypted Key:\n" + decryptedKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * KEY GENERATION METHODS
     */

    /**
     * Generates an RSA key pair used for encrypting the AES key.
     * @return The generated RSA key pair.
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(RSA_SIZE);
        return generator.generateKeyPair();
    }

    /**
     * Generates an AES key used for encrypting the message.
     * @return The generated AES key.
     */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(AES);
        generator.init(AES_SIZE);
        return generator.generateKey();
    }

    /**
     * ENCRYPTION METHODS
     */

    /**
     * Encrypts a string using a key.
     * @param unencryptedString The string to encrypt.
     * @param key The key to encrypt the string with.
     * @param algorithm The algorithm to use to encrypt the string.
     * @return The encrypted string.
     */
    public static String encryptString(String unencryptedString, Key key, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        byte[] unencryptedBytes = unencryptedString.getBytes(CHARSET);
        return encryptBytes(unencryptedBytes, key, algorithm);
    }

    /**
     * Encrypts a key as a string.
     * @param keyToEncrypt The key to encrypt.
     * @param keyForEncryption A second key to use to encrypt the first key.
     * @param algorithm The algorithm to use to encrypt the key.
     * @return The encrypted key as a string.
     */
    public static String encryptKey(Key keyToEncrypt, PublicKey keyForEncryption, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        return encryptBytes(keyToEncrypt.getEncoded(), keyForEncryption, algorithm);
    }

    /**
     * Encrypts bytes as a string using a key.
     * @param unencryptedBytes The bytes to encrypt.
     * @param key The key to encrypt the bytes with.
     * @param algorithm The algorithm to use to encrypt the bytes.
     * @return The encrypted bytes as a string.
     */
    public static String encryptBytes(byte[] unencryptedBytes, Key key, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = encryptCipher.doFinal(unencryptedBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * DECRYPTION METHODS
     */

    /**
     * Decrypts a string using a key.
     * @param encryptedString The string to decrypt.
     * @param key The key to decrypt the string with.
     * @param algorithm The algorithm to use to decrypt the string.
     * @return The decrypted string.
     */
    public static String decryptString(String encryptedString, Key key, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        return new String(decryptBytes(encryptedString, key, algorithm), CHARSET);
    }

    /**
     * Decrypts a key.
     * @param keyToEncrypt The key to decrypt as a string.
     * @param keyForEncryption A second key to use to decrypt the first key.
     * @param algorithm The algorithm to use to decrypt the key.
     * @return The decrypted key.
     */
    public static SecretKeySpec decryptKey(String encryptedKey, String encryptedKeyAlgorithm,
            PrivateKey keyForDecryption,
            String decryptionAlgorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        return new SecretKeySpec(decryptBytes(encryptedKey, keyForDecryption, decryptionAlgorithm),
                encryptedKeyAlgorithm);
    }

    /**
     * Decrypts bytes using a key.
     * @param unencryptedBytes The bytes to decrypt as a string.
     * @param key The key to decrypt the bytes with.
     * @param algorithm The algorithm to use to decrypt the bytes.
     * @return The decrypted bytes.
     */
    public static byte[] decryptBytes(String encryptedString, Key key, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);
        Cipher decryptCipher = Cipher.getInstance(algorithm);
        decryptCipher.init(Cipher.DECRYPT_MODE, key);
        return decryptCipher.doFinal(encryptedBytes);
    }

    /**
     * KEY FILE MANAGEMENT METHODS
     */

    /**
     * Saves an RSA key pair.
     * @param chatID The chat the key pair is for.
     * @param rsaKeyPair The key pair to save.
     */
    public static void saveRSAKeyPair(int chatID, KeyPair rsaKeyPair) {
        saveRSAPublicKey(chatID, rsaKeyPair.getPublic());
        saveRSAPrivateKey(chatID, rsaKeyPair.getPrivate());
    }

    public static void saveRSAPublicKey(int chatID, PublicKey publicKey) {
        try (FileOutputStream fos = new FileOutputStream(KEY_FOLDER_STRING + chatID + PUBLIC_KEY_STRING)) {
            fos.write(publicKey.getEncoded());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    public static void saveRSAPrivateKey(int chatID, PrivateKey privateKey) {
        try (FileOutputStream fos = new FileOutputStream(KEY_FOLDER_STRING + chatID + PRIVATE_KEY_STRING)) {
            fos.write(privateKey.getEncoded());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Saves an AES key.
     * @param chatID The chat the key is for.
     * @param aesKey The key to save.
     */
    public static void saveAESKey(int chatID, SecretKey aesKey) {
        try (FileOutputStream fos = new FileOutputStream(KEY_FOLDER_STRING + chatID + AES_KEY_STRING)) {
            fos.write(aesKey.getEncoded());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Reads the public key for a given chat.
     * @param chatID The chat the key is for.
     * @return The public key for the chat.
     * @throws InvalidKeySpecException If the key file does not match the key specification.
     * @throws IOException If the file containing the key could not be found or read properly.
     */
    public static PublicKey readRSAPublicKey(int chatID)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        byte[] bytes = readKeyBytes(chatID + PUBLIC_KEY_STRING);
        return readRSAPublicKey(bytes);
    }

    public static PublicKey readRSAPublicKey(byte[] bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * Reads the public key for a given chat.
     * @param chatID The chat the key is for.
     * @return The public key for the chat.
     * @throws InvalidKeySpecException If the key file does not match the key specification.
     * @throws IOException If the file containing the key could not be found or read properly.
     */
    public static PrivateKey readRSAPrivateKey(int chatID)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PKCS8EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(readKeyBytes(chatID + PRIVATE_KEY_STRING));
        return keyFactory.generatePrivate(publicKeySpec);
    }

    /**
     * Reads the AES key for a given chat.
     * @param chatID The chat the key is for.
     * @return The AES key for the chat.
     * @throws IOException If the file containing the key could not be found or read properly.
     */
    public static SecretKey readAESKey(int chatID) throws IOException {
        return new SecretKeySpec(readKeyBytes(chatID + AES_KEY_STRING), AES);
    }

    /**
     * Reads a key at a given path.
     * @param fileName The name of the key file.
     * @return The bytes of the key.
     * @throws IOException If the file could not be found or read properly.
     */
    public static byte[] readKeyBytes(String fileName) throws IOException {
        File keyFile = new File(KEY_FOLDER_STRING + fileName);
        return Files.readAllBytes(keyFile.toPath());
    }
}