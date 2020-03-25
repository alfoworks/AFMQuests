package ru.allformine.afmcp.tablist

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.tab.TabListEntry


object WrappedTabList {
    private val entries = ArrayList<WrappedTabListEntry>()

    fun removeEntry(player: Player) { entries.removeIf { it.uuid == player.uniqueId } }
    fun clearEntries() { entries.clear() }
    fun sortEntries() { entries.sortWith(compareBy<WrappedTabListEntry> { it.priority }.thenBy { it.name }) }

    fun addEntry(player: Player) {
        if(entries.any { it.uuid == player.uniqueId }) return
        entries.add(WrappedTabListEntry(player))
    }

    fun writeAll() {
        for(player in Sponge.getServer().onlinePlayers) {
            val tablist = player.tabList
            tablist.entries.forEach { tablist.removeEntry(it.profile.uniqueId) }
        }
        entries.forEach { entry ->
            val nativeEntry = TabListEntry.builder()
                    .displayName(entry.name)
                    .gameMode(entry.gameMode.get())
                    .latency(entry.latency)
                    .profile(entry.player.profile)
                    .build()
            entry.setHeaderAndFooter()
            Sponge.getServer().onlinePlayers.forEach { it.tabList.addEntry(nativeEntry) }
        }
    }

}