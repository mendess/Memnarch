package com.github.mendess2526.discordbot;

import org.apache.commons.lang3.text.WordUtils;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class SfxModule {
    public static Map<String,Command> commandMap = new HashMap<>();

    static {

        commandMap.put("<LIST", SfxModule::sfxList);

        commandMap.put("<ADD", SfxModule::sfxAdd);

        commandMap.put("<DELETE", SfxModule::sfxDelete);

        commandMap.put("<RETRIEVE", SfxModule::sfxRetrieve);
    }
    public static void sfx(MessageReceivedEvent event, List<String> args){
        if (args.size() == 0) {
            HashMap<String, Set<String>> cmds = new HashMap<>();
            Set<String> options = new HashSet<>();
            options.addAll(commandMap.keySet());
            options.add("\"name\"");
            cmds.put("Sfx", options);
            BotUtils.help(event.getAuthor(), event.getChannel(), cmds);
            return;
        }
        if(args.get(0).startsWith("<")){
            commandMap.get(args.get(0)).runCommand(event,args.subList(1,args.size()));
        }else{
            sfxPlay(event,args);
        }
    }
    public static void sfxPlay(MessageReceivedEvent event, List<String> args) {
        IVoiceChannel vChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
        if (vChannel == null) {
            BotUtils.sendMessage(event.getChannel(), "Please join a voice channel before using this command!", 120, false);
            return;
        }
        String searchStr = String.join(" ", args);
        AudioPlayer audioP = AudioPlayer.getAudioPlayerForGuild(event.getGuild());
        File[] songDir = songsDir(event, file -> file.getName().toUpperCase().contains(searchStr));
        if (songDir == null) {
            return;
        }
        if (songDir.length == 0) {
            BotUtils.sendMessage(event.getChannel(), "No files in the sfx folder match your query", 120, false);
            return;
        }
        audioP.clear();
        vChannel.join();
        LoggerService.log(event.getGuild(),"Songs that match: "+ Arrays.toString(songDir),LoggerService.INFO);
        try {
            audioP.queue(songDir[0]);
        } catch (IOException e) {
            LoggerService.log(event.getGuild(),e.getMessage(), LoggerService.ERROR);
            BotUtils.sendMessage(event.getChannel(), "There was a problem playing that sound.", 120, false);
        } catch (UnsupportedAudioFileException e) {
            LoggerService.log(event.getGuild(),e.getMessage(), LoggerService.ERROR);
            BotUtils.sendMessage(event.getChannel(), "There was a problem playing that sound.", 120, false);
            e.printStackTrace();
        }
    }
    public static void sfxList(MessageReceivedEvent event, List<String> args) {
        File[] songDir = songsDir(event,File::isFile);
        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("List of sfx files:");
        if(songDir==null || songDir.length==0){
            eb.withDesc("**No files :(**");
        }else{
            List<String> sfxNames = Arrays.stream(songDir)
                                          .map(File::getName)
                                          .map(WordUtils::capitalizeFully).collect(Collectors.toList());
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
        BotUtils.sendMessage(event.getChannel(),event.getAuthor().mention(),eb.build(),-1,true);
    }
    public static void sfxAdd(MessageReceivedEvent event, List<String> args) {
        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.MANAGE_CHANNELS)
            && !event.getAuthor().equals(event.getClient().getApplicationOwner())){
            BotUtils.sendMessage(event.getChannel(),"You don't have permission to use that command",120,false);
            return;
        }
        List<IMessage.Attachment> attachments = event.getMessage().getAttachments();
        if(attachments.size()==0){
            BotUtils.sendMessage(event.getChannel(),"Please attach a file to the message",120,false);
            return;
        }
        IMessage.Attachment attach = attachments.get(0);
        BotUtils.downloadFile(event,attach,"sfx");
    }
    public static void sfxDelete(MessageReceivedEvent event, List<String> args) {
        if(!event.getAuthor().equals(event.getClient().getApplicationOwner())){
            BotUtils.sendMessage(event.getChannel(),"Only the owner of the bot can use that command",120,false);
            return;
        }
        String searchStr = String.join(" ", args);
        File[] songDir = songsDir(event,s -> s.getName().toUpperCase().contains(searchStr));

        List<File> toDelete = Arrays.asList(songDir);
        LoggerService.log(event.getGuild(),"Files to delete: "+toDelete.toString(),LoggerService.INFO);
        if(toDelete.size()==0){
            BotUtils.sendMessage(event.getChannel(),"No files in the sfx folder match your query",120,false);
        }else if(toDelete.size()>1){
            BotUtils.sendMessage(event.getChannel(),"More than one file fits your query, please be more specific",120,false);
        }else{
            String name = toDelete.get(0).getName();
            BotUtils.sendFile(event.getChannel(),toDelete.get(0));
            if(toDelete.get(0).delete()){
                BotUtils.sendMessage(event.getChannel(),"Sfx: `"+name+"` deleted!",-1,false);
                LoggerService.log(event.getGuild(),"Deleted",LoggerService.SUCC);
            }else{
                BotUtils.sendMessage(event.getChannel(),"Sfx: `"+name+"` not deleted",120,false);
                LoggerService.log(event.getGuild(),"File not deleted",LoggerService.ERROR);
            }
        }
    }
    public static void sfxRetrieve(MessageReceivedEvent event, List<String> args){
        args.remove(0);
        String searchStr = String.join(" ", args);
        File[] songDir = songsDir(event,s -> s.getName().toUpperCase().contains(searchStr));

        List<File> toRetrieve = Arrays.asList(songDir);
        LoggerService.log(event.getGuild(),"Files to retrieve: "+toRetrieve.toString(),LoggerService.INFO);
        if(toRetrieve.size()==0){
            BotUtils.sendMessage(event.getChannel(),"No files in the sfx folder match your query",120,false);
        }else if(toRetrieve.size()>1){
            BotUtils.sendMessage(event.getChannel(),"More than one file fits your query, please be more specific",120,false);
        }else{
            BotUtils.sendFile(event.getChannel(), toRetrieve.get(0));
        }
    }
    public static File[] songsDir(Event event, FileFilter filter){
        File sfx = new File("sfx");
        File[] songDir = null;
        if(sfx.exists()){
            songDir = new File("sfx").listFiles(filter);
        }else{
            BotUtils.mkFolder(event,"sfx");
        }
        return songDir;
    }
}