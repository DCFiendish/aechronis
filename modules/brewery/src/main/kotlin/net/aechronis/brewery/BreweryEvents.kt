package net.aechronis.brewery

import net.aechronis.server.modules.ModuleEvents
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent

/**
 * The module's event node. Runs before guard (-1000) and nodes protection (-999) so town
 * protection does not swallow station clicks. Parts add their own listeners to [node] in init.
 */
object BreweryEvents {
    val node = EventNode.all("brewery").setPriority(-1500)

    fun init() {
        node.addListener(PlayerEntityInteractEvent::class.java) { event ->
            if (event.hand == PlayerHand.MAIN) Hitbox.of(event.target)?.onRight?.invoke(event.player)
        }
        node.addListener(EntityAttackEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            Hitbox.of(event.target)?.onLeft?.invoke(player)
        }
    }

    /** Called once after every part's init: module nodes cannot change after attachment. */
    fun attach() = ModuleEvents.addChild(MinecraftServer.getGlobalEventHandler(), node)
}
