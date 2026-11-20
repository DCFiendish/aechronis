package net.aechronis.brewery

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.json.Json
import net.aechronis.utils.JsonRegistryFile
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** A block position in a named world (`Instance.getDimensionName()`). */
@Serializable
data class PosKey(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun instance(): Instance? = MinecraftServer.getInstanceManager().instances.firstOrNull { it.getDimensionName() == world }

    fun isLoaded(): Boolean = instance()?.isChunkLoaded(x shr 4, z shr 4) == true

    companion object {
        fun of(
            instance: Instance,
            point: Point,
        ) = PosKey(instance.getDimensionName(), point.blockX(), point.blockY(), point.blockZ())
    }
}

/**
 * Position-keyed JSON maps under `run/brewery/<name>.json`. Each part asks for its own map once in
 * init; everything is written on checkpoint/saveState (and on [saveAll] after important edits).
 */
object BreweryStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val savers = mutableListOf<() -> Unit>()

    fun <V> map(
        name: String,
        serializer: KSerializer<V>,
    ): MutableMap<PosKey, V> {
        val map = ConcurrentHashMap<PosKey, V>()
        val file = JsonRegistryFile("brewery $name")
        val listSerializer = ListSerializer(PairSerializer(PosKey.serializer(), serializer))
        file.load(Path.of("brewery", "$name.json"), map) { json.decodeFromString(listSerializer, it).toMap() }
        savers += { file.save { json.encodeToString(listSerializer, map.toList()) } }
        return map
    }

    fun saveAll() = savers.forEach { it() }
}
