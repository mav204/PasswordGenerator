package ro.mateiavram.pm;

import ro.mateiavram.crypto.PasswordGenerator;
import ro.mateiavram.vault.Entry;
import ro.mateiavram.vault.Vault;

import javax.crypto.AEADBadTagException;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// Meniu interactiv pentru toate functionalitatile Vault.
public class Main {

    // Pe macOS, AWT (folosit de clipboard) ar afisa o iconita Java in Dock.
    // Trebuie setat inainte sa fie creat ClipboardHelper (campurile statice de mai jos).
    static {
        System.setProperty("apple.awt.UIElement", "true");
    }

    private static final Path PATH = Path.of("test-vault.json");
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final PasswordGenerator GENERATOR = new PasswordGenerator();
    private static final int GENERATED_LENGTH = 16;
    private static final ClipboardHelper CLIPBOARD = new ClipboardHelper();
    private static final int CLIPBOARD_SECONDS = 20;

    public static void main(String[] args) {
        Vault vault = null;

        while (true) {
            printMenu(vault != null);
            String choice = SCANNER.nextLine().trim();

            try {
                if (vault == null) {
                    switch (choice) {
                        case "1" -> vault = createVault();
                        case "2" -> vault = openVault();
                        case "0" -> {
                            CLIPBOARD.clearNow();
                            System.out.println("La revedere!");
                            return;
                        }
                        default -> System.out.println("Optiune invalida.");
                    }
                } else {
                    switch (choice) {
                        case "1" -> addEntry(vault);
                        case "2" -> listEntries(vault);
                        case "3" -> getEntry(vault);
                        case "4" -> deleteEntry(vault);
                        case "6" -> changePassword(vault);
                        case "5" -> {
                            vault.close();
                            vault = null;
                            System.out.println("Seif inchis. DEK-ul a fost sters din memorie.");
                        }
                        case "0" -> {
                            vault.close();
                            CLIPBOARD.clearNow();
                            System.out.println("Seif inchis. La revedere!");
                            return;
                        }
                        default -> System.out.println("Optiune invalida.");
                    }
                }
            } catch (AEADBadTagException e) {
                System.out.println("Parola gresita sau seif modificat.");
            } catch (IOException e) {
                System.out.println("Eroare la fisier: " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("Eroare: " + e.getMessage());
            }
        }
    }

    private static void printMenu(boolean isOpen) {
        System.out.println();
        if (!isOpen) {
            System.out.println("=== Seif inchis (" + PATH.toAbsolutePath() + ") ===");
            System.out.println("1. Creeaza seif nou");
            System.out.println("2. Deschide seif");
            System.out.println("0. Iesire");
        } else {
            System.out.println("=== Seif deschis ===");
            System.out.println("1. Adauga intrare");
            System.out.println("2. Listeaza intrari");
            System.out.println("3. Cauta dupa serviciu");
            System.out.println("4. Sterge intrare");
            System.out.println("5. Inchide seiful");
            System.out.println("6. Schimba parola master");
            System.out.println("0. Inchide si iesi");
        }
        System.out.print("> ");
    }

    private static Vault createVault() throws IOException {
        if (Files.exists(PATH)) {
            System.out.print("Exista deja un seif. Il suprascrii? Toate intrarile se pierd (da/nu): ");
            if (!SCANNER.nextLine().trim().equalsIgnoreCase("da")) {
                System.out.println("Anulat.");
                return null;
            }
        }
        char[] password = readPassword("Parola master noua: ");
        try {
            Vault vault = Vault.create(PATH, password);
            System.out.println("Seif creat.");
            return vault;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static Vault openVault() throws IOException, AEADBadTagException {
        char[] password = readPassword("Parola master: ");
        try {
            Vault vault = Vault.open(PATH, password);
            System.out.println("Seif deschis.");
            return vault;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void addEntry(Vault vault) throws IOException {
        String service = ask("Serviciu: ");
        String username = ask("Utilizator: ");
        String password = ask("Parola (Enter = genereaza): ");

        if (password.isEmpty()) {
            password = GENERATOR.generate(GENERATED_LENGTH, true, true, true);
            double bits = GENERATOR.entropyBits(GENERATED_LENGTH, true, true, true);
            // parola nu se afiseaza: merge direct in clipboard, ca sa poata fi lipita pe site
            CLIPBOARD.copyAndClearLater(password, CLIPBOARD_SECONDS);
            System.out.printf("Parola generata (%d caractere, entropie: %.1f biti).%n", GENERATED_LENGTH, bits);
            System.out.println("A fost copiata in clipboard. Se sterge in " + CLIPBOARD_SECONDS + " de secunde.");
        }

        String notes = ask("Notite (optional): ");
        vault.add(new Entry(service, username, password, notes));
        System.out.println("Adaugat.");
    }

    private static void listEntries(Vault vault) {
        List<Entry> entries = vault.list();
        if (entries.isEmpty()) {
            System.out.println("Seiful e gol.");
            return;
        }
        System.out.println(entries.size() + " intrari:");
        for (Entry entry : entries) {
            // doar serviciu + utilizator: parola si notitele nu se afiseaza in lista
            System.out.println("  " + entry.getService() + "  -  " + entry.getUsername());
        }
    }

    private static void getEntry(Vault vault) {
        String service = ask("Serviciu: ");
        List<Entry> found = vault.get(service);
        if (found.isEmpty()) {
            System.out.println("Nu exista nicio intrare pentru " + service + ".");
            return;
        }
        Entry entry = found.get(0);
        if (found.size() > 1) {
            // mai multe conturi pentru acelasi serviciu: clipboard-ul tine un singur text, deci alegi unul
            for (int i = 0; i < found.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + found.get(i).getUsername());
            }
            String choice = ask("Care cont? (1-" + found.size() + "): ").trim();
            try {
                entry = found.get(Integer.parseInt(choice) - 1);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println("Optiune invalida.");
                return;
            }
        }

        System.out.println("  Serviciu:   " + entry.getService());
        System.out.println("  Utilizator: " + entry.getUsername());
        System.out.println("  Notite:     " + entry.getNotes());
        // parola nu apare pe ecran: merge in clipboard si se sterge automat
        CLIPBOARD.copyAndClearLater(entry.getPassword(), CLIPBOARD_SECONDS);
        System.out.println("  Parola a fost copiata in clipboard. Se sterge in " + CLIPBOARD_SECONDS + " de secunde.");
    }

    private static void deleteEntry(Vault vault) throws IOException {
        String service = ask("Serviciu: ");
        String username = ask("Utilizator: ");
        boolean deleted = vault.delete(new Entry(service, username, null, null));
        System.out.println(deleted ? "Sters." : "Nu exista nicio intrare " + service + " / " + username + ".");
    }

    private static void changePassword(Vault vault) throws IOException, AEADBadTagException {
        char[] oldPassword = readPassword("Parola master actuala: ");
        char[] newPassword = null;
        char[] confirm = null;
        try {
            if (!vault.verifyPassword(oldPassword)) {
                System.out.println("Parola actuala e gresita. Nimic nu a fost schimbat.");
                return;
            }
            newPassword = readPassword("Parola master noua: ");
            confirm = readPassword("Confirma parola noua: ");
            if (!Arrays.equals(newPassword, confirm)) {
                System.out.println("Parolele noi nu coincid. Nimic nu a fost schimbat.");
                return;
            }
            vault.changePassword(oldPassword, newPassword);
            System.out.println("Parola master a fost schimbata.");
        } finally {
            Arrays.fill(oldPassword, '\0');
            if (newPassword != null) Arrays.fill(newPassword, '\0');
            if (confirm != null) Arrays.fill(confirm, '\0');
        }
    }

    private static String ask(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine();
    }


    private static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(prompt);
        }

        System.out.print(prompt);
        return SCANNER.nextLine().toCharArray();
    }
}
