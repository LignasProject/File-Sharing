package org.alter.objects.DoorOpens

import dev.openrune.ServerCacheManager.getObject
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GameObject
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM.getRSCM

/**
 * Generic door/gate open-close plugin.
 *
 * @author Lignas & Ayakashi
 *
 * Confirmed patterns from Discord:
 *  - Use player.world (not inherited PluginEvent.world)
 *  - Use world.queue {}
 *  - Rotation must change when opening/closing — direction depends on hinge side
 *    Left hinge:  openRot = (closedRot + 3) % 4
 *    Right hinge: openRot = (closedRot + 1) % 4
 *
 * All RSCM pairs extracted from gamevals.dat [gamevals.objects].
 * Special/locked/quest-gated doors should be handled in their own area plugins.
 */
class DoorPlugin : PluginEvent() {

    private enum class Hinge { LEFT, RIGHT }

    private data class SingleDoor(
        val closedRscm: String,
        val openRscm:   String,
        val hinge:      Hinge = Hinge.LEFT
    )

    private data class DoubleDoor(
        val closedLeft:  String,
        val closedRight: String,
        val openedLeft:  String,
        val openedRight: String
    )

    // ── Single door definitions ───────────────────────────────────────────────
    // All closed→open pairs extracted from gamevals.dat.
    // Hinge side is inferred from name suffix (_l/_left = LEFT, _r/_right = RIGHT).
    // If a door behaves incorrectly, add an area-specific override plugin.

