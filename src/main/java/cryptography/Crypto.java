package cryptography;

import com.lambdaworks.crypto.SCrypt;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.DestroyFailedException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

/**
 * Allows the encryption of Strings and byte arrays, along with methods to encrypt and write data and read and decrypt
 * data from files. Uses SCrypt to derive keys from passwords and uses those keys to encrypt the data using AES.
 */
public class Crypto {
    // The length of the initialization vector
    private static final int IV_LENGTH = 16;
    // Used for creating byte arrays from Strings/char[] and vice versa
    private static final Charset ENCODER = StandardCharsets.UTF_8;
    // The algorithm used during encryption
    private static final String ALGORITHM_MODE_PADDING = "AES/CBC/PKCS5PADDING";
    private static final String ALGORITHM = "AES";

    // The length of the salt, in bytes, generated for SCrypt during key generation. Ensures encryption output
    // differs each time encryption occurs
    private static final int SALT_LENGTH = 32;
    // The number of iterations for SCrypt to make when creating a key; a power of 2; bigger numbers = more time
    private static final int SCRYPT_ITERATIONS_COUNT = 262144;
    // The block size used by SCrypt during key generation. Bigger = more memory required
    private static final int SCRYPT_BLOCK_SIZE = 8;
    // The number of threads used by SCrypt; bigger = more threads, = more memory
    private static final int SCRYPT_PARALLELISM_FACTOR = 1;
    // The length of the key generated by SCrypt and used by AES; must be 128, 192 or 256 bits
    private static final int SCRYPT_KEY_LENGTH_BITS = 256;
    private static final int SCRYPT_KEY_LENGTH = SCRYPT_KEY_LENGTH_BITS / 8;

    /**
     * Generates a 16 byte AES key from a password to be used for encryption and decryption purposes.
     * Uses SCrypt and a given salt
     * @param password a byte array containing a password with which to create the key
     * @param salt The salt to use
     * @return the SecretKey created.
     */
    public static SecretKey generateKey(byte[] password, byte[] salt) {
        byte[] bytes;
        try {
            // Use SCrypt to create a 16 byte hash of the password with a randomized salt
            bytes = SCrypt.scrypt(password, salt, SCRYPT_ITERATIONS_COUNT,
                    SCRYPT_BLOCK_SIZE, SCRYPT_PARALLELISM_FACTOR, SCRYPT_KEY_LENGTH);
        } catch (GeneralSecurityException e) {
            return null;
        }
        // Wipe the array
        // Create a new SecretKeySpec with the bytes given
        return new DestroyableKey(bytes);
    }

    /**
     * Used SCrypt to derive a key from a password and salt
     * @param password The password to use during derivation
     * @param salt The salt to use
     * @return a {@link SecretKey} object containing the derived key which may be used for encryption and decryption.
     */
    public static SecretKey deriveKey(char[] password, byte[] salt) {
        byte[] passwordBytes = charsToBytes(password);
        SecretKey key = generateKey(passwordBytes, salt);
        wipeArray(passwordBytes);
        return key;
    }

