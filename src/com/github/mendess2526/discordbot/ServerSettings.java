package com.github.mendess2526.discordbot;

import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.mendess2526.discordbot.BotUtils.*;
import static com.github.mendess2526.discordbot.LoggerService.*;


class ServerSettings {

    // Class Variables
    //private static Permissions permissions = Permissions.MANAGE_SERVER;
    private static final Map<String,Command> commandMap = new HashMap<>();
    static {
        commandMap.put("WELCOMEMSG",ServerSettings::setMessagesNewUsers);
        commandMap.put("SETWELCOMEMSG",ServerSettings::setNewUserMessage);
        commandMap.put("GREETINGS",ServerSettings::setAllowsGreetings);
        commandMap.put("STATUS",ServerSettings::showServerSettings);
    }

    // Class Methods
    static void serverSettings(MessageReceivedEvent event, List<String> args) {
        log(event.getGuild(), "Server settings args: " + args.toString(), INFO);
        if (hasPermission(event, EnumSet.of(Permissions.MANAGE_SERVER))) {
            if (args.size() == 0 || !commandMap.containsKey(args.get(0).toUpperCase())) {
                HashMap<String, Set<String>> cmds = new HashMap<>();
                cmds.put("ServerSettings", commandMap.keySet());
                help(event.getAuthor(), event.getChannel(), cmds);
            } else {
                log(event.getGuild(), "Valid Argument: " + args.get(0).toUpperCase(), INFO);
                commandMap.get(args.get(0).toUpperCase()).runCommand(event, args.subList(1, args.size()));
            }
        }
    }
    @SuppressWarnings("unused")
    private static void setMessagesNewUsers(MessageReceivedEvent event, List<String> args){
        Main.serverSettings.get(event.getGuild().getLongID()).setMessagesNewUsers(event);
    }

    private static void setNewUserMessage(MessageReceivedEvent event, List<String> args){
        List<String> msg = Arrays.asList(event.getMessage().getContent().split("\\s")).subList(2,args.size()+2);
        Main.serverSettings.get(event.getGuild().getLongID())
                .setNewUserMessage(String.join(" ",msg));
        sendMessage(event.getChannel(),"Message set!",120,false);
    }
    @SuppressWarnings("unused")
    private static void setAllowsGreetings(MessageReceivedEvent event, List<String> args){
        Main.serverSettings.get(event.getGuild().getLongID()).setAllowsGreetings(event);
    }
    @SuppressWarnings("unused")
    private static void showServerSettings(MessageReceivedEvent event, List<String> args){
        Main.serverSettings.get(event.getGuild().getLongID()).showServerSettings(event);
    }

    /**----------------------------------------------------------------------------------------------*/
    // Instance Variables
    private String id;
    private IGuild guild;
    private ReadWriteLock lock;
    private Wini iniFile;

    ServerSettings(IGuild guild) throws IOException {
        this.id=Long.toString(guild.getLongID());
        this.guild=guild;
        this.lock = new ReentrantReadWriteLock();
        try{
            lock.readLock().lock();
            iniFile = new Wini(new File("./settings.ini"));
        }finally {
            lock.readLock().unlock();
        }
        if(!iniFile.containsKey(this.id)){
            iniFile.put(this.id,"Messages new users",false);
            iniFile.put(this.id,"New User Message","");
            iniFile.put(this.id,"Allows Greetings",true);
            updateIni();
        }
    }

    // Instance Methods
    boolean messagesNewUsers() {
        return iniFile.get(this.id,"Messages new users",boolean.class);
    }

    boolean allowsGreetings() {
        return iniFile.get(this.id,"Allows Greetings",boolean.class);
    }

    String getNewUserMessage() {
        return iniFile.get(this.id,"New User Message",String.class);
    }

    private void setAllowsGreetings(MessageReceivedEvent event) {
        boolean status = iniFile.get(this.id,"Allows Greetings",boolean.class);
        iniFile.put(this.id,"Allows Greetings",!status);
        updateIni();
        sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Greetings enabled on this server!":":x: Greetings disabled on this server!").build(),
                -1,true);
    }

    private void setMessagesNewUsers(MessageReceivedEvent event) {
        if(iniFile.get(id).get("New User Message").length()==0){
            sendMessage(event.getChannel(),"Please set a message first",120,false);
            return;
        }
        boolean status = iniFile.get(this.id,"Messages new users",boolean.class);
        iniFile.put(this.id,"Messages new users",!status);
        updateIni();
        sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Bot now messages new users!":":x: Bot no longer messages new users!").build(),
                -1,true);
    }

    private void setNewUserMessage(String newUserMessage) {
        iniFile.put(this.id,"New User Message",newUserMessage);
        updateIni();
    }

    private void updateIni(){
        try{
            lock.writeLock().lock();
            iniFile.store();
        }catch (IOException e){
            log(guild,"Couldn't update server settings", ERROR);
        }finally {
            lock.writeLock().unlock();
        }
    }

    private void showServerSettings(MessageReceivedEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.withTitle("Server Settings Status");
        iniFile.get(id).keySet().forEach(k -> {
            try {
                eb.appendField(k,iniFile.get(id).get(k),false);
            }catch (IllegalArgumentException e){
                eb.appendField(k,"<empty>",false);
            }
        });
        sendMessage(event.getChannel(),event.getAuthor().mention(),eb.build(),-1,true);
    }
}
