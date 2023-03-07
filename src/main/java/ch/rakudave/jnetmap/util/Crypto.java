package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author rakudave
 */
public class Crypto {

    protected static byte[] convert(String password) throws Exception { // changed from private for testing
        byte[] keyB = new byte[24];
        if (validate(password)) {
            for (int i = 0; i < password.length() && i < keyB.length; i++)
                keyB[i] = (byte) password.charAt(i);
            return keyB;
        } else {
            return null;
        }
    }


    public static String decrypt(String encryptedBase64, String password) {
        try {
            Cipher cipher = Cipher.getInstance("DESede");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(convert(password), "DESede"));
            return new String(cipher.doFinal(Base64.decode(encryptedBase64)));
        } catch (Exception e) {
            Logger.error("Failed to decrypt string", e);
        }
        return null;
    }

    public static String encrypt(String plainText, String password) {
        try {
            Cipher cipher = Cipher.getInstance("DESede");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(convert(password), "DESede"));
            return Base64.encodeBytes(cipher.doFinal(plainText.getBytes()));
        } catch (Exception e) {
            Logger.error("Failed to encrypt string", e);
        }
        return null;
    }

    public static boolean validate(String password) throws Exception {
        if (password.length() <= 24)
            return true;

        else throw new Exception("Password to long");

    }
}
