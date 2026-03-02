package org.alter.objects.stairs

import dev.openrune.ServerCacheManager.getObject
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM.getRSCM

/**
 * Generic staircase handler. Checkout openrune https://github.com/OpenRune/OpenRune-Server
 *
 * Registers every stair/spiral-stair RSCM name extracted from gamevals.dat.
 * All stairs simply move the player ±1 height level, or show a menu
 * if the stair object has both climb-up and climb-down options.
 *
 * Location specific stairs that require teleport to fixed coordinates
 * ex Lumbridge castle, Al-Kharid should be handled in their own
 * area plugin and will naturally override these generic handlers.
 */
class StairsPlugin : PluginEvent() {

    // Every stair RSCM name from gamevals.dat [gamevals.objects].
    // Skewsteps, decoration-only steps, and purely visual objects are excluded.
    private val ALL_STAIRS = arrayOf(
        // ── Generic / reused ──────────────────────────────────────────────────
        "objects.stairs",
        "objects.stairs_higher",
        "objects.stairstop",
        "objects.stairstop_higher",
        "objects.stairsdown",
        "objects.stairs_shallow_base",
        "objects.stairs_shallow_top",
        "objects.stairs_cellar",
        "objects.stairs_from_cellar",
        "objects.woodenstairs",
        "objects.woodenstairstop",
        "objects.woodenstairsdown",
        "objects.outdoorstairs_wooden_top",
        "objects.outdoorstairs_wooden_bottom",
        "objects.narrowstairs_wooden_top",
        "objects.narrowstairs_wooden_bottom",
        "objects.spiralstairs",
        "objects.spiralstairsmiddle",
        "objects.spiralstairstop",
        "objects.spiralstairsdown",
        "objects.spiralstairs_wooden",
        "objects.spiralstairsmiddle_wooden",
        "objects.spiralstairstop_wooden",
        "objects.spiralstairsdown_wooden",
        "objects.spiralstairsbottom_3",
        "objects.spiralstairstop_3",
        "objects.spookystairs",
        "objects.spookystairstop",
        "objects.spookystairsdown",
        "objects.poor_stairs",
        "objects.poor_stairstop",
        "objects.desertoutsidestairs",
        "objects.yanillestairsdown",
        "objects.yanillestairsup",

        // ── Falador ───────────────────────────────────────────────────────────
        "objects.fai_falador_stairs",
        "objects.fai_falador_stairstop",
        "objects.fai_falador_spiralstairs",
        "objects.fai_falador_spiralstairstop",
        "objects.fai_falador_castle_stairs",
        "objects.fai_falador_castle_stairstop",
        "objects.fai_falador_castle_spiralstairs",
        "objects.fai_falador_castle_spiralstairstop",
        "objects.fai_falador_outdoorstairs_bottom",
        "objects.fai_falador_outdoorstairs_top",
        "objects.fai_falador_party_room_spiralstairs",
        "objects.fai_falador_party_room_spiralstairs_small",
        "objects.fai_falador_party_room_spiralstairs_top",
        "objects.fai_falador_party_room_spiralstairs_top_small",
        "objects.stairs_falador",
        "objects.stairstop_falador",

        // ── Varrock ───────────────────────────────────────────────────────────
        "objects.varrock_spiralstairs",
        "objects.varrock_spiralstairs_taller",
        "objects.varrock_spiralstairs_middle",
        "objects.varrock_spiralstairs_middle_taller",
        "objects.varrock_spiralstairstop",
        "objects.fai_varrock_stairs",
        "objects.fai_varrock_stairs_taller",
        "objects.fai_varrock_stairs_taller_new_fix",
        "objects.fai_varrock_woodenstairs_castle",
        "objects.fai_varrock_stairs_top",
        "objects.fai_varrock_bank_stairs",
        "objects.fai_varrock_stairs_from_cellar",
        "objects.fai_varrock_mill_spiralstairs",
        "objects.fai_varrock_mill_spiralstairs_middle",
        "objects.fai_varrock_mill_spiralstairs_top",
        "objects.fai_darkwiztower_spiralstairs",
        "objects.fai_darkwiztower_spiralstairs_middle",
        "objects.fai_darkwiztower_spiralstairstop",
        "objects.fai_wiztower_spiralstairs",
        "objects.fai_wiztower_spiralstairs_middle",
        "objects.fai_wiztower_spiralstairstop",
        "objects.draynor_spiralstairs",
        "objects.draynor_manor_stairs_up",
        "objects.draynor_manor_stairs_down",

        // ── Seers / Catherby ──────────────────────────────────────────────────
        "objects.kr_stairs",
        "objects.kr_stairstop",
        "objects.kr_spiralstairs",
        "objects.kr_spiralstairstop",
        "objects.kr_cam_woodenstairs",
        "objects.kr_cam_woodenstairstop",
        "objects.kr_court_stairs",
        "objects.kr_courthouse_stairs_bottom_01",
        "objects.kr_courthouse_stairs_bottom_02",
        "objects.kr_courthouse_stairs_bottom_03",
        "objects.kr_courthouse_stairs_top",
        "objects.murder_qip_spiralstairs",
        "objects.murder_qip_spiralstairstop",

        // ── Canifis / Morytania ───────────────────────────────────────────────
        "objects.canafis_spiralstairs",
        "objects.canafis_spiralstairstop",

        // ── Keldagrim / Dwarves ───────────────────────────────────────────────
        "objects.dwarf_keldagrim_stairs_lower",
        "objects.dwarf_keldagrim_stairs_upper",
        "objects.dwarf_keldagrim_wide_stairs_lower",
        "objects.dwarf_keldagrim_wide_stairs_upper",
        "objects.dwarf_keldagrim_wide_stairs_3_lower",
        "objects.dwarf_keldagrim_wide_stairs_3_upper",
        "objects.vc_keldagrim_stairs_lower",
        "objects.vc_keldagrim_stairs_upper",
        "objects.blast_furnace_stairs_up",

        // ── Grand Exchange ────────────────────────────────────────────────────
        "objects.exchange_stairs_var01",
        "objects.exchange_stairs_var01_63",
        "objects.exchange_stairs_var01_64",
        "objects.exchange_stairs_var01_65",
        "objects.exchange_stairs_var01_66",
        "objects.exchange_stairs_var01_67",
        "objects.exchange_stairs_var01_69",
        "objects.exchange_stairs_var01_70",
        "objects.exchange_stairs_var02",

        // ── Kourend / Zeah ────────────────────────────────────────────────────
        "objects.archeuus_stairs_lower",
        "objects.archeuus_stairs_upper",
        "objects.archeuus_stairs_lower_right",
        "objects.archeuus_stairs_lower_left",
        "objects.archeuus_stairs_upper_right",
        "objects.archeuus_stairs_upper_left",
        "objects.piscarilius_bank_stairs",
        "objects.shayzien_stairs_bottom_01",
        "objects.shayzien_stairs_top_01",
        "objects.shayzien_stairs_bottom_02",
        "objects.shayzien_stairs_top_02",
        "objects.shayzien_stairs_bottom_03",
        "objects.shayzien_stairs_top_03",
        "objects.shayzien_manor_stairs",
        "objects.shayzien_manor_stairstop",
        "objects.akd_shayzien_bar_spiralstairs",
        "objects.akd_shayzien_bar_spiralstairstop",
        "objects.kebos_spiralstairs_bottom",
        "objects.kebos_spiralstairs_top",
        "objects.karuulm_wall_stairs",
        "objects.karuulm_wall_stairs_top",
        "objects.darkm_rich_staircase_bottom",
        "objects.darkm_rich_staircase_top",
        "objects.zeah_cata_stairs_01",
        "objects.lovaquest_spiral_stairs",
        "objects.lovaquest_spiral_stairs_m",
        "objects.lovaquest_spiral_stairs_top",
        "objects.lovaquest_spiral_stairs_top_m",

        // ── Prifddinas ────────────────────────────────────────────────────────
        "objects.prif_crystal_staircase_a",
        "objects.prif_crystal_staircase_b",
        "objects.prif_crystal_staircase_c",
        "objects.prif_crystal_staircase_d",
        "objects.prif_crystal_staircase_e",
        "objects.prif_crystal_staircase_f",
        "objects.prif_crystal_staircase_g",
        "objects.prif_crystal_staircase_h",
        "objects.prif_crystal_staircase_i",
        "objects.prif_crystal_staircase_j",
        "objects.prif_crystal_staircase_k",
        "objects.prif_crystal_staircase_l",
        "objects.prif_crystal_staircase_m",
        "objects.prif_crystal_staircase_top_arches",

        // ── Lunar Isle / Moonclan ─────────────────────────────────────────────
        "objects.lunar_moonclan_stairs",
        "objects.lunar_moonclan_stairs_top",
        "objects.lunar_moonclan_stairs_top_west",
        "objects.sarim_spiralstairs",
        "objects.sarim_spiralstairstop",

        // ── Misc dungeons / quests ────────────────────────────────────────────
        "objects.tunnelstairs",
        "objects.tunnelstairstop",
        "objects.tunnelstairs2",
        "objects.tunnelstairstop2",
        "objects.ikov_darkstairs",
        "objects.ikov_darkstairstop",
        "objects.ikov_darkstairsdown",
        "objects.slayer_stairs_lv1",
        "objects.slayer_stairs_lv1_top",
        "objects.slayer_stairs_lv2",
        "objects.slayer_stairs_lv2_top",
        "objects.slayer_stairs_lv3",
        "objects.slayer_stairs_lv3_top",
        "objects.plaguehousestairsdown",
        "objects.plaguehousestairsup",
        "objects.hazeelcultstairs",
        "objects.cryptstairsdown",
        "objects.cryptstairsup",
        "objects.troll_stronghold_stairs",
        "objects.troll_stronghold_stairstop",
        "objects.magictraining_stairs_base",
        "objects.magictraining_stairs_upper",
        "objects.magictraining_stairs_top",
        "objects.magictraining_stairs_top2",
        "objects.spiralstairs_with_killerwatt_check",
        "objects.fenk_stairs_lv1",
        "objects.fenk_stairs_lv1_top",
        "objects.harmless_black_spiral_stairs",
        "objects.elemental_workshop_spiralstairs",
        "objects.elemental_workshop_spiralstairstop",
        "objects.horror_lighthouse_spiralstairs_base",
        "objects.horror_lighthouse_spiralstairs_middle",
        "objects.horror_lighthouse_spiralstairs_top",
        "objects.hauntedmine_dark_stairs_top",
        "objects.hauntedmine_dark_stairs_bottom",
        "objects.hauntedmine_light_stairs_bottom",
        "objects.dk_spiralstairs",
        "objects.dk_spiralstairstop",
        "objects.wild6_spiralstairs",
        "objects.wild6_spiralstairs_middle",
        "objects.wild6_spiralstairstop",
        "objects.wildy_hub_stairs",
        "objects.wildy_hub_stairs_cave",
        "objects.wildy_hub_stairs_top",
        "objects.dorgesh_1stairs",
        "objects.dorgesh_1stairs_top",
        "objects.dorgesh_1stairs_posh",
        "objects.dorgesh_1stairs_posh_top",
        "objects.dorgesh_2stairs_internal",
        "objects.dorgesh_2stairs_internal_top",
        "objects.dorgesh_2stairs_posh",
        "objects.dorgesh_2stairs_posh_top",
        "objects.karam_dungeon_cavestairs",
        "objects.karam_dungeon_cavestairs_lev2",
        "objects.karam_dungeon_cavestairs_lev2_down",
        "objects.karam_dungeon_cavestairs2",
        "objects.karam_dungeon_cavestairs_lev2_down2",
        "objects.barrows_stairs_dharok",
        "objects.barrows_stairs_guthan",
        "objects.barrows_stairs_karil",
        "objects.barrows_stairs_torag",
        "objects.barrows_stairs_verac",
        "objects.carnillean_stairs",
        "objects.carnillean_stairstop",
        "objects.slp_manor_stairs",
        "objects.slp_manor_stairs_top",
        "objects.slp_posh_stairs_lower",
        "objects.slp_posh_stairs_upper",
        "objects.elid_mayor_stairs",
        "objects.elid_mayor_stairstop",
        "objects.golem_stairs_up",
        "objects.golem_stairs_down",
        "objects.icthalarins_stairs_lv1",
        "objects.icthalarins_stairs_lv1_top",
        "objects.ogre_stairs",
        "objects.ogre_stairs_down",
        "objects.mourning_temple_stairs_base",
        "objects.mourning_temple_stairs_top",
        "objects.mourning_temple_circle_stairs_base",
        "objects.mourning_temple_circle_stairs_top",
        "objects.myq3_hideout_stairs_down",
        "objects.myq3_hideout_stairs_up",
        "objects.myq3_lab_stairs_down",
        "objects.myq3_lab_stairs_up",
        "objects.frisd_izso_spiral_stairs",
        "objects.fossil_dkl_staircase",
        "objects.fossil_dkl_staircase_top",
        "objects.fossil_cave_stairs_1_lower",
        "objects.fossil_cave_stairs_1_top",
        "objects.fossil_cave_stairs_2_lower",
        "objects.fossil_cave_stairs_2_top",
        "objects.fossil_volcano_staircase_bottom",
        "objects.fossil_volcano_staircase_top",
        "objects.ds2_guild_mid_stairs",
        "objects.ds2_guild_mid_stairs_top",
        "objects.ds2_guild_stairs_up",
        "objects.ds2_guild_stairs_down",
        "objects.ds2_corsair_cove_stairs",
        "objects.ds2_corsair_cove_stairs_down",
        "objects.ds2_ungael_staircase",
        "objects.ds2_tomb_stairs",
        "objects.ds2_tomb_stairs_top",
        "objects.ds2_lithkren_surface_staircase_down",
        "objects.ds2_lithkren_surface_staircase_1",
        "objects.ds2_lithkren_surface_staircase_1_top",
        "objects.ds2_lithkren_dungeon_stairs_active",
        "objects.ds2_forge_stairs_top",
        "objects.ds2_forge_stairs_bottom",
        "objects.tob_treasureroom_stairsup",
        "objects.tob_dungeon_treasure_room_stairs_right",
        "objects.tob_dungeon_treasure_room_stairs_left",
        "objects.my2arm_throneroom_stairsdown",
        "objects.my2arm_fortress_stairs_main",
        "objects.my2arm_fortress_stairs_top_large",
        "objects.my2arm_fortress_stairs_top_small",
        "objects.deal_stairs_bottom",
        "objects.deal_stairs_top",
        "objects.mm_stairs_top",
        "objects.mm_stairs_base",
        "objects.romeo_juliet_stairs_up",
        "objects.ahoy_cavern_stairs",
        "objects.ahoy_cavern_stairs_top",
        "objects.ahoy_tower_stairs_lv1",
        "objects.ahoy_tower_stairs_lv1_top",
        "objects.rehnisonstairs",
        "objects.rehnisonstairstop",
        "objects.bh_portal_stairs",
        "objects.hallowed_progress_stairs",
        "objects.hallowed_stairs_return",
        "objects.hallowed_floor_1_eastpath_stairs",
        "objects.hallowed_floor_1_southpath_stairs",
        "objects.hallowed_floor_2_northpath_stairs",
        "objects.hallowed_floor_2_eastpath_stairs",
        "objects.hallowed_floor_2_southpath_stairs",
        "objects.hallowed_floor_4_southpath_stairs",
        "objects.hallowed_stairs_floor1",
        "objects.hallowed_stairs_floor2",
        "objects.hallowed_stairs_floor3",
        "objects.hallowed_stairs_floor4",
        "objects.hs_stairs_wide_up_01",
        "objects.hs_stairs_wide_down_01",
        "objects.hs_stairs_narrow_up_01",
        "objects.hs_stairs_narrow_down_01",
        "objects.bcs_tomb_stairs",
        "objects.bcs_tomb_stairs_top",
        "objects.toa_vault_stairs01",
        "objects.toa_vault_stairs02",
        "objects.toa_vault_stairs03",
        "objects.dt2_vault_exit_staircase",
        "objects.dt2_vault_main_staircase",
        "objects.dt2_vault_spiral_staircase",
        "objects.dt2_vault_staircase_bottom",
        "objects.dt2_duke_staircase01",
        "objects.dt2_duke_staircase02",
        "objects.dt2_duke_staircase03",
        "objects.dt2_duke_staircase04",
        "objects.dt2_duke_staircase05",
        "objects.spiralstairstop_wg",

        // ── Varlamore ─────────────────────────────────────────────────────────
        "objects.civitas_palace_stairs_up",
        "objects.civitas_palace_stairs_down",
        "objects.civitas_stairs_spiral",
        "objects.civitas_stairs_spiral_down",
        "objects.civitas_stairs_1x3",
        "objects.civitas_stairs_1x3_down",
        "objects.civitas_stairs_bottom_mine",
        "objects.civitas_stairs_top_mine",
        "objects.fortis_wooden_spiralstairs_bottom",
        "objects.fortis_wooden_spiralstairs_middle",
        "objects.fortis_wooden_spiralstairs_top",
        "objects.fortis_wooden_spiralstairs_bottom_1_floor",
        "objects.fortis_wooden_spiralstairs_top_1_floor",
        "objects.varlamore_thieving_house_stairs",
        "objects.varlamore_thieving_house_stairstop",
        "objects.hunterguild_stairs_down01",
        "objects.hunterguild_stairs_up01",
        "objects.avium_sunset_stairs_up",
        "objects.avium_sunset_stairs_down",
        "objects.aldarin_stairs_1x2",
        "objects.aldarin_stairs_1x2_down",
        "objects.aldarin_winery_stairs",
        "objects.aldarin_winery_stairs_entry",
        "objects.aldarin_stairs_cellar_underground_down",
        "objects.conch_tortugan_staircase_bottom",
        "objects.conch_tortugan_staircase_top",
        "objects.last_light_spiralstairs_base",
        "objects.last_light_spiralstairs_middle",
        "objects.last_light_spiralstairs_top",
    )

