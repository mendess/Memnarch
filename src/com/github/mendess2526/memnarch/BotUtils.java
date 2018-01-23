package com.github.mendess2526.memnarch;

import org.apache.commons.lang3.text.WordUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.github.mendess2526.memnarch.LoggerService.*;

@SuppressWarnings("UnusedReturnValue")
public class BotUtils {

    private static final String ERROR_SMTH_WRONG = "Something went wrong, contact the owner of the bot";
    static final String X = "\u2716";
    static final String R = "\uD83C\uDDF7";
    public static final String DEFAULT_FILE_PATH = "files/";
    public static final String USERS_PATH = DEFAULT_FILE_PATH+"users.ini";
    public static final String SERVERS_PATH = DEFAULT_FILE_PATH+"servers.ini";

    private static void autoDelete(IMessage msg, IDiscordClient client, int delay) {
        Thread t = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(delay);
                client.getDispatcher().waitFor(MessageReceivedEvent.class,120,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log(msg.getGuild(),"Interrupted while waiting to autoDelete Message", ERROR);
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
    @SuppressWarnings("SameParameterValue")
    public static IMessage sendMessage(IChannel channel, String message, int autoDeleteDelay, boolean reactCross){
        IMessage msg = RequestBuffer.request(() -> {return channel.sendMessage(message);}).get();
        if(reactCross){closeButton(msg);}
        if(autoDeleteDelay != -1){autoDelete(msg,channel.getClient(),autoDeleteDelay);}
        return msg;
    }
    @SuppressWarnings("SameParameterValue")
    public static IMessage sendMessage(IChannel channel, EmbedObject eb, int autoDeleteDelay, boolean reactCross){
        IMessage msg = RequestBuffer.request(() -> {return channel.sendMessage(eb);}).get();
        if(reactCross){closeButton(msg);}
        if(autoDeleteDelay != -1){autoDelete(msg,channel.getClient(),autoDeleteDelay);}
        return msg;
    }

    public static IMessage sendMessage(IChannel channel, String message, EmbedObject eb, int autoDeleteDelay,
                                boolean reactCross){
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
                log(ch.getGuild(),"File not found when sending to channel", ERROR);
                e.printStackTrace();
            }
        }).get();
    }

    public static void closeButton(IMessage msg){
        RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(X))).get();
    }
    @SuppressWarnings("unused")
    public static void waitForReaction(IMessage msg, String reaction){
        if(msg.getReactionByUnicode(reaction)==null){
            try {
                msg.getClient().getDispatcher().waitFor((ReactionEvent e) -> msg.getReactionByUnicode(reaction)==null,2,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log(msg.getGuild(),"Interrupted while waiting for close button", ERROR);
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
        if(!event.getAuthor().equals(event.getClient().getApplicationOwner())
            && !event.getAuthor().getPermissionsForGuild(event.getGuild()).containsAll(permissions)){
            sendMessage(event.getChannel(), "You don't have permission to use that command", 120, false);
            return false;
        }else {
            return true;
        }
    }
    @SuppressWarnings("SameParameterValue")
    public static void downloadFile(MessageReceivedEvent event, IMessage.Attachment attach, String folderName){
        String[] name = attach.getFilename().split(Pattern.quote("."));
        String filename = attach.getFilename().replaceAll(Pattern.quote("_")," ");
        if(!name[name.length-1].equals("mp3")){
            sendMessage(event.getChannel(),"You can only add `.mp3` files",120,false);
        }else if(attach.getFilesize()>204800) {
            sendMessage(event.getChannel(), "File too big, please keep it under 200kb", 120, false);
        }else if(new File(folderName+filename).exists()){
            sendMessage(event.getChannel(),"File with that name already exists",120,false);
        }else {
            if (!mkFolder(event,folderName)) return;
            log(event.getGuild(),"FilePath: "+folderName+filename, INFO);
            URL url;
            ReadableByteChannel rbc;
            FileOutputStream fos;
            try {
                url = new URL(attach.getUrl());
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.addRequestProperty("User-Agent", "Mozilla/4.0");
                rbc = Channels.newChannel(httpCon.getInputStream());
                fos = new FileOutputStream(folderName+"/"+filename);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (MalformedURLException e) {
                log(event.getGuild(),"Malformed Url: \""+attach.getUrl()+"\" File: "+filename, ERROR);
                e.printStackTrace();
                return;
            } catch (FileNotFoundException e) {
                log(event.getGuild(),"File not found: "+filename, ERROR);
                e.printStackTrace();
                return;
            } catch (IOException e) {
                log(event.getGuild(),"IOException for file: "+filename, ERROR);
                e.printStackTrace();
                return;
            }
            if(new File(folderName+"/"+filename).exists()){
                sendMessage(event.getChannel(),"File added successfully!",-1,false);
            }else{
                sendMessage(event.getChannel(),"File couldn't be added",120,false);
            }
        }
    }

    public static boolean mkFolder(Event event, String folderName){
        boolean success = true;
        if(!new File(folderName).exists()){
            success = new File(folderName).mkdirs();
            if (!success) {
                IGuild guild = null;
                if(event instanceof GuildEvent){
                    guild = ((GuildEvent) event).getGuild();
                }
                contactOwner(event,"Couldn't create "+folderName+" folder");
                log(guild,"Couldn't create "+folderName+" folder", ERROR);
            }
        }
        return success;
    }
}
