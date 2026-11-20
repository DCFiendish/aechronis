package net.aechronis.brewery

import net.aechronis.combat.objects.Item
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack

/**
 * A brewery item. [id] is the short name used by `/brewery give`; the registered combat name is
 * `brewery-<id>` and the item model is `aechronis:brewery/<id>`.
 */
class BreweryItem(
    val id: String,
    displayName: String,
    color: NamedTextColor = NamedTextColor.WHITE,
    val maxStack: Int = 64,
) : Item(
        name = "brewery-$id",
        itemName = Component.text(displayName, color).decoration(TextDecoration.ITALIC, false),
        itemModel = "aechronis:brewery/$id",
    ) {
    override fun toItemStack(): ItemStack =
        super.toItemStack().let {
            if (maxStack ==
                64
            ) {
                it
            } else {
                it.with(DataComponents.MAX_STACK_SIZE, maxStack)
            }
        }

    fun stack(amount: Int = 1): ItemStack = toItemStack().withAmount(amount)
}

/** A liquid container; filled variants use the model `aechronis:brewery/<id>_filled`. */
class Container(
    val item: BreweryItem,
    val capacity: Int,
) {
    val filledModel = "${item.itemModel}_filled"

    /** Empty when [data] has no measures, otherwise the filled model with its lore. */
    fun stack(data: ProductData = ProductData()): ItemStack {
        val base = item.toItemStack()
        return if (data.liquid == null || data.measures <= 0) base else data.applyTo(base.withItemModel(filledModel))
    }
}

object BreweryItems {
    private val all = mutableListOf<BreweryItem>()

    private fun item(
        id: String,
        name: String,
        color: NamedTextColor = NamedTextColor.WHITE,
        maxStack: Int = 64,
    ) = BreweryItem(id, name, color, maxStack).also { all += it }

    // --- ingredients ---
    val grapes = item("grapes", "Grapes", NamedTextColor.DARK_PURPLE)
    val barley = item("barley", "Barley", NamedTextColor.YELLOW)
    val honeycomb = item("honeycomb", "Honeycomb", NamedTextColor.GOLD)
    val mint = item("mint", "Mint", NamedTextColor.GREEN)
    val pennyroyal = item("pennyroyal", "Pennyroyal", NamedTextColor.DARK_GREEN)
    val thyme = item("thyme", "Thyme", NamedTextColor.GREEN)
    val grapeCutting = item("grape_cutting", "Grape Cutting", NamedTextColor.DARK_GREEN)
    val barleySeeds = item("barley_seeds", "Barley Seeds", NamedTextColor.YELLOW)
    val barleyMeal = item("barley_meal", "Barley Meal", NamedTextColor.YELLOW)

    // --- containers (fill state lives in ProductData.liquid/measures) ---
    val jug = Container(item("jug", "Clay Jug", NamedTextColor.GOLD, maxStack = 1), capacity = 4)
    val pitcher = Container(item("pitcher", "Oinochoe", NamedTextColor.GOLD, maxStack = 1), capacity = 2)
    val amphora = Container(item("amphora", "Amphora", NamedTextColor.GOLD, maxStack = 1), capacity = 16)
    val containers = listOf(jug, pitcher, amphora)

    // --- station kits ---
    val testBenchKit = item("test_bench_kit", "Test Bench (admin)", NamedTextColor.RED, maxStack = 1)
    val winePressKit = item("wine_press_kit", "Wine Press Kit", NamedTextColor.AQUA, maxStack = 1)
    val grainMillKit = item("grain_mill_kit", "Grain Mill Kit", NamedTextColor.AQUA, maxStack = 1)
    val fermenterKit = item("fermenter_kit", "Fermenting Pithos Kit", NamedTextColor.AQUA, maxStack = 1)
    val mixingKraterKit = item("mixing_krater_kit", "Mixing Krater Kit", NamedTextColor.AQUA, maxStack = 1)
    val cellarRackKit = item("cellar_rack_kit", "Cellar Rack Kit", NamedTextColor.AQUA, maxStack = 1)

    // --- beverages (one cup; quality etc. in ProductData) ---
    val wine = item("wine", "Oinos", NamedTextColor.DARK_RED, maxStack = 1)
    val kykeon = item("kykeon", "Kykeon", NamedTextColor.YELLOW, maxStack = 1)
    val hydromel = item("hydromel", "Hydromel", NamedTextColor.GOLD, maxStack = 1)
    val zythos = item("zythos", "Zythos", NamedTextColor.YELLOW, maxStack = 1)
    val beverages = listOf(wine, kykeon, hydromel, zythos)

    // --- part B ---
    // --- part C ---
    // --- part D ---
    // --- part E ---
    // --- end parts ---

    fun all(): List<BreweryItem> = all

    fun byId(id: String): BreweryItem? = all.firstOrNull { it.id == id || it.name == id }

    fun of(stack: ItemStack): BreweryItem? = Item.getFromItemStack(stack) as? BreweryItem

    fun register() = Item.registerItems(*all.toTypedArray())

    /** Combat keeps the registry across a brewery-only reload; drop our old-classloader entries. */
    fun unregister() {
        all.forEach { Item.registeredItems.remove(it.name) }
    }
}
