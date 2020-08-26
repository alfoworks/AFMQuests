package ru.allformine.afmcp.listeners;

import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.events.FactionCreateEvent;
import io.github.aquerr.eaglefactions.api.events.FactionDisbandEvent;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.events.FactionAreaEnterEventImpl;
import io.github.aquerr.eaglefactions.common.events.FactionJoinEventImpl;
import io.github.aquerr.eaglefactions.common.events.FactionLeaveEventImpl;
import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import ru.allformine.afmcp.AFMCorePlugin;
import ru.allformine.afmcp.PacketChannels;
import ru.allformine.afmcp.quests.PlayerContribution;

import java.util.Optional;

public class FactionEventListener {
    private static final Logger logger = AFMCorePlugin.logger;

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();

        Optional<Faction> faction = EagleFactionsPlugin.getPlugin().getFactionLogic().getFactionByChunk(player.getWorld().getUniqueId(), player.getLocation().getChunkPosition());
        sendToPlayer(player, getFactionNameForPlayer(faction.orElse(null), player));
    }

    @Listener
    public void onFactionAreaChange(FactionAreaEnterEventImpl event) {
        sendToPlayer(event.getCreator(), getFactionNameForPlayer(event.getEnteredFaction().orElse(null), event.getCreator()));
    }

    // ============================== //

    private void sendToPlayer(Player player, String string) {
        PacketChannels.FACTIONS.sendTo(player, buf -> buf.writeString(string));
    }

    private String getFactionNameForPlayer(Faction faction, Player player) {
        String factionName = faction == null ? "Общая" : faction.getName();
        String factionColor;

        if (AFMCorePlugin.currentLobby != null && AFMCorePlugin.currentLobby.isPlayerInLobby(player)) {
            factionColor = "§9";
            factionName = "Лобби";
        } else if (factionName.equals("SafeZone") || EagleFactionsPlugin.getPlugin().getConfiguration().getProtectionConfig().getSafeZoneWorldNames().contains(player.getWorld().getName())) {
            factionColor = "§d";
            factionName = "SafeZone";
        } else if (factionName.equals("WarZone") || EagleFactionsPlugin.getPlugin().getConfiguration().getProtectionConfig().getWarZoneWorldNames().contains(player.getWorld().getName())) {
            factionColor = "§4";
            factionName = "WarZone";
        } else if (faction == null) {
            factionColor = "§2";
        } else {
            if (faction.containsPlayer(player.getUniqueId())) {
                factionColor = "§a";
            } else {
                factionColor = "§6";
            }
        }

        return factionColor + factionName;
    }

    /* ====================================== */
    /*            Quests block
    /* ====================================== */

    @Listener(order = Order.POST)
    public void factionJoinEventImpl(FactionJoinEventImpl event) {
        PlayerContribution p = new PlayerContribution(event.getCreator().getUniqueId(), event.getFaction());
        AFMCorePlugin.questDataManager.updateContribution(p);
    }



    @Listener(order = Order.POST)
    public void factionLeaveEventImpl(FactionLeaveEventImpl event) {
        PlayerContribution p = AFMCorePlugin.questDataManager.getContribution(event.getCreator().getUniqueId());
        p.removeFlag = true;
        AFMCorePlugin.questDataManager.updateContribution(p);
    }

    @Listener(order = Order.POST)
    public void factionCreateEvent(FactionCreateEvent event) {
        try {
            logger.debug("Triggered quest FACTION CREATE");
            PlayerContribution p = new PlayerContribution(event.getCreator().getUniqueId(), event.getFaction());
            AFMCorePlugin.questDataManager.updateContribution(p);
        } catch (AssertionError e) {
            logger.warn(e.getMessage());
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Listener(order = Order.POST)
    public void factionDisbandEvent(FactionDisbandEvent event) {
        try {
            logger.debug("Triggered quest FACTION DISBAND");
            PlayerContribution p = AFMCorePlugin.questDataManager.getContribution(event.getCreator().getUniqueId());
            p.setFactionName("");
            AFMCorePlugin.questDataManager.questFactionContainer
                    .disbandQuestFaction(
                            AFMCorePlugin.questDataManager.questFactionContainer.getQuestFaction(p.getPlayer()).get());
            AFMCorePlugin.questDataManager.updateContribution(p);
        } catch (NullPointerException ignore) {
            logger.debug("Disbandned faction wasn't present in faction list");
        }
    }

    @Listener(order = Order.POST)
    public void renameCommandEvent(SendCommandEvent event) {
        String command = event.getCommand();
        if (command.equalsIgnoreCase("f") ||
                command.equalsIgnoreCase("faction") ||
                command.equalsIgnoreCase("factions")) {
            if (event.getArguments().split(" ")[0].equalsIgnoreCase("rename")) {
                logger.debug("Triggered quest FACTION RENAME");
                PlayerContribution p = AFMCorePlugin.questDataManager.getContribution(((Player) event.getSource()).getUniqueId());
                p.setFactionName(event.getArguments().split(" ")[1]);
                AFMCorePlugin.questDataManager.updateContribution(p);
            }
        }
    }
}