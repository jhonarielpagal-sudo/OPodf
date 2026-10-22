package com.goodf.android

object Ids {
    private var n = 0L

    @Synchronized
    fun next(): Long {
        n += 1
        return n
    }
}

fun p3(n: Int): String = n.toString().padStart(3, '0')

/** props = lahat ng key sa [RankNNN] maliban sa Name (kasama ang Pipe001..., atbp.) */
data class RankItem(val id: Long, val name: String, val props: Map<String, String>)

/** opts = karagdagang key ng stop para sa rank na ito, hal. FirstAccessiblePipeLogicalKeyNumber */
data class RankLink(val rankId: Long, val opts: Map<String, String> = emptyMap())

data class StopItem(
    val id: Long,
    val name: String,
    val links: List<RankLink>,
    val extras: Map<String, String> = emptyMap()
)

data class ManualItem(
    val id: Long,
    val name: String,
    val isPedal: Boolean,
    val stopIds: List<Long>
)

data class OrganState(
    val churchName: String,
    val builder: String,
    val buildDate: String,
    val manuals: List<ManualItem>,
    val stops: List<StopItem>,
    val ranks: List<RankItem>
) {
    companion object {
        fun default(): OrganState = OrganState(
            churchName = "New Organ",
            builder = "",
            buildDate = "",
            manuals = listOf(
                ManualItem(Ids.next(), "Pedal", true, emptyList()),
                ManualItem(Ids.next(), "Great", false, emptyList()),
                ManualItem(Ids.next(), "Swell", false, emptyList())
            ),
            stops = emptyList(),
            ranks = emptyList()
        )

        /** Gumawa ng OrganState mula sa buong binasang .organ file. */
        fun fromSource(src: SourceOrgan): OrganState {
            val manuals = ArrayList<ManualItem>()
            val stops = ArrayList<StopItem>()
            val orphanStops = ArrayList<StopItem>()
            for (sm in src.manuals) {
                if (sm.unassigned) {
                    orphanStops.addAll(sm.stops)
                    continue
                }
                stops.addAll(sm.stops)
                manuals.add(ManualItem(Ids.next(), sm.name, sm.isPedal, sm.stops.map { it.id }))
            }
            // Siguraduhing may pedal sa unahan
            val pedalIdx = manuals.indexOfFirst { it.isPedal }
            if (pedalIdx < 0) {
                manuals.add(0, ManualItem(Ids.next(), "Pedal", true, emptyList()))
            } else if (pedalIdx > 0) {
                val p = manuals.removeAt(pedalIdx)
                manuals.add(0, p)
            }
            if (manuals.none { !it.isPedal }) {
                manuals.add(ManualItem(Ids.next(), "Manual 1", false, emptyList()))
            }
            if (orphanStops.isNotEmpty()) {
                val firstIdx = manuals.indexOfFirst { !it.isPedal }
                val m = manuals[firstIdx]
                manuals[firstIdx] = m.copy(stopIds = m.stopIds + orphanStops.map { it.id })
                stops.addAll(orphanStops)
            }
            return OrganState(
                churchName = src.churchName.ifBlank { "New Organ" },
                builder = src.builder,
                buildDate = src.buildDate,
                manuals = manuals,
                stops = stops,
                ranks = src.ranks
            )
        }
    }
}
