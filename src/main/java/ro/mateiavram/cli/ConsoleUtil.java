package ro.mateiavram.cli;

import java.io.Console;
import java.util.Scanner;

public class ConsoleUtil {

    private static final Scanner SCANNER = new Scanner(System.in);

     protected static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(prompt);
        }

        System.out.print(prompt);
        return SCANNER.nextLine().toCharArray();
    }
}
