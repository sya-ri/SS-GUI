package me.syari.ss.gui.page

import me.syari.ss.core.inventory.CreateInventory.inventory
import me.syari.ss.core.inventory.CustomInventory
import me.syari.ss.core.item.CustomItemStack
import me.syari.ss.core.scheduler.CustomScheduler.runLater
import me.syari.ss.gui.Main.Companion.guiPlugin
import me.syari.ss.item.chest.ItemChest
import me.syari.ss.item.chest.PlayerChestData.Companion.chestData
import me.syari.ss.item.general.GeneralItemWithAmount
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag

object ItemPage: Page {
    override val display = "アイテム"
    override val icon = Material.LEATHER

    override fun getItem(player: Player): Map<Int, Pair<CustomItemStack, (() -> Unit)?>> {
        return mutableMapOf<Int, Pair<CustomItemStack, (() -> Unit)?>>().also { map ->
            val chestData = player.chestData
            val generalChest = chestData.general
            val generalChestIcon = CustomItemStack.create(Material.RED_TULIP, "&6一般")
            for (page in 1..generalChest.maxPage) {
                map[page + 11] = generalChestIcon to { openGeneralChest(player, generalChest, page) }
            }
            val equipChest = chestData.equip
            val equipChestIcon = CustomItemStack.create(Material.IRON_CHESTPLATE, "&6装備")
            for (page in 1..equipChest.maxPage) {
                map[page + 20] = equipChestIcon to { openEquipChest(player, equipChest, page) }
            }
            val compassChest = chestData.compass
            val compassChestIcon = CustomItemStack.create(Material.COMPASS, "&6コンパス")
            for (page in 1..compassChest.maxPage) {
                map[page + 29] = compassChestIcon to { openCompassChest(player, compassChest, page) }
            }
        }
    }

    private data class SelectableGeneralItem(val item: GeneralItemWithAmount) {
        var selectCount = 0
            set(value) {
                field = when {
                    value < 0 -> 0
                    value < item.amount -> value
                    else -> item.amount
                }
            }
    }

    private fun openGeneralChest(player: Player, generalChest: ItemChest.General, page: Int) {
        if (!generalChest.isSorted) generalChest.sort()
        val sortType = generalChest.sortType
        val isType = sortType == ItemChest.General.SortType.Type
        val isRarity = sortType == ItemChest.General.SortType.Rarity
        val itemList = generalChest.getList(page)?.map { SelectableGeneralItem(it) }
        var confirmDump = false
        var protectDump = false
        inventory("&9&l通常アイテム", 6) {
            fun CustomInventory.updateItemList() {
                val selectList = mutableListOf<String>()
                itemList?.forEachIndexed { index, generalItem ->
                    val amount = generalItem.item.amount
                    val customItem = generalItem.item.data.itemStack.clone {
                        this.amount = amount
                        val selectCount = generalItem.selectCount
                        display += if (selectCount != 0) {
                            addEnchant(Enchantment.DURABILITY, 0)
                            addItemFlag(ItemFlag.HIDE_ENCHANTS)
                            selectList.add("&7- &a$display &a(x$selectCount)")
                            " &7x$amount &a(x$selectCount)"
                        } else {
                            " &7x$amount"
                        }
                    }
                    item(18 + index, customItem).event(ClickType.SHIFT_LEFT) {
                        generalItem.selectCount += 10
                    }.event(ClickType.LEFT) {
                        generalItem.selectCount += 1
                    }.event(ClickType.SHIFT_RIGHT) {
                        generalItem.selectCount -= 10
                    }.event(ClickType.RIGHT) {
                        generalItem.selectCount -= 1
                    }.event(ClickType.MIDDLE) {
                        generalItem.selectCount = 0
                    }.event {
                        updateItemList()
                    }
                }
                val dumpMessage = if (confirmDump) "&c本当に捨てますか？" else "&c選択したアイテムを捨てる"
                item(53, Material.LAVA_BUCKET, dumpMessage, selectList, shine = confirmDump).event {
                    if (protectDump) return@event
                    if (confirmDump) {
                        itemList?.forEach { generalItem ->
                            generalChest.remove(generalItem.item.data, generalItem.selectCount)
                            generalItem.selectCount = 0
                        }
                        openGeneralChest(player, generalChest, page)
                    } else {
                        confirmDump = true
                        updateItemList()
                        runLater(guiPlugin, 100) {
                            if (confirmDump) {
                                confirmDump = false
                                updateItemList()
                            }
                        }
                    }
                    protectDump = true
                    runLater(guiPlugin, 20) {
                        protectDump = false
                    }
                }
                open(player)
            }

            item(0..6, Material.GRAY_STAINED_GLASS_PANE)
            item(7, Material.YELLOW_STAINED_GLASS_PANE, "&6種類並び替え", shine = isType).event {
                if (isType) return@event
                generalChest.sortType = ItemChest.General.SortType.Type
                openGeneralChest(player, generalChest, page)
            }
            item(8, Material.YELLOW_STAINED_GLASS_PANE, "&6レア度並び替え", shine = isRarity).event {
                if (isRarity) return@event
                generalChest.sortType = ItemChest.General.SortType.Rarity
                openGeneralChest(player, generalChest, page)
            }
            item(9..17, Material.BLACK_STAINED_GLASS_PANE)
            item(45..52, Material.BLACK_STAINED_GLASS_PANE)
            updateItemList()
        }
    }

    private fun openEquipChest(player: Player, equipChest: ItemChest.Equip, page: Int) {
        if (!equipChest.isSorted) equipChest.sort()
        val itemList = equipChest.getList(page)

    }

    private fun openCompassChest(player: Player, compassChest: ItemChest.Compass, page: Int) {
        val itemList = compassChest.getList(page)

    }
}