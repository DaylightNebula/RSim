package daylightnebula.voxelidea

abstract class TileProcessor(val tileID: Int, val expense: Int, val delayAdd: Int) {
    companion object {
        val processors = mutableListOf<TileProcessor>()
        val airProcessor = AirProcessor()
        val wireProcessor = WireProcessor()
        val inverterProcessor = InverterProcessor()
        val inputProcessor = InputProcessor()
        val outputProcessor = OutputProcessor()
        val blockProcessor = BlockProcessor()

        fun get(id: Int): TileProcessor {
            return processors.firstOrNull { it.tileID == id } ?: error("Tile process does not exist for id $id")
        }
    }

    init {
        processors.add(this)
    }

    abstract fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean
    abstract fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>)
    abstract fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean
}
data class TileInstance(
    val tileID: Int,
    val data: Int,
    val arrIndex: Int,
    val index: Int
)

class AirProcessor(): TileProcessor(0, 0, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>) { }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
}

class BlockProcessor: TileProcessor(6, 1, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>) { }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
}

class WireProcessor: TileProcessor(1, 2, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        if ((sides.firstOrNull { it.arrIndex == me.arrIndex + 1 }?.tileID ?: true) != 6)
            return false
        return true
    }

    override fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>) {
        // use sides with extras, loop through each, and if its power state does not equal mine, change it
        val myPowerState = simData.getPowerState(me.arrIndex, me.index) ?: return
        simData.getSidesWithExtras(me.arrIndex, me.index).forEach {
            if ((it.tileID == 1 || it.tileID == 6 || it.tileID == 5) && simData.getPowerState(it.arrIndex, it.index) != myPowerState) {
                simData.setPowerState(it.arrIndex, it.index, myPowerState)
                simData.addTickTask(TickTask(it, me, simData.getTick()))
            }
        }
    }

    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
}

class InverterProcessor: TileProcessor(2, 3, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        sides.forEach {
            if (it.arrIndex + 1 == me.arrIndex && it.tileID != 6) return@forEach
            if ((it.arrIndex - 1 == me.arrIndex || it.arrIndex == me.arrIndex) && it.tileID != 1) return@forEach
            println("Setting pre process inverter state")
            simData.setPowerState(it.arrIndex, it.index, true)
            simData.addTickTask(TickTask(it, me, simData.getTick()))
        }
        return true
    }
    override fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>) {
        val myPowerState = simData.getPowerState(me.arrIndex, me.index) ?: return
        sides.forEach {
            if (it.arrIndex + 1 == me.arrIndex && it.tileID != 6) return@forEach
            if ((it.arrIndex - 1 == me.arrIndex || it.arrIndex == me.arrIndex) && it.tileID != 1) return@forEach
            simData.setPowerState(it.arrIndex, it.index, !myPowerState)
            simData.addTickTask(TickTask(it, me, simData.getTick()))
        }
    }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
}

class InputProcessor: TileProcessor(4, 0, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        // if we have a positive input state, loop for each wire that is level with this tile, and set its state to true
        if (simData.getInputState(me)) {
            var shouldCancel = true // if this stays true, return false since no wires where found
            sides.filter { it.arrIndex == me.arrIndex }.forEach {
                if (it.tileID != 1) return@forEach
                shouldCancel = false
                simData.setPowerState(it.arrIndex, it.index, true)
                simData.addTickTask(
                    TickTask(
                        it,
                        me,
                        0
                    )
                )
            }
            if (shouldCancel) return false
        }
        return true
    }
    override fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>) { }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
}

class OutputProcessor: TileProcessor(5, 0, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun tickTask(simData: SimData, me: TileInstance, sides: List<TileInstance>) {
        simData.addMidRunCheck(me)
    }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        val myOutputState = simData.getOutputState(me)
        val myPowerState = simData.getPowerState(me.arrIndex, me.index)
        return myOutputState == myPowerState
    }
}