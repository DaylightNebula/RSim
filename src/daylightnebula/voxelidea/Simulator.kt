package daylightnebula.voxelidea

object Simulator {
    val blankTile = TileInstance(0, 0, false, -1, -1)

    fun simulate(template: Template, printData: Boolean = false): Boolean {
        // loop through each truth to run each
        for (truth in template.truths.indices) {

            // start timer
            val startTime = System.currentTimeMillis()
            if (printData) println("[SIM-TRUTH-${truth}] Starting simulation for truth $truth")

            // build sim data
            val simData = SimData(truth, 0, mutableListOf(), mutableListOf(), template)

            // loop through each time, and run each preprocess function
            template.tiles.forEachIndexed { arrIndex, arr ->
                arr.forEachIndexed { index, tile ->
                    val success = TileProcessor.get(tile.tileID).preprocess(
                        simData,
                        tile,
                        arrayOf(
                            specialGet(template.tiles, arrIndex, index - 1),
                            specialGet(template.tiles, arrIndex, index + 1),
                            specialGet(template.tiles, arrIndex - 1, index),
                            specialGet(template.tiles, arrIndex + 1, index),
                        )
                    )
                    if (!success) {
                        if (printData) println("[SIM-TRUTH-${truth}] Failing pre process on truth $truth")
                        return false
                    }
                }
            }

            // timer stuff
            val preprocessTime = System.currentTimeMillis()
            if (printData) println("[SIM-TRUTH-${truth}] Passed preprocess for truth $truth in ${preprocessTime - startTime} MS")

            while (simData.tickTasks.size > 0) {

                // while loop until there are no tick tasks left for the current tick
                while (simData.tickTasks.count { it.tick == simData.currentTick } > 0) {
                    val tasks = simData.tickTasks.filter { it.tick == simData.currentTick }
                    for (it in tasks) {
                        val result = TileProcessor.get(it.tile.tileID).tickTask(
                            simData,
                            it.tile,
                            arrayOf(
                                specialGet(template.tiles, it.tile.arrIndex, it.tile.index - 1),
                                specialGet(template.tiles, it.tile.arrIndex, it.tile.index + 1),
                                specialGet(template.tiles, it.tile.arrIndex - 1, it.tile.index),
                                specialGet(template.tiles, it.tile.arrIndex + 1, it.tile.index),
                            )
                        )
                        println("[SIM-TRUTH-${truth}-TICK-${simData.currentTick}] Ticked task for (${it.tile.tileID}, (${it.tile.index}, ${it.tile.arrIndex}))")
                        if (!result) {
                            if (printData) println("[SIM-TRUTH-${truth}] Failing on truth $truth")
                            return false
                        }
                    }
                    simData.tickTasks.removeAll(tasks)
                }

                // run mid run check passes
                for (it in simData.midRunCheckPass)
                    if (!TileProcessor.get(it.tileID).checkPass(simData, it))
                        return false
                simData.midRunCheckPass.clear()

                // remove any tick tasks that are too old
                simData.tickTasks.removeIf { it.tick < simData.currentTick }

                // update current tick
                simData.currentTick++
            }

            // timer stuff
            val mainLoopTime = System.currentTimeMillis()
            if (printData) println("[SIM-TRUTH-${truth}] Passed main loop for truth $truth in ${mainLoopTime - preprocessTime} MS")

            // check pass loop
            template.tiles.forEach { arr ->
                arr.forEach {
                    if (!TileProcessor.get(it.tileID).checkPass(simData, it)) {
                        if (printData) println("[SIM-TRUTH-${truth}] Failing check pass on truth $truth")
                        return false
                    }
                }
            }

            // timer stuff
            val checkPassTimer = System.currentTimeMillis()
            if (printData) println("[SIM-TRUTH-${truth}] Passed check pass with truth $truth in ${checkPassTimer - mainLoopTime} MS")
        }

        return true
    }

    fun specialGet(arrs: Array<Array<TileInstance>>, arrIndex: Int, index: Int): TileInstance {
        if (arrIndex < 0 || arrIndex >= arrs.size) return blankTile
        if (index < 0 || index >= arrs[0].size) return blankTile
        return arrs[arrIndex][index]
    }
}
data class TickTask(
    val tile: TileInstance,
    val tickedBy: TileInstance,
    val tick: Int
)
data class SimData(
    val truth: Int,
    var currentTick: Int,
    val tickTasks: MutableList<TickTask>,
    val midRunCheckPass: MutableList<TileInstance>,
    val template: Template
)