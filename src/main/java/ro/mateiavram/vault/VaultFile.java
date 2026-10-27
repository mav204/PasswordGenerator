package ro.mateiavram.vault;

import com.fasterxml.jackson.annotation.JsonCreator;
import ro.mateiavram.crypto.EncryptedData;

public class VaultFile {
    private VaultHeader header;
    private EncryptedData wrappedDek;
    private EncryptedData entries;

    @JsonCreator
    public VaultFile(){}

    public VaultFile(VaultHeader header, EncryptedData wrappedDek, EncryptedData entries) {
        this.header = header;
        this.wrappedDek = wrappedDek;
        this.entries = entries;
    }

    public VaultHeader getHeader() {
        return header;
    }

    public void setHeader(VaultHeader header) {
        this.header = header;
    }

    public EncryptedData getWrappedDek() {
        return wrappedDek;
    }

    public void setWrappedDek(EncryptedData wrappedDek) {
        this.wrappedDek = wrappedDek;
    }

    public EncryptedData getEntries() {
        return entries;
    }

    public void setEntries(EncryptedData entries) {
        this.entries = entries;
    }
}
