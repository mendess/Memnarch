package com.github.mendess2526.discordbot;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.audio.AudioPlayer;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class AudioModule {

    public static void sfx(MessageReceivedEvent event, List<String> args) {
        IVoiceChannel vChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();

        if(vChannel == null){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Please join a voice channel before using this command!"));
            return;
        }

        String searchStr = String.join(" ", args);

        AudioPlayer audioP = AudioPlayer.getAudioPlayerForGuild(event.getGuild());

        File[] songDir = new File("sfx").listFiles(file -> file.getName().toUpperCase().contains(searchStr));

        if(songDir == null){
            boolean success = (new File("sfx")).mkdirs();
            if(!success){
                RequestBuffer.request(() -> event.getChannel().sendMessage("Something went wrong, contact and admin"));
                LoggerService.log("Couldn't create sfx folder",LoggerService.ERROR);
                vChannel.leave();
                return;
            }else{
                songDir = new File("sfx").listFiles(file -> file.getName().toUpperCase().contains(searchStr));
                if(songDir==null){return;}
            }
        }
        if(songDir.length == 0) {
            RequestBuffer.request(() -> event.getChannel().sendMessage("No files in the sfx folder match your query")).get();
            return;
        }

        //audioP.clear();

        vChannel.join();
        try{
            audioP.queue(songDir[0]);
        }catch (IOException e){
            LoggerService.log(e.getMessage(),LoggerService.ERROR);
            RequestBuffer.request(() -> event.getChannel().sendMessage("There was a problem playing that sound."));
        } catch (UnsupportedAudioFileException e) {
            LoggerService.log(e.getMessage(),LoggerService.ERROR);
            RequestBuffer.request(() -> event.getChannel().sendMessage("There was a problem playing that sound."));
            e.printStackTrace();
        }
        try {
            event.getClient().getDispatcher().waitFor(TrackFinishEvent.class);
        } catch (InterruptedException e) {
            LoggerService.log("Interrupted while waiting for track to finish",LoggerService.ERROR);
            e.printStackTrace();
        }
        vChannel.leave();
    }

    public static void sfxlist(MessageReceivedEvent event, List<String> strings) {
        File[] songDir = new File("sfx").listFiles();
        if(songDir == null){
            boolean success = (new File("sfx")).mkdirs();
            if(!success){
                RequestBuffer.request(() -> event.getChannel().sendMessage("Something went wrong, contact and admin"));
                LoggerService.log("Couldn't create sfx folder",LoggerService.ERROR);
                return;
            }else{
                songDir = new File("sfx").listFiles();
            }
        }
        if(songDir==null){return;}
        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("List of sfx files:");
        StringBuilder s = new StringBuilder();
        if(songDir.length==0){
            s.append("**No files :(**");
        }else{
            Arrays.asList(songDir).forEach(f -> s.append(f.getName()).append("\n"));
        }
        eb.withDesc(s.toString());

        eb.withFooterText("Use "+Events.BOT_PREFIX+"sfx <name> to play one");
        IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage(eb.build());}).get();
        RequestBuffer.request(() -> msg.addReaction(":x:")).get();
    }

    public static void sfxAdd(MessageReceivedEvent event, List<String> args) {
        List<IMessage.Attachment> attachments = event.getMessage().getAttachments();
        if(attachments.size()==0){
            IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage("Please attach a file to the message");}).get();
            BotUtils.autoDelete(msg,event.getClient(),6);
            return;
        }
        IMessage.Attachment attach = attachments.get(0);
        String[] name = attach.getFilename().split(Pattern.quote("."));
        LoggerService.log("FileName: "+ attach.getFilename(),LoggerService.INFO);
        LoggerService.log("FileName: "+ Arrays.toString(name),LoggerService.INFO);

        if(!name[name.length-1].equals("mp3")){
            IMessage msg = RequestBuffer.request(() ->{return event.getChannel().sendMessage("You can only add `.mp3` files");}).get();
            BotUtils.autoDelete(msg,event.getClient(),6);
            return;
        }
        if(attach.getFilesize()>200){
            IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage("File too big, please keep it under 200kb");}).get();
            BotUtils.autoDelete(msg,event.getClient(),6);
        }
        if(!new File("sfx").exists()){
            boolean success = (new File("sfx")).mkdirs();
            if(!success){
                RequestBuffer.request(() -> event.getChannel().sendMessage("Something went wrong, contact and admin"));
                LoggerService.log("Couldn't create sfx folder",LoggerService.ERROR);
                return;
            }
        }
        URL url;
        ReadableByteChannel rbc;
        FileOutputStream fos;
        try {
            url = new URL(attach.getUrl());
        } catch (MalformedURLException e) {
            LoggerService.log("Malformed Url: \""+attach.getUrl()+"\" File: "+attach.getFilename(),LoggerService.ERROR);
            e.printStackTrace();
            return;
        }
        try {
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
            rbc = Channels.newChannel(httpcon.getInputStream());
        } catch (IOException e) {
            LoggerService.log("IOException initializing ReadableByteChannel",LoggerService.ERROR);
            e.printStackTrace();
            return;
        }
        try {
            fos = new FileOutputStream("sfx/"+attach.getFilename());
        } catch (FileNotFoundException e) {
            LoggerService.log("File not found: "+attach.getFilename(),LoggerService.ERROR);
            e.printStackTrace();
            return;
        }
        try {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            LoggerService.log("IOException for file: "+attach.getFilename(),LoggerService.ERROR);
            e.printStackTrace();
            return;
        }
        if(new File("sfx/"+attach.getFilename()).exists()){
            RequestBuffer.request(() -> event.getChannel().sendMessage("File added successfully!"));
        }else{
            RequestBuffer.request(() -> event.getChannel().sendMessage("File couldn't be added"));
        }
    }
}


