    // ── Landing position overrides ────────────────────────────────────────────
    //
    // The generic climb handler moves the player to the same x/y tile ±1 height,
    // which places them on the staircase object rather than at the natural OSRS
    // "in front of the stairs" landing position.
    //
    // Each entry maps (rscm, playerX, playerZ) -> (destinationX, destinationZ).
    // Height is still always ±1; only x/y changes.
    //
    // NOTE: Some RSCMs (ex fai_varrock_stairs_taller_new_fix) are reused across
    // multiple cities facing different directions, so offsets are per-tile, not
    // per-RSCM.
    //
    // To add a new entry: stand in front of the staircase, note X/Y and object ID,
    // look up the RSCM name from gamevals.dat, and add a row below in the form:
    //   Triple("objects.rscm_name", fromX, fromZ) to Pair(toX, toZ)
    //
    private val LANDING_OVERRIDES: Map<Triple<String, Int, Int>, Pair<Int, Int>> = mapOf(

        // ── Falador ──────────────────────────────────────────────────────────
        // fai_falador_stairs (ID 24079) / fai_falador_stairstop (ID 24080)
        // Faces north; top landing is +4 Z from bottom approach tile.
        // #1 Falador (White Knights' Castle west staircase)
        Triple("objects.fai_falador_stairs",    2959, 3368) to Pair(2959, 3372),
        Triple("objects.fai_falador_stairs",    2960, 3368) to Pair(2960, 3372),
        Triple("objects.fai_falador_stairstop", 2959, 3372) to Pair(2959, 3368),
        Triple("objects.fai_falador_stairstop", 2960, 3372) to Pair(2960, 3368),
        // #2 Falador (east staircase)
        Triple("objects.fai_falador_stairs",    2971, 3369) to Pair(2971, 3373),
        Triple("objects.fai_falador_stairs",    2972, 3369) to Pair(2972, 3373),
        Triple("objects.fai_falador_stairstop", 2971, 3373) to Pair(2971, 3369),
        Triple("objects.fai_falador_stairstop", 2972, 3373) to Pair(2972, 3369),

        // fai_falador_spiralstairs (ID 24075) — single-tile spiral
        // #3 Falador castle spiral: top landing is (-1 X, -1 Z)
        Triple("objects.fai_falador_spiralstairs", 2973, 3386) to Pair(2972, 3385),

        // ── Varrock ───────────────────────────────────────────────────────────
        // fai_varrock_stairs (ID 11796) — faces east, top landing is +4 X
        // #4 Varrock (pub / general store staircase)
        Triple("objects.fai_varrock_stairs",    3226, 3394) to Pair(3230, 3394),
        Triple("objects.fai_varrock_stairs",    3226, 3393) to Pair(3230, 3393),
        // fai_varrock_stairs_top (ID 11799) — going back down from #4
        Triple("objects.fai_varrock_stairs_top", 3230, 3394) to Pair(3226, 3394),
        Triple("objects.fai_varrock_stairs_top", 3230, 3393) to Pair(3226, 3393),

        // fai_varrock_stairs_taller_new_fix (ID 11807) — reused in multiple cities
        // #5 Varrock (Grand Exchange area) — faces north, top landing is +4 Z
        Triple("objects.fai_varrock_stairs_taller_new_fix", 3212, 3472) to Pair(3212, 3476),
        Triple("objects.fai_varrock_stairs_taller_new_fix", 3213, 3472) to Pair(3213, 3476),
        // fai_varrock_stairs_top (ID 11799) — going back down from #5
        Triple("objects.fai_varrock_stairs_top", 3212, 3476) to Pair(3212, 3472),
        Triple("objects.fai_varrock_stairs_top", 3213, 3476) to Pair(3213, 3472),

        // #8 Kourend — same RSCM as #5 but faces west, top landing is -4 X
        Triple("objects.fai_varrock_stairs_taller_new_fix", 1618, 3680) to Pair(1614, 3680),
        Triple("objects.fai_varrock_stairs_taller_new_fix", 1618, 3681) to Pair(1614, 3681),
        // fai_varrock_stairs_top (ID 11799) — going back down from #8
        Triple("objects.fai_varrock_stairs_top", 1614, 3680) to Pair(1618, 3680),
        Triple("objects.fai_varrock_stairs_top", 1614, 3681) to Pair(1618, 3681),
    )

