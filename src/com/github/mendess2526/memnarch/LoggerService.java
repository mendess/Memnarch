package com.github.mendess2526.memnarch;


import sx.blah.discord.handle.obj.IGuild;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


@SuppressWarnings("WeakerAccess")
public class LoggerService {
    public static final int INFO = 1;
    public static final int ERROR = 2;
    public static final int SUCC = 3;
    public static final int UERROR = 4;

    public static void log(IGuild guild, String message, int type){
        //noinspection SpellCheckingInspection
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        switch (type) {
            case INFO:
                message = " INFO - " + message;
                break;
            case ERROR:
                message = " ERROR - " + message;
                break;
            case SUCC:
                message = " SUCC - " + message;
                break;
            case UERROR:
                message = " USER_E - " + message;
                break;
            default:
                message = " XXX - " + message;
                break;
        }
        String gName = guild!=null ? "["+guild.getName()+"]:" : "[MAIN]:";
        message = sdf.format(new Date())+gName+message;
        logToConsole(message,type);
        logToFile(message);
    }

    public static void log(IGuild guild, Exception e, String method){
        log(guild,e.getClass().getCanonicalName()+" in "+method+": "+e.getMessage(), ERROR);
        logToFile(Arrays.toString(e.getStackTrace()));
    }

    private static void logToFile(String message){
        try {
            Writer writer = new BufferedWriter(new FileWriter("log.txt", true));
            writer.write( message + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            logToConsole(e.getMessage(), ERROR);
        }
    }

    private static void logToConsole(String message, int type) {
        final String ANSI_RESET = " \u001B[0m ";
        final String ANSI_RED = " \u001B[31m ";
        final String ANSI_GREEN = " \u001B[32m ";
        final String ANSI_CYAN = " \u001B[36m ";
        final String ANSI_LRED = " \u001B[91;1m";

        switch (type) {
            case INFO:
                message = ANSI_CYAN + message + ANSI_RESET;
                break;
            case ERROR:
                message = ANSI_RED  + message + ANSI_RESET;
                break;
            case SUCC:
                message = ANSI_GREEN + message + ANSI_RESET;
                break;
            case UERROR:
                message = ANSI_LRED + message + ANSI_RESET;
                break;
            default:
                break;
        }
        System.out.println(message);
    }
}
