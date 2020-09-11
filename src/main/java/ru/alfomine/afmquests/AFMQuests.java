package ru.alfomine.afmquests;

import com.google.inject.Inject;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import ru.alfomine.afmquests.commands.*;
import ru.alfomine.afmquests.listeners.*;
import ru.alfomine.afmquests.quests.PlayerContribution;
import ru.alfomine.afmquests.quests.QuestDataManager;
import ru.alfomine.afmquests.quests.QuestFactionContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "afmquests",
        name = "AFMQuests",
        description = "EF quests plugin",
        version = "1.0",
        authors = {
                "Iterator, ReDestroyDeR"
        }
)
public class AFMQuests {
    public static boolean serverRestart = false;
    @Inject
    public static Logger logger;
    public static boolean debugSwitch = false;
    public static AFMQuests instance;
    public static Task lagTask;
    public static QuestDataManager questDataManager;
    private static CommentedConfigurationNode configNode;
    private static ConfigurationLoader<CommentedConfigurationNode> configLoader;
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    private Path configFile;
    private Path questsFile;
    private Path factionListFile;

    public final static String[] defaultFactions = {
            "safezone",
            "warzone"
    };

    public static boolean questToggle;

    public static CommentedConfigurationNode getConfig() {
        return configNode;
    }

    public static void saveConfig() {
        try {
            configLoader.save(configNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Inject
    private void setLogger(Logger logger) {
        AFMQuests.logger = logger;
    }

    @Listener
    public void ats(GameAboutToStartServerEvent event) {
        CommentedConfigurationNode quests = getConfig().getNode("quests");
        String questsFilePath = quests.getNode("questsFile").getString();
        assert questsFilePath != null;

        factionListFile = configDir.resolve("factionList.json");
        if (!Files.exists(factionListFile)) {
            try {
                Files.createFile(Paths.get(configDir.toString() + "/factionList.json"));
                logger.debug("Created faction list json file");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        try {
            String pathA = configDir.toString() + "/questFiles";
            if (!Files.exists(Paths.get(pathA))) {
                Files.createDirectory(Paths.get(pathA));
                logger.debug("Created quest files directory");
            }
            if (!questsFilePath.equals("")) {
                questsFile = configDir.resolve("questFiles").resolve(questsFilePath);
                if (!Files.exists(questsFile)) {
                    logger.error("Quests file is set in config, but file is not present in directory");
                    logger.error("Creating new empty quests file with the same name - " + questsFilePath);
                    Files.createFile(Paths.get(pathA + "/" + questsFilePath));
                    questsFile = configDir.resolve("questFiles").resolve(questsFilePath);
                    questToggleOff();
                    logger.debug("Created quests file json file with name " + questsFilePath);
                } else {
                    if (EagleFactionsPlugin.getPlugin().getConfiguration().getPowerConfig().getKillAward() == 0
                            && EagleFactionsPlugin.getPlugin().getConfiguration().getPowerConfig().getPenalty() == 0
                            && EagleFactionsPlugin.getPlugin().getConfiguration().getPowerConfig().getPowerDecrement() == 0
                            && EagleFactionsPlugin.getPlugin().getConfiguration().getPowerConfig().getPowerIncrement() == 0) {
                        questToggleOn();
                    } else {
                        logger.error("To work with Quests system you need to set following settings into Eagle Factions config:\n" +
                                "power {\n" +
                                "    # How much power will be removed on player death. Default: 2.0\n" +
                                "    decrement=0\n" +
                                "    # How much power will be restored for player after 1 minute of playing. (0.04 per minute = 1,2 per hour.) Default: 0.04\n" +
                                "    increment=0\n" +
                                "    # Player kill award. Default: 2.0\n" +
                                "    kill-award=0\n" +
                                "    # Maximum amount of power a player can have. Default: 10.0\n" +
                                "    max-power=[ANY]\n" +
                                "    # Penalty after killing a teammate. Default: 1.0\n" +
                                "    penalty=0\n" +
                                "    # Starting amount of power. Default: 5.0\n" +
                                "    start-power=[ANY]\n" +
                                "}");
                        questToggleOff();
                    }
                }
            } else {
                logger.warn("Quest file isn't present in config. Quest System is disabled");
                questToggleOff();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void init(GameInitializationEvent event) {

    }

    @Listener
    public void preInit(GamePreInitializationEvent event) {
        instance = this;

        Sponge.getEventManager().registerListeners(this, new QuestEventListener());

        if (Sponge.getPluginManager().isLoaded("eaglefactions")) {
            Sponge.getEventManager().registerListeners(this, new FactionEventListener());
        }

        configFile = configDir.resolve("config.conf");
        configLoader = HoconConfigurationLoader.builder().setPath(configFile).build();

        configSetup();

        CommandSpec questGUICommandSpec = CommandSpec.builder()
                .description(Text.of("Меню квестов."))
                .executor(new QuestGUICommand())
                .build();

        Sponge.getCommandManager().register(this, questGUICommandSpec, "questgui");

        CommandSpec questEDITORCommandSpec = CommandSpec.builder()
                .description(Text.of("Редактор квестов."))
                .executor(new QuestEDITORCommand(configDir, this))
                //.permission("afmcp.admin")
                .build();

        Sponge.getCommandManager().register(this, questEDITORCommandSpec, "questeditor");
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        if (questToggle)
            cleanQuestFactions();
    }

    public static void cleanQuestFactions() {
        Map<String, Faction> map = EagleFactionsPlugin.getPlugin().getFactionLogic().getFactions();
        QuestFactionContainer container = questDataManager.getQuestFactions();

        // Constructing QuestFaction from Faction data
        for (Map.Entry<String, Faction> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase("WarZone") || e.getKey().equalsIgnoreCase("SafeZone")) {
                continue;
            }

            // Faction
            if (!container.getQuestFaction(e.getKey()).isPresent()) {
                UUID leader = e.getValue().getLeader();
                Set<UUID> officers = e.getValue().getOfficers();
                Set<UUID> recruits = e.getValue().getRecruits();

                questDataManager.updateContribution(new PlayerContribution(leader, e.getValue()));

                // Adding members
                for (UUID u : officers) {
                    questDataManager.updateContribution(new PlayerContribution(u, e.getValue()));
                }
                for (UUID u : recruits) {
                    questDataManager.updateContribution(new PlayerContribution(u, e.getValue()));
                }
            }
        }
    }

    private void configSetup() {
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        if (!Files.exists(configFile)) {
            try {
                //noinspection OptionalGetWithoutIsPresent
                Sponge.getAssetManager().getAsset(this, "config.conf").get().copyToFile(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        load();

    }

    private void load() {
        try {
            configNode = configLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void questToggleOff() {
        questToggle = false;
        questDataManager = null;

        logger.warn("Using vanilla EagleFactions power system");
    }

    private void questToggleOn() {
        questToggle = true;
        questDataManager = new QuestDataManager(questsFile, factionListFile);
        questDataManager.initializeQuestFactionContainer();
        if (!questToggle) {
            questToggleOff();
            return;
        }

        logger.warn("Using AFMQuests EagleFactions power system");
    }
}
