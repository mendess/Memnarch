package com.github.mendess2526.discordbot;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.util.RequestBuffer;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class Events {
    public final static String BOT_PREFIX = "|";
    public static Map<String, Command> commandMap = new HashMap<>();

    static {
        commandMap.put("HELP", MiscCommands::help);

        commandMap.put("PING", MiscCommands::ping);

        commandMap.put("HI", MiscCommands::hi);

        commandMap.put("ROLECHANNEL", (RoleChannels::handle));

        commandMap.put("JOIN", RoleChannels::showJoinableChannels);

        commandMap.put("LEAVE", RoleChannels::showLeavableChannels);

        commandMap.put("SFX", AudioModule::sfx);

        commandMap.put("SFXLIST", AudioModule::sfxlist);

        commandMap.put("SFXADD", AudioModule::sfxAdd);

        commandMap.put("SFXDELETE", AudioModule::sfxDelete);

    }

    @EventSubscriber
    public void handleReactionEvent(ReactionEvent event) {
        // If it wasn't the bot adding the reaction and it was a reaction added my the bot
        if(event.getReaction().getUserReacted(event.getClient().getOurUser()) &&
                !(event.getUser().equals(event.getClient().getOurUser()))){
            if(event.getReaction().getUnicodeEmoji().getAliases().get(0).equals("x")){
                LoggerService.log(event.getUser().getName()+" reacted to the \"no more channels\" message with :"+event.getReaction().getUnicodeEmoji().getAliases().get(0)+":",LoggerService.INFO);
                event.getMessage().delete();
            }else if(event.getMessage().getEmbeds().get(0).getTitle().equals("Select the channel you want to join!")){
                RoleChannels.join(event);
            }else if(event.getMessage().getEmbeds().get(0).getTitle().equals("Select the channel you want to leave!")){
                RoleChannels.leave(event);
            }
        }
    }
    @EventSubscriber
    public void handleMessageReceived(MessageReceivedEvent event) {
        // When @everyone is tagged with a ? in the same message
        if(event.getMessage().mentionsEveryone() && event.getMessage().getContent().contains("?")){
            RequestBuffer.request(() -> event.getMessage().addReaction(":white_check_mark:")).get();
            RequestBuffer.request(() -> event.getMessage().addReaction(":x:")).get();
            return;
        }

        String command[] = event.getMessage().getContent().toUpperCase().split("\\s");

        if(command.length == 0){
            return;
        }
        if(!command[0].startsWith(BOT_PREFIX)){
            return;
        }else{
            LoggerService.log("Command: "+ Arrays.toString(command),LoggerService.INFO);
        }
        String cmd = command[0].substring(1);

        List<String> args = new ArrayList<>(Arrays.asList(command));
        args.remove(0);

        if(commandMap.containsKey(cmd)){
            LoggerService.log("Valid command: "+cmd, LoggerService.INFO);
            commandMap.get(cmd).runCommand(event,args);
        }else{
            LoggerService.log("Invalid command "+cmd, LoggerService.INFO);
        }
    }
}
