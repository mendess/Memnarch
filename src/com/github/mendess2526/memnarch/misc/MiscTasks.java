package com.github.mendess2526.memnarch.misc;

import com.github.mendess2526.memnarch.BotUtils;
import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.mendess2526.memnarch.BotUtils.USERS_PATH;
import static com.github.mendess2526.memnarch.BotUtils.sendMessage;
import static com.github.mendess2526.memnarch.LoggerService.log;

public class MiscTasks {

    /**
     * The iniFile
     */
    private static Wini iniFile;
    /**
     * The format of the date used to store the last R
     */
    private static final DateTimeFormatter dateForm = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String rReactionsStr = "rReactions";
    private static final String rReactionsDateStr = "rReactionsDate";
    private static final String rEmojisStr = "rEmojis";
    private static final String rEmojisDateStr = "rEmojisDate";
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static String userName = "UserName";

    public static void rRank(MessageReceivedEvent event){
        String userID = Long.toString(event.getAuthor().getLongID());
        if(iniFile == null) initFile(event.getGuild());
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
        if(iniFile == null) initFile(event.getGuild());
        Integer rReactions = iniFile.get(userID,"rReactions", Integer.class);
        if(rReactions==null) rReactions = 0;
        iniFile.put(userID,userName,event.getUser().getName());
        iniFile.put(userID,rReactionsStr, rReactions + 1);
        iniFile.put(userID,rReactionsDateStr, LocalDate.now().format(dateForm));
        updateFile(event.getGuild());
    }

    public static void handleR(MessageReceivedEvent event){
        String userID = Long.toString(event.getAuthor().getLongID());
        if(iniFile == null) initFile(event.getGuild());
        Integer rEmojis = iniFile.get(userID,"rEmojis",Integer.class);
        if(rEmojis==null) rEmojis = 0;
        iniFile.put(userID,userName,event.getAuthor().getName());
        iniFile.put(userID,rEmojisStr,rEmojis+1);
        iniFile.put(userID,rEmojisDateStr, LocalDate.now().format(dateForm));
        updateFile(event.getGuild());
    }

    public static void leaderBoard(MessageReceivedEvent event){
        EmbedBuilder leaderBoard = new EmbedBuilder();
        if(iniFile == null) initFile(event.getGuild());
        leaderBoard.withTitle("LeaderBoard");
        List<RUser> rUsers = new ArrayList<>();
        for(String s: iniFile.keySet()){
            String userName = iniFile.get(s,MiscTasks.userName,String.class);
            Integer rEmojis = iniFile.get(s,MiscTasks.rEmojisStr,Integer.class);
            if(rEmojis == null) rEmojis = 0;
            Integer rReactions = iniFile.get(s,MiscTasks.rReactionsStr,Integer.class);
            if(rReactions == null) rReactions = 0;
            rUsers.add(new RUser(userName,rEmojis,rReactions));
        }
        rUsers.sort(Comparator.comparingInt(RUser::score));
        int size = rUsers.size();
        for(int i = size-1; i>=0; i--){
            int place = size - i;
            leaderBoard.appendDesc(place + ": " + rUsers.get(i).userName+"\n");
            if(place == 10) break;
        }
        sendMessage(event.getChannel(),leaderBoard.build(),30,true);
    }

    public static void refreshUserNames(MessageReceivedEvent event){
        if(iniFile == null) initFile(event.getGuild());
        if(!event.getAuthor().equals(event.getClient().getApplicationOwner())){
            sendMessage(event.getChannel(),"Only the owner of the bot can use that command",120,false);
            return;
        }
        for(String s: iniFile.keySet()){
            IUser user = event.getClient().getUserByID(Long.parseLong(s));
            if(user == null) continue;
            String name = user.getName();
            iniFile.put(s,userName,name);
        }
        updateFile(event.getGuild());
    }

    private static void initFile(IGuild guild){
        try{
            lock.readLock().lock();
            iniFile = new Wini(new File(USERS_PATH));
        }catch(IOException e){
            log(guild,e);
        }finally{
            lock.readLock().unlock();
        }
    }

    private static void updateFile(IGuild guild){
        try{
            lock.writeLock().lock();
            iniFile.store();
        }catch(IOException e){
            log(guild,e);
        }finally{
            lock.writeLock().unlock();
        }
    }

    private static final class RUser{
        private final String userName;
        private final int rEmoji;

        private final int rReactions;
        RUser(String userName, int rEmoji, int rReactions){
            this.userName = userName;
            this.rEmoji = rEmoji;
            this.rReactions = rReactions;
        }

        int score(){
            return this.rEmoji + this.rReactions;
        }
    }
}
