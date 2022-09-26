package daylightnebula.voxelidea

data class TileInstance(
    val tileID: Int,
    val data: Int,
    var state: Boolean,
    val arrIndex: Int,
    val index: Int
)

abstract class TileProcessor(val tileID: Int) {
    companion object {
        val processors = mutableListOf<TileProcessor>()
        val airProcessor = AirProcessor()
        val wireProcessor = WireProcessor()
        val inverterProcessor = InverterProcessor()
        val inputProcessor = InputProcessor()
        val outputProcessor = OutputProcessor()

        fun get(id: Int): TileProcessor {
            return processors.firstOrNull { it.tileID == id } ?: error("Could not find tile processor for id $id")
        }
    }

    init {
        processors.add(this)
    }

    abstract fun preprocess(
        simData: SimData,
        me: TileInstance,
        sides: Array<TileInstance>
    ): Boolean

    abstract fun tickTask(
        simData: SimData,
        me: TileInstance,
        sides: Array<TileInstance>
    ): Boolean

    abstract fun checkPass(
        simData: SimData,
        me: TileInstance
    ): Boolean
}

class AirProcessor: TileProcessor(0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return true
    }

    override fun tickTask(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return true
    }

    override fun checkPass(simData: SimData, me: TileInstance): Boolean {
        return true
    }
}

class WireProcessor: TileProcessor(1) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return true
    }

    override fun tickTask(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        sides.forEach { tile ->
            // if tile is not input and its state does not equal my state
            if (tile.tileID != 4 && tile.tileID != 0 && tile.state != me.state) {
                // update tiles state and add a tick task to call its update
                tile.state = me.state
                simData.tickTasks.add(TickTask(tile, me, simData.currentTick))
            }
        }
        return true
    }

    override fun checkPass(simData: SimData, me: TileInstance): Boolean {
        return true
    }
}
class InverterProcessor: TileProcessor(2) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return true
    }
    override fun checkPass(simData: SimData, me: TileInstance): Boolean { return true }

    override fun tickTask(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return true
    }
}
class InputProcessor: TileProcessor(4) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        val truth = simData.template.truths[simData.truth].inputs[me.data]
        if (truth) {
            sides.forEach {
                if (it.tileID == 1) {
                    // set wires state to my truth
                    it.state = truth

                    // call wires tick task
                    simData.tickTasks.add(TickTask(it, me, simData.currentTick))
                }
            }
        }
        return sides.any { it.tileID == 1 }
    }

    override fun tickTask(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return true
    }

    override fun checkPass(simData: SimData, me: TileInstance): Boolean {
        return true
    }
}

class OutputProcessor: TileProcessor(5) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        return sides.any { it.tileID == 1 }
    }

    override fun tickTask(simData: SimData, me: TileInstance, sides: Array<TileInstance>): Boolean {
        //val truth = simData.template.truths[simData.truth].outputs[me.data]

        // only return true if my state equals the truth state
        //return truth == me.state
        if (!simData.midRunCheckPass.contains(me)) simData.midRunCheckPass.add(me)
        return true
    }

    override fun checkPass(simData: SimData, me: TileInstance): Boolean {
        val truth = simData.template.truths[simData.truth].outputs[me.data]
        return truth == me.state
    }
}