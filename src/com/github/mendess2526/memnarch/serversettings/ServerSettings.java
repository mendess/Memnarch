package com.github.mendess2526.memnarch.serversettings;

import com.github.mendess2526.memnarch.Command;
import com.github.mendess2526.memnarch.Main;
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

import static com.github.mendess2526.memnarch.BotUtils.*;
import static com.github.mendess2526.memnarch.LoggerService.*;


public class ServerSettings {
    static abstract class CServerSettingSub implements Command{
        //TODO implement
        @Override
        public String getCommandGroup(){
            return "Server Settings";
        }

        @Override
        public Set<Permissions> getPermissions(){
            return null;
        }
    }
    // Class Variables
    //private static Permissions permissions = Permissions.MANAGE_SERVER;
    private static final Map<String,Command> commandMap = new HashMap<>();
    static {
        commandMap.put("WELCOMEMSG",    new CServerSettingSub() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                setMessagesNewUsers(event);
            }
        });
        commandMap.put("SETWELCOMEMSG", new CServerSettingSub() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                setWelcomeMessage(event,args);
            }
        });
        commandMap.put("GREETINGS",     new CServerSettingSub() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                setAllowsGreetings(event);
            }
        });
        commandMap.put("STATUS",        new CServerSettingSub() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                showServerSettings(event);
            }
        });
    }
    // Class Methods
    public static void serverSettings(MessageReceivedEvent event, List<String> args) {
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

    private static void setMessagesNewUsers(MessageReceivedEvent event){
        Main.serverSettings.get(event.getGuild().getLongID()).iSetMessagesNewUsers(event);
    }

    private static void setWelcomeMessage(MessageReceivedEvent event, List<String> args){
        List<String> msg = Arrays.asList(event.getMessage().getContent().split("\\s")).subList(2,args.size()+2);
        Main.serverSettings.get(event.getGuild().getLongID())
                .iSetWelcomeMessage(String.join(" ",msg));
        sendMessage(event.getChannel(),"Message set!",120,false);
    }

    private static void setAllowsGreetings(MessageReceivedEvent event){
        Main.serverSettings.get(event.getGuild().getLongID()).iSetAllowsGreetings(event);
    }

    private static void showServerSettings(MessageReceivedEvent event){
        Main.serverSettings.get(event.getGuild().getLongID()).iShowServerSettings(event);
    }

    /*----------------------------------------------------------------------------------------------*/
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
            File f = new File(SERVERS_PATH);
            iniFile = new Wini(f);
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

    private void iSetAllowsGreetings(MessageReceivedEvent event) {
        boolean status = iniFile.get(this.id,"Allows Greetings",boolean.class);
        iniFile.put(this.id,"Allows Greetings",!status);
        updateIni();
        sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Greetings enabled on this server!":":x: Greetings disabled on this server!").build(),
                -1,true);
    }

    private void iSetMessagesNewUsers(MessageReceivedEvent event) {
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

    private void iSetWelcomeMessage(String newUserMessage) {
        iniFile.put(this.id,"New User Message",newUserMessage);
        updateIni();
    }

    private void iShowServerSettings(MessageReceivedEvent event) {
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
}
