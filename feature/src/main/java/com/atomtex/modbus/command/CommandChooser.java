package com.atomtex.feature.command;

import android.util.SparseArray;

import static com.atomtex.feature.util.BTD3Constant.*;

public class CommandChooser {

    private static final SparseArray<Command> commands;

    static {
        commands = new SparseArray<>();
        commands.put(READ_SW, new ReadStatusWordCommand());
    }


    public static SparseArray<Command> getCommands() {
        return commands;
    }

    public static Command getCommand(byte command) {
        return commands.get(command);
    }


}