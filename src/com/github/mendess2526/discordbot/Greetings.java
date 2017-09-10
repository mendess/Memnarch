package com.github.mendess2526.discordbot;

import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class Greetings {
    // Class variables
    private static Map<String,Command> commandMap = new HashMap<>();
    static {
        commandMap.put("ME",Greetings::greetme);
        commandMap.put("ADD",Greetings::addGreeting);
        commandMap.put("REMOVE",Greetings::removeGreeting);
        commandMap.put("LIST",Greetings::list);
    }

    // Class methods
    public static void greetings(MessageReceivedEvent event, List<String> args) {
        if(canGreet(event)){
            if (args.size() == 0 || !commandMap.containsKey(args.get(0))) {
                if(args.size()!=0){LoggerService.log(event.getGuild(),"Invalid Argument: "+args.get(0),LoggerService.INFO);}
                HashMap<String, Set<String>> cmds = new HashMap<>();
                cmds.put("Greetings", commandMap.keySet());
                boolean enabled = Main.greetings.get(event.getGuild().getLongID()).isGreetable(event.getAuthor().getLongID());
                EmbedBuilder eb = new EmbedBuilder().withTitle(enabled ? "Greetings are enabled for you" : "Greetings are not enabled for you")
                                                    .withColor(enabled?0:255,enabled?255:0,0);
                BotUtils.sendMessage(event.getChannel(),eb.build(),120,false);
                BotUtils.help(event.getAuthor(), event.getChannel(), cmds);
            }else {
                LoggerService.log(event.getGuild(), "Valid Argument: " + args.get(0), LoggerService.INFO);
                commandMap.get(args.get(0)).runCommand(event, args.subList(1, args.size()));
            }
        }else{
            BotUtils.sendMessage(event.getChannel(),"Greetings are disabled in this server",120,false);
        }
    }
    private static void list(MessageReceivedEvent event, List<String> strings) {
        Main.greetings.get(event.getGuild().getLongID()).list(event.getChannel());
    }

    private static void removeGreeting(MessageReceivedEvent event, List<String> args) {
        if(BotUtils.hasPermission(event,EnumSet.of(Permissions.MANAGE_SERVER))){
            String searchStr = String.join(" ",args);
            Main.greetings.get(event.getGuild().getLongID()).removeGreeting(event,searchStr);
        }
    }

    public static void greetme(MessageReceivedEvent event, List<String> args){
        Main.greetings.get(event.getGuild().getLongID()).greetme(event);
    }

    public static void addGreeting(MessageReceivedEvent event, List<String> args){
        if(BotUtils.hasPermission(event,EnumSet.of(Permissions.MANAGE_SERVER))){
            String searchStr = String.join(" ",args);
            Main.greetings.get(event.getGuild().getLongID()).addGreeting(event,searchStr);
        }
    }

    public static File guildFile(Long id) throws IOException{
        LoggerService.log(null,"Reading greetings ini for guild: "+id,LoggerService.INFO);
        File greetDir = new File("greetings");
        if(!greetDir.exists()){
            LoggerService.log(null,"Folder doesn't exist, creating folder...",LoggerService.INFO);
            boolean dirMade = greetDir.mkdirs();
            if(!dirMade) {
                LoggerService.log(null, "Couldn't create folder",LoggerService.ERROR);
                throw new IOException("Could't create folder");
            }
            LoggerService.log(null,"Folder created",LoggerService.SUCC);
        }
        File greetFile = new File("greetings/"+id+".ini");
        if(!greetFile.exists()){
            LoggerService.log(null,"File doesn't exist, creating file...",LoggerService.INFO);
            boolean fileMade = greetFile.createNewFile();
            if(!fileMade) {
                LoggerService.log(null, "Couldn't create file",LoggerService.ERROR);
                throw new IOException("Could't create folder");
            }
            LoggerService.log(null,"File created",LoggerService.SUCC);
        }
        return greetFile;
    }
    public static boolean canGreet(GuildEvent event){
        return Main.serverSettings.get(event.getGuild().getLongID()).allowsGreetings();
    }
    // Instance Variables
    private Long id;
    private IGuild guild;
    private ReadWriteLock iniLock;
    private ReadWriteLock listLock;
    private Wini iniFile;
    private List<String> greetings;
    // Instance methods
    public Greetings(IGuild guild) throws IOException{
        this.id = guild.getLongID();
        this.guild = guild;
        this.iniLock = new ReentrantReadWriteLock();
        this.listLock = new ReentrantReadWriteLock();
        this.iniFile = new Wini(guildFile(id));
        this.greetings = new ArrayList<>();
        File greetingsFile = new File("greetings/"+this.id);
        if(!greetingsFile.exists()){
            if(!greetingsFile.createNewFile()){
                throw new IOException("Couldn't create greetings list file");
            }
        }
        try(BufferedReader br = new BufferedReader(new FileReader(greetingsFile))){
            for(String line; (line = br.readLine())!=null; ){
                greetings.add(line);
            }
        }
    }

    public void greetme(MessageReceivedEvent event) {
        LoggerService.log(event.getGuild(),"Toggling "+event.getAuthor()+"'s status",LoggerService.INFO);
        String id = Long.toString(event.getAuthor().getLongID());
        boolean status;
        if(iniFile.containsKey(id) && iniFile.get(id).containsKey("enabled")){
            status = iniFile.get(id,"enabled",boolean.class);
        }else{
            status = false;
        }
        iniFile.put(id,"enabled",!status);
        BotUtils.sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Greetings enabled!":":x: Greetings disabled!").build(),
                -1,true);
        try {
            iniLock.writeLock().lock();
            iniFile.store();
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            iniLock.writeLock().unlock();
            LoggerService.log(event.getGuild(),"Toggle unlocked",LoggerService.INFO);
        }
    }
    public void greet(UserVoiceChannelJoinEvent event) {
        IVoiceChannel vChannel = event.getUser().getVoiceStateForGuild(event.getGuild()).getChannel();

        Random rand = new Random();
        int voiceLine = rand.nextInt(greetings.size());
        String searchStr = greetings.get(voiceLine).toUpperCase();

        AudioPlayer audioP = AudioPlayer.getAudioPlayerForGuild(event.getGuild());

        File[] songDir = SfxModule.songsDir(event, file -> file.getName().toUpperCase().contains(searchStr));
        if (songDir == null) {
            return;
        }
        if(songDir.length==0){
            LoggerService.log(event.getGuild(),"No greeting by the name: "+searchStr,LoggerService.ERROR);
            return;
        }
        audioP.clear();

        vChannel.join();
        try {
            audioP.queue(songDir[0]);
        } catch (IOException e) {
            LoggerService.log(event.getGuild(),"Greet failed: "+e.getMessage(), LoggerService.ERROR);
        } catch (UnsupportedAudioFileException e) {
            LoggerService.log(event.getGuild(),"Greet failed: "+e.getMessage(), LoggerService.ERROR);
            e.printStackTrace();
        }
    }
    public boolean isGreetable(Long longID) {
        return iniFile.get(longID.toString()).get("enabled",boolean.class);
    }
    public void addGreeting(MessageReceivedEvent event, String searchStr){
        File[] songDir = SfxModule.songsDir(event, file -> file.getName().toUpperCase().contains(searchStr));
        LoggerService.log(event.getGuild(),"Songs that match: "+ Arrays.toString(songDir),LoggerService.INFO);
        if (songDir == null || songDir.length == 0) {
            BotUtils.sendMessage(event.getChannel(), "No files in the sfx folder match your query", 120, false);
            return;
        }
        if(songDir.length>1){
            BotUtils.sendMessage(event.getChannel(),"More than one file matches your query",120,false);
            return;
        }
        if(this.greetings.stream().filter(s -> s.contains(songDir[0].getName())).count()>0){
            BotUtils.sendMessage(event.getChannel(),"Greeting already added",120,false);
            return;
        }
        greetings.add(songDir[0].getName());
        LoggerService.log(event.getGuild(),"Updating file",LoggerService.INFO);
        updateFile();
        BotUtils.sendMessage(event.getChannel(),"Greeting added",120,false);
    }

    private void removeGreeting(MessageReceivedEvent event, String searchStr) {
        List<String> gList = this.greetings.stream().map(String::toUpperCase).filter(s -> s.contains(searchStr)).collect(Collectors.toList());
        if(gList.size()==0){
            BotUtils.sendMessage(event.getChannel(),"No Greeting with that name",120,false);
        }else if(gList.size()>1){
            BotUtils.sendMessage(event.getChannel(),"More then one greeting matches that name",120,false);
        }else{
            this.greetings = this.greetings.stream().filter(s -> !s.toUpperCase().contains(searchStr)).collect(Collectors.toList());
            LoggerService.log(event.getGuild(),"List of greetings after removing: "+ this.greetings.toString(),LoggerService.INFO);
            updateFile();
            BotUtils.sendMessage(event.getChannel(),"Greeting removed",120,false);
        }
    }
    public void updateFile(){
        LoggerService.log(guild,"Updating List File",LoggerService.INFO);
        try{
            listLock.writeLock().lock();
            BufferedWriter bw = new BufferedWriter(new FileWriter("greetings/"+id.toString()));
            for (String greeting : greetings) {
                LoggerService.log(guild, "Writing "+greeting+" to file",LoggerService.INFO);
                bw.write(greeting+"\n");
            }
            bw.close();
        }catch (IOException e){
            LoggerService.log(guild,"Couldn't update file",LoggerService.ERROR);
        }finally {
            listLock.writeLock().unlock();
        }
    }

    private void list(IChannel channel) {
        BotUtils.sendMessage(channel,new EmbedBuilder().withTitle("List of greetings").withDesc(greetings.toString()).build(),-1,true);
    }
}
