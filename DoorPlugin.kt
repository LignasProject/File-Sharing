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
 * Confirmed patterns with help from Mark & Community https://discord.gg/v2qcXzBCwf
 *  - Use player.world (not inherited PluginEvent.world)
 *  - Use world.queue {}
 *  - Rotation must change when opening/closing — direction depends on hinge side
 *    Left hinge:  openRot = (closedRot + 3) % 4
 *    Right hinge: openRot = (closedRot + 1) % 4
 *
 * All RSCM names confirmed against gamevals.dat [gamevals.objects].
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

    private val SINGLE_DOORS = listOf(
        // Poor doors
        SingleDoor("objects.poordoor",   "objects.poordooropen",   Hinge.LEFT),
        SingleDoor("objects.poordoor_m", "objects.poordooropen_m", Hinge.LEFT),

        // Posh doors
        SingleDoor("objects.poshdoor",   "objects.poshdooropen",   Hinge.LEFT),

        // Elf doors
        SingleDoor("objects.elfdoor",    "objects.elfdooropen",    Hinge.LEFT),

        // Fence gates
        SingleDoor("objects.fencegate_l",   "objects.openfencegate_l",  Hinge.LEFT),
        SingleDoor("objects.fencegate_r",   "objects.openfencegate_r",  Hinge.RIGHT),

        // Falador
        SingleDoor("objects.fai_falador_poor_door",          "objects.fai_falador_poor_door_open",          Hinge.LEFT),
        SingleDoor("objects.fai_falador_poor_castle_door",   "objects.fai_falador_poor_castle_door_open",   Hinge.LEFT),

        // Varrock
        SingleDoor("objects.fai_varrock_door",          "objects.fai_varrock_door_open",          Hinge.LEFT),
        SingleDoor("objects.fai_varrock_castle_door",   "objects.fai_varrock_castle_door_open",   Hinge.LEFT),
        SingleDoor("objects.fai_varrock_poor_door",     "objects.fai_varrock_poor_door_open",     Hinge.LEFT),

        // Seers Village church
        SingleDoor("objects.kr_churchdoor_l",  "objects.kr_openchurchdoor_l",  Hinge.LEFT),
        SingleDoor("objects.kr_churchdoor_r",  "objects.kr_openchurchdoor_r",  Hinge.RIGHT)
    )

    // ── Double door definitions ───────────────────────────────────────────────

    private val DOUBLE_DOORS = listOf(
        DoubleDoor(
            "objects.castledoubledoorl",         "objects.castledoubledoorr",
            "objects.opencastledoubledoorl",     "objects.opencastledoubledoorr"
        ),
        DoubleDoor(
            "objects.fai_falador_double_door_l",       "objects.fai_falador_double_door_r",
            "objects.fai_falador_double_door_l_open",  "objects.fai_falador_double_door_r_open"
        )
    )

    // ── Lookup maps (built from above) ────────────────────────────────────────

    // closed RSCM → SingleDoor
    private val byClosedRscm = SINGLE_DOORS.associateBy { it.closedRscm }
    // open RSCM   → SingleDoor (for clicking an open door to close it)
    private val byOpenRscm   = SINGLE_DOORS.associateBy { it.openRscm }

    // any RSCM → DoubleDoor
    private val byDoubleRscm = buildMap<String, DoubleDoor> {
        DOUBLE_DOORS.forEach { d ->
            put(d.closedLeft,  d); put(d.closedRight, d)
            put(d.openedLeft,  d); put(d.openedRight, d)
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    override fun init() {
        // Single — closed side
        SINGLE_DOORS.forEach { door ->
            doorOptionsFor(door.closedRscm).forEach { opt ->
                onObjectOption(door.closedRscm, opt) { handleSingleOpen(this) }
            }
            // Single — open side (click to close)
            doorOptionsFor(door.openRscm).forEach { opt ->
                onObjectOption(door.openRscm, opt) { handleSingleClose(this) }
            }
        }

        // Double doors
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
        val obj     = event.gameObject
        val isLeft  = obj.id == door.closedLeft
        val tile    = obj.tile
        val type    = obj.type
        val w       = event.player.world

        w.queue {
            // Open clicked half (left hinge opens left, right hinge opens right)
            val fresh = w.getObject(tile, type) ?: return@queue
            val newRot = openRot(fresh.rot, if (isLeft) Hinge.LEFT else Hinge.RIGHT)
            w.remove(fresh)
            w.spawn(DynamicObject(if (isLeft) door.openedLeft else door.openedRight, type, newRot, tile))

            // Open partner half
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

    /** Rotation to use when opening a door. */
    private fun openRot(currentRot: Int, hinge: Hinge) = when (hinge) {
        Hinge.LEFT  -> (currentRot + 3) % 4   // -90°
        Hinge.RIGHT -> (currentRot + 1) % 4   // +90°
    }

    /** Rotation to use when closing (reverse of open). */
    private fun closeRot(currentRot: Int, hinge: Hinge) = when (hinge) {
        Hinge.LEFT  -> (currentRot + 1) % 4   // +90° back
        Hinge.RIGHT -> (currentRot + 3) % 4   // -90° back
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
        val actions = getObject(getRSCM(rscmName))?.actions
            ?.filterNotNull()?.filter { it.isNotBlank() }
        return if (actions.isNullOrEmpty()) listOf("Open", "Close") else actions
    }
}