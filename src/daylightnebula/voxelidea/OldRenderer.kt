package daylightnebula.voxelidea

import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.random.Random

object OldRenderer {
    fun start() {
        val image = BufferedImage(500, 500, BufferedImage.TYPE_3BYTE_BGR)

        val width = 10
        val height = 10
        val xStep = 500 / (width)
        val yStep = 500 / (height)
        val points = Array<VoxelPoint>((width + 1) * (height + 1)) { index ->
            val x = index % (width + 1)
            val y = index / (width + 1)

            VoxelPoint(
                Vector(
                    xStep * (x) + (if (y % 2 == 0) -(xStep / 4) else xStep / 4),
                    yStep * (y)
                ),
                Color(Random.nextInt())
            )
        }

        val g = image.createGraphics()
        render(g, points)

        ImageIO.write(image, "png", File(System.getProperty("user.dir"), "test.png"))

        println("Done!")
    }

    fun render(g: Graphics, points: Array<VoxelPoint>) {
        val renderStart = System.currentTimeMillis()
        repeat(500) { x ->
            repeat (500) { y ->
                //val currentVec = Vector(x, y)
                val closestPoint = points.sortedBy { (it.pos.x - x) + (it.pos.y - y) }.first()
                g.color = closestPoint.color
                g.drawRect(x,y,1,1)
            }
        }
        val renderEnd = System.currentTimeMillis()
        println("Total render time: ${renderEnd - renderStart} ms")
    }

    class Vector(var x: Int, var y: Int) {
        fun distance(other: Vector): Int {
            return ((other.x - x) + (other.y - y)).absoluteValue
            //return /*sqrt(*/(other.x - x)/*.toFloat().pow(2)*/ + (other.y - y)/*.toFloat().pow(2)*//*)*/
        }
    }

    class VoxelPoint(var pos: Vector, val color: Color)
}

fun main() {
    OldRenderer.start()
}