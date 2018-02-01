package com.github.mendess2526.memnarch.sounds;

import com.github.mendess2526.memnarch.Command;
import sx.blah.discord.handle.obj.Permissions;

import java.util.EnumSet;
import java.util.Set;

public abstract class CSounds implements Command{

    @Override
    public String getCommandGroup(){
        return "Sfx";
    }

    @Override
    public Set<Permissions> getPermissions(){
        return EnumSet.of(Permissions.VOICE_USE_VAD);
    }
}
