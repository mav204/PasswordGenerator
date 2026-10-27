package ro.mateiavram.vault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import ro.mateiavram.crypto.CryptoService;
import ro.mateiavram.crypto.EncryptedData;

import javax.crypto.AEADBadTagException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class Vault {
    private Path path;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private VaultHeader vaultHeader;
    private static final CryptoService cryptoService = new CryptoService();
    private List<Entry> entries;
    private EncryptedData wrappedDek;
    private final byte[] dek;
    private static int VERSION=1;
    private boolean closed = false;

    private Vault(Path path, VaultHeader vaultHeader, List<Entry> entries, EncryptedData wrappedDek, byte[] dek) {
        this.path = path;
        this.vaultHeader = vaultHeader;
        this.entries = entries;
        this.wrappedDek = wrappedDek;
        this.dek = dek;
    }

    public boolean verifyPassword(char[] password) {
        ensureOpen();
        byte[] kek = cryptoService.deriveKek(password, vaultHeader.getSalt());
        try {
            byte[] newDek = cryptoService.decrypt(kek, wrappedDek, objectMapper.writeValueAsBytes(vaultHeader));
            Arrays.fill(newDek, (byte) 0);
            return true;
        } catch (AEADBadTagException | JsonProcessingException e) {
            return false;
        } finally {
            Arrays.fill(kek, (byte) 0);
        }
    }

    private static void validateHeader(VaultHeader header) throws IOException {
        if (header == null) {
            throw new IOException("Header lipsa din fisier");
        }
        if (header.getVersion() != VERSION) {
            throw new IOException("Versiune de seif nesuportata: " + header.getVersion());
        }
        if (!CryptoService.KDF_ALGORITHM.equals(header.getKdf())) {
            throw new IOException("Algoritm KDF necunoscut: " + header.getKdf());
        }
        if (header.getIterations() != CryptoService.ITERATIONS) {
            throw new IOException("Numar de iteratii nesuportat: " + header.getIterations());
        }
        byte[] salt = header.getSalt();
        if (salt == null) {
            throw new IOException("Salt lipsa din header");
        }
        if (salt.length != 16) {
            throw new IOException("Lungime salt invalida: " + salt.length + " bytes (asteptat 16)");
        }
    }

    public static Vault create(Path path, char[] password) throws IOException {

        byte[] salt = cryptoService.randomBytes(16);
        byte[] kek = cryptoService.deriveKek(password, salt);

        VaultHeader header = new VaultHeader();
        header.setIterations(CryptoService.ITERATIONS);
        header.setKdf(CryptoService.KDF_ALGORITHM);
        header.setSalt(salt);
        header.setVersion(VERSION);

        byte[] aad = objectMapper.writeValueAsBytes(header);

        byte[] dek = cryptoService.randomBytes(32);
        EncryptedData wrappedDek = cryptoService.encrypt(kek, dek, aad);
        Arrays.fill(kek, (byte) 0);
        List<Entry> entries = new ArrayList<>();
        Vault vault = new Vault(path, header, entries, wrappedDek, dek);
        vault.save();
        System.out.println("Vault created");
        return vault;


    }


    public void save() throws IOException {
        ensureOpen();
        byte[] aad = objectMapper.writeValueAsBytes(vaultHeader);
        byte[] jsonEntries = objectMapper.writeValueAsBytes(entries);
        EncryptedData encryptedEntries = cryptoService.encrypt(dek, jsonEntries, aad);
        Arrays.fill(jsonEntries, (byte) 0);

        //vaultHeader.setVersion(++VERSION);
        VaultFile file = new VaultFile(vaultHeader, wrappedDek, encryptedEntries);
        byte[] fileBytes = objectMapper.writeValueAsBytes(file);
        Path tempFile = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(tempFile, fileBytes);
        Files.move(tempFile, this.path, ATOMIC_MOVE);
    }

    public static Vault open(@NotNull Path path, char @NotNull [] password) throws IOException, AEADBadTagException {
        VaultFile file = objectMapper.readValue(path.toFile(), VaultFile.class);
        VaultHeader vaultHeader = file.getHeader();
        validateHeader(vaultHeader);
        byte[] salt = vaultHeader.getSalt();

        byte[] kek = cryptoService.deriveKek(password, salt);
        EncryptedData wrappedDek = file.getWrappedDek();
        byte[] aad = objectMapper.writeValueAsBytes(vaultHeader);
        byte[] dek;
        try {
            dek = cryptoService.decrypt(kek, wrappedDek, aad);
        } finally {
            Arrays.fill(kek, (byte) 0);
        }
        Arrays.fill(kek, (byte) 0);
        byte[] byteEntries = cryptoService.decrypt(dek, file.getEntries(), aad);
        List<Entry> entries = new ArrayList<>(objectMapper.readValue(byteEntries, new TypeReference<List<Entry>>() {
        }));
        Arrays.fill(byteEntries, (byte) 0);
        System.out.println("Vault open");
        return new Vault(path, vaultHeader, entries, wrappedDek, dek);

    }

    public void add(Entry entry) throws IOException {
        ensureOpen();
        entries.add(entry);
        this.save();
    }

    public List<Entry> list() {
        ensureOpen();
        return List.copyOf(entries);
    }

    public List<Entry> get(String service) {
        ensureOpen();
        return entries.stream().filter(e -> service.equals(e.getService())).collect(Collectors.toList());
    }


    public boolean delete(Entry entry) throws IOException {

        ensureOpen();
        if (entries.removeIf(e -> (entry.getService().equals(e.getService()) && entry.getUsername().equals(e.getUsername())))) {
            this.save();
            return true;
        }
        return false;
    }

    private void ensureOpen() {
        if (closed)
            throw new IllegalStateException("Seif inchis");
    }

    public void close() {
        Arrays.fill(dek, (byte) 0);
        entries.clear();
        closed = true;
    }

    public void changePassword(char[] oldPassword, char[] newPassword) throws IOException, AEADBadTagException {
        ensureOpen();
        byte[] oldKek = cryptoService.deriveKek(oldPassword, vaultHeader.getSalt());
        byte[] copie;
        try {
            copie = cryptoService.decrypt(oldKek, wrappedDek, objectMapper.writeValueAsBytes(vaultHeader));
            Arrays.fill(copie, (byte) 0);

        } catch (AEADBadTagException e) {
            throw new AEADBadTagException("PAROLA GRESITA");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(oldKek, (byte) 0);
        }

        byte[] salt = cryptoService.randomBytes(16);
        VaultHeader newHeader = new VaultHeader(vaultHeader.getVersion(), vaultHeader.getKdf(), vaultHeader.getIterations(), salt);
        byte[] aad = objectMapper.writeValueAsBytes(newHeader);
        byte[] newKek = cryptoService.deriveKek(newPassword, salt);
        EncryptedData wrappedDekNew;
        try {
            wrappedDekNew = cryptoService.encrypt(newKek, dek, aad);

        } finally {
            Arrays.fill(newKek, (byte) 0);
        }
        vaultHeader = newHeader;
        this.wrappedDek = wrappedDekNew;
        save();

    }
}
