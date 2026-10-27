package ro.mateiavram.vault;


public class VaultHeader {
    private int version;
    private String kdf;
    private int iterations;
    private byte[] salt;


    public VaultHeader(int version, String kdf, int iterations, byte[] salt) {
        this.version = version;
        this.kdf = kdf;
        this.iterations = iterations;
        this.salt = salt;
    }

    public VaultHeader(){

    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getKdf() {
        return kdf;
    }

    public void setKdf(String kdf) {
        this.kdf = kdf;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public byte[] getSalt() {
        if(salt!=null)
        return salt.clone();
        return null;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt.clone();
    }
}
