package org.alter.plugins.content.combat

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Combat Dummy — an immortal NPC target for testing max hits, accuracy and DPS.
 *
 * Uses npcs.test_combat_dummy rather than poh_combat_dummy_npc because
 * poh_combat_dummy_npc is a POH furniture piece that is not flagged as
 * attackable in the client cache (def.isAttackable() == false), which causes
 * Combat.canEngage to reject the attack regardless of our combatDef.
 * test_combat_dummy is Jagex's own internal test NPC and passes the check.
 *
 * Commands:
 *   ::spawndummy  — spawn a combat dummy on your current tile
 *   ::dummystats  — print your session stats in chat
 *   ::dummyreset  — clear your session stats
 *
 * @author Lignas & Ayakashi
 */

// --- Per-player session attribute keys ---
val DUMMY_SESSION_MAX_HIT   = AttributeKey<Int>()
val DUMMY_SESSION_TOTAL_DMG = AttributeKey<Int>()
val DUMMY_SESSION_HITS      = AttributeKey<Int>()
val DUMMY_SESSION_ZEROS     = AttributeKey<Int>()

private const val DUMMY_NPC = "npcs.test_combat_dummy"

class CombatDummyPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {

        /**
         * Combat definition for the dummy.
         *
         * - hitpoints = 100: round number, clear HP bar.
         * - All defence bonuses = 0: hits land cleanly, damage reflects true
         *   max hit with no defence interference.
         * - respawnDelay = 0: we handle "respawn" via fullNpcDeath instead.
         * - block/death anims: warguild_dummy_spring wobble on hit;
         *   human_death as death fallback — fullNpcDeath intercepts before it
         *   plays, but NpcDeathAction iterates the list so it must be set.
         */
        setCombatDef(DUMMY_NPC) {
            configs {
                attackSpeed  = 0
                respawnDelay = 0
                poisonChance = 0.0
                venomChance  = 0.0
            }
            stats {
                hitpoints = 100
                attack    = 1
                strength  = 1
                defence   = 1
                magic     = 1
                ranged    = 1
            }
            bonuses {
                defenceStab   = 0
                defenceSlash  = 0
                defenceCrush  = 0
                defenceMagic  = 0
                defenceRanged = 0
            }
            anims {
                attack = "sequences.warguild_dummy_spring"
                block  = "sequences.warguild_dummy_spring"
                death  = "sequences.human_death"
            }
        }

        /**
         * Completely override death so the dummy never de-registers.
         * ctx must be cast manually — `npc` convenience property is not
         * available in fullNpcDeath lambdas (confirmed via NpcDeathAction.kt).
         */
        fullNpcDeath(DUMMY_NPC) {
            val dummy = ctx as Npc
            dummy.setCurrentHp(dummy.getMaxHp())
            dummy.clearHits()
        }

        /**
         * ::spawndummy — spawn a combat dummy on your current tile.
         *
         * setActive(true) is required — without it the NPC is invisible to
         * the AI scheduler and some combat checks reject it. KotlinPlugin's
         * own spawnNpc() always sets active=true; we must do the same here.
         */
        onCommand("spawndummy") {
            val p = player
            val dummy = Npc(getRSCM(DUMMY_NPC), p.tile, world)
            dummy.respawns = false
            dummy.setActive(true)
            world.spawn(dummy)
            p.message("Combat dummy spawned. Use ::dummystats to view hit stats, ::dummyreset to clear them.")
        }

        /**
         * ::dummystats — show this session's combat stats in chat.
         */
        onCommand("dummystats") {
            val p = player
            val hits  = p.attr[DUMMY_SESSION_HITS]      ?: 0
            val zeros = p.attr[DUMMY_SESSION_ZEROS]     ?: 0
            val total = p.attr[DUMMY_SESSION_TOTAL_DMG] ?: 0
            val max   = p.attr[DUMMY_SESSION_MAX_HIT]   ?: 0

            if (hits == 0) {
                p.message("You haven't hit the dummy yet this session.")
                return@onCommand
            }

            val avg      = total.toDouble() / hits
            val missRate = zeros.toDouble() / hits * 100

            p.message("<col=ff6600>[Combat Dummy — Session Stats]</col>")
            p.message(
                "Hits: <col=ffffff>$hits</col>  " +
                        "Zeros: <col=ffffff>$zeros</col>  " +
                        "(${String.format("%.1f", missRate)}% zero)"
            )
            p.message(
                "Max hit: <col=ff0000>$max</col>  " +
                        "Total dmg: <col=ffffff>$total</col>  " +
                        "Avg: <col=ffffff>${String.format("%.2f", avg)}</col>"
            )
        }

        /**
         * ::dummyreset — clear this player's session stats.
         */
        onCommand("dummyreset") {
            val p = player
            p.attr.remove(DUMMY_SESSION_MAX_HIT)
            p.attr.remove(DUMMY_SESSION_TOTAL_DMG)
            p.attr.remove(DUMMY_SESSION_HITS)
            p.attr.remove(DUMMY_SESSION_ZEROS)
            p.message("Combat dummy session stats have been reset.")
        }
    }
}

/**
 * Utility object called from the combat strategy pipeline once a hit value
 * is finalised against the dummy.
 *
 * Integration point — add this to each strategy's hit-resolution site:
 *
 *   if (target is Npc && CombatDummy.isDummy(target) && pawn is Player) {
 *       CombatDummy.recordHit(pawn, damage)
 *   }
 */
object CombatDummy {

    private val DUMMY_ID by lazy { getRSCM(DUMMY_NPC) }

    fun isDummy(npc: Npc): Boolean = npc.id == DUMMY_ID

    fun recordHit(player: Player, damage: Int) {
        val hits  = (player.attr[DUMMY_SESSION_HITS]      ?: 0) + 1
        val zeros = (player.attr[DUMMY_SESSION_ZEROS]     ?: 0) + if (damage == 0) 1 else 0
        val total = (player.attr[DUMMY_SESSION_TOTAL_DMG] ?: 0) + damage
        val prev  =  player.attr[DUMMY_SESSION_MAX_HIT]   ?: 0
        val max   = maxOf(prev, damage)

        player.attr[DUMMY_SESSION_HITS]      = hits
        player.attr[DUMMY_SESSION_ZEROS]     = zeros
        player.attr[DUMMY_SESSION_TOTAL_DMG] = total
        player.attr[DUMMY_SESSION_MAX_HIT]   = max

        val colour = when {
            damage == 0  -> "808080"
            damage >= 40 -> "ff0000"
            damage >= 20 -> "ff6600"
            else         -> "ffffff"
        }
        val newMax = if (damage > prev && hits > 1) " <col=ffff00>(New max!)</col>" else ""
        player.message("<col=$colour>You hit the dummy for $damage damage.</col>$newMax")
    }
}
