package ro.mateiavram.pm;

import org.junit.jupiter.api.Test;
import ro.mateiavram.crypto.CryptoService;
import ro.mateiavram.crypto.EncryptedData;
import static org.junit.jupiter.api.Assertions.*;
import javax.crypto.AEADBadTagException;
import java.util.Arrays;


public class OpenCloseTests {

    private static final CryptoService cryptoService=new CryptoService();
    private static final String CORRECT_PASSWORD="PASSWORD";
    private static final String WRONG_PASSWORD="INCORRECT PASSWORD";
    private static final String DATA="gmail:parola1234";
    private static final String AAD="header";

    @Test
    void correctKeyOpenSeif() throws AEADBadTagException {
        byte[] salt=cryptoService.randomBytes(16);
        byte[] kek=cryptoService.deriveKek(CORRECT_PASSWORD.toCharArray(),salt);

        byte[] dek=cryptoService.randomBytes(32);

        EncryptedData wrappedDek=cryptoService.encrypt(kek,dek,AAD.getBytes());

        EncryptedData encryptedEntries=cryptoService.encrypt(dek,DATA.getBytes(), AAD.getBytes());

        //new open try
        byte[] kek2=cryptoService.deriveKek(CORRECT_PASSWORD.toCharArray(),salt);
        byte[] dek2=cryptoService.decrypt(kek2,wrappedDek,AAD.getBytes());

        byte[] decrypted=cryptoService.decrypt(dek2,encryptedEntries,AAD.getBytes());

        assertTrue(Arrays.equals(DATA.getBytes(),decrypted));
    }

    @Test
    void incorrectPasswordClosedSeif() throws AEADBadTagException {
       byte[] salt=cryptoService.randomBytes(16);
       byte[] kek=cryptoService.deriveKek(CORRECT_PASSWORD.toCharArray(),salt);
       byte[] dek=cryptoService.randomBytes(32);

       EncryptedData wrappedDek=cryptoService.encrypt(kek,dek,AAD.getBytes());

       EncryptedData encryptedData=cryptoService.encrypt(dek,DATA.getBytes(),AAD.getBytes());

       //new open try
        byte[] kek2=cryptoService.deriveKek(WRONG_PASSWORD.toCharArray(),salt);

        assertThrows(AEADBadTagException.class,()->cryptoService.decrypt(kek2,wrappedDek,AAD.getBytes()));


    }
}
