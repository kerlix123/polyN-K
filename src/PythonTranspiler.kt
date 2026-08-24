class PythonTranspiler(val statements: List<Statement>) {
    private val symbolTable = SymbolTable()

    private fun typeOf(node: AST): Type? {
        return when (node) {
            is Num -> {
                val tokenType = if (node.value?.contains('.') == true) TokenType.FLOAT else TokenType.INT
                ScalarType(Token(tokenType, node.value))
            }
            is Bool -> ScalarType(Token(TokenType.BOOL, "bool"))
            is StringLiteral, is StringInterpolation -> ScalarType(Token(TokenType.STRING, "string"))
            is ListLiteral -> node.type
            is MapLiteral -> node.type
            is SetLiteral -> node.type
            is Var -> symbolTable.lookup(node.value ?: "")
            is BinOp -> typeOf(node.left) // Simplified: assumes operation preserves left operand type
            is FunctionCall -> symbolTable.lookup(visit(node.callee))
            else -> null
        }
    }

    private val lineNames = mutableMapOf<String, AST>()
    private var program = mutableListOf<String>()
    private var indent = 0

    private val types = mapOf(
        TokenType.INT to "int",
        TokenType.FLOAT to "float",
        TokenType.STRING to "string",
        TokenType.BOOL to "bool",
        TokenType.NULL to "None"
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
            is ListLiteral -> return visitList(node)
            is MapElement -> return visitMapElement(node)
            is MapLiteral -> return visitMap(node)
            is SetLiteral -> return visitSet(node)
            is StringLiteral -> return visitString(node)
            is StringInterpolation -> return visitStringInterpolation(node)
            is Var -> return visitVar(node)
            is IndexAccess -> return visitIndexAccess(node)
            is MemberAccess -> return visitMemberAccess(node)
            is MethodCall -> return visitMethodCall(node)
            is FunctionCall -> return visitFunctionCall(node)
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
            is FunParameter -> return visitFunParameter(node)
            is FunStatement -> visitFunStatement(node)
            is ReturnStatement -> return visitReturnStatement(node)
            is ExpressionStatement -> return visitExpressionStatement(node)
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

    fun visitList(node: ListLiteral): String {
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

    fun visitMap(node: MapLiteral): String {
        var result = "{"

        for (i in node.elements.indices) {
            result += visit(node.elements[i])
            if (i != node.elements.size - 1)
                result += ", "
        }

        return "$result}"
    }

    fun visitSet(node: SetLiteral): String {
        var result = "{"

        for (i in node.elements.indices) {
            result += visit(node.elements[i])
            if (i != node.elements.size - 1)
                result += ", "
        }

        return "$result}"
    }

    fun visitString(node: StringLiteral): String {
        return "\"${node.value ?: ""}\""
    }

    fun visitStringInterpolation(node: StringInterpolation): String {
        var string = "f\""
        for (part in node.parts) {
            string += if (part is StringLiteral) {
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

    fun visitIndexAccess(node: IndexAccess): String {
        return "${visit(node.target)}[${visit(node.index)}]"
    }

    fun visitMemberAccess(node: MemberAccess): String {
        val targetType = typeOf(node.target)
        val memberName = node.member.value
        val targetCode = visit(node.target)

        return when (targetType) {
            is ListType -> when (memberName) {
                "size" -> "len($targetCode)"
                else -> "$targetCode.$memberName"
            }
            is SetType -> when (memberName) {
                "size" -> "len($targetCode)"
                else -> "$targetCode.$memberName"
            }
            is MapType -> when (memberName) {
                "size" -> "len($targetCode)"
                "keys" -> "$targetCode.keys()"
                "values" -> "$targetCode.values()"
                else -> "$targetCode.$memberName"
            }
            else -> "$targetCode.$memberName"
        }
    }

    fun visitMethodCall(node: MethodCall): String {
        val targetType = typeOf(node.target)
        val methodName = node.method.value
        val targetCode = visit(node.target)
        val argsCode = node.arguments.joinToString(", ") { visit(it) }

        return when (targetType) {
            is ListType -> when (methodName) {
                "add" -> "$targetCode.append($argsCode)"
                "addAt" -> "$targetCode.insert($argsCode)"
                "addAll" -> "$targetCode.extend($argsCode)"
                "remove" -> "$targetCode.remove($argsCode)"
                "removeAt" -> "$targetCode.pop($argsCode)"
                "clear" -> "$targetCode.clear()"
                "count" -> "$targetCode.count($argsCode)"
                else -> "$targetCode.$methodName($argsCode)"
            }
            is SetType -> when (methodName) {
                "add" -> "$targetCode.add($argsCode)"
                "remove" -> "$targetCode.remove($argsCode)"
                "clear" -> "$targetCode.clear()"
                else -> "$targetCode.$methodName($argsCode)"
            }
            is MapType -> when (methodName) {
                "get" -> "$targetCode.get($argsCode)"
                "remove" -> "$targetCode.pop($argsCode)"
                "clear" -> "$targetCode.clear()"
                else -> "$targetCode.$methodName($argsCode)"
            }
            else -> "$targetCode.$methodName($argsCode)"
        }
    }

    fun visitFunctionCall(node: FunctionCall): String {
        var result = "${visit(node.callee)}("

        for (i in node.arguments.indices) {
            result += visit(node.arguments[i])
            if (i != node.arguments.size - 1)
                result += ", "
        }

        return "$result)"
    }

    fun visitType(node: Type): String {
        return when(node) {
            is ScalarType -> visitScalarType(node)
            is ListType -> visitListType(node)
            is MapType -> visitMapType(node)
            is SetType -> visitSetType(node)
        }
    }

    fun visitScalarType(node: ScalarType): String {
        return types[node.token.type] ?: throw Exception("Unknown type")
    }

    fun visitListType(node: ListType): String {
        return "list[${visit(node.type)}]"
    }

    fun visitMapType(node: MapType): String {
        return "dict[${visit(node.keyType)}, ${visit(node.valueType)}]"
    }

    fun visitSetType(node: SetType): String {
        return "set[${visit(node.type)}]"
    }

    fun visitVarDecl(node: VarDecl): String {
        val type = typeOf(node.expr)
        symbolTable.define(node.left.value!!, type!!)
        return "${node.left.value} = ${visit(node.expr)}"
    }

    fun visitAssign(node: Assign): String {
        val op = assignOperators[node.op.type] ?: throw Exception("Unsupported assign operator: ${node.op.type}")
        return "${visit(node.left)} $op ${visit(node.right)}"
    }

    fun visitPrintStatement(node: PrintStatement): String {
        return "print(${visit(node.expr)})"
    }

    fun visitInputStatement(node: InputStatement): String {
        symbolTable.define(node.variable.value!!, node.type)
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
        repeat(indent) { result += '\t' }
        val start = visit(node.start)
        var end = visit(node.end)
        if (node.direction == "ASC") {
            end += "+ 1"
        } else {
            end += "- 1"
        }
        result += "for ${node.variable.value} in range(${start}, ${end}, ${if (node.direction == "ASC") 1 else -1}):"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitForEachStatement(node: ForEachStatement) {
        var result = ""
        repeat(indent) { result += '\n' }
        result += "for ${node.variable.value} in ${visit(node.list)}:"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--
    }

    fun visitFunParameter(node: FunParameter): String {
        symbolTable.define(node.variable.value!!, node.type)
        return "${visit(node.variable)}: ${visit(node.type)}"
    }

    fun visitFunStatement(node: FunStatement) {
        symbolTable.define(node.name.value!!, node.returnType)
        var result = ""
        repeat(indent) { result += '\t' }
        result += "def ${node.name.value}("

        symbolTable.enterScope()

        for (i in node.parameters.indices) {
            result += visit(node.parameters[i])
            if (i != node.parameters.size - 1)
                result += ", "
        }
        result += ") -> ${visit(node.returnType)}:"
        program.add(result)

        indent++
        executeLines(node.exeStatement.lines)
        indent--

        symbolTable.exitScope()
    }

    fun visitReturnStatement(node: ReturnStatement): String {
        return "return ${visit(node.value)}"
    }

    fun visitExpressionStatement(node: ExpressionStatement): String {
        return visit(node.expr)
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