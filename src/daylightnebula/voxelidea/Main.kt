package daylightnebula.voxelidea

import java.io.File

data class Template(val tiles: Array<Array<TileInstance>>, val truths: List<Truth>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Template

        if (!tiles.contentDeepEquals(other.tiles)) return false
        if (truths != other.truths) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tiles.contentDeepHashCode()
        result = 31 * result + truths.hashCode()
        return result
    }
}

data class Truth(val inputs: List<Boolean>, val outputs: List<Boolean>)

enum class LoaderState {
    NONE,
    TEMPLATE,
    TRUTHS
}

fun loadTemplate(file: File): Template {
    // get file lines
    val lines = file.readLines()

    // setup
    var state = LoaderState.NONE
    val currentTemplate = mutableListOf<Array<TileInstance>>()
    val currentTruths = mutableListOf<Truth>()

    // loop through each line serially
    for (index in lines.indices) {
        val line = lines[index]

        // skip blank lines or comments
        if (line.isBlank() || line.startsWith("#")) continue

        // if '===', change loader state
        if (line.startsWith("===")) {
            if (line.contains("template", true))
                state = LoaderState.TEMPLATE
            else if (line.contains("truth table", true))
                state = LoaderState.TRUTHS
        }
        // otherwise, load according to state
        else {
            when(state) {
                LoaderState.TEMPLATE -> {
                    val tokens = line.split(" ")
                    // yes I know this is a mess, just go with it
                    currentTemplate.add(
                        tokens.mapIndexed { index, it ->
                            TileInstance(it.first().digitToInt(), it.last().digitToInt(), currentTemplate.size, index)
                        }.toTypedArray()
                    )
                }
                LoaderState.TRUTHS -> {
                    val inAndOut = line.split("=")
                    val inTokens = inAndOut.first().split(" ").filter { !it.isBlank() }
                    val outTokens = inAndOut.last().split(" ").filter { !it.isBlank() }
                    currentTruths.add(
                        Truth(
                            inTokens.map { it != "0" },
                            outTokens.map { it != "0" }
                        )
                    )
                }
                else -> { println("Stateless line, index $index line $line") }
            }
        }
    }

    return Template(
        currentTemplate.toTypedArray(),
        currentTruths
    )
}

fun main() {
    val templateFile = File(System.getProperty("user.dir"), "res/test_design.rstemplate")
    val template = loadTemplate(templateFile)
    val result = Simulator.simulate(template, true)
    if (result)
        println("Passed! :)")
    else
        println("Failed! :(")
}