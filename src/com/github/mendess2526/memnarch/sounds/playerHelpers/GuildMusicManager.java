package com.github.mendess2526.memnarch.sounds.playerHelpers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import sx.blah.discord.handle.obj.IGuild;

/**
 * Holder for both the player and a track scheduler for one guild.
 */
public class GuildMusicManager {
    private final AudioPlayer player;
    private final AudioProvider provider;
    private final TrackScheduler scheduler;
    private final IGuild guildID;

    /**
     * Creates a player and a track scheduler.
     * @param manager Audio player manager to use for creating the player.
     * @param guild Guild the audio player will be associated with
     */
    public GuildMusicManager(AudioPlayerManager manager, IGuild guild) {
        this.player = manager.createPlayer();
        this.guildID = guild;
        this.provider = new AudioProvider(player);
        this.scheduler = new TrackScheduler(player, guild);
    }

    /**
     * Adds a listener to be registered for audio events.
     */
    public void addAudioListener(AudioEventListener listener) {
        player.addListener(listener);
    }

    /**
     * Removes a listener that was registered for audio events.
     */
    public void removeAudioListener(AudioEventListener listener) {
        player.removeListener(listener);
    }

    /**
     * @return The scheduler for AudioTracks.
     */
    public TrackScheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioProvider getAudioProvider() {
        return provider;
    }
}