    private val SINGLE_DOORS = listOf(

        // ── Generic ───────────────────────────────────────────────────────────
        SingleDoor("objects.ahoy_harbour_door", "objects.ahoy_harbour_door_open", Hinge.LEFT),
        SingleDoor("objects.ahoy_town_walldoor_left", "objects.ahoy_town_walldoor_left_open", Hinge.LEFT),
        SingleDoor("objects.ahoy_town_walldoor_right", "objects.ahoy_town_walldoor_right_open", Hinge.RIGHT),
        SingleDoor("objects.ahoy_trapdoor", "objects.ahoy_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.aide_ornaterailing_gate", "objects.aide_ornaterailing_gate_open", Hinge.LEFT),
        SingleDoor("objects.akd_lookout_trapdoor", "objects.akd_lookout_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.akd_lookout_trapdoor_closed", "objects.akd_lookout_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.akd_rose_basement_door", "objects.akd_rose_basement_door_open", Hinge.LEFT),
        SingleDoor("objects.akd_temple_door", "objects.akd_temple_door_open", Hinge.LEFT),
        SingleDoor("objects.akd_xamphur_prisongate", "objects.akd_xamphur_prisongateopen", Hinge.LEFT),
        SingleDoor("objects.archeddoorclosed", "objects.archeddooropen", Hinge.LEFT),
        SingleDoor("objects.arcquest_tower_door_left", "objects.arcquest_tower_door_left_open", Hinge.LEFT),
        SingleDoor("objects.arcquest_tower_door_right", "objects.arcquest_tower_door_right_open", Hinge.RIGHT),
        SingleDoor("objects.bedabin_tentdoor", "objects.bedabin_tentdoor_open", Hinge.LEFT),
        SingleDoor("objects.blackarmdoor", "objects.blackarmdoor_open", Hinge.LEFT),
        SingleDoor("objects.bluedoor", "objects.bluedoor_open", Hinge.LEFT),
        SingleDoor("objects.brain_mill_doubledoorl", "objects.brain_mill_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.brain_mill_doubledoorr", "objects.brain_mill_doubledoorr_open", Hinge.RIGHT),
        SingleDoor("objects.castlewars_saradomin_maindoorl", "objects.castlewars_saradomin_maindoorl_open", Hinge.LEFT),
        SingleDoor("objects.castlewars_saradomin_maindoorr", "objects.castlewars_saradomin_maindoorr_open", Hinge.RIGHT),
        SingleDoor("objects.castlewars_saradomin_sidedoor", "objects.castlewars_saradomin_sidedoor_open", Hinge.LEFT),
        SingleDoor("objects.castlewars_zamorak_maindoorl", "objects.castlewars_zamorak_maindoorl_open", Hinge.LEFT),
        SingleDoor("objects.castlewars_zamorak_maindoorr", "objects.castlewars_zamorak_maindoorr_open", Hinge.RIGHT),
        SingleDoor("objects.castlewars_zamorak_sidedoor", "objects.castlewars_zamorak_sidedoor_open", Hinge.LEFT),
        SingleDoor("objects.championdoor", "objects.championdoor_open", Hinge.LEFT),
        SingleDoor("objects.champions_trap_door_closed", "objects.champions_trap_door_open", Hinge.LEFT),
        SingleDoor("objects.chefdoor", "objects.chefdoor_open", Hinge.LEFT),
        SingleDoor("objects.contact_temple_trapdoor_closed", "objects.contact_temple_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.darkm_middle_door_closed", "objects.darkm_middle_door_open", Hinge.LEFT),
        SingleDoor("objects.darkm_poor_door_closed", "objects.darkm_poor_door_open", Hinge.LEFT),
        SingleDoor("objects.darkm_rich_door_closed", "objects.darkm_rich_door_open", Hinge.LEFT),
        SingleDoor("objects.deal_gate_closed", "objects.deal_gate_open", Hinge.LEFT),
        SingleDoor("objects.desert_door_l", "objects.desert_door_l_open", Hinge.LEFT),
        SingleDoor("objects.desert_door_r", "objects.desert_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.desertdoorclosed", "objects.desertdooropen", Hinge.LEFT),
        SingleDoor("objects.door", "objects.door_open", Hinge.LEFT),
        SingleDoor("objects.dorgesh_inner_door_closed", "objects.dorgesh_inner_door_open", Hinge.LEFT),
        SingleDoor("objects.dorgesh_inner_door_posh_closed", "objects.dorgesh_inner_door_posh_open", Hinge.LEFT),
        SingleDoor("objects.dorgesh_nursery_door_closed", "objects.dorgesh_nursery_door_open", Hinge.LEFT),
        SingleDoor("objects.dov_base_door_closed", "objects.dov_base_door_open", Hinge.LEFT),
        SingleDoor("objects.dragon_slayer_qip_magic_door", "objects.dragon_slayer_qip_magic_door_open", Hinge.LEFT),
        SingleDoor("objects.dragonsecretdoor", "objects.dragonsecretdoor_open", Hinge.LEFT),
        SingleDoor("objects.draynor_panelled_door", "objects.draynor_panelled_door_open", Hinge.LEFT),
        SingleDoor("objects.dttd_ham_trapdoor", "objects.dttd_ham_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.dttd_ham_trapdoor_closed", "objects.dttd_ham_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.dttd_mill_trapdoor", "objects.dttd_mill_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.dttd_mill_trapdoor_closed", "objects.dttd_mill_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.eadgar_storeroomdoor", "objects.eadgar_storeroomdoor_open", Hinge.LEFT),
        SingleDoor("objects.eaglepeak_eagle_gate_wing", "objects.eaglepeak_eagle_gate_wing_open", Hinge.LEFT),
        SingleDoor("objects.elem2_door_mind", "objects.elem2_door_mind_open", Hinge.LEFT),
        SingleDoor("objects.elem2_door_mindl", "objects.elem2_door_mindl_open", Hinge.LEFT),
        SingleDoor("objects.elem2_stairs_door", "objects.elem2_stairs_door_open", Hinge.LEFT),
        SingleDoor("objects.elenadoor2", "objects.elenadoor2open", Hinge.LEFT),
        SingleDoor("objects.elf_village_treegate", "objects.elf_village_treegate_open", Hinge.LEFT),
        SingleDoor("objects.elid_genie_door", "objects.elid_genie_door_open", Hinge.LEFT),
        SingleDoor("objects.elid_underground_lake_door", "objects.elid_underground_lake_door_open", Hinge.LEFT),
        SingleDoor("objects.elid_underground_robe_door", "objects.elid_underground_robe_door_open", Hinge.LEFT),
        SingleDoor("objects.fai_barbarian_poordoor", "objects.fai_barbarian_poordooropen", Hinge.LEFT),
        SingleDoor("objects.fai_wiztower_poor_door", "objects.fai_wiztower_poor_door_open", Hinge.LEFT),
        SingleDoor("objects.fairy_mushroom_gate_door_l", "objects.fairy_mushroom_gate_door_l_open", Hinge.LEFT),
        SingleDoor("objects.fairy_mushroom_gate_door_r", "objects.fairy_mushroom_gate_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.famcrest_doori2h1", "objects.famcrest_doori2h1_open", Hinge.LEFT),
        SingleDoor("objects.farmdoor", "objects.farmdooropen", Hinge.LEFT),
        SingleDoor("objects.fenk_door", "objects.fenk_door_open", Hinge.LEFT),
        SingleDoor("objects.fenk_mausoleum_door", "objects.fenk_mausoleum_door_open", Hinge.LEFT),
        SingleDoor("objects.fenk_shed_door", "objects.fenk_shed_door_open", Hinge.LEFT),
        SingleDoor("objects.fenk_tower_door", "objects.fenk_tower_door_open", Hinge.LEFT),
        SingleDoor("objects.feud_closed_door_left", "objects.feud_open_door_left", Hinge.LEFT),
        SingleDoor("objects.feud_closed_door_right", "objects.feud_open_door_right", Hinge.RIGHT),
        SingleDoor("objects.fencegate_l", "objects.openfencegate_l", Hinge.LEFT),
        SingleDoor("objects.fencegate_r", "objects.openfencegate_r", Hinge.RIGHT),
        SingleDoor("objects.fossil_dkl_trapdoor_closed", "objects.fossil_dkl_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.fossil_pool_gate", "objects.fossil_pool_gate_open", Hinge.LEFT),
        SingleDoor("objects.fossil_pool_gate_closed", "objects.fossil_pool_gate_open", Hinge.LEFT),
        SingleDoor("objects.fris_troll_trapdoor", "objects.fris_troll_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.fris_troll_trapdoor_closed", "objects.fris_troll_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.fris_troll_trapdoor_final", "objects.fris_troll_trapdoor_final_open", Hinge.LEFT),
        SingleDoor("objects.fris_troll_trapdoor_nurse", "objects.fris_troll_trapdoor_nurse_open", Hinge.LEFT),
        SingleDoor("objects.fris_troll_trapdoor_r1", "objects.fris_troll_trapdoor_r1_open", Hinge.LEFT),
        SingleDoor("objects.fris_troll_trapdoor_r2", "objects.fris_troll_trapdoor_r2_open", Hinge.LEFT),
        SingleDoor("objects.frisb_abode_door", "objects.frisb_abode_door_open", Hinge.LEFT),
        SingleDoor("objects.frisd_outer_city_wall_door_left", "objects.frisd_outer_city_wall_door_left_open", Hinge.LEFT),
        SingleDoor("objects.frisd_outer_city_wall_door_right", "objects.frisd_outer_city_wall_door_right_open", Hinge.RIGHT),
        SingleDoor("objects.frisd_town_wall_door", "objects.frisd_town_wall_door_open", Hinge.LEFT),
        SingleDoor("objects.frisd_turret_door", "objects.frisd_turret_door_open", Hinge.LEFT),
        SingleDoor("objects.ghorrock_metal_gate", "objects.ghorrock_metal_gate_open", Hinge.LEFT),
        SingleDoor("objects.ghorrock_metal_gate_closed", "objects.ghorrock_metal_gate_open", Hinge.LEFT),
        SingleDoor("objects.ghorrock_metal_gate_padlock_closed", "objects.ghorrock_metal_gate_padlock_open", Hinge.LEFT),
        SingleDoor("objects.grab_evil_twin_poor_door", "objects.grab_evil_twin_poor_door_open", Hinge.LEFT),
        SingleDoor("objects.grandtree_trapdoorclosed", "objects.grandtree_trapdooropen", Hinge.LEFT),
        SingleDoor("objects.greendoor", "objects.greendoor_open", Hinge.LEFT),
        SingleDoor("objects.grim_witch_house_door", "objects.grim_witch_house_door_open", Hinge.LEFT),
        SingleDoor("objects.guidordoor", "objects.guidordooropen", Hinge.LEFT),
        SingleDoor("objects.guidorgatelclosed", "objects.guidorgatelopen", Hinge.LEFT),
        SingleDoor("objects.guidorgaterclosed", "objects.guidorgateropen", Hinge.RIGHT),
        SingleDoor("objects.hiddentrapdoor_closed", "objects.hiddentrapdoor_open", Hinge.LEFT),
        SingleDoor("objects.icthalarins_door", "objects.icthalarins_door_open", Hinge.LEFT),
        SingleDoor("objects.icthalarins_door_closed", "objects.icthalarins_door_open", Hinge.LEFT),
        SingleDoor("objects.ig_mausoleum_door", "objects.ig_mausoleum_door_open", Hinge.LEFT),
        SingleDoor("objects.ikov_luciendoor", "objects.ikov_luciendoor_open", Hinge.LEFT),
        SingleDoor("objects.inaccastledoubledoorl", "objects.inaccastledoubledoorlopen", Hinge.LEFT),
        SingleDoor("objects.inaccastledoubledoorr", "objects.inaccastledoubledoorropen", Hinge.RIGHT),
        SingleDoor("objects.kore2_hos_door_closed", "objects.kore2_hos_door_open", Hinge.LEFT),
        SingleDoor("objects.lathastraining_gatel", "objects.lathastraining_gatelopen", Hinge.LEFT),
        SingleDoor("objects.lathastraining_gater", "objects.lathastraining_gateropen", Hinge.RIGHT),
        SingleDoor("objects.lgmagictrialgateclosed", "objects.lgmagictrialgateopen", Hinge.LEFT),
        SingleDoor("objects.luc2_darksquall_cell_door_closed", "objects.luc2_darksquall_cell_door_open", Hinge.LEFT),
        SingleDoor("objects.luc2_dwt_jailbars_gate_closed", "objects.luc2_dwt_jailbars_gate_open", Hinge.LEFT),
        SingleDoor("objects.luc2_dwt_jailbars_outer_gate_closed", "objects.luc2_dwt_jailbars_outer_gate_open", Hinge.LEFT),
        SingleDoor("objects.luc2_mov_door_closed", "objects.luc2_mov_door_open", Hinge.LEFT),
        SingleDoor("objects.luc2_mov_khazard_cell_door_closed", "objects.luc2_mov_khazard_cell_door_open", Hinge.LEFT),
        SingleDoor("objects.luc2_movario_main_base_door_closed", "objects.luc2_movario_main_base_door_open", Hinge.LEFT),
        SingleDoor("objects.lunar_moonclan_door", "objects.lunar_moonclan_door_open", Hinge.LEFT),
        SingleDoor("objects.magentadoor", "objects.magentadoor_open", Hinge.LEFT),
        SingleDoor("objects.mah3_base_door", "objects.mah3_base_door_open", Hinge.LEFT),
        SingleDoor("objects.mah3_base_gate_closed", "objects.mah3_base_gate_open", Hinge.LEFT),
        SingleDoor("objects.mah3_base_gate_closed2", "objects.mah3_base_gate_open2", Hinge.LEFT),
        SingleDoor("objects.mah3_door_to_tapestry", "objects.mah3_door_to_tapestry_open", Hinge.LEFT),
        SingleDoor("objects.makinghistory_door", "objects.makinghistory_door_open", Hinge.LEFT),
        SingleDoor("objects.makinghistory_doubledoorl", "objects.makinghistory_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.makinghistory_doubledoorr", "objects.makinghistory_doubledoorr_open", Hinge.RIGHT),
        SingleDoor("objects.mdaughter_rocktent_door", "objects.mdaughter_rocktent_door_open", Hinge.LEFT),
        SingleDoor("objects.mdaughter_tent_door", "objects.mdaughter_tent_door_open", Hinge.LEFT),
        SingleDoor("objects.metalgateclosedl", "objects.metalgateopenl", Hinge.LEFT),
        SingleDoor("objects.metalgateclosedr", "objects.metalgateopenr", Hinge.RIGHT),
        SingleDoor("objects.misc_viking_abode_door_low", "objects.misc_viking_abode_door_low_open", Hinge.LEFT),
        SingleDoor("objects.mm2_lab_door_left", "objects.mm2_lab_door_left_open", Hinge.LEFT),
        SingleDoor("objects.mm2_lab_door_right", "objects.mm2_lab_door_right_open", Hinge.RIGHT),
        SingleDoor("objects.mm_bamboo_largedoor", "objects.mm_bamboo_largedoor_open", Hinge.LEFT),
        SingleDoor("objects.mm_eastern_warehouse_trapdoor", "objects.mm_eastern_warehouse_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.mm_temple_trapdoor", "objects.mm_temple_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.mm_trapdoor_ne", "objects.mm_trapdoor_ne_open", Hinge.LEFT),
        SingleDoor("objects.mm_trapdoor_nw", "objects.mm_trapdoor_nw_open", Hinge.LEFT),
        SingleDoor("objects.mm_trapdoor_se", "objects.mm_trapdoor_se_open", Hinge.LEFT),
        SingleDoor("objects.mm_trapdoor_sw", "objects.mm_trapdoor_sw_open", Hinge.LEFT),
        SingleDoor("objects.mourner_hideout_door1", "objects.mourner_hideout_door1_open", Hinge.LEFT),
        SingleDoor("objects.mourner_hideout_door2", "objects.mourner_hideout_door2_open", Hinge.LEFT),
        SingleDoor("objects.mourner_hideout_door3", "objects.mourner_hideout_door3_open", Hinge.LEFT),
        SingleDoor("objects.mourner_hideout_door4", "objects.mourner_hideout_door4_open", Hinge.LEFT),
        SingleDoor("objects.mournerquaters_gatel", "objects.mournerquaters_gatelopen", Hinge.LEFT),
        SingleDoor("objects.mournerquaters_gater", "objects.mournerquaters_gateropen", Hinge.RIGHT),
        SingleDoor("objects.mournerstewdoor", "objects.mournerstewdooropen", Hinge.LEFT),
        SingleDoor("objects.mournerstewdoorup", "objects.mournerstewdoorupopen", Hinge.LEFT),
        SingleDoor("objects.mourning_hideout_trap_door", "objects.mourning_hideout_trap_door_open", Hinge.LEFT),
        SingleDoor("objects.mourning_poordoor", "objects.mourning_poordooropen", Hinge.LEFT),
        SingleDoor("objects.mourning_prisondoor", "objects.mourning_prisondooropen", Hinge.LEFT),
        SingleDoor("objects.murder_qip_metalgateclosedl", "objects.murder_qip_metalgateopenl", Hinge.LEFT),
        SingleDoor("objects.murder_qip_metalgateclosedr", "objects.murder_qip_metalgateopenr", Hinge.RIGHT),
        SingleDoor("objects.myq3_lab_door_locked_l", "objects.myq3_lab_door_locked_l_open", Hinge.LEFT),
        SingleDoor("objects.myq3_lab_door_locked_r", "objects.myq3_lab_door_locked_r_open", Hinge.RIGHT),
        SingleDoor("objects.myq4_hideout_trapdoor", "objects.myq4_hideout_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.myq4_serafina_door", "objects.myq4_serafina_door_open", Hinge.LEFT),
        SingleDoor("objects.ogreguardgate1", "objects.ogreguardgate1_open", Hinge.LEFT),
        SingleDoor("objects.ogreguardgate1right", "objects.ogreguardgate1right_open", Hinge.RIGHT),
        SingleDoor("objects.ogreguardgate2", "objects.ogreguardgate2_open", Hinge.LEFT),
        SingleDoor("objects.ogreguardgate2right", "objects.ogreguardgate2right_open", Hinge.RIGHT),
        SingleDoor("objects.olaf2_rusty_gate_puzzle", "objects.olaf2_rusty_gate_puzzle_open", Hinge.LEFT),
        SingleDoor("objects.orangedoor", "objects.orangedoor_open", Hinge.LEFT),
        SingleDoor("objects.osf_trapdoor_closed", "objects.osf_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.overpass_gate_left", "objects.overpass_gate_left_open", Hinge.LEFT),
        SingleDoor("objects.overpass_gate_right", "objects.overpass_gate_right_open", Hinge.RIGHT),
        SingleDoor("objects.pandemonium_door", "objects.pandemonium_door_open", Hinge.LEFT),
        SingleDoor("objects.pandemonium_door_reverse", "objects.pandemonium_door_reverse_open", Hinge.LEFT),
        SingleDoor("objects.pen_wall_doorl", "objects.pen_wall_doorl_open", Hinge.LEFT),
        SingleDoor("objects.pen_wall_doorr", "objects.pen_wall_doorr_open", Hinge.RIGHT),
        SingleDoor("objects.peng_agility_fencing_door", "objects.peng_agility_fencing_door_open", Hinge.LEFT),
        SingleDoor("objects.peng_base_door", "objects.peng_base_door_open", Hinge.LEFT),
        SingleDoor("objects.peng_base_door_agility", "objects.peng_base_door_agility_open", Hinge.LEFT),
        SingleDoor("objects.peng_base_door_bard", "objects.peng_base_door_bard_open", Hinge.LEFT),
        SingleDoor("objects.pest_poordoor", "objects.pest_poordooropen", Hinge.LEFT),
        SingleDoor("objects.pest_wall_gate_damaged_1", "objects.pest_wall_gate_damaged_1_open", Hinge.LEFT),
        SingleDoor("objects.pest_wall_gate_damaged_2", "objects.pest_wall_gate_damaged_2_open", Hinge.LEFT),
        SingleDoor("objects.pest_wall_gate_destroyed", "objects.pest_wall_gate_destroyed_open", Hinge.LEFT),
        SingleDoor("objects.pest_wall_gate_fixed", "objects.pest_wall_gate_fixed_open", Hinge.LEFT),
        SingleDoor("objects.phasmatys_brewery_trapdoor", "objects.phasmatys_brewery_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.pier_rail_double_gate", "objects.pier_rail_double_gate_open", Hinge.LEFT),
        SingleDoor("objects.pipeastsidetrapdoor", "objects.pipeastsidetrapdoor_open", Hinge.LEFT),
        SingleDoor("objects.plaguesheep_gatel", "objects.plaguesheep_gatel_open", Hinge.LEFT),
        SingleDoor("objects.plaguesheep_gater", "objects.plaguesheep_gater_open", Hinge.RIGHT),
        SingleDoor("objects.pog_gate_door", "objects.pog_gate_door_open", Hinge.LEFT),
        SingleDoor("objects.pog_hatch_closed_door", "objects.pog_hatch_open_door", Hinge.LEFT),
        SingleDoor("objects.pog_sewer_grate_door_closed", "objects.pog_sewer_grate_door_open", Hinge.LEFT),
        SingleDoor("objects.pog_sewer_grate_door_instance_closed", "objects.pog_sewer_grate_door_instance_open", Hinge.LEFT),
        SingleDoor("objects.poordoor", "objects.poordooropen", Hinge.LEFT),
        SingleDoor("objects.poordoor_cross", "objects.poordoor_crossopen", Hinge.LEFT),
        SingleDoor("objects.poordoor_m", "objects.poordooropen_m", Hinge.LEFT),
        SingleDoor("objects.posh_doubledoor_reverse_l", "objects.posh_doubledoor_reverse_l_open", Hinge.LEFT),
        SingleDoor("objects.posh_doubledoor_reverse_r", "objects.posh_doubledoor_reverse_r_open", Hinge.RIGHT),
        SingleDoor("objects.poshdoor", "objects.poshdooropen", Hinge.LEFT),
        SingleDoor("objects.prisonbarsdoor", "objects.prisonbarsdoor_open", Hinge.LEFT),
        SingleDoor("objects.prisondoor", "objects.prisondooropen", Hinge.LEFT),
        SingleDoor("objects.prisongate", "objects.prisongateopen", Hinge.LEFT),
        SingleDoor("objects.pvpa_access_gate_l_closed", "objects.pvpa_access_gate_l_open", Hinge.LEFT),
        SingleDoor("objects.pvpa_access_gate_r_closed", "objects.pvpa_access_gate_r_open", Hinge.RIGHT),
        SingleDoor("objects.pvpa_desertdoor_closed", "objects.pvpa_desertdoor_open", Hinge.LEFT),
        SingleDoor("objects.pvpa_desertgate_closed", "objects.pvpa_desertgate_open", Hinge.LEFT),
        SingleDoor("objects.qip_cook_trapdoor_closed", "objects.qip_cook_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.qip_digsite_poshdoor", "objects.qip_digsite_poshdoor_open", Hinge.LEFT),
        SingleDoor("objects.qip_obs_reception_door", "objects.qip_obs_reception_door_open", Hinge.LEFT),
        SingleDoor("objects.qip_sheep_shearer_poordoor", "objects.qip_sheep_shearer_poordooropen", Hinge.LEFT),
        SingleDoor("objects.qip_watchtower_gate_l", "objects.qip_watchtower_gate_l_open", Hinge.LEFT),
        SingleDoor("objects.qip_watchtower_gate_r", "objects.qip_watchtower_gate_r_open", Hinge.RIGHT),
        SingleDoor("objects.quest_lunar_galleon_pirate_door", "objects.quest_lunar_galleon_pirate_door_open", Hinge.LEFT),
        SingleDoor("objects.rd_door", "objects.rd_door_open", Hinge.LEFT),
        SingleDoor("objects.reddoor", "objects.reddoor_open", Hinge.LEFT),
        SingleDoor("objects.rocko_prison_door01", "objects.rocko_prison_door01_open", Hinge.LEFT),
        SingleDoor("objects.rocko_prison_door02", "objects.rocko_prison_door02_open", Hinge.LEFT),
        SingleDoor("objects.roguesden_door_to_pub", "objects.roguesden_door_to_pub_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_bloodmoon_closed", "objects.ror_gate_bloodmoon_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_bluemoon_closed", "objects.ror_gate_bluemoon_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_fullmoon_closed", "objects.ror_gate_fullmoon_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_newmoon_closed", "objects.ror_gate_newmoon_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_solareclipse_closed", "objects.ror_gate_solareclipse_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_wanmoon_closed", "objects.ror_gate_wanmoon_open", Hinge.LEFT),
        SingleDoor("objects.ror_gate_waxmoon_closed", "objects.ror_gate_waxmoon_open", Hinge.LEFT),
        SingleDoor("objects.royal_village_door", "objects.royal_village_door_open", Hinge.LEFT),
        SingleDoor("objects.secretdoor", "objects.secretdooropen", Hinge.LEFT),
        SingleDoor("objects.slayertower_door", "objects.slayertower_door_open", Hinge.LEFT),
        SingleDoor("objects.slayertower_small_door", "objects.slayertower_small_door_open", Hinge.LEFT),
        SingleDoor("objects.slp_dungeon_door", "objects.slp_dungeon_door_open", Hinge.LEFT),
        SingleDoor("objects.sos_death_door_face", "objects.sos_death_door_face_open", Hinge.LEFT),
        SingleDoor("objects.sos_death_door_face_mirr", "objects.sos_death_door_face_mirr_open", Hinge.LEFT),
        SingleDoor("objects.sos_fam_door_face", "objects.sos_fam_door_face_open", Hinge.LEFT),
        SingleDoor("objects.sos_fam_door_face_mirr", "objects.sos_fam_door_face_mirr_open", Hinge.LEFT),
        SingleDoor("objects.sos_pest_door_face", "objects.sos_pest_door_face_open", Hinge.LEFT),
        SingleDoor("objects.sos_pest_door_face_mirr", "objects.sos_pest_door_face_mirr_open", Hinge.LEFT),
        SingleDoor("objects.sos_war_door_face", "objects.sos_war_door_face_open", Hinge.LEFT),
        SingleDoor("objects.sos_war_door_face_mirr", "objects.sos_war_door_face_mirr_open", Hinge.LEFT),
        SingleDoor("objects.swan_building_door", "objects.swan_building_door_open", Hinge.LEFT),
        SingleDoor("objects.tapo_gateway_portcullis01_closed01", "objects.tapo_gateway_portcullis01_open01", Hinge.LEFT),
        SingleDoor("objects.timberwall_door", "objects.timberwall_door_open", Hinge.LEFT),
        SingleDoor("objects.timberwall_doorl", "objects.timberwall_doorl_open", Hinge.LEFT),
        SingleDoor("objects.tol_tower_wall_door", "objects.tol_tower_wall_door_open", Hinge.LEFT),
        SingleDoor("objects.trapdoor", "objects.trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.treedoorl", "objects.treedoorl_open", Hinge.LEFT),
        SingleDoor("objects.treedoorr", "objects.treedoorr_open", Hinge.RIGHT),
        SingleDoor("objects.treegate_closed", "objects.treegate_open", Hinge.LEFT),
        SingleDoor("objects.troll_celldoor", "objects.troll_celldoor_open", Hinge.LEFT),
        SingleDoor("objects.troll_celldoor_eadgar", "objects.troll_celldoor_eadgar_open", Hinge.LEFT),
        SingleDoor("objects.troll_celldoor_godric", "objects.troll_celldoor_godric_open", Hinge.LEFT),
        SingleDoor("objects.troll_stronghold_interior_door", "objects.troll_stronghold_interior_door_open", Hinge.LEFT),
        SingleDoor("objects.troll_stronghold_prison_door_closed", "objects.troll_stronghold_prison_door_open", Hinge.LEFT),
        SingleDoor("objects.twilight_temple_door_closed", "objects.twilight_temple_door_open", Hinge.LEFT),
        SingleDoor("objects.tzhaar_cave_door_closed", "objects.tzhaar_cave_door_open", Hinge.LEFT),
        SingleDoor("objects.upass_templedoor_closed_left", "objects.upass_templedoor_open_left", Hinge.LEFT),
        SingleDoor("objects.upass_templedoor_closed_right", "objects.upass_templedoor_open_right", Hinge.RIGHT),
        SingleDoor("objects.viking_abode_door", "objects.viking_abode_door_open", Hinge.LEFT),
        SingleDoor("objects.viking_fur_door", "objects.viking_fur_door_open", Hinge.LEFT),
        SingleDoor("objects.viking_seer_trapdoor_closed", "objects.viking_seer_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.village_door_l", "objects.village_door_l_open", Hinge.LEFT),
        SingleDoor("objects.village_door_r", "objects.village_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.vine_glass_house_door", "objects.vine_glass_house_door_open", Hinge.LEFT),
        SingleDoor("objects.waa_trapdoor", "objects.waa_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.wcguild_gatel", "objects.wcguild_gatel_open", Hinge.LEFT),
        SingleDoor("objects.wcguild_gater", "objects.wcguild_gater_open", Hinge.RIGHT),
        SingleDoor("objects.win05_trapdoor", "objects.win05_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.win05_trapdoor_closed", "objects.win05_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.wooden_fur_door", "objects.wooden_fur_door_open", Hinge.LEFT),
        SingleDoor("objects.wooden_fur_door_always_closed", "objects.wooden_fur_door_always_open", Hinge.LEFT),
        SingleDoor("objects.wydindoor", "objects.wydindooropen", Hinge.LEFT),
        SingleDoor("objects.yellowdoor", "objects.yellowdoor_open", Hinge.LEFT),
        SingleDoor("objects.zanarisdoor", "objects.zanarisdoor_open", Hinge.LEFT),

        // ── Varrock ───────────────────────────────────────────────────────────
        SingleDoor("objects.fai_varrock_castle_door", "objects.fai_varrock_castle_door_open", Hinge.LEFT),
        SingleDoor("objects.fai_varrock_door", "objects.fai_varrock_door_open", Hinge.LEFT),
        SingleDoor("objects.fai_varrock_member_gatel", "objects.fai_varrock_member_gatel_open", Hinge.LEFT),
        SingleDoor("objects.fai_varrock_member_gater", "objects.fai_varrock_member_gater_open", Hinge.RIGHT),
        SingleDoor("objects.fai_varrock_museum_door_closed_l", "objects.fai_varrock_museum_door_open_l", Hinge.LEFT),
        SingleDoor("objects.fai_varrock_museum_door_closed_r", "objects.fai_varrock_museum_door_open_r", Hinge.RIGHT),
        SingleDoor("objects.fai_varrock_poor_door", "objects.fai_varrock_poor_door_open", Hinge.LEFT),
        SingleDoor("objects.fai_varrock_poor_door_flipped", "objects.fai_varrock_poor_door_open_flipped", Hinge.LEFT),
        SingleDoor("objects.fai_varrock_shanty_door", "objects.fai_varrock_shanty_door_open", Hinge.LEFT),

        // ── Falador ───────────────────────────────────────────────────────────
        SingleDoor("objects.fai_falador_double_door_l", "objects.fai_falador_double_door_l_open", Hinge.LEFT),
        SingleDoor("objects.fai_falador_double_door_r", "objects.fai_falador_double_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.fai_falador_poor_castle_door", "objects.fai_falador_poor_castle_door_open", Hinge.LEFT),
        SingleDoor("objects.fai_falador_poor_door", "objects.fai_falador_poor_door_open", Hinge.LEFT),
        SingleDoor("objects.fai_falador_poor_door_closed_m", "objects.fai_falador_poor_door_open_m", Hinge.LEFT),

        // ── Seers / Catherby ──────────────────────────────────────────────────
        SingleDoor("objects.kr_cam_doubledoorl", "objects.kr_cam_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.kr_cam_doubledoorr", "objects.kr_cam_doubledoorr_open", Hinge.RIGHT),
        SingleDoor("objects.kr_cam_poshdoor", "objects.kr_cam_poshdoor_open", Hinge.LEFT),
        SingleDoor("objects.kr_camelot_metalgateclosedl", "objects.kr_camelot_metalgateopenl", Hinge.LEFT),
        SingleDoor("objects.kr_camelot_metalgateclosedr", "objects.kr_camelot_metalgateopenr", Hinge.RIGHT),
        SingleDoor("objects.kr_churchdoor_l", "objects.kr_openchurchdoor_l", Hinge.LEFT),
        SingleDoor("objects.kr_churchdoor_r", "objects.kr_openchurchdoor_r", Hinge.RIGHT),
        SingleDoor("objects.kr_courthouse_double_door_l", "objects.kr_courthouse_double_door_l_open", Hinge.LEFT),
        SingleDoor("objects.kr_courthouse_double_door_r", "objects.kr_courthouse_double_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.kr_poordoor", "objects.kr_poordooropen", Hinge.LEFT),
        SingleDoor("objects.kr_poshdoor", "objects.kr_poshdooropen", Hinge.LEFT),
        SingleDoor("objects.kr_sin_poordoor", "objects.kr_sin_poordooropen", Hinge.LEFT),
        SingleDoor("objects.kr_sin_poshdoor", "objects.kr_sin_poshdooropen", Hinge.LEFT),
        SingleDoor("objects.kr_underground_jail_bars_gate", "objects.kr_underground_jail_bars_gate_open", Hinge.LEFT),

        // ── Canifis / Morytania ───────────────────────────────────────────────
        SingleDoor("objects.burgh_inn_trapdoor_closed", "objects.burgh_inn_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.burgh_trapdoor_closed", "objects.burgh_trapdoor_open", Hinge.LEFT),
        SingleDoor("objects.canafis_door", "objects.canafis_door_open", Hinge.LEFT),
        SingleDoor("objects.canafis_door_ground", "objects.canafis_door_ground_open", Hinge.LEFT),

        // ── Kourend / Zeah ────────────────────────────────────────────────────
        SingleDoor("objects.archeuus_door_double_left_blue", "objects.archeuus_door_double_left_blue_open", Hinge.LEFT),
        SingleDoor("objects.archeuus_door_double_left_green", "objects.archeuus_door_double_left_green_open", Hinge.LEFT),
        SingleDoor("objects.archeuus_door_double_left_red", "objects.archeuus_door_double_left_red_open", Hinge.LEFT),
        SingleDoor("objects.archeuus_door_double_left_turquoise", "objects.archeuus_door_double_left_turquoise_open", Hinge.LEFT),
        SingleDoor("objects.archeuus_door_double_right_blue", "objects.archeuus_door_double_right_blue_open", Hinge.RIGHT),
        SingleDoor("objects.archeuus_door_double_right_green", "objects.archeuus_door_double_right_green_open", Hinge.RIGHT),
        SingleDoor("objects.archeuus_door_double_right_red", "objects.archeuus_door_double_right_red_open", Hinge.RIGHT),
        SingleDoor("objects.archeuus_door_double_right_turquoise", "objects.archeuus_door_double_right_turquoise_open", Hinge.RIGHT),
        SingleDoor("objects.hosidius_door", "objects.hosidius_door_open", Hinge.LEFT),
        SingleDoor("objects.hosidius_poh_doubledoor", "objects.hosidius_poh_doubledoor_open", Hinge.LEFT),
        SingleDoor("objects.hosidius_poh_doubledoorl", "objects.hosidius_poh_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.karuulm_hydra_room_door", "objects.karuulm_hydra_room_door_open", Hinge.LEFT),
        SingleDoor("objects.karuulm_hydra_room_door_m", "objects.karuulm_hydra_room_door_m_open", Hinge.LEFT),
        SingleDoor("objects.kebos_farming_guild_door_left_closed", "objects.kebos_farming_guild_door_left_open", Hinge.LEFT),
        SingleDoor("objects.kebos_farming_guild_door_right_closed", "objects.kebos_farming_guild_door_right_open", Hinge.RIGHT),
        SingleDoor("objects.kebos_farming_guild_gate_t2_left_closed", "objects.kebos_farming_guild_gate_t2_left_open", Hinge.LEFT),
        SingleDoor("objects.kebos_farming_guild_gate_t2_right_closed", "objects.kebos_farming_guild_gate_t2_right_open", Hinge.RIGHT),
        SingleDoor("objects.kebos_farming_guild_gate_t3_left_closed", "objects.kebos_farming_guild_gate_t3_left_open", Hinge.LEFT),
        SingleDoor("objects.kebos_farming_guild_gate_t3_right_closed", "objects.kebos_farming_guild_gate_t3_right_open", Hinge.RIGHT),
        SingleDoor("objects.lovaquest_inner_door", "objects.lovaquest_inner_door_open", Hinge.LEFT),
        SingleDoor("objects.lovaquest_tower_entry_door", "objects.lovaquest_tower_entry_door_open", Hinge.LEFT),
        SingleDoor("objects.wallkit_shayzien_door01_double_l", "objects.wallkit_shayzien_door01_double_l_open", Hinge.LEFT),
        SingleDoor("objects.wallkit_shayzien_door01_double_r", "objects.wallkit_shayzien_door01_double_r_open", Hinge.RIGHT),
        SingleDoor("objects.wallkit_shayzien_door01_l", "objects.wallkit_shayzien_door01_l_open", Hinge.LEFT),
        SingleDoor("objects.wallkit_shayzien_door01_l_reverse", "objects.wallkit_shayzien_door01_l_reverse_open", Hinge.LEFT),
        SingleDoor("objects.wallkit_shayzien_door01_r", "objects.wallkit_shayzien_door01_r_open", Hinge.RIGHT),
        SingleDoor("objects.wallkit_shayzien_door01_r_reverse", "objects.wallkit_shayzien_door01_r_reverse_open", Hinge.RIGHT),

        // ── Varlamore ─────────────────────────────────────────────────────────
        SingleDoor("objects.civitas_poh_door_l", "objects.civitas_poh_door_l_open", Hinge.LEFT),
        SingleDoor("objects.civitas_poh_door_r", "objects.civitas_poh_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.fortis_door_double_l", "objects.fortis_door_double_l_open", Hinge.LEFT),
        SingleDoor("objects.fortis_door_double_r", "objects.fortis_door_double_r_open", Hinge.RIGHT),
        SingleDoor("objects.fortis_door_l", "objects.fortis_door_l_open", Hinge.LEFT),
        SingleDoor("objects.fortis_door_l_reverse", "objects.fortis_door_l_reverse_open", Hinge.LEFT),
        SingleDoor("objects.fortis_door_r", "objects.fortis_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.fortis_door_r_reverse", "objects.fortis_door_r_reverse_open", Hinge.RIGHT),
        SingleDoor("objects.wallkit_colosseum01_gate02_closed", "objects.wallkit_colosseum01_gate02_open", Hinge.LEFT),
        SingleDoor("objects.wallkit_colosseum01_gate02_closed_m", "objects.wallkit_colosseum01_gate02_open_m", Hinge.LEFT),
        SingleDoor("objects.wallkit_colosseum03_gate02_closed01", "objects.wallkit_colosseum03_gate02_open01", Hinge.LEFT),
        SingleDoor("objects.wallkit_colosseum03_gate02_closed01_m", "objects.wallkit_colosseum03_gate02_open01_m", Hinge.LEFT),
        SingleDoor("objects.wallkit_easter24_door01_closed_l", "objects.wallkit_easter24_door01_open_l", Hinge.LEFT),
        SingleDoor("objects.wallkit_easter24_door01_closed_r", "objects.wallkit_easter24_door01_open_r", Hinge.RIGHT),
        SingleDoor("objects.wallkit_red_rock_door01_closed_l", "objects.wallkit_red_rock_door01_open_l", Hinge.LEFT),
        SingleDoor("objects.wallkit_red_rock_door01_closed_r", "objects.wallkit_red_rock_door01_open_r", Hinge.RIGHT),
        SingleDoor("objects.wallkit_wooden01_default01_door", "objects.wallkit_wooden01_default01_door_open", Hinge.LEFT),

        // ── Keldagrim ─────────────────────────────────────────────────────────
        SingleDoor("objects.dwarf_keldagrim_door", "objects.dwarf_keldagrim_door_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_interior", "objects.dwarf_keldagrim_door_interior_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_interior_factory", "objects.dwarf_keldagrim_door_interior_factory_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_interior_palace", "objects.dwarf_keldagrim_door_interior_palace_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_interior_poor", "objects.dwarf_keldagrim_door_interior_poor_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_ornate", "objects.dwarf_keldagrim_door_ornate_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_palace", "objects.dwarf_keldagrim_door_palace_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_door_poor", "objects.dwarf_keldagrim_door_poor_open", Hinge.LEFT),
        SingleDoor("objects.dwarf_keldagrim_factory_door", "objects.dwarf_keldagrim_factory_door_open", Hinge.LEFT),

        // ── Prifddinas ────────────────────────────────────────────────────────
        SingleDoor("objects.elfdoor", "objects.elfdooropen", Hinge.LEFT),
        SingleDoor("objects.prif_house_door_closed", "objects.prif_house_door_open", Hinge.LEFT),
        SingleDoor("objects.prif_house_door_stone_closed", "objects.prif_house_door_stone_open", Hinge.LEFT),
        SingleDoor("objects.prif_house_square_door_closed", "objects.prif_house_square_door_open", Hinge.LEFT),
        SingleDoor("objects.vc_elfdoor", "objects.vc_elfdooropen", Hinge.LEFT),

        // ── Wilderness ────────────────────────────────────────────────────────
        SingleDoor("objects.wild_door", "objects.wild_door_open", Hinge.LEFT),
        SingleDoor("objects.wildy_hub_gate_l", "objects.wildy_hub_gate_l_open", Hinge.LEFT),
        SingleDoor("objects.wildy_hub_gate_r", "objects.wildy_hub_gate_r_open", Hinge.RIGHT),

        // ── Raids / Dungeons ──────────────────────────────────────────────────
        SingleDoor("objects.ds2_varrock_door", "objects.ds2_varrock_door_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_diamond_gate_closed", "objects.dt2_ghorrock_diamond_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_dragonstone_gate_closed", "objects.dt2_ghorrock_dragonstone_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_emerald_gate_closed", "objects.dt2_ghorrock_emerald_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_gate_closed", "objects.dt2_ghorrock_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_onyx_gate_closed", "objects.dt2_ghorrock_onyx_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_padlock_gate_closed", "objects.dt2_ghorrock_padlock_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_ruby_gate_closed", "objects.dt2_ghorrock_ruby_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_ghorrock_sapphire_gate_closed", "objects.dt2_ghorrock_sapphire_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_hideout_prisongate", "objects.dt2_hideout_prisongate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_lassar_church_gate", "objects.dt2_lassar_church_gate_open", Hinge.LEFT),
        SingleDoor("objects.dt2_stranglewood_temple_door_l", "objects.dt2_stranglewood_temple_door_l_open", Hinge.LEFT),
        SingleDoor("objects.dt2_stranglewood_temple_door_r", "objects.dt2_stranglewood_temple_door_r_open", Hinge.RIGHT),
        SingleDoor("objects.lassar_door_closed", "objects.lassar_door_open", Hinge.LEFT),
        SingleDoor("objects.lassar_door_closed_alt", "objects.lassar_door_open_alt", Hinge.LEFT),
        SingleDoor("objects.lassar_door_closed_l", "objects.lassar_door_open_l", Hinge.LEFT),
        SingleDoor("objects.lassar_door_closed_l_alt", "objects.lassar_door_open_l_alt", Hinge.LEFT),
        SingleDoor("objects.lassar_door_closed_r", "objects.lassar_door_open_r", Hinge.RIGHT),
        SingleDoor("objects.lassar_door_closed_r_alt", "objects.lassar_door_open_r_alt", Hinge.RIGHT),
        SingleDoor("objects.lassar_locked_door_normal_closed_l", "objects.lassar_locked_door_normal_open_l", Hinge.LEFT),
        SingleDoor("objects.lassar_locked_door_normal_closed_r", "objects.lassar_locked_door_normal_open_r", Hinge.RIGHT),
        SingleDoor("objects.lotg_temple_jaildoor_closed", "objects.lotg_temple_jaildoor_open", Hinge.LEFT),
        SingleDoor("objects.lotr_ruins_cage_door_closed", "objects.lotr_ruins_cage_door_open", Hinge.LEFT),
        SingleDoor("objects.lotr_ruins_tran_door_closed", "objects.lotr_ruins_tran_door_open", Hinge.LEFT),
        SingleDoor("objects.lotr_trap_trap_door_closed", "objects.lotr_trap_trap_door_open", Hinge.LEFT),
        SingleDoor("objects.lotr_trap_trap_door_closed_lvl1", "objects.lotr_trap_trap_door_open_lvl1", Hinge.LEFT),
        SingleDoor("objects.lotr_trap_trap_door_closed_lvl2", "objects.lotr_trap_trap_door_open_lvl2", Hinge.LEFT),
        SingleDoor("objects.lotr_trap_trap_door_closed_lvl3", "objects.lotr_trap_trap_door_open_lvl3", Hinge.LEFT),
        SingleDoor("objects.toa_nexus_wardens_door", "objects.toa_nexus_wardens_door_open", Hinge.LEFT),

        // ── Player Owned House ────────────────────────────────────────────────
        SingleDoor("objects.canifis_poh_doubledoor", "objects.canifis_poh_doubledoor_open", Hinge.LEFT),
        SingleDoor("objects.canifis_poh_doubledoorl", "objects.canifis_poh_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.deathly_poh_double_door", "objects.deathly_poh_double_door_open", Hinge.LEFT),
        SingleDoor("objects.deathly_poh_double_doorl", "objects.deathly_poh_double_doorl_open", Hinge.LEFT),
        SingleDoor("objects.poh_cage_dungeon_bones_door", "objects.poh_cage_dungeon_bones_door_open", Hinge.LEFT),
        SingleDoor("objects.poh_cage_dungeon_oak_door", "objects.poh_cage_dungeon_oak_door_open", Hinge.LEFT),
        SingleDoor("objects.poh_cage_dungeon_oaksteel_door", "objects.poh_cage_dungeon_oaksteel_door_open", Hinge.LEFT),
        SingleDoor("objects.poh_cage_dungeon_steel_door", "objects.poh_cage_dungeon_steel_door_open", Hinge.LEFT),
        SingleDoor("objects.poh_cage_dungeon_steelspikes_door", "objects.poh_cage_dungeon_steelspikes_door_open", Hinge.LEFT),
        SingleDoor("objects.poh_dungeon_ldoor_marble", "objects.poh_dungeon_ldoor_marble_open", Hinge.LEFT),
        SingleDoor("objects.poh_dungeon_ldoor_oak", "objects.poh_dungeon_ldoor_oak_open", Hinge.LEFT),
        SingleDoor("objects.poh_dungeon_ldoor_steel", "objects.poh_dungeon_ldoor_steel_open", Hinge.LEFT),
        SingleDoor("objects.poh_dungeon_rdoor_marble", "objects.poh_dungeon_rdoor_marble_open", Hinge.RIGHT),
        SingleDoor("objects.poh_dungeon_rdoor_oak", "objects.poh_dungeon_rdoor_oak_open", Hinge.RIGHT),
        SingleDoor("objects.poh_dungeon_rdoor_steel", "objects.poh_dungeon_rdoor_steel_open", Hinge.RIGHT),
        SingleDoor("objects.poh_optionsdoors_closed", "objects.poh_optionsdoors_open", Hinge.LEFT),
        SingleDoor("objects.poh_optionsicon_doors_closed", "objects.poh_optionsicon_doors_open", Hinge.LEFT),
        SingleDoor("objects.rellekka_poh_doubledoor", "objects.rellekka_poh_doubledoor_open", Hinge.LEFT),
        SingleDoor("objects.rellekka_poh_doubledoorl", "objects.rellekka_poh_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.twisted_poh_doubledoor", "objects.twisted_poh_doubledoor_open", Hinge.LEFT),
        SingleDoor("objects.twisted_poh_doubledoorl", "objects.twisted_poh_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.xmas2020_poh_doubledoor", "objects.xmas2020_poh_doubledoor_open", Hinge.LEFT),
        SingleDoor("objects.xmas2020_poh_doubledoorl", "objects.xmas2020_poh_doubledoorl_open", Hinge.LEFT),
        SingleDoor("objects.yanille_poh_double_door", "objects.yanille_poh_double_door_open", Hinge.LEFT),
        SingleDoor("objects.yanille_poh_double_doorl", "objects.yanille_poh_double_doorl_open", Hinge.LEFT),

        // ── Ardougne ──────────────────────────────────────────────────────────
        SingleDoor("objects.ardougnedoor_l", "objects.ardougnedoor_l_open", Hinge.LEFT),
        SingleDoor("objects.ardougnedoor_r", "objects.ardougnedoor_r_open", Hinge.RIGHT),
        SingleDoor("objects.peng_ardougne_enclosure_door", "objects.peng_ardougne_enclosure_door_open", Hinge.LEFT),
        SingleDoor("objects.w_ardougnedoubledoorl", "objects.w_ardougnedoubledoorlopen", Hinge.LEFT),
        SingleDoor("objects.w_ardougnedoubledoorr", "objects.w_ardougnedoubledoorropen", Hinge.RIGHT),

        // ── Varlamore Quests ──────────────────────────────────────────────────
        SingleDoor("objects.vm_store_room_door", "objects.vm_store_room_door_open", Hinge.LEFT),
        SingleDoor("objects.vmq1_bandit_door", "objects.vmq1_bandit_door_open", Hinge.LEFT),
        SingleDoor("objects.vmq1_prisondoor", "objects.vmq1_prisondoor_open", Hinge.LEFT),
        SingleDoor("objects.vmq2_temple_gate", "objects.vmq2_temple_gate_open", Hinge.LEFT),
        SingleDoor("objects.vmq4_crypt_door_to_moki", "objects.vmq4_crypt_door_to_moki_open", Hinge.LEFT),
        SingleDoor("objects.vmq4_crypt_door_to_moki_closed", "objects.vmq4_crypt_door_to_moki_open", Hinge.LEFT),
        SingleDoor("objects.vmq4_janus_house_front_door_left", "objects.vmq4_janus_house_front_door_left_open", Hinge.LEFT),
        SingleDoor("objects.vmq4_janus_house_front_door_right", "objects.vmq4_janus_house_front_door_right_open", Hinge.RIGHT),
        SingleDoor("objects.vmq4_janus_house_puzzle_door", "objects.vmq4_janus_house_puzzle_door_open", Hinge.LEFT),
        SingleDoor("objects.vmq4_queen_chamber_door_l", "objects.vmq4_queen_chamber_door_l_open", Hinge.LEFT),
        SingleDoor("objects.vmq4_queen_chamber_door_r", "objects.vmq4_queen_chamber_door_r_open", Hinge.RIGHT)
    )

