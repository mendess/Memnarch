package com.github.mendess2526.memnarch.misc;

import com.github.mendess2526.memnarch.BotUtils;
import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.util.EmbedBuilder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.mendess2526.memnarch.LoggerService.log;

public class MiscTasks {
    @SuppressWarnings("SpellCheckingInspection")
    private static final String aRcountingFile = BotUtils.DEFAULT_FILE_PATH+"aRcounting.ini";
    private static Wini iniFile;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static void rRank(MessageReceivedEvent event){
        String userID = Long.toString(event.getAuthor().getLongID());
        lock.readLock().lock();
        Wini iniFile;
        try{
            iniFile = new Wini(new File(MiscTasks.aRcountingFile));
        }catch(IOException e){
            log(event.getGuild(),e,"MiscCommands.rRank");
            return;
        }finally{
            lock.readLock().unlock();
        }
        Integer rReactions = iniFile.get(userID, "rReaction", Integer.class);
        LocalDateTime rReactionsDate = iniFile.get(userID, "rReactionDate", LocalDateTime.class);
        Integer rEmojis = iniFile.get(userID, "rEmojis", Integer.class);
        LocalDateTime rEmojisDate = iniFile.get(userID, "rEmojisDate", LocalDateTime.class);

        boolean ranked = true;
        if(rReactions == null || rReactionsDate == null || rEmojis == null || rEmojisDate == null)
            ranked = false;
        EmbedBuilder eb = new EmbedBuilder();
        if(ranked){
            eb.withTitle("Your RRank:");
            eb.appendField("Emoji Rank:", "" +
                    "#: " + rEmojis
                    + "\n" + "Last Post: " + rEmojisDate, true);
            eb.appendField("Reaction Rank:", "" +
                    "#: " + rReactions
                    + "\n" + "LastPost: " + rReactionsDate, true);
            if(LocalDateTime.now().minus(1, ChronoUnit.DAYS).isAfter(rEmojisDate)
                    || LocalDateTime.now().minus(1, ChronoUnit.DAYS).isAfter(rReactionsDate)){
                eb.withFooterText("You've been slacking... Post some R's ffs");
            }
        }else{
            eb.withTitle("Not ranked...");
        }
        BotUtils.sendMessage(event.getChannel(), eb.build(), 30, true);
    }

    public static void handleR(ReactionAddEvent event){
        String userID = Long.toString(event.getAuthor().getLongID());
        lock.writeLock().lock();
        try{
            iniFile = new Wini(new File(aRcountingFile));
            Integer rReactions = iniFile.get(userID,"rReactions",Integer.class);
            if(rReactions==null) rReactions = 0;
            iniFile.put(userID,"rReactions", rReactions + 1);
            iniFile.put(userID,"rReactionsDate", LocalDateTime.now());
            iniFile.store();
        }catch(IOException e){
            log(event.getGuild(),e,"MiscTasks.handleR(Reaction)");
        }finally{
            lock.writeLock().unlock();
        }
    }

    public static void handleR(MessageReceivedEvent event){
        String userID = Long.toString(event.getAuthor().getLongID());
        lock.writeLock().lock();
        try{
            iniFile = new Wini(new File(aRcountingFile));
            Integer rEmojis = iniFile.get(userID,"rEmojis",Integer.class);
            if(rEmojis==null) rEmojis = 0;
            iniFile.put(userID,"rEmojis",rEmojis+1);
            iniFile.put(userID,"rEmojisDate", LocalDateTime.now());
            iniFile.store();
        }catch(IOException e){
            log(event.getGuild(),e,"MiscTasks.handleR(Message)");
        }finally{
            lock.writeLock().unlock();
        }
    }
}
