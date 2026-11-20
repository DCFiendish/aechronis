package net.aechronis.brewery.stations

import net.aechronis.brewery.BreweryItems
import net.aechronis.brewery.fx.Fx
import net.aechronis.brewery.fx.HandAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.item.ItemStack

/** Admin-only demo proving the framework: one prop, one hitbox, a persisted click counter. */
object TestBench : StationType {
    override val id = "test_bench"
    override val kit = BreweryItems.testBenchKit
    override val displayName: Component = Component.text("Test Bench", NamedTextColor.GOLD)

    override fun parts(state: StationState) =
        listOf(StationPart("top", "aechronis:brewery/test_bench", offset = Vec(0.0, 0.5, 0.0), hitboxHeight = 1f))

    override fun use(
        ctx: StationContext,
        part: String,
        held: ItemStack,
        click: Click,
    ) {
        val clicks = (ctx.state.contents["clicks"] ?: 0) + 1
        ctx.state.contents["clicks"] = clicks
        ctx.lock(part, Fx.handAction(ctx.player, HandAction.PLACE))
        ctx.player.sendMessage(Component.text("Test bench clicks: $clicks", NamedTextColor.YELLOW))
    }

    override fun label(state: StationState): Component =
        displayName.append(Component.text(" • ${state.contents["clicks"] ?: 0} clicks • ${state.stage}", NamedTextColor.GRAY))
}
