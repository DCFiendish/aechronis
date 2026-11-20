package net.aechronis.brewery.recipes

/**
 * A beverage recipe. [ingredients] maps brewery item ids (or liquid ids such as `grape_must`) to
 * amounts. Durations are reference values; stations scale them by BreweryConfig.
 */
data class BeverageRecipe(
    val id: String,
    val beverage: String,
    val ingredients: Map<String, Int>,
    val fermentMs: Long,
    val idealAgeMs: Long = 0,
    val potency: Int = 0,
)

/** The recipe catalogue. Part D fills it. */
object Recipes {
    private val recipes = linkedMapOf<String, BeverageRecipe>()

    fun init() {
        // --- part D ---
    }

    fun register(recipe: BeverageRecipe) {
        require(recipe.id !in recipes) { "Recipe ${recipe.id} registered twice" }
        recipes[recipe.id] = recipe
    }

    fun get(id: String): BeverageRecipe? = recipes[id]

    fun all(): Collection<BeverageRecipe> = recipes.values
}
