package daylightnebula.voxelidea

import java.awt.Color
import java.awt.Graphics
import kotlin.math.absoluteValue

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
    abstract fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>)
    abstract fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean
    abstract fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int)
}
data class TileInstance(
    val tileID: Int,
    val data: Int,
    val arrIndex: Int,
    val index: Int
)

class AirProcessor(): TileProcessor(0, 0, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>) { }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int) { }
}

class BlockProcessor: TileProcessor(6, 1, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>) {
        val myPowerState = simData.getPowerState(me.arrIndex, me.index) ?: return
        sides.forEach {  tile ->
            if (tile.tileID == 2) {
                if (me.arrIndex + 1 == tile.tileID) return@forEach
                simData.setPowerState(tile.arrIndex, tile.index, myPowerState)
                simData.addTickTask(TickTask(tile, me, simData.getTick()))
            }
        }
    }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color.yellow
        g.fillRect(x, y, width, height)
    }
}

class WireProcessor: TileProcessor(1, 2, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        if ((sides.firstOrNull { it.arrIndex == me.arrIndex + 1 }?.tileID ?: true) != 6)
            return false
        return true
    }

    override fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>) {
        // use sides with extras, loop through each, and if its power state does not equal mine, change it
        val myPowerState = simData.getPowerState(me.arrIndex, me.index) ?: return
        simData.getSidesWithExtras(me.arrIndex, me.index).forEach {
            if (
                (it.tileID == 1 || it.tileID == 6 || it.tileID == 5)
                && simData.getPowerState(it.arrIndex, it.index) != myPowerState
                && it != tickedBy
            ) {
                if ((me.arrIndex - it.arrIndex).absoluteValue + (me.index - it.arrIndex).absoluteValue > 1 && it.tileID != 1) return@forEach
                simData.setPowerState(it.arrIndex, it.index, myPowerState)
                simData.addTickTask(TickTask(it, me, simData.getTick()))
            }
        }
    }

    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }

    override fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color.RED
        val widthSegment = (width * 0.375).toInt()
        val heightSegment = (height * 0.375).toInt()
        g.fillRect(x + widthSegment, y, width / 4, height)
        g.fillRect(x, y + heightSegment, width, height / 4)
    }
}

class InverterProcessor: TileProcessor(2, 3, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        sides.forEach {
            if (it.arrIndex + 1 == me.arrIndex && it.tileID != 6) return@forEach
            if ((it.arrIndex - 1 == me.arrIndex || it.arrIndex == me.arrIndex) && it.tileID != 1) return@forEach
            simData.setPowerState(it.arrIndex, it.index, true)
            simData.addTickTask(TickTask(it, me, simData.getTick()))
        }
        return true
    }
    override fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>) {
        val myPowerState = simData.getPowerState(me.arrIndex, me.index) ?: return
        sides.forEach {
            if (it.tileID != 1 && it.tileID != 6) return@forEach
            if (it.tileID == 6 && it.arrIndex >= me.arrIndex) return@forEach
            simData.setPowerState(it.arrIndex, it.index, !myPowerState)
            simData.addTickTask(TickTask(it, me, simData.getTick()))
        }
    }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }

    override fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color.RED
        val widthSegment = (width * 0.375).toInt()
        val heightSegment = (height * 0.375).toInt()
        g.fillOval(x + widthSegment, y + heightSegment, width / 4, height / 4)
    }
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
    override fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>) { }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color.BLUE
        g.fillRect(x, y, width, height)
    }
}

class OutputProcessor: TileProcessor(5, 0, 0) {
    override fun preprocess(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean { return true }
    override fun tickTask(simData: SimData, me: TileInstance, tickedBy: TileInstance, sides: List<TileInstance>) {
        simData.addMidRunCheck(me)
    }
    override fun checkPass(simData: SimData, me: TileInstance, sides: List<TileInstance>): Boolean {
        val myOutputState = simData.getOutputState(me)
        val myPowerState = simData.getPowerState(me.arrIndex, me.index)
        return myOutputState == myPowerState
    }
    override fun draw(g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color.CYAN
        g.fillRect(x, y, width, height)
    }
}