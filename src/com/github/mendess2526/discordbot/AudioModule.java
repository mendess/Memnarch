package com.github.mendess2526.discordbot;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.audio.AudioPlayer;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class AudioModule {

    public static void sfx(MessageReceivedEvent event, List<String> args) {
        if(args.size()==0){
            IMessage msg = RequestBuffer.request(() ->{
                return event.getChannel().sendMessage(new EmbedBuilder()
                                                            .withTitle("Usage:")
                                                            .withDesc("`"+Events.BOT_PREFIX+"sfx <name>`. This name can be partial")
                                                            .build());
            }).get();
            BotUtils.autoDelete(msg,event.getClient(),6);
            return;
        }
        IVoiceChannel vChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();

        if(vChannel == null){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Please join a voice channel before using this command!"));
            return;
        }

        String searchStr = String.join(" ", args);

        AudioPlayer audioP = AudioPlayer.getAudioPlayerForGuild(event.getGuild());

        File[] songDir = songsDir(event,file -> file.getName().toUpperCase().contains(searchStr));
        if(songDir==null){return;}

        if(songDir.length == 0) {
            RequestBuffer.request(() -> event.getChannel().sendMessage("No files in the sfx folder match your query")).get();
            return;
        }

        audioP.clear();

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
        //TODO Afk timeout
        /*
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vChannel.leave();*/
    }

    public static void sfxlist(MessageReceivedEvent event, List<String> strings) {
        File[] songDir = songsDir(event,File::isFile);
        if(songDir==null){return;}

        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("List of sfx files:");
        if(songDir.length==0){
            eb.withDesc("**No files :(**");
        }else{
            List<String> sfxNames = Arrays.stream(songDir).map(File::getName).collect(Collectors.toList());
            Collections.sort(sfxNames);
            Iterator<String> it = sfxNames.iterator();
            int count=0;
            int column=0;
            while (it.hasNext()){
                StringBuilder s = new StringBuilder();
                while (it.hasNext() && count<12){
                    s.append(it.next()).append("\n");
                    count++;
                }
                eb.appendField((sfxNames.get(column*12).charAt(0)+"-"+sfxNames.get(column*12+count-1).charAt(0)).toUpperCase()
                        ,s.toString(),true);
                count=0;column++;
            }
        }

        eb.withFooterText("Use "+Events.BOT_PREFIX+"sfx <name> to play one");
        IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage(eb.build());}).get();
        RequestBuffer.request(() -> msg.addReaction(":x:")).get();
    }

    public static void sfxAdd(MessageReceivedEvent event, List<String> args) {
        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.MANAGE_CHANNELS)
            && !event.getAuthor().equals(event.getClient().getApplicationOwner())){
            RequestBuffer.request(() -> event.getChannel().sendMessage("You don't have permission to use that command"));
            return;
        }
        List<IMessage.Attachment> attachments = event.getMessage().getAttachments();
        if(attachments.size()==0){
            IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage("Please attach a file to the message");}).get();
            BotUtils.autoDelete(msg,event.getClient(),6);
            return;
        }
        IMessage.Attachment attach = attachments.get(0);
        String[] name = attach.getFilename().split(Pattern.quote("."));

        if(!name[name.length-1].equals("mp3")){
            IMessage msg = RequestBuffer.request(() ->{return event.getChannel().sendMessage("You can only add `.mp3` files");}).get();
            BotUtils.autoDelete(msg,event.getClient(),6);
            return;
        }
        if(attach.getFilesize()>200000){
            IMessage msg = RequestBuffer.request(() -> {return event.getChannel().sendMessage("File too big, please keep it under 200kb");}).get();
            LoggerService.log("File size: "+attach.getFilesize(),LoggerService.INFO);
            BotUtils.autoDelete(msg,event.getClient(),6);
            return;
        }
        if(!new File("sfx").exists()){
            if(songsDir(event,File::isFile)==null){
                return;
            }
        }
        String filename = attach.getFilename().replaceAll(Pattern.quote("_")," ");
        LoggerService.log("Filepath: sfx/"+filename,LoggerService.INFO);
        URL url;
        ReadableByteChannel rbc;
        FileOutputStream fos;
        try {
            url = new URL(attach.getUrl());
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
            rbc = Channels.newChannel(httpcon.getInputStream());
            fos = new FileOutputStream("sfx/"+filename);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (MalformedURLException e) {
            LoggerService.log("Malformed Url: \""+attach.getUrl()+"\" File: "+filename,LoggerService.ERROR);
            e.printStackTrace();
            return;
        } catch (FileNotFoundException e) {
            LoggerService.log("File not found: "+filename,LoggerService.ERROR);
            e.printStackTrace();
            return;
        } catch (IOException e) {
            LoggerService.log("IOException for file: "+filename,LoggerService.ERROR);
            e.printStackTrace();
            return;
        }
        if(new File("sfx/"+filename).exists()){
            RequestBuffer.request(() -> event.getChannel().sendMessage("File added successfully!"));
        }else{
            RequestBuffer.request(() -> event.getChannel().sendMessage("File couldn't be added"));
        }
    }

    public static void sfxDelete(MessageReceivedEvent event, List<String> args) {
        if(!event.getAuthor().equals(event.getClient().getApplicationOwner())){
            RequestBuffer.request(() -> event.getChannel().sendMessage("You can't use that command"));
            return;
        }
        File[] songDir = songsDir(event,File::isFile);
        String searchStr = String.join(" ", args);

        List<File> toDelete = Arrays.stream(songDir)
                                    .filter(s -> s.getName().toUpperCase().contains(searchStr))
                                    .collect(Collectors.toList());
        LoggerService.log("Files to delete: "+toDelete.toString(),LoggerService.INFO);
        if(toDelete.size()==0){
            RequestBuffer.request(() -> event.getChannel().sendMessage("No files in the sfx folder match your query"));
        }else if(toDelete.size()>1){
            RequestBuffer.request(() -> event.getChannel().sendMessage("More than one file fits your query, please be more specific"));
        }else{
            String name = toDelete.get(0).getName();
            BotUtils.sendFile(event.getChannel(),toDelete.get(0));
            if(toDelete.get(0).delete()){
                RequestBuffer.request(() -> event.getChannel().sendMessage("Sfx: `"+name+"` deleted!"));
                LoggerService.log("Deleted",LoggerService.SUCC);
            }else{
                RequestBuffer.request(() -> event.getChannel().sendMessage("Sfx: `"+name+"` not deleted"));
                LoggerService.log("File not deleted",LoggerService.ERROR);
            }
        }
    }
    public static void sfxRetrieve(MessageReceivedEvent event, List<String> args){
        File[] songDir = songsDir(event,File::isFile);
        String searchStr = String.join(" ", args);

        List<File> toRetrieve = Arrays.stream(songDir)
                .filter(s -> s.getName().toUpperCase().contains(searchStr))
                .collect(Collectors.toList());
        LoggerService.log("Files to retrieve: "+toRetrieve.toString(),LoggerService.INFO);
        if(toRetrieve.size()==0){
            RequestBuffer.request(() -> event.getChannel().sendMessage("No files in the sfx folder match your query"));
        }else if(toRetrieve.size()>1){
            RequestBuffer.request(() -> event.getChannel().sendMessage("More than one file fits your query, please be more specific"));
        }else{
            BotUtils.sendFile(event.getChannel(), toRetrieve.get(0));
        }
    }
    public static File[] songsDir(MessageReceivedEvent event, FileFilter filter){
        File[] songDir = new File("sfx").listFiles(filter);
        if(songDir == null){
            boolean success = (new File("sfx")).mkdirs();
            if (success) {
                songDir = new File("sfx").listFiles(filter);
            }
        }
        if(songDir == null){
            BotUtils.contactOwner(event,"Couldn't create sfx folder");
            LoggerService.log("Couldn't create sfx folder",LoggerService.ERROR);
        }
        return songDir;
    }
}


















