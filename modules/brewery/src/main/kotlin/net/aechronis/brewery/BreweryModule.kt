package net.aechronis.brewery

import net.aechronis.brewery.commands.BreweryCommand
import net.aechronis.brewery.crops.Agriculture
import net.aechronis.brewery.drink.Drinking
import net.aechronis.brewery.fx.Fx
import net.aechronis.brewery.recipes.Recipes
import net.aechronis.brewery.stations.StationTypes
import net.aechronis.brewery.stations.Stations
import net.aechronis.brewery.stations.TestBench
import net.aechronis.brewery.stations.cellar.CellarRack
import net.aechronis.brewery.stations.fermenter.Fermenter
import net.aechronis.brewery.stations.mill.GrainMill
import net.aechronis.brewery.stations.mixer.MixingKrater
import net.aechronis.brewery.stations.press.WinePress
import net.aechronis.server.modules.AechronisModule
import net.aechronis.server.modules.ModuleCommands
import net.aechronis.server.modules.ModuleContext
import java.util.concurrent.CompletableFuture

class BreweryModule : AechronisModule {
    override val id = "brewery"
    override val dependencies = setOf("utils", "vanilla", "combat", "nodes", "economy")

    override fun initialize(context: ModuleContext) {
        BreweryConfig.current = takeConfiguration()
        BreweryItems.register()
        BreweryEvents.init()
        Fx.init()
        Recipes.init()
        StationTypes.register(TestBench)
        // --- part B ---
        Agriculture.init()
        // --- part C ---
        WinePress.init()
        GrainMill.init()
        // --- part D ---
        Fermenter.init()
        MixingKrater.init()
        CellarRack.init()
        // --- part E ---
        Drinking.init()
        // --- end parts ---
        Stations.init()
        BreweryEvents.attach()
        BreweryTicker.start()
        ModuleCommands.register(BreweryCommand())
    }

    override fun saveCheckpoint(context: ModuleContext): CompletableFuture<Void> {
        context.captureLive(BreweryStore::saveAll)
        return CompletableFuture.completedFuture(null)
    }

    override fun saveState(context: ModuleContext) {
        context.captureLive(BreweryStore::saveAll)
    }

    override fun shutdown(context: ModuleContext) {
        context.captureLive(Stations::shutdown)
        BreweryItems.unregister()
    }

    companion object {
        private var configured: BreweryConfig? = null

        /** Called by an iteration module during configure to override durations. */
        fun configure(config: BreweryConfig) {
            check(configured == null) { "Brewery was configured by more than one active composition module" }
            configured = config
        }

        private fun takeConfiguration(): BreweryConfig = (configured ?: BreweryConfig()).also { configured = null }
    }
}
