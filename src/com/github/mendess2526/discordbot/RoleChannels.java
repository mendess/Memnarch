package com.github.mendess2526.discordbot;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiParser;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class RoleChannels {
    static final String NEW = "NEW";
    static final String DELETE = "DELETE";

    static final String[] NUMBERS = {":one:",":two:",":three:",":four:",":five:",":six:"};

    public static void handle(String[] command, MessageReceivedEvent event){
        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.MANAGE_CHANNELS)){
            RequestBuffer.request(() -> event.getChannel().sendMessage("You don't have permission to use that command :("));
        }else{
            switch (command[1]){
                case NEW:
                    IChannel ch = newChannel(command[2], event.getGuild());
                    if(ch!=null){
                        LoggerService.log("Channel "+ch.getName()+" created successfully!",LoggerService.SUCC);
                        RequestBuffer.request(() -> event.getChannel().sendMessage("Channel "+ch.getName()+" created successfully!"));
                    }else{
                        LoggerService.log("Couldn't create channel",LoggerService.ERROR);
                        RequestBuffer.request(() -> event.getChannel().sendMessage("Couldn't create channel"));
                    }
                    break;
                case DELETE:
                    deleteChannel(command[2], event);
                    break;
            }
        }
    }

    private static void deleteChannel(String name, MessageReceivedEvent event) {
        LoggerService.log("Getting channels to delete",LoggerService.INFO);
        IChannel ch;
        long id;
        try {
            id = Long.parseLong(name.replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Use `#` to specify what channel you want to delete."));
            return;
        }
        ch = event.getGuild().getChannelByID(id);
        LoggerService.log("Name of the channel to delete "+ch.getName(),LoggerService.INFO);
        RequestBuffer.request(ch::delete);
    }

    private static IChannel newChannel(String name, IGuild guild) {
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = guild.getEveryoneRole();

        IChannel ch;
        ch = RequestBuffer.request(() -> {
            try{
                return guild.createChannel(name);
            }catch (MissingPermissionsException e){
                return null;
            }
        }).get();
        if(ch==null){
            return null;
        }
        boolean success = false;
        int count = 0;
        while (!success && count<10){
            RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,noPermits,readMessages)).get();
            success = !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES);
            if(success){
                LoggerService.log("Changed permissions of @everyone for channel "+ch.getName(),LoggerService.SUCC);
                count=0;
            }else{
                count++;
                LoggerService.log("Couldn't change permissions of @everyone for channel "+ch.getName()+"\tError count: "+count,LoggerService.ERROR);
            }
        }
        success=false;
        while (!success && count<10){
            String topic = "PRIVATE CHANNEL: "+name;
            RequestBuffer.request(() -> ch.changeTopic(topic)).get();
            success = topic.equals(ch.getTopic());
            if(success){
                LoggerService.log("Changed topic of channel "+ch.getName(),LoggerService.SUCC);
                count=0;
            }else{
                count++;
                LoggerService.log("Couldn't change topic for channel "+ch.getName()+"\tError count: "+count,LoggerService.ERROR);
            }
        }
        if(count==10){
            ch.delete();
        }
        return ch;
    }

    private static List<IChannel> joinableChannels(IGuild guild, IUser user){
        List<IChannel> chList = guild.getChannels();
        LoggerService.log("Channel list: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()),LoggerService.INFO);
        Iterator<IChannel> it = chList.iterator();

        // Filter out channels that can't be joined
        while (it.hasNext()){
            IChannel c = it.next();
            LoggerService.log("Channel name: "+c.getName(),LoggerService.INFO);
            String[] topic;

            try{
                topic = c.getTopic().split(":");
            }catch (NullPointerException e){
                topic = new String[]{"NOT"};
            }
            LoggerService.log("Channel topic "+ Arrays.toString(topic),LoggerService.INFO);
            if(c.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES)){
                LoggerService.log("removed by the 1st if",LoggerService.INFO);
                it.remove();
            }else if(!topic[0].equals("PRIVATE CHANNEL")) {
                LoggerService.log("removed by the 2nd if",LoggerService.INFO);
                it.remove();
            }
            LoggerService.log("Channel list: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()),LoggerService.INFO);
        }
        return chList;
    }
    private static EmbedObject joinableChannelsEmbed(List<IChannel> chList, int currentPage) {
        EmbedBuilder e = new EmbedBuilder();
        int count = 1;
        if(chList.size()==0){
            e.withTitle("No more channels to join. You're EVERYWHERE!");
            return e.build();
        }else{
            // Make a sub list of channels, i.e. a page.
            LoggerService.log("List size: "+chList.size(),LoggerService.INFO);
            LoggerService.log("Channel list: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()),LoggerService.INFO);
            int from = currentPage*6;
            int to = from+6 > chList.size() ? chList.size() : from+6;
            LoggerService.log("Making sub list. From: "+from+" To: "+to,LoggerService.INFO);
            List<IChannel> page = chList.subList(from,to);
            LoggerService.log(page.toString(),LoggerService.INFO);

            e.withTitle("Select the channel you want to join!");
            // Print the list to the Embed
            for(IChannel c : page){
                e.appendDesc("**"+count+":** "+ c.getName()+"\n");
                count++;
            }
            // Print the page count
            int numPages = chList.size()/6;
            e.withFooterText("Page "+(currentPage+1)+"/"+(numPages+1));

            return e.build();
        }
    }

    public static void showJoinableChannels(IGuild guild, IChannel channel, IUser user) {
        List<IChannel> chList = joinableChannels(guild,user);
        EmbedObject e = joinableChannelsEmbed(chList,0);
        IMessage msg;
        msg = RequestBuffer.request(() -> {
            return channel.sendMessage(user.mention(),e);
        }).get();
        if(chList.size()!=0){
            for(int i=0;i<chList.size() && i<6;i++){
                int finalI = i;
                RequestBuffer.request(() -> msg.addReaction(NUMBERS[finalI])).get();
            }
            int numPages = chList.size()/6;
            if(numPages!=0){
                RequestBuffer.request(() -> msg.addReaction(":arrow_forward:")).get();
            }
        }
    }

    private static void editJoinableChannels(IMessage message, IUser user, int page) {
        List<IChannel> chList = joinableChannels(message.getGuild(),user);
        EmbedObject e = joinableChannelsEmbed(chList,page);

        IMessage msg;
        msg = RequestBuffer.request(() -> {
            return message.edit(user.mention(),e);
        }).get();
        RequestBuffer.request(message::removeAllReactions).get();
        if(page!=0){
            RequestBuffer.request(() -> msg.addReaction(":arrow_backward:")).get();
        }
        boolean isLastPage = page==chList.size()/6;
        int count = isLastPage ? chList.size()%6 : 6;

        for(int i=0;i<count;i++){
            int finalI = i;
            RequestBuffer.request(() -> msg.addReaction(NUMBERS[finalI])).get();
        }
        if(!isLastPage){
            RequestBuffer.request(() -> msg.addReaction(":arrow_forward:")).get();
        }
    }

    public static void join(ReactionEvent event) {
        if(event.getMessage().getMentions().get(0).equals(event.getUser())){
            LoggerService.log("ToString() "+ event.getReaction().getUnicodeEmoji(),LoggerService.INFO);
            String opt = event.getReaction().getUnicodeEmoji().getAliases().get(0);
            if(opt.equals("arrow_backward")){
                LoggerService.log(event.getMessage().getEmbeds().get(0).getFooter().getText(),LoggerService.INFO);
                String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                int page = Integer.parseInt(pageStr) - 1;
                editJoinableChannels(event.getMessage(),event.getUser(),page - 1);
            }else if(opt.equals("arrow_forward")){
                LoggerService.log(event.getMessage().getEmbeds().get(0).getFooter().getText(),LoggerService.INFO);
                String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                int page = Integer.parseInt(pageStr) - 1;
                editJoinableChannels(event.getMessage(),event.getUser(),page + 1);
            }
        }
    }
}
