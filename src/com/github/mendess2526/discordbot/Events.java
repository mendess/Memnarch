package com.github.mendess2526.discordbot;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.audio.events.TrackFinishEvent;
import sx.blah.discord.util.audio.events.TrackStartEvent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.mendess2526.discordbot.LoggerService.*;

public class Events {

    final static String BOT_PREFIX = "|";
    private static final String CHECK_MARK = "\u2705";
    private static final String RED_X = "\u274C";

    private static final Map<String, Command> miscMap = new HashMap<>();
    private static final Map<String, Command> sfxMap = new HashMap<>();
    private static final Map<String, Command> roleChannelsMap = new HashMap<>();
    private static final Map<String, Command> greetingsMap = new HashMap<>();
    private static final Map<String, Command> serverSettingsMap = new HashMap<>();

    static final Map<String, Map<String, Command>> commandMap = new HashMap<>();

    static {
        miscMap.put("HELP", MiscCommands::help);
        miscMap.put("PING", MiscCommands::ping);
        miscMap.put("HI", MiscCommands::hi);
        //noinspection SpellCheckingInspection
        miscMap.put("WHOAREYOU",MiscCommands::whoAreYou);
        miscMap.put("SHUTDOWN",MiscCommands::shutdown);
        //noinspection SpellCheckingInspection
        miscMap.put("RRANK",MiscTasks::rRank);
        //noinspection SpellCheckingInspection
        roleChannelsMap.put("ROLECHANNEL", (RoleChannels::handle));
        roleChannelsMap.put("JOIN", RoleChannels::startJoinUI);
        roleChannelsMap.put("LEAVE", RoleChannels::startLeaveUI);

        sfxMap.put("SFX", SfxModule::sfx);
        //noinspection SpellCheckingInspection
        sfxMap.put("SFXLIST", SfxModule::sfxList);

        greetingsMap.put("GREET",Greetings::greetings);
        //noinspection SpellCheckingInspection
        serverSettingsMap.put("SERVERSET",ServerSettings::serverSettings);

        commandMap.put("Miscellaneous",miscMap);
        commandMap.put("Sfx",sfxMap);
        commandMap.put("RoleChannels", roleChannelsMap);
        commandMap.put("Greetings",greetingsMap);
        commandMap.put("Server Settings",serverSettingsMap);

    }

    @EventSubscriber
    public void guildJoin(GuildCreateEvent event){
        Main.initialiseGreetings(event.getGuild());
        Main.initialiseServerSettings(event.getGuild());
    }
    //TODO refactor this according to MVC
    @EventSubscriber
    public void reactionEvent(ReactionAddEvent event) {
        // If it wasn't the bot adding the reaction and it was a reaction added my the bot
        if(event.getReaction().getUserReacted(event.getClient().getOurUser()) &&
                !(event.getUser().equals(event.getClient().getOurUser()))){

            List<IUser> mentions = event.getMessage().getMentions();
            if(mentions.isEmpty() || mentions.contains(event.getUser())){
                if(event.getReaction().getEmoji().getName().equals(BotUtils.X)){
                    log(event.getGuild(),event.getUser().getName()+" clicked an :heavy_multiplication_x:", INFO);
                    event.getMessage().delete();
                }else{
                    int size = 0;
                    switch (event.getMessage().getEmbeds().get(0).getTitle()){
                        case RoleChannels.TITLE_INIT_QUERY_J:
                            RoleChannels.processInitialQuery(event,event.getUser(),true);
                            break;
                        case RoleChannels.TITLE_INIT_QUERY_L:
                            RoleChannels.processInitialQuery(event,event.getUser(),false);
                            break;
                        case RoleChannels.TITLE_CH_QUERY_J:
                            size = RoleChannels.processChannelQuery(event,true);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                        case RoleChannels.TITLE_CH_QUERY_L:
                            size = RoleChannels.processChannelQuery(event,false);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                        case RoleChannels.TITLE_CT_QUERY_J:
                            size = RoleChannels.processCategoryQuery(event,true);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                        case RoleChannels.TITLE_CT_QUERY_L:
                            size = RoleChannels.processCategoryQuery(event,false);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                    }
                    if(size>0 && size<7){RoleChannels.cutSuperfluousReactions(event.getMessage(),size);}
                }
            }
        }
        if(!event.getReaction().getUserReacted(event.getClient().getOurUser())){
            if(event.getReaction().getEmoji().getName().equals(BotUtils.R)){
                MiscTasks.handleR(event);
            }
        }
    }

