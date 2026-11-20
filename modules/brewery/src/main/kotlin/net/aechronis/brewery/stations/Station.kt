package net.aechronis.brewery.stations

import kotlinx.serialization.Serializable
import net.aechronis.brewery.BreweryConfig
import net.aechronis.brewery.BreweryItem
import net.aechronis.brewery.Hitbox
import net.aechronis.brewery.PosKey
import net.aechronis.brewery.ProductData
import net.aechronis.brewery.Prop
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack

/** Persisted station. [town] is the owning town's uuid at placement; access follows the territory's current owner. */
@Serializable
data class StationState(
    val id: String,
    val type: String,
    val pos: PosKey,
    val facing: Float,
    val town: String,
    val contents: MutableMap<String, Int> = mutableMapOf(),
    var stage: String = "idle",
    var stageStartedAt: Long = 0,
    var batch: ProductData? = null,
) {
    fun setStage(
        stage: String,
        now: Long = System.currentTimeMillis(),
    ) {
        this.stage = stage
        stageStartedAt = now
    }
}

enum class Click { RIGHT, LEFT }

/**
 * One prop and/or hitbox of a station. [offset] is in station-local blocks (+z = front, towards
 * the placer) and turns with the station's facing. [model] null = hitbox only.
 */
data class StationPart(
    val id: String,
    val model: String?,
    val offset: Vec = Vec.ZERO,
    val scale: Vec = Vec.ONE,
    val hitboxWidth: Float = 1f,
    val hitboxHeight: Float = 1f,
    val interactive: Boolean = true,
)

interface StationType {
    val id: String
    val kit: BreweryItem
    val displayName: Component
    val labelHeight: Double get() = 1.4

    fun parts(state: StationState): List<StationPart>

    /** A resident clicked [part]. Locked parts never reach here. */
    fun use(
        ctx: StationContext,
        part: String,
        held: ItemStack,
        click: Click,
    )

    /** Every 20 ticks while the station's chunk is loaded. */
    fun tick(
        station: Station,
        now: Long,
    ) = Unit

    fun label(state: StationState): Component = displayName

    /** Bossbar title and 0..1 progress shown to nearby players; null hides it. */
    fun progress(
        state: StationState,
        now: Long,
    ): Pair<Component, Float>? = null
}

class StationContext(
    val player: Player,
    val station: Station,
) {
    val state get() = station.state

    /** Blocks further clicks on [part] for [ticks]. */
    fun lock(
        part: String,
        ticks: Int,
    ) = station.lock(part, ticks)

    /** Re-reads [StationType.parts] after a stage change that adds or removes parts. */
    fun rebuild() = station.rebuild()
}

/** The live (spawned) side of a station: props, hitboxes, label, bossbar. */
class Station(
    val state: StationState,
    val type: StationType,
    val instance: Instance,
) {
    val base = Pos(state.pos.x + 0.5, state.pos.y.toDouble(), state.pos.z + 0.5, state.facing, 0f)
    val props = mutableMapOf<String, Prop>()
    private val hitboxes = mutableListOf<Hitbox>()
    private val locks = mutableMapOf<String, Long>()
    private val label = Entity(EntityType.TEXT_DISPLAY)
    private var labelText: Component? = null
    private val bar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS)
    private val viewers = mutableSetOf<Player>()

    fun spawn() {
        spawnParts()
        label.setNoGravity(true)
        (label.entityMeta as TextDisplayMeta).apply {
            billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            isShadow = true
        }
        label.setInstance(instance, base.add(0.0, type.labelHeight, 0.0))
        present(System.currentTimeMillis())
    }

    private fun spawnParts() {
        for (part in type.parts(state)) {
            val position = world(part.offset)
            part.model?.let { props[part.id] = Prop(it, instance, position, part.scale) }
            if (part.interactive) {
                hitboxes +=
                    Hitbox(
                        instance,
                        position.withView(0f, 0f),
                        part.hitboxWidth,
                        part.hitboxHeight,
                        { Stations.click(this, part.id, it, Click.RIGHT) },
                        { Stations.click(this, part.id, it, Click.LEFT) },
                    )
            }
        }
    }

    fun despawn() {
        props.values.forEach(Prop::remove)
        props.clear()
        hitboxes.forEach(Hitbox::remove)
        hitboxes.clear()
        label.remove()
        labelText = null
        viewers.forEach { it.hideBossBar(bar) }
        viewers.clear()
    }

    fun rebuild() {
        props.values.forEach(Prop::remove)
        props.clear()
        hitboxes.forEach(Hitbox::remove)
        hitboxes.clear()
        spawnParts()
    }

    /** World position of a station-local offset. */
    fun world(offset: Vec): Pos = base.add(offset.rotateAroundY(-Math.toRadians(state.facing.toDouble())))

    fun isLocked(
        part: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean = (locks[part] ?: 0) > now

    fun lock(
        part: String,
        ticks: Int,
    ) {
        locks[part] = System.currentTimeMillis() + ticks * 50L
    }

    fun present(now: Long) {
        val text = type.label(state)
        if (text != labelText) {
            (label.entityMeta as TextDisplayMeta).text = text
            labelText = text
        }
        val progress = type.progress(state, now)
        val range = BreweryConfig.current.nearbyRange
        val nearby =
            if (progress == null) {
                emptySet()
            } else {
                instance.players.filterTo(mutableSetOf()) { it.position.distanceSquared(base) <= range * range }
            }
        progress?.let { (title, value) ->
            bar.name(title)
            bar.progress(value.coerceIn(0f, 1f))
        }
        (viewers - nearby).forEach { it.hideBossBar(bar) }
        (nearby - viewers).forEach { it.showBossBar(bar) }
        viewers.clear()
        viewers.addAll(nearby)
    }
}
