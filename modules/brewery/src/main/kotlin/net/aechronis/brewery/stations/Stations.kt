package net.aechronis.brewery.stations

import net.aechronis.brewery.BreweryEvents
import net.aechronis.brewery.BreweryItems
import net.aechronis.brewery.BreweryStore
import net.aechronis.brewery.BreweryTicker
import net.aechronis.brewery.Ownership
import net.aechronis.brewery.PosKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Instance
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object StationTypes {
    private val types = linkedMapOf<String, StationType>()

    fun register(type: StationType) {
        require(type.id !in types) { "Station type ${type.id} registered twice" }
        types[type.id] = type
    }

    fun get(id: String): StationType? = types[id]

    fun byKit(itemName: String): StationType? = types.values.firstOrNull { it.kit.name == itemName }

    fun all(): Collection<StationType> = types.values
}

/** Placement, removal, persistence and chunk-driven spawning of every station type. */
object Stations {
    private lateinit var states: MutableMap<PosKey, StationState>
    private val live = ConcurrentHashMap<PosKey, Station>()

    fun init() {
        states = BreweryStore.map("stations", StationState.serializer())
        BreweryEvents.node.addListener(PlayerBlockInteractEvent::class.java, ::onBlockInteract)
        BreweryEvents.node.addListener(InstanceChunkLoadEvent::class.java) { spawnChunk(it.instance, it.chunkX, it.chunkZ) }
        BreweryEvents.node.addListener(InstanceChunkUnloadEvent::class.java) { event ->
            live.keys
                .filter { it.x shr 4 == event.chunkX && it.z shr 4 == event.chunkZ && it.world == event.instance.getDimensionName() }
                .forEach { live.remove(it)?.despawn() }
        }
        MinecraftServer.getInstanceManager().instances.forEach { instance ->
            instance.chunks.forEach { spawnChunk(instance, it.chunkX, it.chunkZ) }
        }
        BreweryTicker.register(::tick)
        println("Brewery stations enabled (${states.size} placed)")
    }

    fun shutdown() {
        live.values.forEach(Station::despawn)
        live.clear()
    }

    fun live(): Collection<Station> = live.values

    fun at(pos: PosKey): Station? = live[pos]

    /** The station closest to the player's crosshair within reach. */
    fun lookedAt(player: Player): Station? {
        val eye = player.position.add(0.0, player.eyeHeight, 0.0)
        val direction = player.position.direction()
        return live.values
            .filter { it.instance === player.instance && it.base.distanceSquared(eye) <= 36.0 }
            .map { station ->
                val toward =
                    station.base
                        .add(0.0, 0.5, 0.0)
                        .sub(eye)
                        .asVec()
                        .normalize()
                station to toward.dot(direction)
            }.filter { it.second > 0.9 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun spawnChunk(
        instance: Instance,
        chunkX: Int,
        chunkZ: Int,
    ) {
        val world = instance.getDimensionName()
        for (state in states.values) {
            val pos = state.pos
            if (pos.world != world || pos.x shr 4 != chunkX || pos.z shr 4 != chunkZ || live.containsKey(pos)) continue
            val type = StationTypes.get(state.type) ?: continue
            live[pos] = Station(state, type, instance).also(Station::spawn)
        }
    }

    private fun tick(now: Long) {
        for (station in live.values) {
            station.type.tick(station, now)
            station.present(now)
        }
    }

    private fun onBlockInteract(event: PlayerBlockInteractEvent) {
        if (event.hand != PlayerHand.MAIN) return
        val player = event.player
        val held = player.itemInMainHand
        val type = BreweryItems.of(held)?.let { StationTypes.byKit(it.name) } ?: return
        event.isCancelled = true
        event.isBlockingItemUse = true
        val position = event.blockPosition.relative(event.blockFace)
        val pos = PosKey.of(event.instance, position)
        if (!Ownership.canManage(player, pos)) {
            player.sendMessage(Component.text("Only your town's leader or officers can place stations, on town land.", NamedTextColor.RED))
            return
        }
        if (states.containsKey(pos) || !event.instance.getBlock(position).air()) {
            player.sendMessage(Component.text("There is no room for a station here.", NamedTextColor.RED))
            return
        }
        // Face the placer.
        val facing = ((player.position.yaw + 180f) / 90f).roundToInt() * 90f % 360f
        val town = Ownership.townAt(pos) ?: return
        val state = StationState(UUID.randomUUID().toString(), type.id, pos, facing, town.uuid.toString())
        states[pos] = state
        if (player.gameMode != GameMode.CREATIVE) player.itemInMainHand = held.consume(1)
        live[pos] = Station(state, type, event.instance).also(Station::spawn)
        BreweryStore.saveAll()
        player.sendMessage(Component.text("Placed ", NamedTextColor.GREEN).append(type.displayName))
    }

    /** Routes a hitbox click: managers sneak-left-click to pick a station up, residents use it. */
    internal fun click(
        station: Station,
        part: String,
        player: Player,
        click: Click,
    ) {
        val pos = station.state.pos
        if (live[pos] !== station) return
        if (click == Click.LEFT && player.isSneaking && Ownership.canManage(player, pos)) {
            remove(station, player)
            return
        }
        if (!Ownership.canUse(player, pos)) {
            val owner = Ownership.townAt(pos)?.name ?: "no town"
            player.sendMessage(Component.text("This station belongs to $owner.", NamedTextColor.RED))
            return
        }
        if (station.isLocked(part)) return
        station.type.use(StationContext(player, station), part, player.itemInMainHand, click)
    }

    // ponytail: contents and batch are discarded on pickup; refund them once real stations hold goods
    private fun remove(
        station: Station,
        player: Player,
    ) {
        val pos = station.state.pos
        states.remove(pos)
        live.remove(pos)?.despawn()
        BreweryStore.saveAll()
        player.inventory.addItemStack(station.type.kit.stack())
        player.sendMessage(Component.text("Picked up ", NamedTextColor.YELLOW).append(station.type.displayName))
    }
}
