package ro.mateiavram.cli;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args){
        int exitCode=new CommandLine(new PmCommand()).execute(args);
        System.exit(exitCode);
    }
}
