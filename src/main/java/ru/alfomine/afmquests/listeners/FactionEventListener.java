package ru.alfomine.afmquests.listeners;

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
import ru.alfomine.afmquests.AFMQuests;
import ru.alfomine.afmquests.quests.PlayerContribution;

import java.util.Optional;

public class FactionEventListener {
    private static final Logger logger = AFMQuests.logger;

    /* ====================================== */
    /*            Quests block
    /* ====================================== */

    @Listener(order = Order.POST)
    public void factionJoinEventImpl(FactionJoinEventImpl event) {
        PlayerContribution p = new PlayerContribution(event.getCreator().getUniqueId(), event.getFaction());
        AFMQuests.questDataManager.updateContribution(p);
    }



    @Listener(order = Order.POST)
    public void factionLeaveEventImpl(FactionLeaveEventImpl event) {
        PlayerContribution p = AFMQuests.questDataManager.getContribution(event.getCreator().getUniqueId());
        p.removeFlag = true;
        AFMQuests.questDataManager.updateContribution(p);
    }

    @Listener(order = Order.POST)
    public void factionCreateEvent(FactionCreateEvent event) {
        try {
            logger.debug("Triggered quest FACTION CREATE");
            PlayerContribution p = new PlayerContribution(event.getCreator().getUniqueId(), event.getFaction());
            AFMQuests.questDataManager.updateContribution(p);
        } catch (AssertionError e) {
            logger.warn(e.getMessage());
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Listener(order = Order.POST)
    public void factionDisbandEvent(FactionDisbandEvent event) {
        try {
            logger.debug("Triggered quest FACTION DISBAND");
            PlayerContribution p = AFMQuests.questDataManager.getContribution(event.getCreator().getUniqueId());
            p.setFactionName("");
            AFMQuests.questDataManager.questFactionContainer
                    .disbandQuestFaction(
                            AFMQuests.questDataManager.questFactionContainer.getQuestFaction(p.getPlayer()).get());
            AFMQuests.questDataManager.updateContribution(p);
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
                PlayerContribution p = AFMQuests.questDataManager.getContribution(((Player) event.getSource()).getUniqueId());
                p.setFactionName(event.getArguments().split(" ")[1]);
                AFMQuests.questDataManager.updateContribution(p);
            }
        }
    }
}