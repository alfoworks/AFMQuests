package ru.allformine.afmcp.CFNTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.allformine.afmcp.References;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CFNTaskCustom extends BukkitRunnable {
    private final JavaPlugin plugin;

    public CFNTaskCustom(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            String str = References.CFNTaskCustomText;

            try {
                out.writeUTF(str);
            } catch(IOException e) {
                System.out.println("Error sending FactionsShow data.");
            }

            p.sendPluginMessage(plugin, "FactionsShow", b.toByteArray());
        }
    }
}
