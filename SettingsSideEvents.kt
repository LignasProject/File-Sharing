package org.alter.interfaces.settings

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.ext.setVarbit
import org.alter.api.ext.toggleVarbit
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfOpen
import org.alter.interfaces.ifCloseOverlay
import org.alter.interfaces.ifOpenOverlay
import org.alter.interfaces.ifSetEvents
import org.alter.interfaces.settings.configs.setting_components


class SettingsSideScript : PluginEvent() {

    override fun init() {
        onIfOpen("interfaces.settings_side") { player.updateIfEvents() }

        SettingsTabView.entries.forEach {
            onButton(it.component) {
                player.setVarbit("varbits.settings_side_panel_tab",it.varValue)
            }
        }

        onButton(setting_components.settings_open) {
            player.ifOpenOverlay("interfaces.settings")
        }

        // Bug fix: handle the close button on the main settings overlay (134:4)
        onButton(setting_components.settings_close) {
            player.ifCloseOverlay("interfaces.settings")
        }

        // Full settings panel (134): enable button events on the clickzone so that
        // toggle clicks (handled client-side by CS2) also reach the server for persistence.
        onIfOpen("interfaces.settings") {
            player.ifSetEvents(setting_components.settings_clickzone, 0..500, IfEvent.Op1)
        }

        onButton(setting_components.settings_clickzone) {
            player.handleSettingsToggle(slot)
        }
    }

    private fun Player.handleSettingsToggle(slot: Int) {
        val varbit = SETTINGS_CLICKZONE_TOGGLES[slot]
        if (varbit != null) {
            toggleVarbit(varbit)
        }
    }

    private fun Player.updateIfEvents() {
        ifSetEvents(setting_components.music_bobble_container, 0..21, IfEvent.Op1)
        ifSetEvents(setting_components.sound_bobble_container, 0..21, IfEvent.Op1)
        ifSetEvents(setting_components.areasounds_bobble_container, 0..21, IfEvent.Op1)
        ifSetEvents(setting_components.master_bobble_container, 0..21, IfEvent.Op1)
        ifSetEvents(setting_components.attack_priority_player_buttons, 1..5, IfEvent.Op1)
        ifSetEvents(setting_components.attack_priority_npc_buttons, 1..4, IfEvent.Op1)
        ifSetEvents(setting_components.client_type_buttons, 1..3, IfEvent.Op1)
        ifSetEvents(setting_components.brightness_bobble_container, 0..21, IfEvent.Op1)
    }

    companion object {
        // Maps child slot on settings:settings_clickzone to the varbit toggled by that setting.
        // The client's CS2 handles the visual toggle; this keeps the server in sync for persistence.
        // To discover unmapped slots: enable debug-buttons in dev.yml, open the full settings
        // panel, click toggles, and note the slot=N values in the debug output.
        private val SETTINGS_CLICKZONE_TOGGLES = mapOf(
            176 to "varbits.desktop_shiftclickdrop_enabled",
            212 to "varbits.esc_to_close_desktop",
        )
    }
}

private enum class SettingsTabView(val component : String, val varValue: Int) {
    Control(setting_components.settings_tab,0),
    Audio(setting_components.audio_tab,1),
    Display(setting_components.display_tab,2),
}
