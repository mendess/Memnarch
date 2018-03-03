package com.github.mendess2526.memnarch.misc;

import com.github.mendess2526.memnarch.BotUtils;
import com.github.mendess2526.memnarch.Command;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.mendess2526.memnarch.BotUtils.sendMessage;
import static com.github.mendess2526.memnarch.LoggerService.*;

public class CustomCommands {

    static abstract class CCustomCommands implements Command {

        @Override
        public String getCommandGroup(){
            return "Custom Commands";
        }

        @Override
        public Set<Permissions> getPermissions(){
            return EnumSet.of(Permissions.MANAGE_CHANNELS);
        }

    }
    private static Map<String,JSONObject> guilds = new HashMap<>();
    private static JSONObject root;
    private static File jsonFile;

    private static final String CUSTOM_COMMANDS_FILE = BotUtils.DEFAULT_FILE_PATH+"costumeCommands.json";
    private static final Map<String,Command> commandMap = new HashMap<>();
    static {
        commandMap.put("NEW", new CCustomCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                newCommand(event,args);
            }
        });
        commandMap.put("REMOVE", new CCustomCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                removeCommand(event,args);
            }
        });
    }
    private static final Set<Permissions> permissions = EnumSet.of(Permissions.MANAGE_CHANNELS);

    public static void initializeGuild(IGuild guild){
        log(guild,"Initializing Custom Commands",INFO);
        if(jsonFile == null){
            jsonFile = new File(CUSTOM_COMMANDS_FILE);
            try{
                if(!jsonFile.exists()){
                    if(!jsonFile.createNewFile()){
                        log(guild,"Couldn't create "+CUSTOM_COMMANDS_FILE,ERROR);
                    }else {
                        FileWriter fw = new FileWriter(jsonFile);
                        fw.write("{}");
                        fw.flush();
                        fw.close();
                    }
                }
            }catch(IOException e){
                log(guild,e,"CustomCommands#initializeGuild");
                return;
            }
        }
        if(root == null){
            try{
                root = new JSONObject(new JSONTokener(new FileReader(jsonFile)));
            }catch(FileNotFoundException e){
                log(guild,e,"CustomCommands#initializeGuild");
                return;
            }catch(JSONException e){
                root = new JSONObject(new JSONTokener("{}"));
            }

        }
        if(!root.has(String.valueOf(guild.getLongID()))){
            root.put(String.valueOf(guild.getLongID()),new HashMap<>());
        }
        guilds.put(String.valueOf(guild.getLongID()),root.getJSONObject(String.valueOf(guild.getLongID())));
        log(guild,"Custom Commands initialized",SUCC);
    }

    public static void handle(MessageReceivedEvent event, List<String> args){
        if(BotUtils.hasPermission(event,permissions, true)) {
            if (args.size() == 0 || !commandMap.containsKey(args.get(0).toUpperCase())) {
                if (args.size() != 0) log(event.getGuild(), "Invalid Argument: " + args.get(0), INFO);
                HashMap<String, Set<String>> cmds = new HashMap<>();
                cmds.put("CustomCommands", commandMap.entrySet()
                        .stream()
                        .filter(kv -> BotUtils.hasPermission(event,
                                kv.getValue().getPermissions(),
                                false))
                        .map(Map.Entry::getKey).collect(Collectors.toSet()));
                BotUtils.help(event.getAuthor(), event.getChannel(), cmds);
            } else {
                log(event.getGuild(), "Valid Argument: " + args.get(0).toUpperCase(), INFO);
                Command command = commandMap.get(args.get(0).toUpperCase());
                if(BotUtils.hasPermission(event,command.getPermissions(), true))
                    command.runCommand(event, args.subList(1, args.size()));
            }
        }
    }

    public static boolean invoke(MessageReceivedEvent event, String cmd){
        JSONObject guildCCs = guilds.get(String.valueOf(event.getGuild().getLongID()));
        if(guildCCs!=null){
            if(guildCCs.has(cmd)){
                sendMessage(event.getChannel(),guildCCs.getString(cmd),-1,true);
                return true;
            }
        }
        return false;
    }

    private static void newCommand(MessageReceivedEvent event, List<String> args){
        if(args.size() < 2){
            sendMessage(event.getChannel(),"Use: <Command_name> <Command_output>",30,false);
            return;
        }
        if(!guilds.containsKey(String.valueOf(event.getGuild().getLongID()))){
            log(event.getGuild(),"Guild Costume Commands not initialized",ERROR);
            return;
        }
        JSONObject guildCCs = guilds.get(String.valueOf(event.getGuild().getLongID()));
        guildCCs.put(args.get(0).toUpperCase(),args.get(1));
        log(event.getGuild(),guildCCs.toString(),INFO);
        try{
            updateJsonFile(String.valueOf(event.getGuild().getLongID()),guildCCs);
        }catch(IOException e){
            log(event.getGuild(),e,"CustomCommands#newCommand");
        }
    }

    private static void removeCommand(MessageReceivedEvent event, List<String> args){
        if(!guilds.containsKey(String.valueOf(event.getGuild().getLongID()))){
            log(event.getGuild(),"Guild Costume Commands not initialized",ERROR);
            return;
        }
        JSONObject guildCCs = guilds.get(String.valueOf(event.getGuild().getLongID()));
        args.add(0,args.get(0).toUpperCase());
        if(guildCCs != null){
            if(guildCCs.has(args.get(0))){
                String key = args.get(0);
                String value = guildCCs.getString(args.get(0));
                guildCCs.remove(args.get(0));
                try{
                    updateJsonFile(String.valueOf(event.getGuild().getLongID()),guildCCs);
                    sendMessage(event.getChannel(),"Command "+key+" -> "+value+" removed",-1,true);
                }catch(IOException e){
                    log(event.getGuild(),e,"CustomCommands#removeCommand");
                }
            }else{
                sendMessage(event.getChannel(),"No such command",30,false);
            }
        }
    }

    private static void updateJsonFile(String key, JSONObject jsonObject) throws IOException{
        root.put(key,jsonObject);
        FileWriter fw = new FileWriter(jsonFile);
        fw.write(root.toString());
        fw.flush();
        fw.close();
    }
}
