package com.github.mendess2526.discordbot;


import sx.blah.discord.handle.obj.IGuild;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;


@SuppressWarnings("WeakerAccess")
public class LoggerService {
    static final int INFO = 1;
    static final int ERROR = 2;
    static final int SUCC = 3;
    static final int UERROR = 4;

    public static void log(IGuild guild, String message, int type){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        switch (type) {
            case LoggerService.INFO:
                message = " INFO - " + message;
                break;
            case LoggerService.ERROR:
                message = " ERROR - " + message;
                break;
            case LoggerService.SUCC:
                message = " SUCC - " + message;
                break;
            case LoggerService.UERROR:
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
    private static void logToFile(String message){
        try {
            Writer writer = new BufferedWriter(new FileWriter("log.txt", true));
            writer.write( message + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            LoggerService.logToConsole(e.getMessage(),LoggerService.ERROR);
        }
    }
    private static void logToConsole(String message, int type) {
        final String ANSI_RESET = " \u001B[0m ";
        final String ANSI_RED = " \u001B[31m ";
        final String ANSI_GREEN = " \u001B[32m ";
        final String ANSI_CYAN = " \u001B[36m ";
        final String ANSI_LRED = " \u001B[91;1m";

        switch (type) {
            case LoggerService.INFO:
                message = ANSI_CYAN + message + ANSI_RESET;
                break;
            case LoggerService.ERROR:
                message = ANSI_RED  + message + ANSI_RESET;
                break;
            case LoggerService.SUCC:
                message = ANSI_GREEN + message + ANSI_RESET;
                break;
            case LoggerService.UERROR:
                message = ANSI_LRED + message + ANSI_RESET;
                break;
            default:
                break;
        }
        System.out.println(message);
    }
}
