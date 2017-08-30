package com.github.mendess2526.discordBot;



public class Main {

    public static void main(String[] args) {

        IDiscordClient client = new ClientBuilder().withToken("MzUyMzk5Mjg1NzcxNDM2MDMy.DIgk-w.wgedGSP0lsMz88i0TX3pvXmFMGs");

        try {
            if (login) {
                return clientBuilder.login();
            } else {
                return clientBuilder.build();
            }
        } catch (DiscordExeption e) {
            e.printStackTrace();
            return null;
        }
    }
}