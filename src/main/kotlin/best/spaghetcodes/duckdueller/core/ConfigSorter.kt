package best.spaghetcodes.duckdueller.core

import gg.essential.vigilance.data.Category
import gg.essential.vigilance.data.SortingBehavior

class ConfigSorter : SortingBehavior() {

    private val items = arrayListOf(
        "General",
        "Combat",
        "Auto Requeue",
        "AutoGG",
        "Webhook",
        "Misc"
    )

    override fun getCategoryComparator(): Comparator<in Category> = compareBy { items.indexOf(it.name) }

}