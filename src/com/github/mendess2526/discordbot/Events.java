package com.github.mendess2526.discordbot;

import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.RequestBuffer;

public class Events implements IListener {

    public void handle(Event event) {
        if(event instanceof MessageReceivedEvent){
            handleMessageReceived((MessageReceivedEvent) event);
        }
    }
    private void handleMessageReceived(MessageReceivedEvent event) {
        String command[] = event.getMessage().getContent().toUpperCase().split("\\s");

        if(command[0].equals("!HI")){
            RequestBuffer.request(() -> event.getChannel().sendMessage(command[0]));
        }
        if(command[0].equals("!ROLECHANNEL")){
            RoleChannels.handle(command, event);
        }
    }


}