    // ── Double door definitions ───────────────────────────────────────────────

    private val DOUBLE_DOORS = listOf(
        DoubleDoor(
            "objects.castledoubledoorl",         "objects.castledoubledoorr",
            "objects.opencastledoubledoorl",     "objects.opencastledoubledoorr"
        ),
        DoubleDoor(
            "objects.fai_falador_double_door_l",      "objects.fai_falador_double_door_r",
            "objects.fai_falador_double_door_l_open", "objects.fai_falador_double_door_r_open"
        ),
        DoubleDoor(
            "objects.fai_falador_castledoubledoorl",      "objects.fai_falador_castledoubledoorr",
            "objects.fai_falador_opencastledoubledoorl",  "objects.fai_falador_opencastledoubledoorr"
        ),
        DoubleDoor(
            "objects.kr_cam_doubledoorl",      "objects.kr_cam_doubledoorr",
            "objects.kr_cam_doubledoorl_open", "objects.kr_cam_doubledoorr_open"
        ),
        DoubleDoor(
            "objects.makinghistory_doubledoorl",      "objects.makinghistory_doubledoorr",
            "objects.makinghistory_doubledoorl_open", "objects.makinghistory_doubledoorr_open"
        ),
        DoubleDoor(
            "objects.w_ardougnedoubledoorl",      "objects.w_ardougnedoubledoorr",
            "objects.w_ardougnedoubledoorlopen",  "objects.w_ardougnedoubledoorropen"
        ),
        DoubleDoor(
            "objects.wild_doubledoor_l",      "objects.wild_doubledoor_r",
            "objects.wild_doubledoor_open_l", "objects.wild_doubledoor_open_r"
        )
    )

