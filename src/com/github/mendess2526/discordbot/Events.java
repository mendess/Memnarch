package com.github.mendess2526.discordbot;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiParser;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.obj.IReaction;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;

public class Events implements IListener {
    private final static String BOT_PREFIX = "|";

    public void handle(Event event) {
        if(event instanceof MessageReceivedEvent){
            handleMessageReceived((MessageReceivedEvent) event);
        }
        if(event instanceof ReactionEvent){
            if(((ReactionEvent) event).getReaction().getUserReacted(event.getClient().getOurUser()) &&
                    !((ReactionEvent) event).getUser().equals(event.getClient().getOurUser())){
                handleReactionEvent((ReactionEvent) event);
            }
        }
    }

    private void handleReactionEvent(ReactionEvent event) {
        if(event.getReaction().getUnicodeEmoji().getAliases().get(0).equals("x")){
            LoggerService.log(event.getUser().getName()+" reacted to the \"no more channels\" message with :"+event.getReaction().getUnicodeEmoji().getAliases().get(0)+":",LoggerService.INFO);
            event.getMessage().delete();
        }else if(event.getMessage().getEmbeds().get(0).getTitle().equals("Select the channel you want to join!")){
            RoleChannels.join(event);
        }else if(event.getMessage().getEmbeds().get(0).getTitle().equals("Select the channel you want to leave!")){
            RoleChannels.leave(event);
        }
    }

    private void handleMessageReceived(MessageReceivedEvent event) {
        String command[] = event.getMessage().getContent().toUpperCase().split("\\s");

        if(event.getMessage().mentionsEveryone() && event.getMessage().getContent().contains("?")){
            RequestBuffer.request(() -> event.getMessage().addReaction(":white_check_mark:")).get();
            RequestBuffer.request(() -> event.getMessage().addReaction(":x:")).get();
        }
        if(command[0].contains(BOT_PREFIX)){
            LoggerService.log("Command: "+ Arrays.toString(command),LoggerService.INFO);
        }
        if(command[0].equals(BOT_PREFIX + "HI")){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Hello, minion!"));
        }
        if(command[0].equals(BOT_PREFIX + "ROLECHANNEL")){
            LoggerService.log("Executing a rolechannel command",LoggerService.INFO);
            RoleChannels.handle(command, event);
        }
        if(command[0].equals(BOT_PREFIX + "JOIN")){
            RoleChannels.showJoinableChannels(event.getGuild(),event.getChannel(),event.getAuthor());
        }
        if(command[0].equals(BOT_PREFIX + "LEAVE")){
            RoleChannels.showLeavableChannels(event.getGuild(),event.getChannel(),event.getAuthor());
        }
    }


}
