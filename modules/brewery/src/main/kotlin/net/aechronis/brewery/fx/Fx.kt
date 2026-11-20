package net.aechronis.brewery.fx

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent

enum class HandAction { POUR, STIR, CRANK, PULL, PLACE, DRINK, PICK }

enum class BrewerySound(
    val event: SoundEvent,
) {
    POUR(SoundEvent.ITEM_BOTTLE_EMPTY),
    FILL(SoundEvent.ITEM_BOTTLE_FILL),
    STIR(SoundEvent.ITEM_BUCKET_FILL),
    CRANK(SoundEvent.BLOCK_GRINDSTONE_USE),
    PRESS(SoundEvent.BLOCK_PISTON_EXTEND),
    BUBBLE(SoundEvent.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT),
    CORK(SoundEvent.BLOCK_WOOD_PLACE),
    DRINK(SoundEvent.ENTITY_GENERIC_DRINK),
    PLACE(SoundEvent.BLOCK_DECORATED_POT_PLACE),
    HARVEST(SoundEvent.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES),
}

enum class BreweryParticle(
    val particle: Particle,
) {
    SPLASH(Particle.SPLASH),
    BUBBLE(Particle.BUBBLE_POP),
    DUST(Particle.WHITE_ASH),
    STEAM(Particle.CLOUD),
    SPARKLE(Particle.HAPPY_VILLAGER),
}

/**
 * Presentation facade. Part A stub: arm swing + vanilla sound, fixed 10-tick actions. Part F
 * replaces the bodies with first-person animations via combat's core-shader pipeline.
 */
object Fx {
    private val ACTION_SOUNDS =
        mapOf(
            HandAction.POUR to BrewerySound.POUR,
            HandAction.STIR to BrewerySound.STIR,
            HandAction.CRANK to BrewerySound.CRANK,
            HandAction.PULL to BrewerySound.PRESS,
            HandAction.PLACE to BrewerySound.PLACE,
            HandAction.DRINK to BrewerySound.DRINK,
            HandAction.PICK to BrewerySound.HARVEST,
        )

    fun init() = Unit

    /** Plays the action and returns how many ticks it takes; callers lock the part for that long. */
    fun handAction(
        player: Player,
        action: HandAction,
    ): Int {
        player.swingMainHand()
        player.instance?.let { sound(it, player.position, ACTION_SOUNDS.getValue(action)) }
        return 10
    }

    fun sound(
        instance: Instance,
        pos: Point,
        sound: BrewerySound,
    ) = instance.playSound(Sound.sound(sound.event, Sound.Source.BLOCK, 1f, 1f), pos)

    fun particles(
        instance: Instance,
        pos: Point,
        particle: BreweryParticle,
        count: Int = 8,
    ) = instance.sendGroupedPacket(ParticlePacket(particle.particle, pos, Vec(0.2, 0.2, 0.2), 0f, count))
}
