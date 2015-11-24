package edu.ualberta.med.biobank.tools.cli.command;

import edu.ualberta.med.biobank.tools.cli.CliProvider;

public abstract class Command implements CommandRunner {

    protected CliProvider cliProvider;

    protected final String name;

    protected final String help;

    protected final String usage;

    public Command(CliProvider cliProvider, String name, String help, String usage) {
        this.cliProvider = cliProvider;
        this.name = name;
        this.help = help;
        this.usage = usage;
    }

    public String getName() {
        return name;
    }

    public String getHelp() {
        return help;
    }

    public String getUsage() {
        return usage;
    }

}
