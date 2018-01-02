package com.github.mendess2526.memnarch;

import com.sun.istack.internal.NotNull;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;

import java.util.List;
import java.util.Set;

public interface Command {
    void runCommand(MessageReceivedEvent event, List<String> args);
    String getCommandGroup();
    @NotNull
    Set<Permissions> getPermissions();
}