    // ── Lookup maps ───────────────────────────────────────────────────────────

    private val byClosedRscm = SINGLE_DOORS.associateBy { it.closedRscm }
    private val byOpenRscm   = SINGLE_DOORS.associateBy { it.openRscm }

    private val byDoubleRscm = buildMap<String, DoubleDoor> {
        DOUBLE_DOORS.forEach { d ->
            put(d.closedLeft,  d); put(d.closedRight, d)
            put(d.openedLeft,  d); put(d.openedRight, d)
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    override fun init() {
        SINGLE_DOORS.forEach { door ->
            doorOptionsFor(door.closedRscm).forEach { opt ->
                onObjectOption(door.closedRscm, opt) { handleSingleOpen(this) }
            }
            doorOptionsFor(door.openRscm).forEach { opt ->
                onObjectOption(door.openRscm, opt) { handleSingleClose(this) }
            }
        }

        DOUBLE_DOORS.forEach { door ->
            listOf(door.closedLeft, door.closedRight).forEach { rscm ->
                doorOptionsFor(rscm).forEach { opt ->
                    onObjectOption(rscm, opt) { handleDoubleOpen(this, door) }
                }
            }
            listOf(door.openedLeft, door.openedRight).forEach { rscm ->
                doorOptionsFor(rscm).forEach { opt ->
                    onObjectOption(rscm, opt) { handleDoubleClose(this, door) }
                }
            }
        }
    }

    // ── Single door handlers ──────────────────────────────────────────────────

    private fun handleSingleOpen(event: ObjectClickEvent) {
        val obj  = event.gameObject
        val door = byClosedRscm[obj.id] ?: return
        val tile = obj.tile
        val type = obj.type
        val newRot = openRot(obj.rot, door.hinge)
        event.player.world.queue {
            val fresh = event.player.world.getObject(tile, type) ?: return@queue
            event.player.world.remove(fresh)
            event.player.world.spawn(DynamicObject(door.openRscm, type, newRot, tile))
        }
    }

    private fun handleSingleClose(event: ObjectClickEvent) {
        val obj  = event.gameObject
        val door = byOpenRscm[obj.id] ?: return
        val tile = obj.tile
        val type = obj.type
        val newRot = closeRot(obj.rot, door.hinge)
        event.player.world.queue {
            val fresh = event.player.world.getObject(tile, type) ?: return@queue
            event.player.world.remove(fresh)
            event.player.world.spawn(DynamicObject(door.closedRscm, type, newRot, tile))
        }
    }

    // ── Double door handlers ──────────────────────────────────────────────────

    private fun handleDoubleOpen(event: ObjectClickEvent, door: DoubleDoor) {
        val obj    = event.gameObject
        val isLeft = obj.id == door.closedLeft
        val tile   = obj.tile
        val type   = obj.type
        val w      = event.player.world
        w.queue {
            val fresh = w.getObject(tile, type) ?: return@queue
            val newRot = openRot(fresh.rot, if (isLeft) Hinge.LEFT else Hinge.RIGHT)
            w.remove(fresh)
            w.spawn(DynamicObject(if (isLeft) door.openedLeft else door.openedRight, type, newRot, tile))

            val partnerRscm = if (isLeft) door.closedRight else door.closedLeft
            val partner = findNearby(w, tile, type, partnerRscm) ?: return@queue
            val partnerNewRot = openRot(partner.rot, if (isLeft) Hinge.RIGHT else Hinge.LEFT)
            w.remove(partner)
            w.spawn(DynamicObject(if (isLeft) door.openedRight else door.openedLeft, type, partnerNewRot, partner.tile))
        }
    }

    private fun handleDoubleClose(event: ObjectClickEvent, door: DoubleDoor) {
        val obj    = event.gameObject
        val isLeft = obj.id == door.openedLeft
        val tile   = obj.tile
        val type   = obj.type
        val w      = event.player.world
        w.queue {
            val fresh = w.getObject(tile, type) ?: return@queue
            val newRot = closeRot(fresh.rot, if (isLeft) Hinge.LEFT else Hinge.RIGHT)
            w.remove(fresh)
            w.spawn(DynamicObject(if (isLeft) door.closedLeft else door.closedRight, type, newRot, tile))

            val partnerRscm = if (isLeft) door.openedRight else door.openedLeft
            val partner = findNearby(w, tile, type, partnerRscm) ?: return@queue
            val partnerNewRot = closeRot(partner.rot, if (isLeft) Hinge.RIGHT else Hinge.LEFT)
            w.remove(partner)
            w.spawn(DynamicObject(if (isLeft) door.closedRight else door.closedLeft, type, partnerNewRot, partner.tile))
        }
    }

    // ── Rotation helpers ──────────────────────────────────────────────────────

    private fun openRot(currentRot: Int, hinge: Hinge) = when (hinge) {
        Hinge.LEFT  -> (currentRot + 3) % 4
        Hinge.RIGHT -> (currentRot + 1) % 4
    }

    private fun closeRot(currentRot: Int, hinge: Hinge) = when (hinge) {
        Hinge.LEFT  -> (currentRot + 1) % 4
        Hinge.RIGHT -> (currentRot + 3) % 4
    }

    // ── Tile search ───────────────────────────────────────────────────────────

    private fun findNearby(world: World, near: Tile, type: Int, rscmId: String): GameObject? {
        for (dx in -2..2) for (dz in -2..2) {
            if (dx == 0 && dz == 0) continue
            val obj = world.getObject(near.transform(dx, dz), type) ?: continue
            if (obj.id == rscmId) return obj
        }
        return null
    }

    // ── Option lookup ─────────────────────────────────────────────────────────

    private fun doorOptionsFor(rscmName: String): List<String> {
        val actions = try {
            getObject(getRSCM(rscmName))?.actions?.filterNotNull()?.filter { it.isNotBlank() }
        } catch (e: Exception) { null }
        return if (actions.isNullOrEmpty()) listOf("Open", "Close") else actions
    }
}
