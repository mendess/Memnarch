package com.github.mendess2526.memnarch.sounds;

import com.github.mendess2526.memnarch.sounds.playerHelpers.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSoundModule {
    private final Map<Long, GuildMusicManager> musicManagers;
    final AudioPlayerManager playerManager;

    AbstractSoundModule(){
        this.musicManagers = new HashMap<>();
        this.playerManager = AudioPlayerManagerManager.getPlayerManager();
    }

    synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild){
        long guildId = guild.getLongID();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if(musicManager==null){
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId,musicManager);
        }
        guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

        return musicManager;
    }

    public void pause(MessageReceivedEvent event){
        this.getGuildAudioPlayer(event.getGuild()).getScheduler().pause();
    }

    public void resume(MessageReceivedEvent event){
        getGuildAudioPlayer(event.getGuild()).getScheduler().resume();
    }
}

