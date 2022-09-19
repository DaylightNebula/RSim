package daylightnebula.voxelidea

data class TileInstance(
    val tileID: Int,
    val data: Int,
    var state: Boolean
)

abstract class TileProcessor(val tileID: Int, ) {
    companion object {
        val processors = mutableListOf<TileProcessor>()
    }

    init {
        processors.add(this)
    }

    abstract fun process(
        me: TileInstance,
        north: TileInstance,
        south: TileInstance,
        west: TileInstance,
        east: TileInstance,
        above: TileInstance,
        below: TileInstance
    )
}