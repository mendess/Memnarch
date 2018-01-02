package com.github.mendess2526.memnarch.sfx;

import com.github.mendess2526.memnarch.Command;
import com.github.mendess2526.memnarch.Main;
import org.apache.commons.lang3.text.WordUtils;
import org.ini4j.Wini;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.github.mendess2526.memnarch.BotUtils.hasPermission;
import static com.github.mendess2526.memnarch.BotUtils.sendMessage;
import static com.github.mendess2526.memnarch.LoggerService.*;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
public class Greetings {
    static abstract class CGreetings implements Command{
        //TODO implement
        @Override
        public String getCommandGroup(){
            return "Greetings";
        }
        @Override
        public Set<Permissions> getPermissions(){
            return null;
        }
    }
    // Class variables
    private static Map<String,Command> commandMap = new HashMap<>();
    static {
        commandMap.put("ME",     new CGreetings() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                greetMe(event);
            }
        });
        commandMap.put("ADD",    new CGreetings() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                add(event,args);
            }
        });
        commandMap.put("REMOVE", new CGreetings() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                remove(event,args);
            }
        });
        commandMap.put("LIST",   new CGreetings() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                list(event);
            }
        });
    }

    // Class methods
    public static void greetings(MessageReceivedEvent event) {
        sendMessage(event.getChannel(),"Greetings are disabled because the guys that made the API screwed up. Hopefully it will be fixed soon™",120,false);

        /*if(canGreet(event)){
            if (args.size() == 0 || !commandMap.containsKey(args.get(0).toUpperCase())) {
                if(args.size()!=0){
                    log(event.getGuild(),"Invalid Argument: "+args.get(0), INFO);}
                HashMap<String,Set<String>> cmds = new HashMap<>();
                cmds.put("Greetings", commandMap.keySet());
                boolean enabled = Main.sfx.get(event.getGuild().getLongID()).isGreetable(event.getAuthor().getLongID());
                EmbedBuilder eb = new EmbedBuilder().withTitle(enabled ? "Greetings are enabled for you" : "Greetings are not enabled for you")
                                                    .withColor(enabled?0:255,enabled?255:0,0);
                sendMessage(event.getChannel(),eb.build(),120,false);
                help(event.getAuthor(), event.getChannel(), cmds);
            }else {
                log(event.getGuild(), "Valid Argument: " + args.get(0).toUpperCase(), INFO);
                commandMap.get(args.get(0).toUpperCase()).runCommand(event, args.subList(1, args.size()));
            }
        }else{
            sendMessage(event.getChannel(),"Greetings are disabled in this server",120,false);
        }*/

    }

    private static void list(MessageReceivedEvent event) {
        Main.greetings.get(event.getGuild().getLongID()).list(event.getChannel());
    }

    private static void remove(MessageReceivedEvent event, List<String> args) {
        if(hasPermission(event,EnumSet.of(Permissions.MANAGE_SERVER))){
            String searchStr = String.join(" ",args);
            Main.greetings.get(event.getGuild().getLongID()).remove(event,searchStr);
        }
    }

    private static void greetMe(MessageReceivedEvent event){
        Main.greetings.get(event.getGuild().getLongID()).iGreetMe(event);
    }

    private static void add(MessageReceivedEvent event, List<String> args){
        if(hasPermission(event, EnumSet.of(Permissions.MANAGE_SERVER))){
            String searchStr = String.join(" ",args);
            Main.greetings.get(event.getGuild().getLongID()).add(event,searchStr);
        }
    }

    private static File guildFile(Long id) throws IOException{
        log(null,"Reading sfx ini for guild: "+id, INFO);
        File greetDir = new File("sfx");
        if(!greetDir.exists()){
            log(null,"Folder doesn't exist, creating folder...", INFO);
            boolean dirMade = greetDir.mkdirs();
            if(!dirMade) {
                log(null, "Couldn't create folder", ERROR);
                throw new IOException("Could't create folder");
            }
            log(null,"Folder created", SUCC);
        }
        File greetFile = new File("sfx/"+id+".ini");
        if(!greetFile.exists()){
            log(null,"File doesn't exist, creating file...", INFO);
            boolean fileMade = greetFile.createNewFile();
            if(!fileMade) {
                log(null, "Couldn't create file", ERROR);
                throw new IOException("Could't create folder");
            }
            log(null,"File created", SUCC);
        }
        return greetFile;
    }

    private static boolean canGreet(GuildEvent event){
        return Main.serverSettings.get(event.getGuild().getLongID()).allowsGreetings();
    }

    /*------------------------------------------------------------------------------*/

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
        File greetingsFile = new File("sfx/"+this.id);
        if(!greetingsFile.exists()){
            if(!greetingsFile.createNewFile()){
                throw new IOException("Couldn't create sfx list file");
            }
        }
        try(BufferedReader br = new BufferedReader(new FileReader(greetingsFile))){
            for(String line; (line = br.readLine())!=null; ){
                greetings.add(line);
            }
        }
    }

    private void iGreetMe(MessageReceivedEvent event) {
        log(event.getGuild(),"Toggling "+event.getAuthor()+"'s status", INFO);
        String id = Long.toString(event.getAuthor().getLongID());
        boolean status;
        if(iniFile.containsKey(id) && iniFile.get(id).containsKey("enabled")){
            status = iniFile.get(id,"enabled",boolean.class);
        }else{
            status = false;
        }
        iniFile.put(id,"enabled",!status);
        sendMessage(event.getChannel(),
                new EmbedBuilder().withTitle(!status?":white_check_mark: Greetings enabled!":":x: Greetings disabled!").build(),
                -1,true);
        try {
            iniLock.writeLock().lock();
            iniFile.store();
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            iniLock.writeLock().unlock();
            log(event.getGuild(),"Toggle unlocked", INFO);
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
            log(event.getGuild(),"No greeting by the name: "+searchStr, ERROR);
            return;
        }
        audioP.clear();

        vChannel.join();
        try {
            audioP.queue(songDir[0]);
        } catch (IOException e) {
            log(event.getGuild(),"Greet failed: "+e.getMessage(), ERROR);
            vChannel.leave();
        } catch (UnsupportedAudioFileException e) {
            log(event.getGuild(),"Greet failed: "+e.getMessage(), ERROR);
            vChannel.leave();
            e.printStackTrace();
        }
    }

    private boolean isGreetable(Long longID) {
        if(iniFile.containsKey(longID.toString())){
            return iniFile.get(longID.toString()).get("enabled",boolean.class);
        }
        return false;
    }

    private void add(MessageReceivedEvent event, String searchStr){
        if(searchStr.length()==0){
            sendMessage(event.getChannel(),"I need to know what you want to add. Use `|sfx <list` to know what sounds you can add",120,false);
            return;
        }
        File[] songDir = SfxModule.songsDir(event, file -> file.getName().toUpperCase().contains(searchStr));
        log(event.getGuild(),"Songs that match: "+ Arrays.toString(songDir), INFO);
        if (songDir == null || songDir.length == 0) {
            sendMessage(event.getChannel(), "No files in the sfx folder match your query", 120, false);
            return;
        }
        if(songDir.length>1){
            sendMessage(event.getChannel(),"More than one file matches your query",120,false);
            return;
        }
        if(this.greetings.stream().filter(s -> s.contains(songDir[0].getName())).count()>0){
            sendMessage(event.getChannel(),"Greeting already added",120,false);
            return;
        }
        greetings.add(songDir[0].getName());
        log(event.getGuild(),"Updating file", INFO);
        updateFile();
        sendMessage(event.getChannel(),"Greeting added",120,false);
    }

    private void remove(MessageReceivedEvent event, String searchStr) {
        if(searchStr.length()==0){
            sendMessage(event.getChannel(),"I need to know what you want to remove. Use `|greet list` to know what sounds you can remove",120,false);
            return;
        }
        List<String> gList = this.greetings.stream().map(String::toUpperCase).filter(s -> s.contains(searchStr)).collect(Collectors.toList());
        if(gList.size()==0){
            sendMessage(event.getChannel(),"No Greeting with that name",120,false);
        }else if(gList.size()>1){
            sendMessage(event.getChannel(),"More then one greeting matches that name",120,false);
        }else{
            this.greetings = this.greetings.stream().filter(s -> !s.toUpperCase().contains(searchStr)).collect(Collectors.toList());
            log(event.getGuild(),"List of sfx after removing: "+ this.greetings.toString(), INFO);
            updateFile();
            sendMessage(event.getChannel(),"Greeting removed",120,false);
        }
    }

    private void updateFile(){
        log(guild,"Updating List File", INFO);
        try{
            listLock.writeLock().lock();
            BufferedWriter bw = new BufferedWriter(new FileWriter("sfx/"+id.toString()));
            for (String greeting : greetings) {
                log(guild, "Writing "+greeting+" to file", INFO);
                bw.write(greeting+"\n");
            }
            bw.close();
        }catch (IOException e){
            log(guild,"Couldn't update file", ERROR);
        }finally {
            listLock.writeLock().unlock();
        }
    }

    private void list(IChannel channel) {
        StringBuilder s = new StringBuilder();
        greetings.forEach(g -> s.append(WordUtils.capitalizeFully(g)).append("\n"));
        sendMessage(channel,new EmbedBuilder().withTitle("List of sfx").withDesc(s.toString()).build(),-1,true);
    }
}
