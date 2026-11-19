package boardgame.api

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Incremental object synchronisation: a path plus the new value for that path. */
@GenerateTs
@Serializable
data class SyncPart(val path: String? = null, val value: JsonElement)

@GenerateTs(typeLiteral = "ObjectSyncMessage")
@Serializable
data class ObjectSyncMessage(
    val type: String = "ObjectSyncMessage",
    val objectId: String,
    val parts: List<SyncPart>
)
