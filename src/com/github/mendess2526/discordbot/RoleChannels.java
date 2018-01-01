package com.github.mendess2526.discordbot;

import com.vdurmont.emoji.EmojiParser;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelUpdateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.mendess2526.discordbot.BotUtils.*;
import static com.github.mendess2526.discordbot.LoggerService.*;

public class RoleChannels {

    private static final int JOIN = 0;
    private static final int LEAVE = 1;
    // UNICODE
    private static final String ARROW_BACK = "\u2B05";
    private static final String ARROW_FORW = "\u27A1";
    private static final String ONE = "1⃣", TWO = "2⃣", THREE = "3⃣", FOUR = "4⃣", FIVE = "5⃣", SIX = "6⃣";
    private static final String[] NUMBERS = {ONE,TWO,THREE,FOUR,FIVE,SIX};

    static final String TITLE_INIT_QUERY_J = "Do you want to join a category or individual channels?";
    static final String TITLE_INIT_QUERY_L = "Do you want to leave a category or individual channels?";
    static final String TITLE_CH_QUERY_J = "Select the channel you want to join!";
    static final String TITLE_CH_QUERY_L = "Select the channel you want to leave!";
    static final String TITLE_CT_QUERY_J = "Select the category you want to join!";
    static final String TITLE_CT_QUERY_L = "Select the category you want to leave!";
    private static final String[] TITLE_EMPTY_CHLIST = {"No more channels to join. You're EVERYWHERE!", "No more channels to leave."};
    private static final String[] TITLE_INIT_QUERY = {TITLE_INIT_QUERY_J,TITLE_INIT_QUERY_L};
    private static final String[] TITLE_CH_QUERY = {TITLE_CH_QUERY_J,TITLE_CH_QUERY_L};
    private static final String[] TITLE_CT_QUERY = {TITLE_CT_QUERY_J,TITLE_CT_QUERY_L};

    private static final Set<Permissions> permissions = EnumSet.of(Permissions.MANAGE_CHANNELS);

    private static final Map<String,Command> commandMap = new HashMap<>();
    private static final String PRIVATE_MARKER = ">";

    static {
        // Creates a new text channel of the given name
        commandMap.put("NEW", RoleChannels::newChannel);
        // Deletes the text channel of the given name
        commandMap.put("DELETE", RoleChannels::deleteChannel);
        // Sets all text channels as private
        commandMap.put("SETALL", RoleChannels::setAll);
        // Sets the given channel as private
        commandMap.put("SET", RoleChannels::set);
        // Sets the given channel as public
        commandMap.put("UNSET", RoleChannels::unSet);
        // Converts the private channel markers for all private channels
        commandMap.put("CONVERT", RoleChannels::convert);
    }

    
    // Managing Channels
    public static void handle(MessageReceivedEvent event, List<String> args){
        if(hasPermission(event,permissions)) {
            if (args.size() == 0 || !commandMap.containsKey(args.get(0).toUpperCase())) {
                if (args.size() != 0) log(event.getGuild(), "Invalid Argument: " + args.get(0), INFO);
                HashMap<String, Set<String>> cmds = new HashMap<>();
                cmds.put("RoleChannels", commandMap.keySet());
                help(event.getAuthor(), event.getChannel(), cmds);
            } else {
                log(event.getGuild(), "Valid Argument: " + args.get(0).toUpperCase(), INFO);
                commandMap.get(args.get(0).toUpperCase()).runCommand(event, args.subList(1, args.size()));
            }
        }
    }

