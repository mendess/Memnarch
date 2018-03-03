package com.github.mendess2526.memnarch;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;

import java.util.List;
import java.util.Set;

public interface Command {

    /**
     * Runs the command
     * @param event The event that triggered the command
     * @param args The command args
     */
    void runCommand(MessageReceivedEvent event, List<String> args);

    /**
     * Returns the command group name
     * @return The command group name
     */
    String getCommandGroup();

    /**
     * Returns the set of permissions required to use this command
     * @return The set of permissions required to use this command
     */
    Set<Permissions> getPermissions();
}
