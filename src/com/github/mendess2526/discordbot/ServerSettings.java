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

@SuppressWarnings("WeakerAccess")
public class ServerSettings {
    // Class Variables
    private static Permissions permissions = Permissions.MANAGE_SERVER;
    private static Map<String,Command> commandMap = new HashMap<>();
    static {
        commandMap.put("WELCOMEMSG",ServerSettings::setMessagesNewUsers);
        commandMap.put("SETWELCOMEMSG",ServerSettings::setNewUserMessage);
        commandMap.put("GREETINGS",ServerSettings::setAllowsGreetings);
        commandMap.put("STATUS",ServerSettings::showServerSettings);
    }
    // Class Methods
    public static void serverSettings(MessageReceivedEvent event, List<String> args) {
        LoggerService.log(event.getGuild(), "Server settings args: " + args.toString(), LoggerService.INFO);
        if (BotUtils.hasPermission(event, EnumSet.of(Permissions.MANAGE_SERVER))) {
            if (args.size() == 0 || !commandMap.containsKey(args.get(0))) {
                HashMap<String, Set<String>> cmds = new HashMap<>();
                cmds.put("ServerSettings", commandMap.keySet());
                BotUtils.help(event.getAuthor(), event.getChannel(), cmds);
            } else {
                commandMap.get(args.get(0)).runCommand(event, args.subList(1, args.size()));
            }
        }
    }

    public static void setMessagesNewUsers(MessageReceivedEvent event, List<String> args){
        Main.serverSettings.get(event.getGuild().getLongID()).setMessagesNewUsers(event);
    }

    public static void setNewUserMessage(MessageReceivedEvent event, List<String> args){
        List<String> msg = Arrays.asList(event.getMessage().getContent().split("\\s")).subList(2,args.size()+2);
        Main.serverSettings.get(event.getGuild().getLongID())
                .setNewUserMessage(String.join(" ",msg));
        BotUtils.sendMessage(event.getChannel(),"Message set!",120,false);
    }
    public static void setAllowsGreetings(MessageReceivedEvent event,List<String> args){
        Main.serverSettings.get(event.getGuild().getLongID()).setAllowsGreetings(event);
    }
    public static void showServerSettings(MessageReceivedEvent event, List<String> args){
        Main.serverSettings.get(event.getGuild().getLongID()).showServerSettings(event);
    }
    // Instance Variables
    private String id;
    private IGuild guild;
    private ReadWriteLock lock;
    private Wini iniFile;
    public ServerSettings(IGuild guild) throws IOException {
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
    public boolean messagesNewUsers() {
        return iniFile.get(this.id,"Messages new users",boolean.class);
    }
    public boolean allowsGreetings() {
        return iniFile.get(this.id,"Allows Greetings",boolean.class);
    }
    public String getNewUserMessage() {
        return iniFile.get(this.id,"New User Message",String.class);
    }
    public void setAllowsGreetings(MessageReceivedEvent event) {
        boolean status = iniFile.get(this.id,"Allows Greetings",boolean.class);
        iniFile.put(this.id,"Allows Greetings",!status);
        updateIni();
        BotUtils.sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Greetings enabled on this server!":":x: Greetings disabled on this server!").build(),
                -1,true);
    }
    public void setMessagesNewUsers(MessageReceivedEvent event) {
        if(iniFile.get(id).get("New User Message").length()==0){
            BotUtils.sendMessage(event.getChannel(),"Please set a message first",120,false);
            return;
        }
        boolean status = iniFile.get(this.id,"Messages new users",boolean.class);
        iniFile.put(this.id,"Messages new users",!status);
        updateIni();
        BotUtils.sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Bot now messages new users!":":x: Bot no longer messages new users!").build(),
                -1,true);
    }
    public void setNewUserMessage(String newUserMessage) {
        iniFile.put(this.id,"New User Message",newUserMessage);
        updateIni();
    }
    private void updateIni(){
        try{
            lock.writeLock().lock();
            iniFile.store();
        }catch (IOException e){
            LoggerService.log(guild,"Couldn't update server settings",LoggerService.ERROR);
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
        BotUtils.sendMessage(event.getChannel(),event.getAuthor().mention(),eb.build(),-1,true);
    }
}
