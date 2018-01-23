package com.github.mendess2526.memnarch.sounds;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

class AudioPlayerManagerManager {
    private static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    static {
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        //TODO config bigger buffer
    }
    static AudioPlayerManager getPlayerManager(){
        return playerManager;
    }
}
