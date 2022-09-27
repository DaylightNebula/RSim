package daylightnebula.voxelidea

object Simulator {
    fun simulate(template: Template, printData: Boolean = false): Boolean {
        // loop through all potention truths
        template.truths.forEachIndexed { truthIndex, truth ->
            // setup timers
            if (printData) println("[TRUTH-${truthIndex}] Starting simulation!")
            val startTime = System.currentTimeMillis()

            // create sim data
            val simData = SimData(template.tiles, truth)

            // run preprocess pass ( no braces cause I'm evil >:) )
            for (arr in simData.tiles)
                for (tile in arr)
                    if (!TileProcessor.get(tile.tileID).preprocess(simData, tile, simData.getSides(tile))) {
                        if (printData) println("[TRUTH-${truthIndex}] Failed in pre-process for (${tile.index}, ${tile.arrIndex})")
                        return false
                    }

            // print and update timers
            val finishedPreProcessTime = System.currentTimeMillis()
            if (printData) println("[TRUTH-${truthIndex}] Finished preprocess in ${finishedPreProcessTime - startTime} MS")

            // loop until we run out of tick tasks
            while (simData.waitingTickTasks() > 0) {
                // loop while we have tick tasks for the current tick
                while(simData.haveTickTasks()) {
                    simData.nextTickTasks().forEach {
                        TileProcessor.get(it.me.tileID).tickTask(simData, it.me, simData.getSides(it.me))
                    }
                }

                // run mid-run check pass
                simData.getMidRunChecks().forEach {
                    if (!TileProcessor.get(it.tileID).checkPass(simData, it, simData.getSides(it))) {
                        if (printData) println("[TRUTH-${truthIndex}-TICK-${simData.getTick()}] Failed in mid-run check pass for (${it.index}, ${it.arrIndex})")
                        return false
                    }
                }

                // tick sim data
                simData.tick()
            }

            // print and update timers
            val finishedTickLoopTime = System.currentTimeMillis()
            if (printData) println("[TRUTH-${truthIndex}] Finished tick loop in ${finishedTickLoopTime - finishedPreProcessTime} MS")

            // run final check pass
            for (arr in simData.tiles)
                for (tile in arr)
                    if (!TileProcessor.get(tile.tileID).checkPass(simData, tile, simData.getSides(tile))) {
                        if (printData) println("[TRUTH-${truthIndex}] Failed final check pass for (${tile.index}, ${tile.arrIndex})")
                        return false
                    }

            // print and update timers
            val finishedTime = System.currentTimeMillis()
            if (printData) println("[TRUTH-${truthIndex}] Finished simulation in ${finishedTime - startTime} MS")
        }

        return true
    }
}
class SimData(
    val tiles: Array<Array<TileInstance>>,
    private val truth: Truth
) {
    private val powerStates = Array(tiles.size) { BooleanArray(tiles[0].size) { false } }
    private val tickTasks = mutableListOf<TickTask>()
    private val midRunChecks = mutableListOf<TileInstance>()
    private var currentTick = 0

    fun getTick(): Int { return currentTick }
    fun tick() {
        // remove old tick tasks and mid run checks
        tickTasks.removeIf { it.tick <= currentTick }
        midRunChecks.clear()

        // advance counter
        currentTick++
    }

    fun addMidRunCheck(tile: TileInstance) {
        if (!midRunChecks.contains(tile)) midRunChecks.add(tile)
    }

    fun getMidRunChecks(): List<TileInstance> {
        return midRunChecks
    }

    fun addTickTask(task: TickTask) {
        tickTasks.add(task)
    }

    fun waitingTickTasks(): Int {
        return tickTasks.size
    }

    fun getTickTasks(): List<TickTask> {
        return tickTasks
    }

    fun haveTickTasks(): Boolean {
        return tickTasks.count { it.tick == currentTick } > 0
    }

    fun nextTickTasks(): List<TickTask> {
        val out = mutableListOf<TickTask>()
        tickTasks.removeIf {
            val result = it.tick == currentTick
            if (result)
                out.add(it)
            result
        }
        return out
    }

    fun getPowerState(arrIndex: Int, index: Int): Boolean? {
        return if (isPositionValid(arrIndex, index)) powerStates[arrIndex][index] else null
    }

    fun setPowerState(arrIndex: Int, index: Int, state: Boolean) {
        if (isPositionValid(arrIndex, index)) powerStates[arrIndex][index] = state
    }

    fun getTile(arrIndex: Int, index: Int): TileInstance? {
        return if (isPositionValid(arrIndex, index)) tiles[arrIndex][index] else null
    }

    fun getSides(tile: TileInstance): List<TileInstance> {
        return getSides(tile.arrIndex, tile.index)
    }

    fun getSides(arrIndex: Int, index: Int): List<TileInstance> {
        val out = mutableListOf(
            getTile(arrIndex - 1, index),
            getTile(arrIndex + 1, index),
            getTile(arrIndex, index - 1),
            getTile(arrIndex, index + 1)
        )
        return out.filterNotNull()
    }

    fun getSidesWithExtras(arrIndex: Int, index: Int): List<TileInstance> {
        return listOf<TileInstance?>(
            getTile(arrIndex - 1, index),
            getTile(arrIndex + 1, index),
            getTile(arrIndex - 1, index + 1),
            getTile(arrIndex + 1, index + 1),
            getTile(arrIndex - 1, index - 1),
            getTile(arrIndex + 1, index - 1),
            getTile(arrIndex, index - 1),
            getTile(arrIndex, index + 1)
        ).filterNotNull()
    }

    fun isPositionValid(arrIndex: Int, index: Int): Boolean {
        return (arrIndex >= 0 && arrIndex < tiles.size) && (index >= 0 && index < tiles[0].size)
    }

    fun getInputState(me: TileInstance): Boolean {
        return truth.inputs[me.data]
    }

    fun getOutputState(me: TileInstance): Boolean {
        return truth.outputs[me.data]
    }
}
data class TickTask(
    val me: TileInstance,
    val tickedBy: TileInstance,
    val tick: Int
)