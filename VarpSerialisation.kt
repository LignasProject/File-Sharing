package org.alter.game.saving.impl

import org.alter.game.model.entity.Client
import org.alter.game.saving.DocumentHandler
import org.bson.Document

class VarpSerialisation(override val name: String = "varps") : DocumentHandler {

    override fun fromDocument(client: Client, doc: Document) = doc.forEach { idKey, stateValue ->
        idKey.toIntOrNull()?.let { id ->
            stateValue?.toString()?.toIntOrNull()?.let { state ->
                client.varps.setState(id, state)
            }
        }
    }

    override fun asDocument(client: Client): Document {
        return Document().apply {
            putAll(client.varps.getAll()
                .filter { it.state != 0 }
                .associate { it.id.toString() to it.state.toString() })
        }
    }

}