    /**
     * Creates a new array with a given size and fills it with random values
     * @param size The size of the new array
     * @return a byte array of randomly generated values with the desired size
     */
    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        fillRandomly(bytes);
        return bytes;
    }

    /**
     * Fills an array with randomly generated values, utilizing the highest-priority pseudo-random number generating
     * algorithm provided by {@link SecureRandom}.
     * @param destination The array to fill
     */
    public static void fillRandomly(byte[] destination) {
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(destination);
    }

    /**
     * Encrypts a String into an array of bytes for later decryption and saves the resulting bytes in f.
     * @param f The file to write to
     * @param plaintext The plaintext to encrypt
     * @param password The password to encrypt with
     * @return true if successful, otherwise false
     */
    public static boolean encryptStringToFile(File f, String plaintext, char[] password) {
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] plaintextBytes = encode(plaintext);
        SecretKey key = deriveKey(password, salt);
        // Attempt to encrypt the file
        boolean result = encryptFile(f, plaintextBytes, key, salt);
        destroyKey(key);
        wipeArray(plaintextBytes);
        wipeArray(salt);
        return result;
    }

    /**
     * Reads encrypted data from a file and attempts to decrypt it using the given password
     * @param f The file to decrypt
     * @param password The password to use during decryption
     * @return The resulting String if successful, otherwise null
     */
    public static String decryptStringFromFile(File f, char[] password) {
        byte[] plainText = decryptFile(f, password);
        // Return nothing if decryption failed
        if(plainText == null)
            return null;
        // Generate a new String from the plain text and destroy the plain text array
        String result = new String(plainText, ENCODER);
        wipeArray(plainText);
        return result;
    }

    /**
     * Encrypts plaintext data using the given key and writes it to the given file
     * @param f The file to write to
     * @param data The data, as bytes, to encrypt
     * @param key The key to encrypt with
     * @return true on success, otherwise false
     */
    public static boolean encryptFile(File f, byte[] data, SecretKey key, byte[] salt) {
        try (FileOutputStream out = new FileOutputStream(f)) {
            byte[] cipherText = encrypt(data, key, salt);
            // If encryption failed, return false
            if(cipherText == null)
                return false;
            out.write(cipherText);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads and decrypts data from a given file using a given password.
     * @param f The file to decrypt
     * @param password The password used during key derivation to decrypt the file
     * @return A byte array containing the decrypted plaintext
     */
    public static byte[] decryptFile(File f, char[] password) {
        byte[] iv = new byte[IV_LENGTH];
        byte[] salt = new byte[SALT_LENGTH];
        byte[] cipherText;
        // Read the salt, iv and cipher text from the file
        try(FileInputStream in = new FileInputStream(f)) {
            in.read(salt);
            in.read(iv);
            cipherText = in.readAllBytes();
        } catch (IOException e) {
            // File doesn't exist; exit
            e.printStackTrace();
            return null;
        }// An error occurred while reading the file; exit

        // Generate a key from the given password and salt
        SecretKey key = deriveKey(password, salt);
        // Destroy the salt
        wipeArray(salt);
        // Attempt to decrypt the file; if an error occurs, exit by returning null
        byte[] plainText;
        try {
            plainText = decrypt(cipherText, iv, key);
        } catch (InvalidPasswordException e) {
            return null;
        }
        // Clean up after ourselves to prevent memory leaks
        destroyKey(key);
        wipeArray(cipherText);
        wipeArray(iv);
        return plainText;
    }

    /**
     * Decrypts the given cipher text data using the given key
     * @param cipherText The data to encrypt
     * @param key The key to use during decryption
     * @return The resulting plaintext bytes if successful, otherwise null
     */
    public static byte[] decrypt(byte[] cipherText, byte[] iv, SecretKey key) throws InvalidPasswordException {
        Cipher c;
        try {
            // Generate a Cipher object that uses the predefined algorithm mode
            c = Cipher.getInstance(ALGORITHM_MODE_PADDING);
            // Initialize the Cipher using the generated key and the initialization vector
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            // Attempt to decrypt the data (An exception is thrown here if the password was incorrect)
            return c.doFinal(cipherText);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new InvalidPasswordException("Error: wrong password given");
        }
    }

    /**
     * Attempts to encrypt plaintext data using a given key
     * @param plaintext The data to encrypt
     * @param key The keyto encrypt with
     * @return The resulting encrypted data, otherwise null. Resulting size will be plaintext.length + IV length.
     */
    public static byte[] encrypt(byte[] plaintext, SecretKey key, byte[] salt) {
        /* Generate an initialization vector for encryption
         * The IV is used in combination with the key to encrypt data and is used during decryption
         * The IV is made up of AES_KEY_LENGTH_BYTES randomized bytes of data */
        byte[] iv = randomBytes(IV_LENGTH);

        // Create a cipher using the AES algorithm in CBC mode with PKCS5 padding
        Cipher c;
        try {
            c = Cipher.getInstance(ALGORITHM_MODE_PADDING);

            // Initialize the cipher for encryption using the key and IV
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            //Save all of the password info as a string of characters separated by string terminating characters
            // Encrypt the data
            byte[] cipherText = c.doFinal(plaintext);

            // Place all of the required information for later decryption (aside from the key) in a byte array
            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
            byteBuffer.put(salt);
            wipeArray(salt);
            byteBuffer.put(iv);
            wipeArray(iv);
            byteBuffer.put(cipherText);
            wipeArray(cipherText);

            // Return the result
            return byteBuffer.array();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Attempts to destroy the underlying key data for the given {@link SecretKey} object.
     * @param key The key to destroy
     * @return true if successful, otherwise false
     */
    public static boolean destroyKey(SecretKey key) {
        try {
            key.destroy();
            return true;
        } catch (DestroyFailedException e) {
            return false;
        }
    }

    /**
     * Securely(?) converts an array of char to an array of byte
     * @param c The array to convert
     * @return The resulting byte array
     */
    public static byte[] charsToBytes(char [] c) {
        ByteBuffer bb = ENCODER.encode(CharBuffer.wrap(c));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        wipeArray(bb.array());
        return b;
    }

    /**
     * Encodes a String using the encoding specification specified by ENCODING
     * @param s The String to encode
     * @return A byte array containing the encoded String
     */
    public static byte[] encode(String s) {
        return s.getBytes(ENCODER);
    }

    /**
     * Overwrites all values in the given array with zeroes
     * @param array The array to overwrite
     */
    public static void wipeArray(byte[] array) {
        Arrays.fill(array, (byte)0);
    }

    /**
     * Overwrites all values in the given array with zeroes
     * @param array The array to overwrite
     */
    public static void wipeArray(char[] array) {
        Arrays.fill(array, (char)0);
    }
}
