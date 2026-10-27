package ro.mateiavram.pm;

import org.junit.jupiter.api.Test;
import ro.mateiavram.crypto.CryptoService;
import ro.mateiavram.crypto.EncryptedData;

import javax.crypto.AEADBadTagException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoServiceTest {

    private final CryptoService cryptoService=new CryptoService();
    private final String message="DATA";

    @Test
    void twoRandomBytesArraysAreDifferent(){
        byte[] byte1=cryptoService.randomBytes(16);
        byte[] byte2=cryptoService.randomBytes(16);
        assertFalse(Arrays.equals(byte1,byte2));
    }

    @Test
    void randomArrayHasReqLength(){
        assertEquals(16, cryptoService.randomBytes(16).length);
    }

    @Test
    void kekHasReqLength(){
        assertEquals(32,cryptoService.deriveKek("password".toCharArray(),cryptoService.randomBytes(16)).length);
    }

    @Test
    void kekDeterminism(){
        byte[] bytes=cryptoService.randomBytes(16);
        byte[] kek=cryptoService.deriveKek("password".toCharArray(),bytes);
        byte[] kek2=cryptoService.deriveKek("password".toCharArray(),bytes);
        assertTrue(Arrays.equals(kek,kek2));
    }

    @Test
    void differentSaltsDifferentKek(){
        byte[] salt1=cryptoService.randomBytes(16);
        byte[] salt2=cryptoService.randomBytes(16);

        byte[] kek1=cryptoService.deriveKek("pass1".toCharArray(),salt1);
        byte[] kek2=cryptoService.deriveKek("pass1".toCharArray(),salt2);

        assertFalse(Arrays.equals(kek1,kek2));
    }

    @Test
    void cipherNonceLength(){
        byte[] kek=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();
        EncryptedData encryptedData=cryptoService.encrypt(kek,data,aad);
        assertEquals(12,encryptedData.nonce().length);

    }

    @Test
    void cipherCipherTextLength(){
        byte[] kek=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();
        EncryptedData encryptedData=cryptoService.encrypt(kek,data,aad);
        assertEquals(data.length+16,encryptedData.cipherText().length);
    }

    @Test
    void differentNonceOnSameEncryption(){
        byte[] kek=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();
        EncryptedData e1=cryptoService.encrypt(kek,data,aad);
        EncryptedData e2=cryptoService.encrypt(kek,data,aad);

        assertFalse(Arrays.equals(e1.nonce(),e2.nonce()));
    }

    @Test
    void differentCipherTextOnSameEncryption(){
        byte[] kek=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();
        EncryptedData e1=cryptoService.encrypt(kek,data,aad);
        EncryptedData e2=cryptoService.encrypt(kek,data,aad);

        assertFalse(Arrays.equals(e1.cipherText(),e2.cipherText()));
    }

    @Test
    void encryptTest() throws AEADBadTagException {
        byte[] kek=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();
        EncryptedData e1=cryptoService.encrypt(kek,data,aad);

        byte[] e2= cryptoService.decrypt(kek,e1,aad);

        assertTrue(Arrays.equals(data,e2));
    }

    @Test
    void decryptWithDifferentAadThrows() {
        byte[] kek=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad1="header1".getBytes();
        byte[] aad2="header2".getBytes();

        EncryptedData encryptedData=cryptoService.encrypt(kek,data,aad1);
        assertThrows(AEADBadTagException.class,()->cryptoService.decrypt(kek,encryptedData,aad2));
    }

    @Test
    void decryptWithWrongKey() {
        byte[] key=cryptoService.randomBytes(32);
        byte[] key2=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();
        EncryptedData encryptedData=cryptoService.encrypt(key,data,aad);

        assertThrows(AEADBadTagException.class,()->cryptoService.decrypt(key2,encryptedData,aad));
    }

    @Test
    void modifiedCipherText(){
        byte[] key=cryptoService.randomBytes(32);
        byte[] data=cryptoService.randomBytes(32);
        byte[] aad="header".getBytes();

        EncryptedData encryptedData=cryptoService.encrypt(key,data,aad);
        byte[] ct=encryptedData.cipherText();
        ct[0]^=1;
        EncryptedData wrongCipherText=new EncryptedData(encryptedData.nonce(),ct);
        assertThrows(AEADBadTagException.class,()->cryptoService.decrypt(key,wrongCipherText,aad));
    }

}
