package cryptography;

/**
 * An exception thrown by {@link Crypto} when an invalid password is given during decryption
 */
public class InvalidPasswordException extends Exception {

    public InvalidPasswordException(String desc) {
        super(desc);
    }

}
