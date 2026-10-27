package ro.mateiavram.cli;


import picocli.CommandLine.*;
import ro.mateiavram.vault.Vault;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(name = "init", description = "creaza un seif nou")
public class InitCommand implements Callable<Integer>{

    @Override
    public Integer call() throws Exception {
        if(Files.exists(PmCommand.vaultPath())){
            System.err.println("Exista deja un seif ");
            return 1;
        }
       char[] password=ConsoleUtil.readPassword("  PAROLA MASTER: ");
       char[] confirm =ConsoleUtil.readPassword("  CONFIRMA PAROLA: ");


        try{
            if (!Arrays.equals(password, confirm)) {
                System.err.println("Parolele noi nu coincid");
                return 1;
            }

            Vault vault = Vault.create(PmCommand.vaultPath(), password);
            System.out.println("Seif creat.");
            vault.close();
            return 0;
        }
        finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm,'\0');
        }


    }
}