    private static void newChannel(MessageReceivedEvent event, List<String> args) {
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IGuild guild = event.getGuild();
        IChannel ch;
        IRole everyone = guild.getEveryoneRole();
        IUser ourUser = guild.getClient().getOurUser();
        String name = String.join("-",args);

        if(name.length()<3){
            log(event.getGuild(),"Couldn't create channel", ERROR);
            sendMessage(event.getChannel(),"Couldn't create channel, channel name must have more then 2 characters",120,false);
            return;
        }
        // Attempt to create channel
        ch = RequestBuffer.request(() -> {
            try{
                return guild.createChannel(name);
            }catch (MissingPermissionsException e) {
                log(event.getGuild(), "Couldn't create channel. Missing Permissions: " + e.getMissingPermissions(), ERROR);
                return null;
            }catch (DiscordException e){
                log(event.getGuild(),"Couldn't create channel. Error Message: "+e.getErrorMessage(), ERROR);
                return null;
            }
        }).get();
        if(ch==null){
            sendMessage(event.getChannel(),"Couldn't create channel, maybe I'm missing permissions?",120,false);
            return;
        }

        // Attempt to change permissions
        boolean success;
        RequestBuffer.request(() -> ch.overrideUserPermissions(ourUser,readMessages,noPermits));
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,noPermits,readMessages));
        try {
            guild.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                            !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES)
                            && ch.getModifiedPermissions(ourUser).contains(Permissions.READ_MESSAGES),10,TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            log(event.getGuild(),"Interrupted Exception thrown when waiting for "+ch.getName()+"'s permissions to be changed.", ERROR);
            e.printStackTrace();
        }
        success = !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES);
        if(!success){
            log(event.getGuild(),"Couldn't change permissions for "+ch.getName()+".", ERROR);
            sendMessage(event.getChannel(),
                    "Couldn't change permissions for channel"+ch.getName()+". Deleting channel",120,false);
            ch.delete();
            return;
        }
        log(event.getGuild(),"Changed permissions for "+ch.getName()+".", INFO);

        // Attempt to change topic
        String topic = PRIVATE_MARKER+ch.getName();
        RequestBuffer.request(() -> ch.changeTopic(topic));
        try{
            guild.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) -> topic.equals(ch.getTopic()),10,TimeUnit.SECONDS);
        }catch (InterruptedException e){
            log(event.getGuild(),"Interrupted Exception thrown when waiting for "+ch.getName()+"'s topic to be changed.", ERROR);
            e.printStackTrace();
        }
        success = topic.equals(ch.getTopic());
        if(!success){
            log(event.getGuild(),"Couldn't change topic for channel "+ch.getName()+". Deleting channel", ERROR);
            sendMessage(event.getChannel(),
                    "Couldn't change topic for channel "+ch.getName()+". Deleting channel",120,false);
            ch.delete();
        }
        log(event.getGuild(),"Changed topic of channel "+ch.getName(), INFO);
        log(event.getGuild(),"Channel "+ch.getName()+" created successfully!", SUCC);
        sendMessage(event.getChannel(),
                "Channel "+ch.mention()+" created successfully!",-1,false);
    }

    private static void deleteChannel(MessageReceivedEvent event, List<String> args) {
        if(noArgs(event.getChannel(),args))return;
        IChannel ch;
        long id;
        try {
            id = Long.parseLong(args.get(0).replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            log(event.getGuild(),"Bad channel id: "+args.get(0), UERROR);
            sendMessage(event.getChannel(),"Use `#` to specify what channel you want to delete.",120,false);
            return;
        }
        ch = event.getGuild().getChannelByID(id);
        String topic = EmojiParser.parseToAliases(ch.getTopic());
        if(topic.startsWith(PRIVATE_MARKER)){
            log(event.getGuild(),"Name of the channel to delete: "+ch.getName(), INFO);
            RequestBuffer.request(ch::delete);
            try {
                event.getClient().getDispatcher().waitFor((ChannelDeleteEvent e) -> event.getGuild().getChannelByID(id)==null,10,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log(event.getGuild(),"Interrupted Exception thrown when waiting for channel to be deleted.", ERROR);
                e.printStackTrace();
            }
            sendMessage(event.getChannel(),
                    event.getGuild().getChannelByID(id)==null ? "Channel deleted successfully" : "Couldn't delete channel",
                    120,false);
        }else {
            log(event.getGuild(),ch.getName()+" is not a Role Channel", INFO);
            sendMessage(event.getChannel(),ch.getName()+" is not a Private Channel, I can't delete it",120,false);
        }
    }

    @SuppressWarnings("unused")
    private static void setAll(MessageReceivedEvent event, List<String> args) {
        List<IChannel> chList = event.getGuild().getChannels();
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        chList.forEach(c -> {
            RequestBuffer.request(() -> c.changeTopic(PRIVATE_MARKER+c.getTopic())).get();
            RequestBuffer.request(() -> c.overrideUserPermissions(event.getGuild().getClient().getOurUser(),readMessages,noPermits)).get();
            sendMessage(event.getChannel(),"Changed topic of "+c.getName(),120,false);
        });
    }

    private static void set(MessageReceivedEvent event, List<String> name){
        if(noArgs(event.getChannel(),name))return;
        long id;
        try {
            id = Long.parseLong(name.get(0).replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            sendMessage(event.getChannel(),"Use `#` to specify what channel you want to delete.",120,false);
            return;
        }
        IChannel ch = event.getGuild().getChannelByID(id);
        log(event.getGuild(),"Channel to set: "+ch.getName(), INFO);

        if(ch.getTopic()!=null && ch.getTopic().startsWith(PRIVATE_MARKER)){
            sendMessage(event.getChannel(),"Already a private channel",120,false);
            return;
        }
        String newTopic = PRIVATE_MARKER + (ch.getTopic()!=null ? ch.getTopic() : "");
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = event.getGuild().getEveryoneRole();
        IUser ourUser = event.getGuild().getClient().getOurUser();
        log(event.getGuild(),String.format("Changing topic from: "+ch.getTopic()+" to: "+newTopic+"\n%27sAnd overriding permissions."," "), INFO);
        RequestBuffer.request(() -> ch.changeTopic(newTopic)).get();
        RequestBuffer.request(() -> ch.overrideUserPermissions(ourUser,readMessages,noPermits)).get();
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,noPermits,readMessages));
        sendMessage(event.getChannel(),ch.getName()+" is now private!",-1,false);
    }

    private static void unSet(MessageReceivedEvent event, List<String> name){
        if(noArgs(event.getChannel(),name))return;
        long id;
        try {
            id = Long.parseLong(name.get(0).replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            sendMessage(event.getChannel(),"Use `#` to specify what channel you want to delete.",120,false);
            return;
        }
        IChannel ch = event.getGuild().getChannelByID(id);
        log(event.getGuild(),"Channel to un-set: "+ch.getName(), INFO);

        if(ch.getTopic()==null || !ch.getTopic().startsWith(PRIVATE_MARKER)){
            sendMessage(event.getChannel(),"Not a private channel",120,false);
            return;
        }
        String newTopic = ch.getTopic().replaceFirst(PRIVATE_MARKER,"");
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = event.getGuild().getEveryoneRole();

        log(event.getGuild(),String.format("Changing topic from: "+ch.getTopic()+" to: "+newTopic+"\n%27sAnd overriding permissions."," "), INFO);
        RequestBuffer.request(() -> ch.changeTopic(newTopic)).get();
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,readMessages,noPermits));
        event.getGuild().getUsers().stream()
                                   .filter(user -> ch.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES))
                                   .forEach(user -> RequestBuffer.request(() -> ch.removePermissionsOverride(user)));
        sendMessage(event.getChannel(),ch.getName()+" is now public!",-1,false);
    }

    private static void convert(MessageReceivedEvent event, List<String> args){
        if(args.size()==0){
            sendMessage(event.getChannel(),"Please provide the old private channel marker",30,false);
            return;
        }
        args.remove(0);
        String oldMarker = String.join(" ", args);
        log(event.getGuild(),"Converting "+oldMarker+" to "+PRIVATE_MARKER, INFO);
        List<IChannel> chList = event.getGuild().getChannels();
        chList.stream()
                .filter(c -> c.getTopic()!=null && c.getTopic().startsWith(oldMarker))
                .forEach(c -> {
                    log(event.getGuild(),"Converting "+c.getName(), INFO);
                    String nTopic = c.getTopic().replace(oldMarker,PRIVATE_MARKER);
                    RequestBuffer.request(() -> c.changeTopic(nTopic));
                    try {
                        c.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) -> c.getTopic().startsWith(PRIVATE_MARKER),5,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        log(event.getGuild(),"Interrupted when converting channel "+c.getName(), ERROR);
                        e.printStackTrace();
                    }
                    if(!c.getTopic().startsWith(PRIVATE_MARKER)){
                        sendMessage(event.getChannel(),"Couldn't convert "+c.getName()+". Try to rerun the command",120,false);
                    }
                });
    }

    // Join
    @SuppressWarnings("unused")
    static void startJoinUI(MessageReceivedEvent event, List<String> args) {
        if(hasChannelsInCategories(event.getGuild(), event.getAuthor(), true)){
            createInitialQuery(event,true);
        }else{
            createChannelList(event,true);
        }
    }

    private static boolean addUser2Channel(ReactionEvent event, IChannel ch, IUser user) {
        RequestBuffer.request(() -> ch.overrideUserPermissions(user,EnumSet.of(Permissions.READ_MESSAGES),EnumSet.noneOf(Permissions.class)));
        try {
            event.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                             e.getNewChannel().getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)
                                                      ,10,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log(event.getGuild(),"Interrupted Exception thrown when waiting for "+event.getUser().getName()+"to be added to "+ch.getName()+".", ERROR);
            e.printStackTrace();
        }
        return ch.getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES);
    }

    private static void addUser2Category(ReactionAddEvent event, ICategory ct, IUser user){
        ct.getChannels().stream().filter(privateChannelFilter(true,user)).forEach(c -> addUser2Channel(event,c,user));
    }

    // Leave
    @SuppressWarnings("unused")
    static void startLeaveUI(MessageReceivedEvent event, List<String> args) {
        if(hasChannelsInCategories(event.getGuild(),event.getAuthor(),false)){
            createInitialQuery(event,false);
        }else{
            createChannelList(event,false);
        }
    }

    private static boolean removeUserFromChannel(ReactionEvent event, IChannel ch, IUser user) {
        RequestBuffer.request(() ->ch.removePermissionsOverride(user));
        try {
            event.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                            !e.getNewChannel().getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)
                                                      ,10,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log(event.getGuild(),"Interrupted Exception thrown when waiting for "+event.getUser().getName()+"to be removed from "+ch.getName()+".", ERROR);
            e.printStackTrace();
        }
        return !ch.getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES);
    }

    private static void removeUserFromCategory(ReactionAddEvent event, ICategory ct, IUser user){
        ct.getChannels().stream().filter(privateChannelFilter(false,user)).forEach(c -> removeUserFromChannel(event,c,user));
    }

    // Initial Query
    static void processInitialQuery(ReactionAddEvent event, IUser user, boolean joining){
        RequestBuffer.request(() -> event.getMessage().removeAllReactions());
        switch (event.getReaction().getEmoji().getName()){
            case ONE:
                editCategoryList(event.getMessage(),user,0,joining);
                listReact(event.getMessage(),getCategories(event.getGuild(),user,joining).size());
                break;
            case TWO:
                editChannelList(event.getMessage(),user,0,joining);
                listReact(event.getMessage(),getChannels(event.getGuild(),user,joining).size());
                break;
        }

    }

    private static void createInitialQuery(MessageReceivedEvent event, boolean joining){
        EmbedObject e = initialQueryEmbed(getChannels(event.getGuild(),event.getAuthor(),joining).isEmpty(),joining);
        IMessage msg = sendMessage(event.getChannel(),event.getAuthor().mention(),e,-1,false);
        initialQueryReact(msg);
    }

    private static EmbedObject initialQueryEmbed(boolean channelListIsEmpty, boolean joining){
        EmbedBuilder e = new EmbedBuilder();
        if(channelListIsEmpty){
            e.withTitle(TITLE_EMPTY_CHLIST[joining? 0 : 1]);
            return e.build();
        }
        e.withTitle(TITLE_INIT_QUERY[joining? 0 : 1]);
        e.appendDesc("**1:** Categories\n");
        e.appendDesc("**2:** SingleChannels\n");
        return e.build();
    }

    private static void initialQueryReact(IMessage msg){
        RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(ONE))).get();
        RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(TWO))).get();
        closeButton(msg);
    }

    // Channel List
    static int processChannelQuery(ReactionAddEvent event, boolean joining){
        int size=0;
        // If it was the mentioned user that reacted to a reaction added by the bot
        if(event.getMessage().getMentions().get(0).equals(event.getUser())){
            String opt = event.getReaction().getEmoji().getName();
            log(event.getGuild(),"Selected option: "+ EmojiParser.parseToAliases(opt), INFO);
            switch (opt) {
                case ARROW_BACK: {
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    editChannelList(event.getMessage(), event.getUser(), page - 1,joining);
                    break;
                }
                case ARROW_FORW: {
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    editChannelList(event.getMessage(), event.getUser(), page + 1,joining);
                    break;
                }
                case ONE:
                case TWO:
                case THREE:
                case FOUR:
                case FIVE:
                case SIX: {
                    List<IChannel> chList = getChannels(event.getGuild(), event.getUser(),joining);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    int chNumber = literal2Int(opt);

                    log(event.getGuild(),"Channel list before "+(joining ? "adding" : "removing")+" user: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()), INFO);
                    IChannel ch;
                    try{
                        ch = chList.remove((chNumber - 1) + page * 6);
                    }catch (IndexOutOfBoundsException e){
                        log(event.getGuild(),"Invalid channel number picked", INFO);
                        return chList.size();
                    }
                    boolean success;
                    if(joining){
                        success = addUser2Channel(event, ch, event.getUser());
                    }else{
                        success = removeUserFromChannel(event, ch, event.getUser());
                    }
                    if(!success){
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                new EmbedBuilder().withTitle(":x: Couldn't "+(joining? "join" : "leave") +" channel").build())).get();
                        contactOwner(event,"Couldn't "+(joining ? "add" : "remove") +event.getUser().getName()+" from "+ch.getName());
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        size = chList.size()+1;
                    }else{ size = chList.size();}
                    log(event.getGuild(),"Channel list after "+(joining ? "adding" : "removing")+" user: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()), INFO);

                    editChannelList(event.getMessage(), event.getUser(), page,joining);
                    break;
                }
            }
        }
        return size;
    }

    private static void editChannelList(IMessage message, IUser user, int page, boolean joining) {
        List<IChannel> chList = getChannels(message.getGuild(),user,joining);
        EmbedObject e = channelListEmbed(chList,page,joining? JOIN : LEAVE);
        RequestBuffer.request(() -> message.edit(user.mention(),e)).get();
        if(chList.isEmpty()){RequestBuffer.request(message::removeAllReactions);}
    }

    private static void createChannelList(MessageReceivedEvent event, boolean joining){
        IGuild guild = event.getGuild();
        IChannel channel = event.getChannel();
        IUser user = event.getAuthor();
        List<IChannel> chList = getChannels(guild,user,joining);
        EmbedObject e = channelListEmbed(chList,0,joining ? JOIN : LEAVE);
        IMessage msg = sendMessage(channel,user.mention(),e,-1,false);
        listReact(msg,chList.size());
    }

    private static EmbedObject channelListEmbed(List<IChannel> chList, int currentPage, int mode) {
        EmbedBuilder e = new EmbedBuilder();
        int count = 1;
        if(chList.size()==0){
            e.withTitle(TITLE_EMPTY_CHLIST[mode]);
            return e.build();
        }
        // Make a sub list of channels, i.e. a page.
        log(chList.get(0).getGuild(),"List size: "+chList.size(), INFO);
        log(chList.get(0).getGuild(),"Channel list: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()), INFO);
        int numPages = (chList.size()-1)/6;
        if(currentPage*6 >= chList.size()){currentPage=0;}
        if(currentPage<0){currentPage=numPages;}
        int from = currentPage*6;
        int to = from+6 > chList.size() ? chList.size() : from+6;
        log(chList.get(0).getGuild(),"Making sub list. From: "+from+" To: "+to, INFO);
        List<IChannel> page = chList.subList(from,to);

        e.withTitle(TITLE_CH_QUERY[mode]);
        // Print the list to the Embed
        for(IChannel c : page){
            e.appendDesc("**"+count+":** "+ c.getName()+"\n");
            count++;
        }
        // Print the page count
        e.withFooterText("Page "+(currentPage+1)+"/"+(numPages+1));

        return e.build();
    }

    private static List<IChannel> getChannels(IGuild guild, IUser user, boolean joinable){
        return guild.getChannels().stream().filter(privateChannelFilter(joinable,user))
                                           .collect(Collectors.toList());
    }

    // Category List
    static int processCategoryQuery(ReactionAddEvent event, boolean joining){
        int size = 0;
        // If it was the mentioned user that reacted to a reaction added by the bot
        if(event.getMessage().getMentions().get(0).equals(event.getUser())){

            String opt = event.getReaction().getEmoji().getName();
            log(event.getGuild(),"Selected option: "+ EmojiParser.parseToAliases(opt), INFO);
            switch (opt) {
                case ARROW_BACK: {
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    editCategoryList(event.getMessage(), event.getUser(), page - 1,joining);
                    break;
                }
                case ARROW_FORW: {
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    editCategoryList(event.getMessage(), event.getUser(), page + 1,joining);
                    break;
                }
                case ONE:
                case TWO:
                case THREE:
                case FOUR:
                case FIVE:
                case SIX: {
                    List<ICategory> ctList = getCategories(event.getGuild(), event.getUser(),joining);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    int chNumber = literal2Int(opt);

                    log(event.getGuild(),"Category list before "+(joining ? "adding" : "removing")+" user: "+ Arrays.toString(ctList.stream().map(ICategory::getName).toArray()), INFO);
                    ICategory ct;
                    try{
                        ct = ctList.remove((chNumber - 1) + page * 6);
                    }catch (IndexOutOfBoundsException e){
                        log(event.getGuild(),"Invalid channel number picked", INFO);
                        return ctList.size();
                    }
                    if(joining){
                        addUser2Category(event,ct,event.getUser());
                    }else {
                        removeUserFromCategory(event, ct, event.getUser());
                    }
                    log(event.getGuild(),"Category list before "+(joining ? "adding" : "removing")+" user: "+ Arrays.toString(ctList.stream().map(ICategory::getName).toArray()), INFO);

                    editCategoryList(event.getMessage(), event.getUser(), page,joining);
                    size = ctList.size();
                    break;
                }
            }
        }
        return size;
    }

    private static void editCategoryList(IMessage message, IUser user, int page, boolean joining) {
        List<ICategory> ctList = getCategories(message.getGuild(),user,joining);
        if(ctList.isEmpty()){
            editChannelList(message,user,0,joining);
            RequestBuffer.request(message::removeAllReactions).get();
            listReact(message,getChannels(message.getGuild(),user,joining).size());
        }else{
            EmbedObject e = categoryListEmbed(ctList,page,joining ? JOIN : LEAVE);
            RequestBuffer.request(() -> message.edit(user.mention(),e)).get();
        }
    }

    private static EmbedObject categoryListEmbed(List<ICategory> ctList, int currentPage, int mode){


        EmbedBuilder e = new EmbedBuilder();
        int count = 1;
        // Make a sub list of channels, i.e. a page.
        log(ctList.get(0).getGuild(),"List size: "+ctList.size(), INFO);
        log(ctList.get(0).getGuild(),"Channel list: "+ Arrays.toString(ctList.stream().map(ICategory::getName).toArray()), INFO);
        int numPages = (ctList.size()-1)/6;
        if(currentPage*6 >= ctList.size()){currentPage=0;}
        if(currentPage<0){currentPage=numPages;}
        int from = currentPage*6;
        int to = from+6 > ctList.size() ? ctList.size() : from+6;
        log(ctList.get(0).getGuild(),"Making sub list. From: "+from+" To: "+to, INFO);
        List<ICategory> page = ctList.subList(from,to);

        e.withTitle(TITLE_CT_QUERY[mode]);
        // Print the list to the Embed
        for(ICategory c : page){
            e.appendDesc("**"+count+":** "+ c.getName()+"\n");
            count++;
        }
        // Print the page count
        e.withFooterText("Page "+(currentPage+1)+"/"+(numPages+1));

        return e.build();
    }

    private static List<ICategory> getCategories(IGuild guild, IUser user, boolean joining) {
        return guild.getCategories().stream()
                                    .filter(c -> c.getChannels().stream()//has at least one private channel
                                                                .anyMatch(privateChannelFilter(joining,user)))
                                    .collect(Collectors.toList());
    }

    // Misc
    private static void listReact(IMessage msg, int size){
        if(size>6){
            RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(ARROW_BACK))).get();
            for(int i=0;i<6;i++){
                int finalI = i;
                RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(NUMBERS[finalI]))).get();
            }
            RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(ARROW_FORW))).get();
        }else{
            for(int i=0;i<size;i++){
                int finalI = i;
                RequestBuffer.request(() -> msg.addReaction(ReactionEmoji.of(NUMBERS[finalI]))).get();
            }
        }
        closeButton(msg);
    }

    private static boolean hasChannelsInCategories(IGuild guild, IUser user, boolean joining) {
        return getChannels(guild,user,joining).stream().anyMatch(ch -> ch.getCategory()!=null);
    }

    private static boolean noArgs(IChannel ch, List<String> args){
        if(args.size()==0){
            log(ch.getGuild(),"User didn't provide channel ID", UERROR);
            sendMessage(ch,"Please provide a channel ID",30,false);
            return true;
        }
        return false;
    }

    private static int literal2Int(String literal){
        int number = -1;
        switch (literal){
            case ONE:
                number = 1; break;
            case TWO:
                number = 2; break;
            case THREE:
                number = 3; break;
            case FOUR:
                number = 4; break;
            case FIVE:
                number = 5; break;
            case SIX:
                number = 6; break;
        }
        return number;
    }

    private static Predicate<IChannel> privateChannelFilter(boolean joinable, IUser user){
        return (c -> (c.getTopic() != null && c.getTopic().startsWith(PRIVATE_MARKER) && joinable != c.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES)));
    }
    //TODO For some reason getReactions is not getting an updated reaction
    static void cutSuperfluousReactions(IMessage message, int size) {
        List<String> rList = message.getReactions().stream().map(r -> r.getEmoji().getName()).collect(Collectors.toList());
        log(message.getGuild(),"Reaction List: "+rList.toString(), INFO);
        if(rList.contains(ARROW_BACK)){remReactFromMsg(message,ARROW_BACK);}
        if(rList.contains(ARROW_FORW)){remReactFromMsg(message,ARROW_FORW);}
        for(int i=5; i>=size; i--){
            if(rList.contains(NUMBERS[i])){remReactFromMsg(message,NUMBERS[i]);}
        }
    }

    private static void remReactFromMsg(IMessage msg, String reaction){
        try{
            log(msg.getGuild(),"Removing "+reaction+" from a message", INFO);
            List<IUser> users = msg.getReactionByEmoji(ReactionEmoji.of(reaction)).getUsers();
            users.forEach(u -> RequestBuffer.request(() -> msg.removeReaction(u,ReactionEmoji.of(reaction))).get());
        }catch (NullPointerException e){
            log(msg.getGuild(),"Tried to remove a non-existent reaction", ERROR);
        }catch (Exception e){
            log(msg.getGuild(),e.getClass().getCanonicalName()+":"+e.getMessage(), ERROR);
        }
    }
}
