package com.atomtex.modbus.command;

import android.util.SparseArray;

import static com.atomtex.modbus.util.BTD3Constant.*;

public class CommandChooser {

    private static final SparseArray<Command> btdu3commands;

    static {
        btdu3commands = new SparseArray<>();
        btdu3commands.put(READ_SW, new ReadStatusWordCommand());
    }


    public static SparseArray<Command> getCommands() {
        return btdu3commands;
    }

    public static Command getCommand(byte command) {
        return btdu3commands.get(command);
    }


}
