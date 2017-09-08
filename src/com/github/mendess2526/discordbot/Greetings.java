package com.github.mendess2526.discordbot;

import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

@SuppressWarnings("WeakerAccess")
public class Greetings {
    private static List<String> greetings;
    static {
        greetings.add("Begone Thot.mp3");
        greetings.add("Heeey.mp3");
        greetings.add("Welcome To The Rice Fields.mp3");
        greetings.add("Well Met.mp3");
    }
    public static void toggle(MessageReceivedEvent event, List<String> args) {
        /*LoggerService.log(event.getGuild(),"Reading greetings ini",LoggerService.INFO);
        Wini iniFile;
        try {
            iniFile = new Wini(new File("./greetings.ini"));
        }catch (IOException e){
            LoggerService.log(event.getGuild(),e.getMessage(),LoggerService.ERROR);
            return;
        }
        String id = Long.toString(event.getAuthor().getLongID());
        boolean status;
        if(iniFile.containsKey(id)){
            status = iniFile.get(id,"enabled",boolean.class);
            iniFile.put(id,"enabled",!status);
        }else{
            iniFile.put(id,"enabled",true);
        }
*/
    }

    public static void greet(UserVoiceChannelJoinEvent event) {
        IVoiceChannel vChannel = event.getUser().getVoiceStateForGuild(event.getGuild()).getChannel();

        Random rand = new Random();
        int voiceLine = rand.nextInt(greetings.size());
        String searchStr = greetings.get(voiceLine);

        AudioPlayer audioP = AudioPlayer.getAudioPlayerForGuild(event.getGuild());

        File[] songDir = AudioModule.songsDir(event, file -> file.getName().toUpperCase().contains(searchStr));
        if (songDir == null) {
            return;
        }
        if(songDir.length==0){
            LoggerService.log(event.getGuild(),"No greeting by the name: "+searchStr,LoggerService.ERROR);
            return;
        }
        audioP.clear();

        vChannel.join();
        try {
            audioP.queue(songDir[0]);
        } catch (IOException e) {
            LoggerService.log(event.getGuild(),"Greet failed: "+e.getMessage(), LoggerService.ERROR);
        } catch (UnsupportedAudioFileException e) {
            LoggerService.log(event.getGuild(),"Greet failed: "+e.getMessage(), LoggerService.ERROR);
            e.printStackTrace();
        }
    }
}
