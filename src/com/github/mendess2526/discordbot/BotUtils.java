package com.github.mendess2526.discordbot;

import org.apache.commons.lang3.text.WordUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class BotUtils {
    private static String ERROR_SMTH_WRONG = "Something went wrong, contact the owner of the bot";

    public static void autoDelete(IMessage msg, IDiscordClient client, int delay) {
        Thread t = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(delay);
                client.getDispatcher().waitFor(MessageReceivedEvent.class,120,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LoggerService.log(msg.getGuild(),"Interrupted while waiting for a Message received event on sfxadd",LoggerService.ERROR);
            }
            msg.delete();
        });
        t.start();
    }
    public static void contactOwner(Event event, String msg){
        if(event instanceof MessageReceivedEvent){
            MessageReceivedEvent e = (MessageReceivedEvent) event;
            String guild = e.getGuild().getName();
            String channel = e.getChannel().getName();
            RequestBuffer.request(() -> e.getChannel().sendMessage(ERROR_SMTH_WRONG));
            RequestBuffer.request(() -> event.getClient().getApplicationOwner().getOrCreatePMChannel().
                    sendMessage(msg+"\n```" +
                                "[Guild  : "+guild+"]\n" +
                                "[Channel: "+channel+"]```"));
        }
    }

    public static IMessage sendMessage(IChannel channel, String message, int autoDeleteDelay, boolean reactCross){
        IMessage msg = RequestBuffer.request(() -> {return channel.sendMessage(message);}).get();
        if(reactCross){closeButton(msg);}
        if(autoDeleteDelay != -1){autoDelete(msg,channel.getClient(),autoDeleteDelay);}
        return msg;
    }

    public static IMessage sendMessage(IChannel channel, EmbedObject eb, int autoDeleteDelay, boolean reactCross){
        IMessage msg = RequestBuffer.request(() -> {return channel.sendMessage(eb);}).get();
        if(reactCross){closeButton(msg);}
        if(autoDeleteDelay != -1){autoDelete(msg,channel.getClient(),autoDeleteDelay);}
        return msg;
    }
    public static IMessage sendMessage(IChannel channel, String message, EmbedObject eb, int autoDeleteDelay, boolean reactCross){
        IMessage msg = RequestBuffer.request(() -> {return channel.sendMessage(message,eb);}).get();
        if(reactCross){closeButton(msg);}
        if(autoDeleteDelay != -1){autoDelete(msg,channel.getClient(),autoDeleteDelay);}
        return msg;
    }

    public static void sendFile(IChannel ch, File file) {
        RequestBuffer.request(() -> {
            try {
                ch.sendFile(file);
            } catch (FileNotFoundException e) {
                LoggerService.log(ch.getGuild(),"File not found when sending to channel",LoggerService.ERROR);
                e.printStackTrace();
            }
        }).get();
    }

    public static void closeButton(IMessage msg){
        RequestBuffer.request(() -> msg.addReaction(":heavy_multiplication_x:")).get();
    }
    public static void waitForReaction(IMessage msg, String reaction){
        if(msg.getReactionByUnicode(reaction)==null){
            try {
                msg.getClient().getDispatcher().waitFor((ReactionEvent e) -> msg.getReactionByUnicode(reaction)==null,2,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LoggerService.log(msg.getGuild(),"Interrupted while waiting for close button",LoggerService.ERROR);
                e.printStackTrace();
            }
        }
    }
    public static void help(IUser user, IChannel channel, Map<String,Set<String>> commands) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("List of available commands for:");
        commands.keySet().forEach(k -> {
            StringBuilder s = new StringBuilder();
            commands.get(k).forEach(nestedK -> s.append(nestedK.toLowerCase()).append("\n"));
            eb.appendField(WordUtils.capitalizeFully(k),s.toString(), false);
        });
        eb.withFooterText("Prefix: "+Events.BOT_PREFIX);
        sendMessage(channel,user.mention(),eb.build(),-1,true);
    }
    public static boolean hasPermission(MessageReceivedEvent event, Set<Permissions> permissions){
        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).containsAll(permissions)){
            sendMessage(event.getChannel(), "You don't have permission to use that command", 120, false);
            return false;
        }else {
            return true;
        }
    }
}
