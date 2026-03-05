// Originally part of OpenRune-Server. Fixed and improved by Lignas & Ayakashi.
// - Removed dependency on auto-generated ConsumableFoodRow (silently never loaded if code-gen hadn't run)
// - Rewrote to use Food enum directly; single HashMap-backed listener for all food items
// - Fixed blighted food being consumed from inventory before wilderness check
package org.alter.items.consumables

import dev.openrune.ServerCacheManager
import org.alter.api.ChatMessageType
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.heal
import org.alter.api.ext.inWilderness
import org.alter.api.ext.message
import org.alter.api.ext.sendRunEnergy
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.game.model.timer.COMBO_FOOD_DELAY
import org.alter.game.model.timer.FOOD_DELAY
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemClickEvent
import org.alter.impl.misc.Food
import org.alter.rscm.RSCM.asRSCM

class ConsumeFood : PluginEvent() {

    private val EAT_FOOD_SOUND = 2393

    private data class FoodInfo(
        val food: Food,
        val index: Int,
        val resolvedIds: List<Int>
    )

    private val foodMap = mutableMapOf<Int, FoodInfo>()

    override fun init() {
        Food.entries.forEach { food ->
            val resolvedIds = food.items.mapNotNull { rscm ->
                try { rscm.asRSCM() } catch (_: Exception) { null }
            }
            resolvedIds.forEachIndexed { index, itemId ->
                foodMap[itemId] = FoodInfo(food, index, resolvedIds)
            }
        }

        on<ItemClickEvent> {
            where {
                item in foodMap &&
                op == MenuOption.OP2 &&
                !player.timers.has(getDelayType(foodMap[item]!!.food.comboFood))
            }
            then {
                val info = foodMap[item]!!
                val itemId = info.resolvedIds.getOrNull(info.index) ?: return@then
                val foodName = ServerCacheManager.getItem(itemId)?.name ?: return@then

                if (foodName.contains("Blighted", true) && !player.inWilderness()) {
                    player.message("The $foodName can be eaten only in the Wilderness", ChatMessageType.FILTERED)
                    return@then
                }

                if (player.inventory.remove(item = item, beginSlot = slot).hasSucceeded()) {
                    eat(player, info, slot)
                }
            }
        }
    }

    private fun getAnimation(player: Player): String {
        return if (player.hasEquipped(EquipmentType.WEAPON, "items.trollromance_toboggon"))
            "sequences.trollromance_toboggan_eat" else "sequences.human_eat"
    }

    private fun eat(player: Player, info: FoodInfo, slot: Int) {
        val food = info.food
        val foodConsumedIndex = info.index
        val itemIds = info.resolvedIds
        val itemId = itemIds.getOrNull(foodConsumedIndex) ?: return
        val foodName = ServerCacheManager.getItem(itemId)?.name ?: return
        val isBlighted = foodName.contains("Blighted", true)
        val isSweets = foodName.contains("sweets", true)

        val anim = getAnimation(player)

        var healAmount = food.heal
        val overheal = food.overheal && !player.inWilderness()
        val comboFood = food.comboFood
        val foodDelays = food.eatDelay
        val combatDelays = food.combatDelay

        if (healAmount == -1) {
            healAmount = when {
                isSweets -> (1..3).random()
                isBlighted -> calculateDynamicBlightedHeal(player)
                else -> calculateDynamicHeal(player)
            }
        }

        val oldHp = player.getSkills().getCurrentLevel(Skills.HITPOINTS)
        player.animate(anim)
        player.playSound(EAT_FOOD_SOUND)

        if (healAmount > 0) {
            player.heal(healAmount, if (overheal) healAmount else 0)
        }

        player.timers[getDelayType(comboFood)] = getFoodDelay(foodDelays, foodConsumedIndex)
        player.timers[ATTACK_DELAY] = getCombatDelay(combatDelays, foodConsumedIndex)

        player.resetFacePawn()

        if (isSweets) {
            val energyPerSweet = 1000.0
            val maxEnergy = 10000.0
            if (player.runEnergy < maxEnergy) {
                val actualRestored = ((player.runEnergy + energyPerSweet * 1).coerceAtMost(maxEnergy)) - player.runEnergy
                player.runEnergy += actualRestored
                player.sendRunEnergy(player.runEnergy.toInt())
                player.message("You eat the sweets. The sugary goodness heals some energy.", ChatMessageType.FILTERED)
            }
        } else {
            player.message("You eat the ${foodName.lowercase()}.")
        }
        if (player.getSkills().getCurrentLevel(Skills.HITPOINTS) > oldHp) {
            player.message("It heals some health.")
        }

        handleMultiFoodReplacement(player, itemIds, foodConsumedIndex, slot)
    }

    private fun getDelayType(comboFood: Boolean) = if (comboFood) COMBO_FOOD_DELAY else FOOD_DELAY

    private fun calculateDynamicHeal(player: Player): Int {
        val hp = player.getSkills().getBaseLevel(Skills.HITPOINTS)
        return hp / 10 + 2 * (hp / 25) + 5 * (hp / 93) + 2
    }

    private fun calculateDynamicBlightedHeal(player: Player): Int {
        val hp = player.getSkills().getBaseLevel(Skills.HITPOINTS)
        val c = when (hp) {
            in 1..24 -> 2
            in 25..49 -> 4
            in 50..74 -> 6
            in 75..92 -> 8
            in 93..99 -> 13
            else -> 0
        }
        return hp / 10 + c
    }

    private fun getFoodDelay(delays: List<Int>, index: Int) = delays.getOrNull(index) ?: 3
    private fun getCombatDelay(delays: List<Int>, index: Int) = delays.getOrNull(index) ?: 3
    private fun handleMultiFoodReplacement(player: Player, itemIds: List<Int>, index: Int, slot: Int) {
        if (itemIds.size > 1 && index + 1 < itemIds.size) {
            player.inventory.add(item = itemIds[index + 1], beginSlot = slot)
        }
    }
}
