package net.aechronis.brewery.commands

import net.aechronis.brewery.BreweryConfig
import net.aechronis.brewery.BreweryItems
import net.aechronis.brewery.Origin
import net.aechronis.brewery.Ownership
import net.aechronis.brewery.PosKey
import net.aechronis.brewery.Producer
import net.aechronis.brewery.ProductData
import net.aechronis.brewery.stations.Stations
import net.aechronis.nodes.objects.Territory
import net.aechronis.utils.Command
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

class BreweryCommand : Command("brewery", "brewery.admin") {
    private val itemArg = ArgumentType.Word("item").from(*BreweryItems.all().map { it.id }.toTypedArray())
    private val qualityArg = ArgumentType.Integer("quality").between(0, 100)
    private val stageArg = ArgumentType.Word("stage")
    private val modeArg = ArgumentType.Word("mode").from("on", "off")

    init {
        addSyntax({ player: Player, context -> give(player, context[itemArg], null) }, ArgumentType.Literal("give"), itemArg)
        addSyntax(
            { player: Player, context -> give(player, context[itemArg], context[qualityArg]) },
            ArgumentType.Literal("give"),
            itemArg,
            qualityArg,
        )
        addSyntax({ player: Player, _ -> info(player) }, ArgumentType.Literal("info"))
        addSyntax({ player: Player, context ->
            val station = Stations.lookedAt(player) ?: return@addSyntax player.sendMessage(red("Look at a station."))
            station.state.setStage(context[stageArg])
            station.rebuild()
            player.sendMessage(Component.text("Stage set to ${station.state.stage}.", NamedTextColor.GREEN))
        }, ArgumentType.Literal("setstage"), stageArg)
        addSyntax({ player: Player, context ->
            BreweryConfig.current = if (context[modeArg] == "on") BreweryConfig.TEST else BreweryConfig()
            player.sendMessage(Component.text("Brewery durations: ${BreweryConfig.current}", NamedTextColor.GREEN))
        }, ArgumentType.Literal("testmode"), modeArg)
    }

    private fun give(
        player: Player,
        id: String,
        quality: Int?,
    ) {
        val item = BreweryItems.byId(id) ?: return player.sendMessage(red("Unknown brewery item $id."))
        var stack = item.stack()
        val isBeverage = item in BreweryItems.beverages
        if (quality != null || isBeverage) {
            val pos = player.instance?.let { PosKey.of(it, player.position) }
            val territory = Territory.fromBlock(player.position.blockX(), player.position.blockZ())
            val data =
                ProductData(
                    beverage = if (isBeverage) item.id else null,
                    quality = quality ?: 50,
                    origin = territory?.let { Origin(it.id.toInt(), it.name) },
                    producer = Producer(player.uuid.toString(), player.username),
                    town = pos?.let { Ownership.townAt(it)?.name },
                    brewedAt = System.currentTimeMillis(),
                )
            stack = data.applyTo(stack)
        }
        player.inventory.addItemStack(stack)
        player.sendMessage(Component.text("Gave ${item.id}${quality?.let { " (quality $it)" } ?: ""}.", NamedTextColor.GREEN))
    }

    private fun info(player: Player) {
        val station = Stations.lookedAt(player) ?: return player.sendMessage(red("Look at a station."))
        val state = station.state
        val town = Ownership.townAt(state.pos)?.name ?: "none"
        player.sendMessage(
            Component.text(
                "Station ${state.type} at ${state.pos.x}, ${state.pos.y}, ${state.pos.z} • town $town • stage ${state.stage} " +
                    "• contents ${state.contents} • batch ${state.batch}",
                NamedTextColor.AQUA,
            ),
        )
    }

    private fun red(text: String) = Component.text(text, NamedTextColor.RED)
}
