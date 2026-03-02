package org.alter.objects.ladder

import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption

/**
 * Handles ladders and trapdoors. Original plugin credit is from the original project, i just made a few changes. https://discord.gg/v2qcXzBCwf 
 *
 * https://github.com/OpenRune/OpenRune-Server
 * Stairs are handled separately in StairsPlugin.
 * Location-specific ladders that teleport to fixed coordinates should be
 * handled in their own area plugin. 
 */
class LadderPlugin : PluginEvent() {

    override fun init() {
        registerLadders()
        registerTrapdoors()
    }

    private fun registerLadders() {
        val ladders = arrayOf(
            // ── Generic ──────────────────────────────────────────────────────
            "objects.ladder",
            "objects.laddertop",
            "objects.laddertop2",
            "objects.laddermiddle",
            "objects.laddertop_directional",
            "objects.ladder_directional",
            "objects.laddermiddle_directional",
            "objects.ladder_cellar",
            "objects.ladder_from_cellar",
            "objects.ladder_cellar_directional",
            "objects.ladder_from_cellar_directional",
            "objects.stepladder",

            // ── Cooking Guild / Quest ──────────────────────────────────────
            "objects.qip_cook_ladder",
            "objects.qip_cook_ladder_middle",
            "objects.qip_cook_ladder_top",

            // ── Varrock ────────────────────────────────────────────────────
            "objects.fai_varrock_ladder",
            "objects.fai_varrock_laddertop",
            "objects.fai_varrock_ladder_taller",
            "objects.fai_varrock_ladder_taller_top",
            "objects.fai_varrock_ladder_deep",
            "objects.fai_varrock_ladder_from_cellar",
            "objects.fai_varrock_manhole_ladder",

            // ── Falador ────────────────────────────────────────────────────
            "objects.fai_falador_ladder",
            "objects.fai_falador_ladder_taller",
            "objects.fai_falador_ladder_top",
            "objects.fai_falador_castle_ladder_up",
            "objects.fai_falador_castle_laddertop",

            // ── Seers / Catherby ───────────────────────────────────────────
            "objects.kr_ladder",
            "objects.kr_laddertop",
            "objects.kr_laddertop_directional",
            "objects.kr_ladder_directional",
            "objects.kr_cam_ladder",
            "objects.kr_cam_laddertop",
            "objects.kr_underground_jail_ladder",
            "objects.kr_underground_jail_laddertop",
            "objects.kr_sin_ladder",
            "objects.kr_sin_laddertop",
            "objects.kr_bkf_basement_ladder",
            "objects.kr_bkf_basement_laddertop",
            "objects.favour_seer_ladder",
            "objects.favour_seer_laddertop",

            // ── Canifis / Morytania ────────────────────────────────────────
            "objects.canafis_ladder_up",
            "objects.canafis_laddertop",
            "objects.canafis_stepladder",

            // ── Kourend / Zeah ─────────────────────────────────────────────
            "objects.piscarilius_ladder_bottom",
            "objects.piscarilius_ladder_top_01",
            "objects.piscarilius_ladder_top_02",
            "objects.piscarilius_ladder_bottom_prison",
            "objects.hosidius_ladder",
            "objects.hosidius_ladder_top",
            "objects.hosidius_basement_ladder_entry",
            "objects.hosidius_basement_ladder_exit",
            "objects.lovakengj_ladder_base",
            "objects.lovakengj_ladder_top",
            "objects.shayzien_wooden_ladder",
            "objects.shayzien_wooden_laddertop",
            "objects.shayzien_ladder",
            "objects.shayzien_laddertop",
            "objects.akd_lookout_ladder",
            "objects.akd_barbarian_ladder",
            "objects.akd_barbarian_ladder_top",
            "objects.carnillean_ladder_up",
            "objects.carnillean_ladder_down",

            // ── Varlamore ──────────────────────────────────────────────────
            "objects.civitas_ladder",
            "objects.civitas_ladder_tall",
            "objects.civitas_ladder_middle",
            "objects.civitas_ladder_top",
            "objects.civitas_ladder_dungeon_inner",
            "objects.civitas_ladder_dungeon_inner_top",
            "objects.civitas_ladder_dungeon_outer",
            "objects.civitas_ladder_dungeon_outer_top",
            "objects.soc_ladder",
            "objects.soc_laddertop",

            // ── Misc dungeons / quests ─────────────────────────────────────
            "objects.wild_ladder",
            "objects.wild_laddertop",
            "objects.wild_ladder_middle",
            "objects.wild_laddertop_dungeon",
            "objects.wild_tower_ladder",
            "objects.wild_tower_laddertop",
            "objects.wizards_tower_ladder",
            "objects.wizards_tower_laddertop",
            "objects.barrows_ladder",
            "objects.chaos_temple_ladder",
            "objects.chaos_temple_laddertop",
            "objects.slayer_tower_dungeon_ladderbottom",
            "objects.slayer_tower_dungeon_laddertop",
            "objects.godwars_debris_ladder",
            "objects.hauntedmine_ladder",
            "objects.hauntedmine_laddertop",
            "objects.motherlode_ladder_bottom",
            "objects.motherlode_ladder_top",
            "objects.motherlode_ladder_bottom_active",
            "objects.motherlode_ladder_top_active",
            "objects.blast_mining_ladder_up",
            "objects.blast_mining_ladder_down",
            "objects.dk_ladder",
            "objects.dk_laddertop",
            "objects.dk_meeting_ladder",
            "objects.dk_meeting_laddertop",
            "objects.dagexp_entrance_ladder",
            "objects.dagexp_exit_ladder",
            "objects.dagexp_bossroomladder_up",
            "objects.dagexp_bossroomladder_down",
            "objects.desert_ladder",
            "objects.elid_ladder_up",
            "objects.elid_laddertop",
            "objects.elid_ladder_bottom",
            "objects.elid_ladder_top",
            "objects.ds2_guild_ladder",
            "objects.ds2_guild_ladder_top",
            "objects.ds2_guild_dungeon_ladder",
            "objects.ds2_tomb_ladder",
            "objects.ds2_tomb_ladder_top",
            "objects.lassar_ladder",
            "objects.lassar_laddertop",
            "objects.lassar_ladder_bottom",
            "objects.lassar_ladder_middle",
            "objects.dt2_lassar_church_ladder",
            "objects.dt2_lassar_church_ladder_top",
            "objects.fossil_tall_ladder_west",
            "objects.fossil_tall_ladder_west_top",
            "objects.fossil_tall_ladder_east",
            "objects.fossil_tall_ladder_east_top",
            "objects.fossil_dkl_trapdoor_ladder",
            "objects.hosdun_entrance_ladder",
            "objects.pog_sewer_ladder_top",
        )

        ladders.forEach { id ->
            onObjectOption(id, "climb") { ladderMenu(player) }
            onObjectOption(id, "climb-up") { climbUpLadder(player) }
            onObjectOption(id, "climb-down") { climbDownLadder(player) }
        }
    }

