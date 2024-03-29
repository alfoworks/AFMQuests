package ru.alfomine.afmquests.quests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import ru.alfomine.afmquests.quests.parsers.*;
import ru.alfomine.afmquests.AFMQuests;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class QuestDataManager {
    public QuestFactionContainer questFactionContainer;
    private final QuestLevelContainer questDifficulties;
    private final Path factionPath;
    private final QuestGUI gui;

    // Constructs both data files
    public QuestDataManager(Path questsPath, Path factionsPath) {
        this.factionPath = factionsPath;
        this.questDifficulties = new QuestLevelContainer(getQuests(questsPath));
        this.gui = new QuestGUI();

        // If quest file is present and it's empty
        if (questDifficulties.getQuestLevels() == null) {
            AFMQuests.questToggle = false;
        }
    }

    public void initializeQuestFactionContainer() {
        this.questFactionContainer = getQuestFactions();
    }

    public QuestFactionContainer getQuestFactions() {
        String jsonData = null;
        try {
            jsonData = new String(Files.readAllBytes(factionPath));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            return new QuestFactionContainer();
        }

        if (jsonData == null || jsonData.equals(""))
            return new QuestFactionContainer();


        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(QuestFactionContainer.class, new QuestFactionContainerDeserializer())
                .registerTypeAdapter(QuestFaction.class, new QuestFactionDeserializer())
                .registerTypeAdapter(PlayerContribution.class, new PlayerContributionDeserializer())
                .registerTypeAdapter(Quest.class, new QuestDeserializer())
                .create();

        return gson.fromJson(jsonData, QuestFactionContainer.class);
    }

    private QuestLevel[] getQuests(Path path) {
        String jsonData = null;
        try {
            jsonData = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(QuestLevelContainer.class, new QuestLevelContainerDeserializer())
                .registerTypeAdapter(QuestLevel.class, new QuestLevelDeserializer())
                .registerTypeAdapter(Quest.class, new QuestDeserializer())
                .create();

        return gson.fromJson(jsonData, QuestLevelContainer.class).getQuestLevels();
    }

    private void updateFactionListFile(String s) {
        try {
            Files.write(factionPath, "".getBytes());
            Files.write(factionPath, s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PlayerContribution getContribution(UUID playerUUID) {
        Optional<QuestFaction> factions = questFactionContainer.getQuestFaction(playerUUID);
        return factions.map(faction -> faction.getContribution(playerUUID)).orElse(null);
    }

    // Contribution > Player because it contains more data we want
    public void updateContribution(PlayerContribution contribution) {
        String factionName = contribution.getFactionName();

        // update / append / remove
        if (questFactionContainer.getQuestFaction(factionName).isPresent()) {
            QuestFaction faction = questFactionContainer.getQuestFaction(factionName).get();
            // Investor doesn't exist
            if (contribution.removeFlag) {
                faction.removeInvestor(contribution);
            } else if (!faction.updateInvestor(contribution)) {
                faction.addInvestor(contribution);
            }
        } else {
            // create / rename / disband
            Optional<QuestFaction> questFaction =
                    Arrays.stream(questFactionContainer.getQuestFactions())
                    .filter(faction -> faction.hasInvestor(contribution.getPlayer())).findFirst();

            // Rename
            if (questFaction.isPresent()) {
                questFactionContainer.updateQuestFaction(questFaction.get());
            } else {
                if (!factionName.equals("")) {
                    // Create
                    QuestFaction faction = new QuestFaction(factionName);
                    faction.addInvestor(contribution);
                    faction.setCurrentLeader(contribution.getPlayer());
                    questFactionContainer.createQuestFaction(faction);
                }
            }

        }

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(QuestFactionContainer.class, new QuestFactionContainerSerializer())
                .registerTypeAdapter(QuestFaction.class, new QuestFactionSerializer())
                .registerTypeAdapter(PlayerContribution.class, new PlayerContribtuionSerializer())
                .registerTypeAdapter(Quest.class, new QuestSerializer())
                .create();

        // Update data file after operations
        updateFactionListFile(gson.toJson(questFactionContainer, QuestFactionContainer.class));
    }

    public QuestLevelContainer getQuestDifficulties() {
        return questDifficulties;
    }

    public Quest getQuestById(String levelId, int questId) {
        return questDifficulties.getLevelById(levelId).getQuest(questId);
    }

    public void openGUI(Player player, int id) {
        gui.showToPlayer(getContribution(player.getUniqueId()), player, id);
    }

    public void openGUI(Player player, int id, ClickInventoryEvent event) {
        gui.showToPlayer(getContribution(player.getUniqueId()), player, id, event);
    }

    public void openGUI(Player player, int id, ClickInventoryEvent event, int page) {
        PlayerContribution playerContribution = getContribution(player.getUniqueId());
        playerContribution.page = page;
        gui.showToPlayer(playerContribution, player, id, event);
    }

    public void closeGUI(Player player) {
        gui.closeGUI(player);
    }
}