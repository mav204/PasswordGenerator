package ro.mateiavram.vault;

import java.util.Objects;

public class Entry {
    private String service;
    private String username;
    private String password;
    private String notes;

    public Entry(String service, String username, String password, String notes) {
        this.service = service;
        this.username = username;
        this.password = password;
        this.notes = notes;
    }

    public Entry(){

    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("service='").append(service).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return Objects.equals(getService(), entry.getService()) &&
                Objects.equals(getUsername(), entry.getUsername()) &&
                Objects.equals(getPassword(), entry.getPassword()) &&
                Objects.equals(getNotes(), entry.getNotes());

    }

    @Override
    public int hashCode() {
        return Objects.hash(getService(), getUsername(), getPassword(), getNotes());
    }
}