    private fun registerTrapdoors() {
        // ── Specific trapdoors with hardcoded destination coords ───────────
        onObjectOption("objects.qip_cook_trapdoor_open", "climb-down") {
            player.moveTo(3210, 9616, 0)
        }
        onObjectOption("objects.ladder_from_cellar", "climb-up") {
            player.moveTo(3210, 3216, 0)
        }

        // ── Generic open trapdoors (climb-down goes -1 level) ──────────────
        val genericTrapdoors = arrayOf(
            "objects.trapdoor_open",
            "objects.trapdoor_open_level1",
            "objects.fai_trapdoor",
            "objects.hiddentrapdoor_open",
        )
        genericTrapdoors.forEach { id ->
            onObjectOption(id, "climb-down") { climbDownLadder(player) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun climbUpLadder(player: Player) = climbLadder(player, +1)
    private fun climbDownLadder(player: Player) = climbLadder(player, -1)

    private fun climbLadder(player: Player, deltaZ: Int) {
        player.queue {
            player.animate("sequences.human_reachforladder")
            player.lock()
            wait(2)
            player.moveTo(player.tile.x, player.tile.z, player.tile.height + deltaZ)
            player.unlock()
        }
    }

    private fun ladderMenu(player: Player) {
        player.queue {
            when (options(player, "Climb up the ladder.", "Climb down the ladder")) {
                1 -> climbUpLadder(player)
                2 -> climbDownLadder(player)
            }
        }
    }
}
