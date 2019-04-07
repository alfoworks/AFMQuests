package ru.allformine.afmcp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;

public class CommandAFMCP extends AFMCPCommand {
    @Override
    public String getName() {
        return "afmcp";
    }

    @Override
    public String getDisplayName() {
        return "AFMCP";
    }

    @Override
    public ChatColor getCommandChatColor() {
        return ChatColor.BLACK;
    }

    public boolean run(ArrayList<String> args, CommandSender sender) {
        reply(sender, "Плагин работает!");

        return true;
    }
}