package cryptography;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.util.Arrays;

/**
 *  A destroyable implementation of {@link SecretKey}, with keys based on byte array data. The underlying byte array is
 *  not copied when used by {@link javax.crypto.Cipher}, unlike Java's implementation of
 *  {@link javax.crypto.spec.SecretKeySpec}. In addition, the underlying byte array can be overwritten using the
 *  {@link DestroyableKey#destroy()} method.
 */
public class DestroyableKey implements SecretKey {

    // Constants for the algorithm and format of DestroyableKeys
    private static final String ALGORITHM = "AES";
    private static final String FORMAT = "RAW";

    // Flag set when destroy() is called
    private boolean destroyed;
    // The underlying key data. Call destroy() to overwrite the data
    private byte[] keyData;

    /**
     * Creates a new {@link DestroyableKey} with AES algorithm and RAW format.
     * @param keyData An array of bytes to derive the key from. A copy of the array is created for the object's
     *                underlying byte array. Use {@link DestroyableKey#destroy()} to erase the key from memory for
     *                security purposes.
     */
    public DestroyableKey(byte[] keyData) {
        this.keyData = Arrays.copyOf(keyData, keyData.length);
        destroyed = false;
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public byte[] getEncoded() {
        return keyData;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        if(destroyed)
            throw new IllegalStateException("Key has already been destroyed");
        destroyed = true;
        Crypto.wipeArray(keyData);
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
}
