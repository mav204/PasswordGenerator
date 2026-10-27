package ro.mateiavram.cli;

import picocli.CommandLine.*;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(
        name = "pm",
        description ="Password manager",
        mixinStandardHelpOptions = true,
        subcommands = {InitCommand.class}
)
public class PmCommand implements Runnable{

    @Override
    public void run() {
        System.out.println("Foloseste: pm <comanda>. Vezi: pm --help");
    }

    public static Path vaultPath() throws URISyntaxException {
        Path location=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if(!Files.isDirectory(location)){
            return location.getParent().resolve("vault.json");
        }
        else{
            return Path.of("vault.json");
        }
    }
}
