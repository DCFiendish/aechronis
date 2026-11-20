package net.aechronis.brewery

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

@Serializable
data class Origin(
    val territory: Int,
    val region: String,
)

@Serializable
data class Producer(
    val uuid: String,
    val name: String,
)

enum class Quality(
    val label: String,
    val color: NamedTextColor,
) {
    POOR("Poor", NamedTextColor.GRAY),
    COMMON("Common", NamedTextColor.WHITE),
    FINE("Fine", NamedTextColor.AQUA),
    EXQUISITE("Exquisite", NamedTextColor.LIGHT_PURPLE),
    ;

    companion object {
        fun tier(score: Int): Quality =
            when {
                score < 25 -> POOR
                score < 50 -> COMMON
                score < 80 -> FINE
                else -> EXQUISITE
            }
    }
}

/**
 * Everything a brewery stack or batch carries. Stored on stacks as one JSON string tag; lore is
 * re-rendered whenever it is written. Containers use [liquid] + [measures]; drinks use [beverage].
 */
@Serializable
data class ProductData(
    val beverage: String? = null,
    val quality: Int = 0,
    val potency: Int = 0,
    val origin: Origin? = null,
    val producer: Producer? = null,
    val town: String? = null,
    val brewedAt: Long = 0,
    val agedMs: Long = 0,
    val liquid: String? = null,
    val measures: Int = 0,
) {
    val tier: Quality get() = Quality.tier(quality)

    fun lore(): List<Component> {
        val lines = mutableListOf<Component>()

        fun line(
            label: String,
            value: String,
            color: NamedTextColor = NamedTextColor.WHITE,
        ) {
            lines +=
                Component
                    .text("$label: ", NamedTextColor.GRAY)
                    .append(Component.text(value, color))
                    .decoration(TextDecoration.ITALIC, false)
        }
        if (liquid != null) line("Contains", "$measures × ${liquid.replace('_', ' ')}", NamedTextColor.AQUA)
        if (beverage != null || liquid != null) {
            line("Quality", "${tier.label} ($quality)", tier.color)
            if (potency > 0) line("Potency", "$potency")
        }
        origin?.let { line("Origin", it.region) }
        town?.let { line("Town", it) }
        producer?.let { line("Producer", it.name) }
        if (agedMs > 0) line("Aged", formatDuration(agedMs))
        return lines
    }

    fun applyTo(stack: ItemStack): ItemStack = stack.withTag(TAG, JSON.encodeToString(this)).withLore(lore())

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }
        val TAG: Tag<String> = Tag.String("aechronis:brewery_product")

        fun of(stack: ItemStack): ProductData? =
            stack.getTag(TAG)?.let { runCatching { JSON.decodeFromString<ProductData>(it) }.getOrNull() }

        fun formatDuration(ms: Long): String {
            val minutes = ms / 60_000
            return when {
                minutes >= 24 * 60 -> "${minutes / (24 * 60)}d ${(minutes / 60) % 24}h"
                minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m"
                else -> "${ms / 1000}s"
            }
        }
    }
}
