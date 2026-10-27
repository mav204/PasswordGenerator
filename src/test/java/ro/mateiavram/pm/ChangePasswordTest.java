package ro.mateiavram.pm;

import org.junit.jupiter.api.Test;
import ro.mateiavram.crypto.CryptoService;
import ro.mateiavram.crypto.EncryptedData;

import javax.crypto.AEADBadTagException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ChangePasswordTest {
    private static final CryptoService cryptoService=new CryptoService();
    private static final String OLD_PASSWORD ="PASSWORD";
    private static final String NEW_PASSWORD="NEW PASSWORD";
    private static final String DATA="gmail:parola1234";
    private static final String AAD="header";

    @Test
    void passwordChange() throws AEADBadTagException {
        byte[] salt=cryptoService.randomBytes(16);
        byte[] oldKek=cryptoService.deriveKek(OLD_PASSWORD.toCharArray(),salt);
        byte[] oldDek=cryptoService.randomBytes(32);

        EncryptedData wrappedDek=cryptoService.encrypt(oldKek,oldDek,AAD.getBytes());
        EncryptedData encryptedData=cryptoService.encrypt(oldDek,DATA.getBytes(),AAD.getBytes());


        byte[] salt2=cryptoService.randomBytes(32);
        byte[] newKek=cryptoService.deriveKek(NEW_PASSWORD.toCharArray(),salt2);

        EncryptedData newWrappedDek=cryptoService.encrypt(newKek,oldDek,AAD.getBytes());

        byte[] decryptedDek=cryptoService.decrypt(newKek,newWrappedDek,AAD.getBytes());
        byte[] decryptedData=cryptoService.decrypt(decryptedDek,encryptedData,AAD.getBytes());

        assertTrue(Arrays.equals(decryptedData,DATA.getBytes()));

    }



}
