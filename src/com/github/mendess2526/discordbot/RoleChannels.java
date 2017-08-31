package com.github.mendess2526.discordbot;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.RequestBuffer;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class RoleChannels {
    static final String NEW = "NEW";
    static final String DELETE = "DELETE";
    private static final String[] NUMBERS = {":one:",":two:",":three:",":four:",":five:",":six:",":seven:",":eight:",":nine:",":ten:"};

    public static void handle(String[] command, MessageReceivedEvent event){
        switch (command[1]){
            case NEW:
                newChannel(command[2], event.getGuild());
                break;
            case DELETE:
                deleteChannel(command[2], event);
                break;
        }
    }

    private static void deleteChannel(String name, MessageReceivedEvent event) {
        List<IChannel> chList = event.getGuild().getChannelsByName(name);

        if(chList.size()==0) { // No channel with that name
            RequestBuffer.request(() -> event.getChannel().sendMessage("No channel with that name!"));

        }else if(chList.size()>1){ // More then on channel with the same name
            StringBuilder s = new StringBuilder();
            s.append("More then one channel with the same name. Pick one by reacting!\n");
            int count=1;
            for(IChannel c : chList){
                s.append("**").append(count).append(".** #").append(c.getName()).append("\n");
                count++;
            }
            int finalCount = count;
            RequestBuffer.request(() -> {
                IMessage msg = event.getChannel().sendMessage(s.toString());
                for(int i = 0; i< finalCount; i++){
                    int finalI = i;
                    RequestBuffer.request(() -> msg.addReaction(NUMBERS[finalI]));
                }
            });
        }else{ // Only one channel with that name
            List<IRole> rlList = event.getGuild().getRolesByName(name);
            Iterator<IRole> it = rlList.iterator();
            boolean found=false;
            while (!found && it.hasNext()){
                IRole rl = it.next();
                if(chList.get(0).getModifiedPermissions(rl).contains(Permissions.READ_MESSAGES)){
                    rl.delete();
                    found=true;
                }
            }
            if(!found){
                LoggerService.log("Couldn't find role to delete",LoggerService.ERROR);
            }
            chList.get(0).delete();
        }
    }

    private static void newChannel(String name, IGuild guild) {

        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = guild.getEveryoneRole();

        RequestBuffer.request(() -> {
            IRole nRole = guild.createRole();
            nRole.changeName(name);
            nRole.changePermissions(noPermits);
            RequestBuffer.request(() -> {
                IChannel newChannel = guild.createChannel(name);
                newChannel.overrideRolePermissions(everyone,noPermits,readMessages);
                newChannel.overrideRolePermissions(nRole,readMessages,noPermits);
            });
        });
    }
}