    override fun init() {
        ALL_STAIRS.forEach { rscm ->
            // Safely skip any names that don't exist in this server's cache
            val numId = try { getRSCM(rscm) } catch (e: Exception) { return@forEach }
            val actions = getObject(numId)?.actions?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()

            if (actions.any { it.equals("climb", ignoreCase = true) }) {
                onObjectOption(rscm, "climb") { stairsMenu(player, rscm) }
            }
            if (actions.any { it.equals("climb-up", ignoreCase = true) }) {
                onObjectOption(rscm, "climb-up") { climbUpStairs(player, rscm) }
            }
            if (actions.any { it.equals("climb-down", ignoreCase = true) }) {
                onObjectOption(rscm, "climb-down") { climbDownStairs(player, rscm) }
            }
            // Some stairs only have "climb-up" or only "climb-down" with no menu
            // If neither climb-up nor climb-down nor climb is defined, fall back to both
            if (actions.none { it.lowercase() in setOf("climb", "climb-up", "climb-down") }) {
                onObjectOption(rscm, "climb") { stairsMenu(player, rscm) }
                onObjectOption(rscm, "climb-up") { climbUpStairs(player, rscm) }
                onObjectOption(rscm, "climb-down") { climbDownStairs(player, rscm) }
            }
        }
    }

    /** Returns the overridden landing (x, z) for this player's tile, or null to use same tile. */
    private fun landingFor(player: Player, rscm: String): Pair<Int, Int>? =
        LANDING_OVERRIDES[Triple(rscm, player.tile.x, player.tile.z)]

    private fun climbUpStairs(player: Player, rscm: String) {
        val (toX, toZ) = landingFor(player, rscm) ?: Pair(player.tile.x, player.tile.z)
        player.moveTo(toX, toZ, player.tile.height + 1)
    }

    private fun climbDownStairs(player: Player, rscm: String) {
        val (toX, toZ) = landingFor(player, rscm) ?: Pair(player.tile.x, player.tile.z)
        player.moveTo(toX, toZ, player.tile.height - 1)
    }

    private fun stairsMenu(player: Player, rscm: String) {
        player.queue {
            when (options(player, "Climb up the stairs.", "Climb down the stairs.")) {
                1 -> climbUpStairs(player, rscm)
                2 -> climbDownStairs(player, rscm)
            }
        }
    }
}