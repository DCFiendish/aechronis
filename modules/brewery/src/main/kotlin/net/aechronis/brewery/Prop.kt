package net.aechronis.brewery

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import java.util.concurrent.ConcurrentHashMap

/**
 * An item-display model. Never saved: owners respawn props from their own state on chunk load.
 * Interpolation settings follow combat's AnimatedPart (start delta 0 on every transform).
 */
class Prop(
    model: String,
    instance: Instance,
    position: Pos,
    scale: Vec = Vec.ONE,
) {
    val entity = Entity(EntityType.ITEM_DISPLAY)

    init {
        entity.setNoGravity(true)
        (entity.entityMeta as ItemDisplayMeta).apply {
            itemStack = ItemStack.of(Material.BONE).withItemModel(model)
            posRotInterpolationDuration = 3
            transformationInterpolationDuration = 1
            this.scale = scale
            isHasNoGravity = true
        }
        entity.setInstance(instance, position)
    }

    fun model(model: String) {
        (entity.entityMeta as ItemDisplayMeta).itemStack = ItemStack.of(Material.BONE).withItemModel(model)
    }

    /** Animates to the given local transform over [ticks]; rotation is a quaternion (x, y, z, w). */
    fun transform(
        translation: Vec = Vec.ZERO,
        rotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
        scale: Vec? = null,
        ticks: Int = 1,
    ) {
        val meta = entity.entityMeta as ItemDisplayMeta
        meta.setNotifyAboutChanges(false)
        try {
            meta.transformationInterpolationDuration = ticks
            meta.translation = translation
            meta.rightRotation = rotation
            scale?.let { meta.scale = it }
            meta.transformationInterpolationStartDelta = 0
        } finally {
            meta.setNotifyAboutChanges(true)
        }
    }

    fun remove() = entity.remove()
}

/** An INTERACTION entity routing right clicks (interact) and left clicks (attack) to callbacks. */
class Hitbox(
    instance: Instance,
    position: Pos,
    width: Float,
    height: Float,
    val onRight: (Player) -> Unit,
    val onLeft: (Player) -> Unit,
) {
    val entity = Entity(EntityType.INTERACTION)

    init {
        entity.setNoGravity(true)
        (entity.entityMeta as InteractionMeta).apply {
            this.width = width
            this.height = height
            response = true
        }
        entity.setInstance(instance, position)
        byEntity[entity] = this
    }

    fun remove() {
        byEntity.remove(entity)
        entity.remove()
    }

    companion object {
        private val byEntity = ConcurrentHashMap<Entity, Hitbox>()

        fun of(entity: Entity): Hitbox? = byEntity[entity]
    }
}
