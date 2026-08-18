import java.io.File
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File = File(".")): String? {
    return runCatching {
        val parts = this.split("\\s+".toRegex())
        val process = ProcessBuilder(parts)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        process.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()
}

fun main() {
    val fileIn = File("/Users/antoniomatijevic/Programiranje/polyN-K/src/test.polyn")
    val text = fileIn.readText()
    val lexer = Lexer(text)
    val tokens = lexer.makeTokens()
    println(tokens)
    val parser = Parser(tokens)
    val tree = parser.parse()
     for (el in tree) {
        println(el)
    }
    val transpiler = PythonTranspiler(tree)
    val program = transpiler.transpile()
    val fileOut = File("/Users/antoniomatijevic/Programiranje/polyN-K/src/test.py")
    var textOut = ""
    for (el in program) {
        textOut += el + '\n'
    }
    fileOut.writeText(textOut)
    val result = "python3 /Users/antoniomatijevic/Programiranje/polyN-K/src/test.py".runCommand()
    println("Program output:\n$result")
}