package net.aechronis.brewery

import net.aechronis.nodes.objects.Resident
import net.aechronis.nodes.objects.Territory
import net.aechronis.nodes.objects.Town
import net.minestom.server.entity.Player

/** Stations belong to the town owning the territory they stand in. */
object Ownership {
    fun townAt(pos: PosKey): Town? = Territory.fromBlock(pos.x, pos.z)?.town

    /** Any resident of the owning town. */
    fun canUse(
        player: Player,
        pos: PosKey,
    ): Boolean {
        val town = townAt(pos) ?: return false
        return Resident.fromPlayer(player)?.town === town
    }

    /** Leader or officer of the owning town (as nodes' ActiveBuildings.canManage). */
    fun canManage(
        player: Player,
        pos: PosKey,
    ): Boolean {
        val town = townAt(pos) ?: return false
        val resident = Resident.fromPlayer(player) ?: return false
        return resident.town === town && (resident === town.leader || resident in town.officers)
    }
}