    @EventSubscriber
    public void userGuildJoin(UserJoinEvent event){
        if(Main.serverSettings.get(event.getGuild().getLongID()).messagesNewUsers()){
            String msg = Main.serverSettings.get(event.getGuild().getLongID()).getNewUserMessage();
            event.getUser().getOrCreatePMChannel().sendMessage(msg);
        }
    }
    /*
    @EventSubscriber
    public void userVoiceJoin(UserVoiceChannelJoinEvent event) throws InterruptedException {
        if(Greetings.canGreet(event)){
            TimeUnit.MILLISECONDS.sleep(500);
            if(event.getUser().isBot()){
                return;
            }
            if(Main.greetings.get(event.getGuild().getLongID()).isGreetable(event.getUser().getLongID())){
                Main.greetings.get(event.getGuild().getLongID()).greet(event);
                return;
            }
            Random rand = new Random();
            int randomNum = rand.nextInt(128);
            if(randomNum<2){
                LoggerService.log(event.getGuild(),"Random number: "+randomNum,LoggerService.SUCC);
                Main.greetings.get(event.getGuild().getLongID()).greet(event);
            }else{
                LoggerService.log(event.getGuild(),"Random number: "+randomNum,LoggerService.INFO);
            }
        }
    }*/
    @EventSubscriber
    public void userVoiceLeave(UserVoiceChannelLeaveEvent event){
        if(event.getVoiceChannel().getConnectedUsers().contains(event.getClient().getOurUser())
            && event.getVoiceChannel().getConnectedUsers().size()==1){
            event.getVoiceChannel().leave();
        }
    }

    @EventSubscriber
    public void trackStarted(TrackStartEvent event){
        if(Main.leaveVoice != null){
            Main.leaveVoice.cancel(true);
            log(event.getPlayer().getGuild(),"Leave canceled", INFO);
        }
    }

    @EventSubscriber
    public void trackFinished(TrackFinishEvent event){
        log(event.getPlayer().getGuild(),"Scheduling leaveChannel", INFO);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable leave = () -> event.getPlayer().getGuild().getConnectedVoiceChannel().leave();
        Main.leaveVoice = executor.schedule(leave,1, TimeUnit.MINUTES);
    }

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        if(event.getMessage().getContent().contains(BotUtils.R)){
            MiscTasks.handleR(event);
        }

        // When @everyone is tagged with a ? in the same message. Doesn't trigger on links
        if(event.getMessage().mentionsEveryone() && event.getMessage().getContent().contains("?")
            && !event.getMessage().getContent().contains("https")){
            RequestBuffer.request(() -> event.getMessage().addReaction(ReactionEmoji.of(CHECK_MARK))).get();
            RequestBuffer.request(() -> event.getMessage().addReaction(ReactionEmoji.of(RED_X))).get();
            return;
        }

        String command[] = event.getMessage().getContent().split("\\s");

        if(command.length == 0){
            return;
        }
        if(!command[0].startsWith(BOT_PREFIX)){
            return;
        }

        String cmd = command[0].substring(1).toUpperCase();

        List<String> args = new ArrayList<>(Arrays.asList(command));
        log(event.getGuild(),"Command: "+ args, INFO);
        args.remove(0);
        String[] commandGroup = commandMap.keySet()
                                 .stream()
                                 .filter(k -> commandMap.get(k).containsKey(cmd))
                                 .collect(Collectors.toSet())
                                 .toArray(new String[0]);
        if(commandGroup.length>1){
            BotUtils.contactOwner(event,"More then one command with the same name: "+event.getMessage().getContent());
            log(event.getGuild(),"There is more than one command group with the same command, contacting owner", ERROR);
            return;
        }
        if(commandGroup.length==1 && commandMap.containsKey(commandGroup[0]) && commandMap.get(commandGroup[0]).containsKey(cmd)){
            log(event.getGuild(),"Valid command: "+cmd, SUCC);
            commandMap.get(commandGroup[0]).get(cmd).runCommand(event,args);
        }else{
            log(event.getGuild(),"Invalid command: "+cmd, UERROR);
        }
    }
}
