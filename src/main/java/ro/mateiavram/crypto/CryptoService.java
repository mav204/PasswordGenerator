package ro.mateiavram.crypto;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;


public class CryptoService {
    private final SecureRandom random = new SecureRandom();
    private static final int KEY_LENGTH_BITS = 256;
    private static final int NONCE_LENGTH_BYTES =12;
    private static final int TAG_LENGTH_BITS =128;
    public static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 600_000;




    public byte[] randomBytes(int length) {

        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public byte[] deriveKek(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            spec.clearPassword();
        }
    }

    public EncryptedData encrypt(byte[] key,byte[] data,byte[] aad){

        byte[] nonce=this.randomBytes(NONCE_LENGTH_BYTES);
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec=new SecretKeySpec(key,"AES");
            GCMParameterSpec gcmParameterSpec=new GCMParameterSpec(TAG_LENGTH_BITS,nonce);
            cipher.init(Cipher.ENCRYPT_MODE,keySpec,gcmParameterSpec);
            cipher.updateAAD(aad);
            byte[] cipherText=cipher.doFinal(data);
            return new EncryptedData(nonce,cipherText);


        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }


    }

    public byte[] decrypt (byte[] key,EncryptedData encryptedData,byte[] aad) throws AEADBadTagException {
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec=new SecretKeySpec(key,"AES");
            GCMParameterSpec gcmParameterSpec=new GCMParameterSpec(TAG_LENGTH_BITS, encryptedData.nonce());
            cipher.init(Cipher.DECRYPT_MODE,keySpec,gcmParameterSpec);
            cipher.updateAAD(aad);
            return cipher.doFinal(encryptedData.cipherText());

        }
        catch (AEADBadTagException e){
            throw e;
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }




}
