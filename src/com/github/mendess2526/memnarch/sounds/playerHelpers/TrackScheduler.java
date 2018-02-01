package com.github.mendess2526.memnarch.sounds.playerHelpers;

import com.github.mendess2526.memnarch.LoggerService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import sx.blah.discord.handle.obj.IGuild;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler {
    private final List<AudioTrack> queue;
    private final AudioPlayer player;
    private ScheduledFuture leaveVoice;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final IGuild guild;

    TrackScheduler(AudioPlayer player, IGuild guild) {
        // Because we will be removing from the "head" of the queue frequently, a LinkedList is a better implementation
        // since all elements won't have to be shifted after removing. Additionally, choosing to add in between the queue
        // will similarly not cause many elements to shift and wil only require a couple of node changes.
        this.queue = Collections.synchronizedList(new LinkedList<>());
        this.player = player;
        this.guild = guild;

        // For encapsulation, keep the listener anonymous.
        player.addListener(new AudioEventAdapter() {
            @Override
            public void onTrackStart(AudioPlayer player, AudioTrack track){
                super.onTrackStart(player, track);
                leaveVoice.cancel(true);
            }
            @Override
            public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
                if(endReason.mayStartNext) {
                    nextTrack();
                }
                if(player.getPlayingTrack() == null){
                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    Runnable leave = () ->{
                        guild.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel().leave();
                        LoggerService.log(guild,"Leaving voice channel due to inactivity",LoggerService.INFO);
                    };
                    leaveVoice = executor.schedule(leave,10, TimeUnit.SECONDS);
                }

            }
        });
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public synchronized boolean queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        boolean playing = this.player.startTrack(track, true);

        if(!playing) {
            this.queue.add(track);
        }

        return playing;
    }

    /**
     * Starts the next track, stopping the current one if it is playing.
     * @return The track that was stopped, null if there wasn't anything playing
     */
    public synchronized AudioTrack nextTrack() {
        AudioTrack currentTrack = this.player.getPlayingTrack();
        AudioTrack nextTrack = this.queue.isEmpty() ? null : this.queue.remove(0);

        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        this.player.startTrack(nextTrack, false);
        return currentTrack;
    }

    /**
     * Returns the queue for this scheduler. Adding to the head of the queue (index 0) does not automatically
     * cause it to start playing immediately. The returned collection is thread-safe and can be modified.
     *
     * @apiNote To iterate over this queue, use a synchronized block. For example:
     * {@code synchronize (getQueue()) { // iteration code } }
     */
    public List<AudioTrack> getQueue() {
        return this.queue;
    }


    public void pause() {
        this.player.setPaused(true);
    }

    public void resume(){
       this.player.setPaused(false);
    }

    public void stop(){
        this.player.stopTrack();
        this.queue.clear();
    }
}
