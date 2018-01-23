package com.github.mendess2526.memnarch.sounds;

import com.github.mendess2526.memnarch.Command;
import com.github.mendess2526.memnarch.Events;
import com.github.mendess2526.memnarch.sounds.playerHelpers.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.apache.commons.lang3.text.WordUtils;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.mendess2526.memnarch.BotUtils.*;
import static com.github.mendess2526.memnarch.LoggerService.*;


@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
public class SfxModule extends AbstractSoundModule{
    static abstract class CSFXModule implements Command{
        //TODO implement
        @Override
        public String getCommandGroup(){
            return "Sfx";
        }

        @Override
        public Set<Permissions> getPermissions(){
            return null;
        }
    }
    public SfxModule(){
        commandMap.put("<LIST",     new CSFXModule(){
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                list(event);
            }
        });
        commandMap.put("<ADD",      new CSFXModule() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                add(event);
            }
        });
        commandMap.put("<DELETE",   new CSFXModule() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                delete(event,args);
            }
        });
        commandMap.put("<RETRIEVE", new CSFXModule() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                retrieve(event,args);
            }
        });
    }
    private final Map<String,Command> commandMap = new HashMap<>();
    private final String sfxFolderPath = DEFAULT_FILE_PATH+"sfx/";

    public void sfx(MessageReceivedEvent event, List<String> args){
        if (args.size() == 0) {
            HashMap<String, Set<String>> cmds = new HashMap<>();
            Set<String> options = new HashSet<>();
            options.addAll(commandMap.keySet());
            options.add("\"name\"");
            cmds.put("Sfx", options);
            help(event.getAuthor(), event.getChannel(), cmds);
            return;
        }
        if(args.get(0).startsWith("<") && commandMap.containsKey(args.get(0).toUpperCase())){
            log(event.getGuild(), "Valid Argument: " + args.get(0).toUpperCase(), INFO);
            commandMap.get(args.get(0).toUpperCase()).runCommand(event,args.subList(1,args.size()));
        }else{
            play(event,args);
        }
    }

    private void play(MessageReceivedEvent event, List<String> args) {
        IVoiceChannel vChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
        if (vChannel == null) {
            sendMessage(event.getChannel(), "Please join a voice channel before using this command!", 120, false);
            return;
        }
        String searchStr = String.join(" ", args);
        GuildMusicManager audioP = getGuildAudioPlayer(event.getGuild());
        log(event.getGuild(),searchStr,INFO);
        File[] songDir = songsDir(event, file -> file.getName().toUpperCase().contains(searchStr));
        if (songDir == null) {
            return;
        }
        log(event.getGuild(), Arrays.toString(Arrays.stream(songDir).map(File::getAbsolutePath).toArray()),INFO);
        if (songDir.length == 0) {
            sendMessage(event.getChannel(), "No files in the sfx folder match your query", 120, false);
            return;
        }
        audioP.getScheduler().stop();
        vChannel.join();
        log(event.getGuild(),"Songs that match: "+ Arrays.toString(songDir), INFO);
        playerManager.loadItemOrdered(audioP, songDir[0].getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                //Jukebox.pause(event);
                play(audioP,audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                //
            }

            @Override
            public void noMatches() {
                sendMessage(event.getChannel(),"No files in the sfx folder match you query",30,false);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                sendMessage(event.getChannel(),"Could not play: " +e.getMessage(),-1,false);
                contactOwner(event,"Could not play: "+e.getMessage());
            }
        });
    }

    private void play(GuildMusicManager audioP, AudioTrack audioTrack) {
        audioP.getScheduler().queue(audioTrack);
    }

    public void list(MessageReceivedEvent event) {
        File[] songDir = songsDir(event,File::isFile);
        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("List of sounds files:");
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
                char from = sfxNames.get(column * 12).toUpperCase().charAt(0);
                char to = sfxNames.get(column * 12 + count - 1).toUpperCase().charAt(0);
                eb.appendField(from+"-"+to, s.toString(),true);
                count=0;column++;
            }
        }
        eb.withFooterText("Use "+ Events.BOT_PREFIX+"sounds <name> to play one");
        sendMessage(event.getChannel(),event.getAuthor().mention(),eb.build(),-1,true);
    }

    private void add(MessageReceivedEvent event) {
        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.MANAGE_CHANNELS)
            && !event.getAuthor().equals(event.getClient().getApplicationOwner())){
            sendMessage(event.getChannel(),"You don't have permission to use that command",120,false);
            return;
        }
        List<IMessage.Attachment> attachments = event.getMessage().getAttachments();
        if(attachments.size()==0){
            sendMessage(event.getChannel(),"Please attach a file to the message",120,false);
            return;
        }
        IMessage.Attachment attach = attachments.get(0);
        downloadFile(event,attach,sfxFolderPath);
    }

    private void delete(MessageReceivedEvent event, List<String> args) {
        if(!event.getAuthor().equals(event.getClient().getApplicationOwner())){
            sendMessage(event.getChannel(),"Only the owner of the bot can use that command",120,false);
            return;
        }
        String searchStr = String.join(" ", args);
        File[] songDir = songsDir(event,s -> s.getName().toUpperCase().contains(searchStr));

        List<File> toDelete = Arrays.asList(songDir);
        log(event.getGuild(),"Files to delete: "+toDelete.toString(), INFO);
        if(toDelete.size()==0){
            sendMessage(event.getChannel(),"No files in the sounds folder match your query",120,false);
        }else if(toDelete.size()>1){
            sendMessage(event.getChannel(),"More than one file fits your query, please be more specific",120,false);
        }else{
            String name = toDelete.get(0).getName();
            sendFile(event.getChannel(),toDelete.get(0));
            if(toDelete.get(0).delete()){
                sendMessage(event.getChannel(),"Sfx: `"+name+"` deleted!",-1,false);
                log(event.getGuild(),"Deleted", SUCC);
            }else{
                sendMessage(event.getChannel(),"Sfx: `"+name+"` not deleted",120,false);
                log(event.getGuild(),"File not deleted", ERROR);
            }
        }
    }

    private void retrieve(MessageReceivedEvent event, List<String> args){
        args.remove(0);
        String searchStr = String.join(" ", args);
        File[] songDir = songsDir(event,s -> s.getName().toUpperCase().contains(searchStr));

        List<File> toRetrieve = Arrays.asList(songDir);
        log(event.getGuild(),"Files to retrieve: "+toRetrieve.toString(), INFO);
        if(toRetrieve.size()==0){
            sendMessage(event.getChannel(),"No files in the sounds folder match your query",120,false);
        }else if(toRetrieve.size()>1){
            sendMessage(event.getChannel(),"More than one file fits your query, please be more specific",120,false);
        }else{
            sendFile(event.getChannel(), toRetrieve.get(0));
        }
    }

    File[] songsDir(Event event, FileFilter filter){
        File sfx = new File(sfxFolderPath);
        File[] songDir = null;
        if(sfx.exists()){
            log(null,sfxFolderPath,INFO);
            songDir = sfx.listFiles(/*filter*/);
            log(null, Arrays.toString(Arrays.stream(songDir).map(File::getAbsolutePath).toArray()),INFO);
        }else{
            mkFolder(event,sfxFolderPath);
        }
        return songDir;
    }
}