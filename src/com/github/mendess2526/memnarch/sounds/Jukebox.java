package com.github.mendess2526.memnarch.sounds;

import com.github.mendess2526.memnarch.sounds.playerHelpers.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.mendess2526.memnarch.BotUtils.sendMessage;

public class Jukebox {
    private static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private static final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
    static {
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }
    private static synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild){
        long guildId = guild.getLongID();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if(musicManager==null){
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId,musicManager);
        }
        guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

        return musicManager;
    }
    public static void queue(MessageReceivedEvent event, List<String> args) {
        IVoiceChannel userVChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
        final IChannel channel = event.getChannel();
        if(userVChannel==null){
            sendMessage(channel,"Not in a voice channel",30,false);
            return;
        }
        userVChannel.join();
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
        String trackURL = String.join(" ",args);
        playerManager.loadItemOrdered(musicManager, trackURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                sendMessage(channel,"Adding to queue: "+audioTrack.getInfo().title,
                           -1,false);
                play(musicManager, audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                AudioTrack firstTrack = audioPlaylist.getSelectedTrack();

                if(firstTrack==null){
                    firstTrack = audioPlaylist.getTracks().get(0);
                }

                sendMessage(channel,"Adding to queue: " + firstTrack.getInfo().title
                                              + " (first trak of playlist "+audioPlaylist.getName()+")",
                           -1,false);
                play(musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                sendMessage(channel,"Nothing found by :" + trackURL,30,false);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                sendMessage(channel,"Could not play: " + e.getMessage(),
                           -1,false);
            }
        });
    }

    private static void play(GuildMusicManager musicManager, AudioTrack audioTrack) {
        musicManager.getScheduler().queue(audioTrack);
    }

    public static void skip(MessageReceivedEvent event){
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
        musicManager.getScheduler().nextTrack();
        sendMessage(event.getChannel(),"Skipping",30,false);
        if(musicManager.getScheduler().getQueue().isEmpty())
            event.getClient().getOurUser().getVoiceStateForGuild(event.getGuild()).getChannel().leave();
    }
}
