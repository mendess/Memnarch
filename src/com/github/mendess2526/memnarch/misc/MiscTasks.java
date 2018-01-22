package com.github.mendess2526.memnarch.misc;

import com.github.mendess2526.memnarch.BotUtils;
import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.util.EmbedBuilder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.mendess2526.memnarch.BotUtils.USERS_PATH;
import static com.github.mendess2526.memnarch.LoggerService.log;

public class MiscTasks {
    @SuppressWarnings("SpellCheckingInspection")
    private static Wini iniFile;
    private static final DateTimeFormatter dateForm = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final String rReactionsStr = "rReactions";
    private static final String rReactionsDateStr = "rReactionsDate";
    private static final String rEmojisStr = "rEmojis";
    private static final String rEmojisDateStr = "rEmojisDate";

    public static void rRank(MessageReceivedEvent event){
        String userID = Long.toString(event.getAuthor().getLongID());
        lock.readLock().lock();
        Wini iniFile;
        try{
            iniFile = new Wini(new File(USERS_PATH));
        }catch(IOException e){
            log(event.getGuild(),e,"MiscCommands.rRank");
            return;
        }finally{
            lock.readLock().unlock();
        }
        EmbedBuilder eb = new EmbedBuilder();
        if(iniFile.containsKey(userID)){
            Integer rReactions = iniFile.get(userID, rReactionsStr, Integer.class);
            LocalDate rReactionsDate = null;
            try{
                rReactionsDate = LocalDate
                        .parse(iniFile.get(userID, rReactionsDateStr, String.class),dateForm);
            }catch(NullPointerException ignored){
            }
            Integer rEmojis = iniFile.get(userID, rEmojisStr, Integer.class);
            LocalDate rEmojisDate = null;
            try{
                rEmojisDate = LocalDate
                        .parse(iniFile.get(userID, rEmojisDateStr, String.class), dateForm);
            }catch(NullPointerException ignored){
            }
            if(rEmojis==null) rEmojis = 0;
            if(rReactions==null) rReactions = 0;
            int rank, eRank, rRank, fib = 1, lastFib, eFib, rFib;
            rank = eRank = rRank = lastFib = eFib = rFib = 0;
            while(fib<=rEmojis || fib<=rReactions){
                int tmp = fib;
                fib += lastFib;
                lastFib = tmp;
                rank++;
                if(fib>rEmojis && eFib==0){
                    eRank = rank;
                    eFib = fib;
                }
                if(fib>rReactions && rFib==0){
                    rRank = rank;
                    rFib = fib;
                }
            }
            eb.withTitle("Your RRank:");
            eb.appendField("Emoji Rank:", "" +
                    "Level: " + eRank
                    + "\n" + "Exp: " + rEmojis + "/" + eFib, true);
            eb.appendField("Reaction Rank:", "" +
                    "Level: " + rRank
                    + "\n" + "Exp: " + rReactions + "/" + rFib, true);
            if((rEmojisDate!=null && LocalDate.now().minus(1, ChronoUnit.DAYS).isAfter(rEmojisDate))
                    || (rReactionsDate!=null && LocalDate.now().minus(1, ChronoUnit.DAYS).isAfter(rReactionsDate))){
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
            iniFile = new Wini(new File(USERS_PATH));
            Integer rReactions = iniFile.get(userID,"rReactions", Integer.class);
            if(rReactions==null) rReactions = 0;
            iniFile.put(userID,rReactionsStr, rReactions + 1);
            iniFile.put(userID,rReactionsDateStr, LocalDate.now().format(dateForm));
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
            iniFile = new Wini(new File(USERS_PATH));
            Integer rEmojis = iniFile.get(userID,"rEmojis",Integer.class);
            if(rEmojis==null) rEmojis = 0;
            iniFile.put(userID,rEmojisStr,rEmojis+1);
            iniFile.put(userID,rEmojisDateStr, LocalDate.now().format(dateForm));
            iniFile.store();
        }catch(IOException e){
            log(event.getGuild(),e,"MiscTasks.handleR(Message)");
        }finally{
            lock.writeLock().unlock();
        }
    }
}
