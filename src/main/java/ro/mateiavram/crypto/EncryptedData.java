package ro.mateiavram.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public record EncryptedData(@JsonProperty("nonce") byte[] nonce, @JsonProperty("cipherText") byte[] cipherText) {

    @JsonCreator
    public EncryptedData(byte[] nonce, byte[] cipherText) {
        this.nonce = Arrays.copyOf(nonce, nonce.length);
        this.cipherText = Arrays.copyOf(cipherText, cipherText.length);
    }

    @Override
    public byte[] cipherText() {
        return cipherText.clone();
    }

    @Override
    public byte[] nonce() {
        return nonce.clone();
    }


}
