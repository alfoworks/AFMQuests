package ru.alfomine.afmquests.listeners;

import org.slf4j.Logger;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import ru.alfomine.afmquests.AFMQuests;
import ru.alfomine.afmquests.quests.PlayerContribution;
import ru.alfomine.afmquests.quests.Quest;
import ru.alfomine.afmquests.quests.QuestLevel;

import java.util.*;

@SuppressWarnings("ConstantConditions")
public class QuestEventListener {
    private final Logger logger = AFMQuests.logger;
    private final Map<UUID, Quest[]> questTracker = new HashMap<>();

    @Listener
    public void ClickInventoryEvent(ClickInventoryEvent event) {
        if (AFMQuests.questToggle) {
            Text originTitle = event.getTargetInventory().getInventoryProperty(InventoryTitle.class)
                    .orElse(InventoryTitle.of(Text.of(""))).getValue();
            if (Objects.equals(originTitle, Text.of(TextColors.YELLOW, "Quest Menu"))) {
                event.setCancelled(true);

                Object player = event.getSource();
                assert player instanceof Player;

                Slot slot = event.getTransactions().get(0).getSlot();
                SlotIndex slotIndex = slot.getInventoryProperty(SlotIndex.class).orElse(null);
                int questId = slotIndex.getValue();

                // True - open quest panel; False - try to move to next/previous page
                if (!(questId == 0 || questId == 26)) {
                    AFMQuests.questDataManager.openGUI((Player) player, questId, event);
                } else if (!event.getTransactions().get(0).getOriginal().isEmpty()) {
                    ItemStackSnapshot item = event.getTransactions().get(0).getOriginal();
                    if (item.get(Keys.DISPLAY_NAME).isPresent()) {
                        String[] parts = item.get(Keys.DISPLAY_NAME).get().toPlain().split(" ");
                        try {
                            AFMQuests.questDataManager.openGUI((Player) player, -1, event, Integer.parseInt(parts[parts.length-1])-1);
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            } else if (Objects.equals(originTitle, Text.of(TextColors.DARK_GREEN, "Quest Panel"))) {
                Optional<Slot> slotX = event.getSlot();
                if (slotX.isPresent()) {
                    Optional<SlotIndex> slotIndex = slotX.get().getInventoryProperty(SlotIndex.class);
                    slotIndex.ifPresent(index -> event.setCancelled(index.getValue() <= 26));
                }

                Object player = event.getSource();
                assert player instanceof Player;

                Text slotName = null;

                for (SlotTransaction t : event.getTransactions()) {
                    ItemStack s = t.getOriginal().createStack();

                    Optional<Text> text = s.get(Keys.DISPLAY_NAME);
                    if (text.isPresent()) {
                        slotName = text.get();
                    } else {
                        logger.error("No text data");
                    }
                }

                ItemStack questData = null;

                for (Inventory i : event.getTargetInventory().slots()) {
                    Optional<SlotIndex> slot;
                    slot = i.getInventoryProperty(SlotIndex.class);
                    if (slot.isPresent()) {
                        int slotIndex = slot.get().getValue();
                        Optional<ItemStack> item = i.peek();
                        if (!item.isPresent())
                            continue;

                        if (slotIndex == 4)
                            questData = item.get();
                    } else {
                        logger.error("No slot data");
                    }
                }

                if (questData == null) {
                    event.setCancelled(true);
                    return;
                }

                Player playerR = (Player) player;
                PlayerContribution contribution = AFMQuests.questDataManager.getContribution(playerR.getUniqueId());

                // Quest data
                List<Text> lore = questData.get(Keys.ITEM_LORE).orElse(null);
                int questId = -1;


                // Assigning player to the first level in the array
                if (contribution.getLevelId() == null) {
                    contribution.setQuestLevel(AFMQuests.questDataManager.getQuestDifficulties().getQuestLevels()[0]);
                }

                QuestLevel questLevel = AFMQuests.questDataManager.getQuestDifficulties().getLevelById(contribution.getLevelId());

                if (lore != null) {
                    questId = Integer.parseInt(lore.get(lore.size() - 1).toPlainSingle());
                }
                if (slotName != null) {
                    if (slotName.equals(Text.of(TextColors.GREEN, "Apply"))) { // Apply click
                        Quest quest = questLevel.getQuest(questId);
                        if (contribution.assignQuest(quest)) {
                            AFMQuests.questDataManager.updateContribution(contribution);
                            AFMQuests.questDataManager.closeGUI(playerR);
                            sendSFMessage(playerR, quest.getStartMessage(), quest.getName());

                            updateQuestTracker(contribution);
                            logger.debug("Apply final");
                        } else {
                            playerR.kick(Text.of(
                                    TextColors.DARK_RED, TextStyles.BOLD, "КРИТИЧЕСКАЯ ОШИБКА #1\n",
                                    TextColors.GREEN, "Напиши мне в дискорд как ты это сделал\n",
                                    TextColors.AQUA, "red#4596"));
                        }
                    } else if (slotName.equals(Text.of(TextColors.RED, "Deny"))) { // Deny click
                        AFMQuests.questDataManager.openGUI(playerR, -1, event);
                        logger.debug("Deny final");
                    } else if (slotName.equals(Text.of(TextColors.GREEN, "Continue"))) {
                        AFMQuests.questDataManager.openGUI(playerR, -1, event);
                        logger.debug("Continue final");
                    } else if (slotName.equals(Text.of(TextColors.RED, "Abort"))) {
                        Quest[] temp = contribution.getCompletedQuests();
                        Text name = questLevel.getQuest(questId).getName();

                        Quest quest = contribution.getQuest(name.toPlain());
                        quest.setProgress(quest.getCount());
                        contribution.completeQuest(quest);
                        contribution.resetCompletedQuests(temp);
                        AFMQuests.questDataManager.updateContribution(contribution);

                        AFMQuests.questDataManager.closeGUI(playerR);
                        Text message = Text.of(TextColors.RED, "QUEST HAS BEEN ABORTED");
                        playerR.sendMessage(message);

                        updateQuestTracker(contribution);
                        logger.debug("Abort final");
                    } else if (slotName.equals(Text.of(TextColors.YELLOW, "Insert quest items here"))) {
                        ItemStackSnapshot x = event.getCursorTransaction().getOriginal();
                        if (!x.getType().equals(ItemTypes.ENDER_CHEST)) {
                            Text name = questLevel.getQuest(questId).getName();
                            Quest quest = contribution.getQuest(name.toPlain());
                            if (Objects.requireNonNull(getQuestQuestTracker(quest, playerR.getUniqueId())).getTarget()
                                    .equals(x.getType().getId())) {

                                int quantity = x.getQuantity();

                                event.setCancelled(false);
                                if (quest.getCount() < quest.getProgress() + quantity) {
                                    ItemStack replacement = x.createStack();
                                    replacement.setQuantity(quest.getProgress() + quantity - quest.getCount());
                                    quantity -= replacement.getQuantity();

                                    // Moving left items to the first open slot of the inventory,
                                    // Because when the inventory is closed the items get dropped down. We don't want it
                                    for (Inventory s : playerR.getInventory().slots()) {
                                        if (s.canFit(replacement)) {
                                            if (s.peek().isPresent()) {
                                                replacement.setQuantity(replacement.getQuantity()
                                                        + s.peek().get().getQuantity());
                                            }

                                            s.set(replacement);
                                            break;
                                        }
                                    }
                                }

                                event.getCursorTransaction().setCustom(ItemStackSnapshot.NONE);

                                if (quest.getProgress() < quest.getCount()) {
                                    quest.appendProgress(quantity);

                                    contribution.updateQuest(quest);
                                    AFMQuests.questDataManager.updateContribution(contribution);
                                    updateQuestTracker(contribution);

                                    if (quest.finished())
                                        sendSFMessage(playerR, quest.getFinalMessage(), quest.getName());
                                }

                                // A quick fix that updates gui by just resetting it
                                AFMQuests.questDataManager.openGUI(playerR, questId + 1, event);
                            }
                        /* else {
                            //// TODO: Make wrong item placement reaction
                        }*/
                        }
                    }
                }
            }
        }
    }



    // Quest Tracking system

    private void updateQuestTracker(PlayerContribution contribution) {
        if (!questTracker.containsKey(contribution.getPlayer())) {
            loadQuestTracker(contribution);
        }
        questTracker.replace(contribution.getPlayer(), contribution.getActiveQuests());
        logger.debug(String.format("Updated %s quest data",
                contribution.getPlayer()));
    }

    private void loadQuestTracker(PlayerContribution contribution) {
        if (!questTracker.containsKey(contribution.getPlayer())) {
            questTracker.put(contribution.getPlayer(), contribution.getActiveQuests());
            logger.debug(String.format("Loaded %s quest data",
                    contribution.getPlayer()));
        } else {
            throw new AssertionError(String.format("Player %s hadn't unloaded questTracker", contribution.getPlayer()));
        }
    }

    private void unloadQuestTracker(Player player) {
        if (questTracker.containsKey(player.getUniqueId())) {
            questTracker.remove(player.getUniqueId());
            logger.debug(String.format("Unloaded %s quest data",
                    player.getName()));
        } else {
            throw new AssertionError(String.format("Player %s hadn't created his questTracker", player.getName()));
        }
    }

    private Quest[] getQuestsQuestTracker(UUID uuid) {
        if (questTracker.containsKey(uuid)) {
            return questTracker.get(uuid);
        }
        return null;
    }

    private Quest getQuestQuestTracker(Quest quest, UUID uuid) {
        if (questTracker.containsKey(uuid)) {
            Quest[] quests = questTracker.get(uuid);
            return Arrays.stream(quests).filter(q -> q.getName().equals(quest.getName())).findAny().orElse(null);
        }
        return null;
    }

    // Player join/leave updater
    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        if (AFMQuests.questToggle) {
            Player player = event.getTargetEntity();
            UUID uuid = player.getUniqueId();
            loadQuestTracker(AFMQuests.questDataManager.getContribution(uuid));
        }
    }

    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event) {
        if (AFMQuests.questToggle) {
            Player player = event.getTargetEntity();
            unloadQuestTracker(player);
        }
    }

    // Entity Kill quest
    @Listener
    public void DamageEntityEvent(DamageEntityEvent event) {
        if (AFMQuests.questToggle) {
            if (event.willCauseDeath()) {
                if (event.getCause().first(Player.class).isPresent()) {
                    Player player = event.getCause().first(Player.class).get();
                    UUID uuid = player.getUniqueId();
                    Quest[] quests = getQuestsQuestTracker(uuid);
                    if (quests == null) return;
                    for (Quest quest : quests) {
                        if (quest != null) {
                            if (quest.getTarget().equals(event.getTargetEntity().getType().getId())) {
                                if (quest.getProgress() < quest.getCount()) {
                                    quest.appendProgress(1);

                                    PlayerContribution contribution =
                                            AFMQuests.questDataManager.getContribution(player.getUniqueId());
                                    contribution.updateQuest(quest);

                                    AFMQuests.questDataManager.updateContribution(contribution);

                                    updateQuestTracker(contribution);

                                    if (quest.finished())
                                        sendSFMessage(player, quest.getFinalMessage(), quest.getName());
                                } /*else {
                                    // not possible
                                }*/
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendSFMessage(Player player, Text message, Text name) {
        Text prefix = Text.of(
                TextColors.GRAY, "[ ",
                TextColors.GREEN,"Quests",
                TextColors.GRAY, " ] ",
                TextColors.WHITE, '"',
                name,
                TextColors.WHITE, '"',
                TextColors.GRAY, ": "
        );

        player.sendMessage(message.concat(prefix));
    }
}
