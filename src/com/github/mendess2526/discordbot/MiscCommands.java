package com.github.mendess2526.discordbot;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class MiscCommands {

    public static void help(MessageReceivedEvent event, List<String> args) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("List of available commands");
        Events.commandMap.keySet().forEach(k -> {
            StringBuilder s = new StringBuilder();
            Events.commandMap.get(k).keySet().forEach(nestedK -> s.append(nestedK.toLowerCase()).append("\n"));
            eb.appendField(k,s.toString(),true);
        });
        eb.withFooterText("Prefix: "+Events.BOT_PREFIX);
        IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage(eb.build());}).get();
        RequestBuffer.request(() -> msg.addReaction(":x:"));
    }
    public static void ping(MessageReceivedEvent event, List<String> args){
        LocalDateTime askTime = event.getMessage().getTimestamp();
        LocalDateTime respondTime = LocalDateTime.now();
        IMessage msg = RequestBuffer.request(() -> {
            return event.getChannel().sendMessage(new EmbedBuilder().withTitle("Pong "+(askTime.until(respondTime, ChronoUnit.MILLIS)-2000)+" ms").build());
        }).get();
        RequestBuffer.request(() -> msg.addReaction(":x:"));
    }

    public static void hi(MessageReceivedEvent event, List<String> args) {
        RequestBuffer.request(() -> event.getChannel().sendMessage("Hello, minion!"));
    }
}
