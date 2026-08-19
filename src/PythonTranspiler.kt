class PythonTranspiler(val statements: List<Statement>) {
    val lineNames = mutableMapOf<String, AST>()
    var program = mutableListOf<String>()
    var indent = 0

    private val types = mapOf(
        TokenType.INT to "int",
        TokenType.FLOAT to "float",
        TokenType.STRING to "string",
        TokenType.BOOL to "bool"
    )

    private val binaryOperators = mapOf(
        TokenType.PLUS to "+",
        TokenType.MINUS to "-",
        TokenType.MUL to "*",
        TokenType.FLOAT_DIV to "/",
        TokenType.INT_DIV to "//",
        TokenType.MODULO to "%",
        TokenType.POW to "**",
        TokenType.LEFT_SHIFT to "<<",
        TokenType.RIGHT_SHIFT to ">>",
        TokenType.BITWISE_AND to "&",
        TokenType.BITWISE_OR to "|",
        TokenType.BITWISE_XOR to "^",
        TokenType.EQ to "==",
        TokenType.NOT_EQ to "!=",
        TokenType.GT to ">",
        TokenType.GTE to ">=",
        TokenType.LT to "<",
        TokenType.LTE to "<=",
        TokenType.AND to "and",
        TokenType.OR to "or",
        TokenType.IN to "in"
    )

    private val assignOperators = mapOf(
        TokenType.ASSIGN to "=",
        TokenType.PLUS_ASSIGN to "+=",
        TokenType.MINUS_ASSIGN to "-=",
        TokenType.MUL_ASSIGN to "*=",
        TokenType.FLOAT_DIV_ASSIGN to "/=",
        TokenType.INT_DIV_ASSIGN to "//=",
        TokenType.MODULO_ASSIGN to "%=",
        TokenType.POW_ASSIGN to "**=",
        TokenType.BITWISE_AND_ASSIGN to "&=",
        TokenType.BITWISE_OR_ASSIGN to "|=",
        TokenType.BITWISE_XOR_ASSIGN to "^=",
        TokenType.LEFT_SHIFT_ASSIGN to "<<=",
        TokenType.RIGHT_SHIFT_ASSIGN to ">>="
    )

    private val unaryOperators = mapOf(
        TokenType.PLUS to "+",
        TokenType.MINUS to "-",
        TokenType.NOT to "not ",
        TokenType.BITWISE_NOT to "~"
    )

    fun visit(node: AST): String {
        when (node) {
            is BinOp -> return visitBinOp(node)
            is UnaryOp -> return visitUnaryOp(node)
            is Num -> return visitNum(node)
            is Bool -> return visitBool(node)
            is Null -> return visitNull()
            is List_ -> return visitList(node)
            is MapElement -> return visitMapElement(node)
            is Map_ -> return visitMap(node)
            is Set_ -> return visitSet(node)
            is String_ -> return visitString(node)
            is StringInterpolation -> return visitStringInterpolation(node)
            is Var -> return visitVar(node)
            is Type -> return visitType(node)
            is VarDecl -> return  visitVarDecl(node)
            is Assign -> return visitAssign(node)
            is PrintStatement -> return visitPrintStatement(node)
            is InputStatement -> return visitInputStatement(node)
            is NoOp -> return visitNoOp()
            is Statement -> return visitStatement(node)
            is ExeStatement -> visitExeStatement(node)
            is IfStatement -> visitIfStatement(node)
            is ElifStatement -> visitElifStatement(node)
            is ElseStatement -> visitElseStatement(node)
            is WhileStatement -> visitWhileStatement(node)
            is ForRangeStatement -> visitForRangeStatement(node)
            is ForEachStatement -> visitForEachStatement(node)
        }
        return ""
    }

    fun visitBinOp(node: BinOp): String {
        val op = binaryOperators[node.op.type] ?: throw Exception("Unsupported binary operator: ${node.op.type}")
        return "(${visit(node.left)} $op ${visit(node.right)})"
    }

    fun visitUnaryOp(node: UnaryOp): String {
        val op = unaryOperators[node.op.type] ?: throw Exception("Unsupported unary operator: ${node.op.type}")
        return "(${op}${visit(node.expr)})"
    }

    fun visitNum(node: Num): String {
        return node.value ?: ""
    }

    fun visitBool(node: Bool): String {
        return when (node.value) {
            "true" -> "True"
            "false" -> "False"
            else -> ""
        }
    }

    fun visitNull(): String = "None"

    fun visitList(node: List_): String {
        var result = "["

        for (i in node.elements.indices) {
            result += visit(node.elements[i])
            if (i != node.elements.size - 1)
                result += ", "
        }

        return "$result]"
    }

    fun visitMapElement(node: MapElement): String {
        return "${visit(node.key)}: ${visit(node.value)}"
    }

    fun visitMap(node: Map_): String {
        var result = "{"

        for (i in node.elements.indices) {
            result += visit(node.elements[i])
            if (i != node.elements.size - 1)
                result += ", "
        }

        return "$result}"
    }

    fun visitSet(node: Set_): String {
        var result = "{"

        for (i in node.elements.indices) {
            result += visit(node.elements[i])
            if (i != node.elements.size - 1)
                result += ", "
        }

        return "$result}"
    }

    fun visitString(node: String_): String {
        return "\"${node.value ?: ""}\""
    }

    fun visitStringInterpolation(node: StringInterpolation): String {
        var string = "f\""
        for (part in node.parts) {
            string += if (part is String_) {
                part.value
            } else {
                '{' + visit(part) + '}'
            }
        }
        string += '\"'
        return string
    }

    fun visitVar(node: Var): String {
        return node.value ?: ""
    }

    fun visitType(node: Type): String {
        return types[node.token.type] ?: throw Exception("Unknown type")
    }

    fun visitVarDecl(node: VarDecl): String {
        return "${node.left.value} = ${visit(node.expr)}"
    }

    fun visitAssign(node: Assign): String {
        val op = assignOperators[node.op.type] ?: throw Exception("Unsupported assign operator: ${node.op.type}")
        return "${node.left.value} $op ${visit(node.right)}"
    }

    fun visitPrintStatement(node: PrintStatement): String {
        return "print(${visit(node.expr)})"
    }

    fun visitInputStatement(node: InputStatement): String {
        return "${visit(node.variable)} = ${visit(node.type)}(input())"
    }

    fun visitExeStatement(node: ExeStatement)  {
        executeLines(node.lines)
    }

    fun visitIfStatement(node: IfStatement) {
        var result = ""
        repeat(indent) { result += '\t' }
        result += "if ${visit(node.expr)}:"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitElifStatement(node: ElifStatement) {
        var result = ""
        repeat(indent) { result += '\t' }
        result += "elif ${visit(node.expr)}:"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitElseStatement(node: ElseStatement) {
        var result = ""
        repeat(indent) { result += '\t' }
        result += "else :"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitWhileStatement(node: WhileStatement) {
        var result = ""
        repeat(indent) { result += '\t' }
        result += "while ${visit(node.expr)}:"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitForRangeStatement(node: ForRangeStatement) {
        var result = ""
        repeat(indent) { result += '\n' }
        val start = visit(node.start)
        var end = visit(node.end).toInt()
        if (node.direction == "ASC") {
            end++
        } else {
            end--
        }
        result += "for ${node.variable.value} in range(${start}, ${end}, ${if (node.direction == "ASC") 1 else -1}):"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitForEachStatement(node: ForEachStatement) {
        println("nig")
    }

    fun visitNoOp(): String {
        return ""
    }

    fun visitStatement(node: Statement, execute: Boolean=false): String {
        if (node.lineName.type == TokenType.MINUS || (node.lineName.type == TokenType.ID && execute)) {
            return visit(node.statement)
        }
        return ""
    }

    fun executeLines(lines: List<String>) {
        for (line in lines) {
            var result = ""
            repeat(indent) { result += '\t' }
            if (line.first() == '.') {
                val suffix = line.subSequence(1..<line.length)

                for (key in lineNames.keys) {
                    if (key.endsWith(suffix)) {
                        result += visit(lineNames[key] ?: throw Exception("No line named: $key"))
                        if (result != "")
                            program.add(result)
                        result = ""
                        repeat(indent) { result += '\t' }
                    }
                }
            } else if (line.last() == '.') {
                val prefix = line.subSequence(0..<line.length-1)

                for (key in lineNames.keys) {
                    if (key.startsWith(prefix)) {
                        result += visit(lineNames[key] ?: throw Exception("No line named: $key"))
                        if (result != "")
                            program.add(result)
                        result = ""
                        repeat(indent) { result += '\t' }
                    }
                }
            } else {
                result += visit(lineNames[line] ?: throw Exception("No line named: $line"))
                if (result != "")
                    program.add(result)
                result = ""
                repeat(indent) { result += '\t' }
            }
        }
    }

    fun transpile(): List<String> {
        for ((lineName, statement1) in statements) {
            if (lineName.type == TokenType.ID) {
                val name = lineName.value!!
                lineNames[name] = statement1
            }
        }

        for (statement in statements) {
            val result = visitStatement(statement)
            if (result != "")
                program.add(result)
        }
        return program
    }
}