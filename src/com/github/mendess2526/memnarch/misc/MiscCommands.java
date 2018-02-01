package com.github.mendess2526.memnarch.misc;

import com.github.mendess2526.memnarch.BotUtils;
import com.github.mendess2526.memnarch.Command;
import com.github.mendess2526.memnarch.Events;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.github.mendess2526.memnarch.BotUtils.hasPermission;
import static com.github.mendess2526.memnarch.BotUtils.sendMessage;

public class MiscCommands{

    public static void help(MessageReceivedEvent event) {
        HashMap<String,Set<String>> cmds = new HashMap<>();
        for(Map.Entry<String,Command> k : Events.commandMap.entrySet()){
            String commandGroup = k.getValue().getCommandGroup();
            Set<String> t;
            if(hasPermission(event,k.getValue().getPermissions(), false)){
                if(cmds.containsKey(commandGroup)){
                    t = cmds.get(commandGroup);
                }else{
                    t = new HashSet<>();
                }
                t.add(k.getKey());
                cmds.put(commandGroup,t);
            }
        }
        BotUtils.help(event.getAuthor(),event.getChannel(),cmds);
    }

    public static void ping(MessageReceivedEvent event){
        LocalDateTime askTime = event.getMessage().getTimestamp();
        LocalDateTime respondTime = LocalDateTime.now();
        sendMessage(event.getChannel(),new EmbedBuilder().withTitle("Pong! "+(askTime.until(respondTime, ChronoUnit.MILLIS))+" ms").build(),120,true);
    }

    public static void hi(MessageReceivedEvent event) {
        sendMessage(event.getChannel(),"Hello, minion!",-1,false);
    }

    public static void shutdown(MessageReceivedEvent event) {
        if(!event.getAuthor().equals(event.getClient().getApplicationOwner())){
            sendMessage(event.getChannel(),"Only the owner of the bot can use that command",120,false);
            return;
        }
        sendMessage(event.getChannel(),"Shutting down...",-1,false);
        IDiscordClient client = event.getClient();
        client.logout();
    }

    public static void whoAreYou(MessageReceivedEvent event) {
        sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle("I AM MEMNARCH")
                                  .withImage("http://magiccards.info/scans/en/arc/112.jpg")
                                  .withDesc("Sauce code: [GitHub](https://github.com/Mendess2526/Memnarch)")
                                  .build(),-1,true);
    }
}
