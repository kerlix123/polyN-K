sealed interface AST
sealed interface Type: AST

data class BinOp(val left: AST, val op: Token, val right: AST): AST
data class UnaryOp(val op: Token, val expr: AST): AST
data class Num(val token: Token, val value: String? = token.value): AST
data class Bool(val token: Token, val value: String? = token.value): AST
data object Null: AST
data class ListLiteral(val type: ListType, val elements: List<AST>): AST
data class MapElement(val key: AST, val value: AST): AST
data class MapLiteral(val type: MapType, val elements: List<MapElement>): AST
data class SetLiteral(val type: SetType, val elements: List<AST>): AST
data class StringLiteral(val token: Token, val value: String? = token.value): AST
data class StringInterpolation(val parts: MutableList<AST> = mutableListOf<AST>()): AST
data class Var(val token: Token, val value: String? = token.value): AST
data class IndexAccess(val target: AST, val index: AST) : AST
data class MemberAccess(val target: AST, val member: Var) : AST
data class MethodCall(val target: AST, val method: Var, val arguments: List<AST>) : AST
data class FunctionCall(val callee: AST, val arguments: List<AST>) : AST // For standalone calls like fun()

data class ScalarType(val token: Token, val value: String? = token.value): Type
data class ListType(val type: Type): Type
data class MapType(val keyType: Type, val valueType: Type): Type
data class SetType(val type: Type): Type

data class VarDecl(val left: Var, val expr: AST): AST
data class Assign(val left: AST, val op: Token, val right: AST): AST
data class InputStatement(val type: Type, val variable: Var): AST
data class PrintStatement(val token: Token, val expr: AST): AST
data class ExeStatement(val lines: List<String>): AST
data class IfStatement(val expr: AST, val exeStatement: ExeStatement): AST
data class ElifStatement(val expr: AST, val exeStatement: ExeStatement): AST
data class ElseStatement(val exeStatement: ExeStatement): AST
data class WhileStatement(val expr: AST, val exeStatement: ExeStatement): AST
data class ForRangeStatement(val variable: Var, var start: AST, val end: AST, val direction: String, val exeStatement: ExeStatement): AST
data class ForEachStatement(val variable: Var, var list: AST, var exeStatement: ExeStatement): AST
data class FunParameter(val type: Type, val variable: Var): AST
data class FunStatement(val name: Var, val parameters: List<FunParameter>, val returnType: Type, val exeStatement: ExeStatement): AST
data class ReturnStatement(val value: AST): AST
data class ExpressionStatement(val expr: AST) : AST
data object NoOp: AST
data class Statement(val lineName: Token, val statement: AST): AST

class Parser(val tokens: List<Token>) {
    var pos = 0
    var currentToken = tokens[pos]

    private fun error() {
        throw Exception("Invalid syntax")
    }

    private fun eat(tokenType: TokenType) {
        when (currentToken.type) {
            tokenType -> currentToken = tokens[++pos]
            else -> error()
        }
    }

    private fun parsePathSegment(): String = when (currentToken.type) {
        TokenType.ID -> {
            val id = currentToken.value
            eat(TokenType.ID)
            if (currentToken.type == TokenType.DOT) {
                eat(TokenType.DOT)
                "$id."
            } else id!!
        }
        TokenType.DOT -> {
            eat(TokenType.DOT)
            val id = currentToken.value
            eat(TokenType.ID)
            ".$id"
        }
        else -> ""
    }

    private fun getLines(): List<String> {
        val lines = mutableListOf(parsePathSegment())

        while (currentToken.type == TokenType.BITWISE_AND) {
            eat(TokenType.BITWISE_AND)
            lines.add(parsePathSegment())
        }

        return lines
    }

    private fun argumentList(): List<AST> {
        val arguments = mutableListOf(logicalOr())

        while (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA)
            arguments.add(logicalOr())
        }

        return arguments
    }

    private fun funParameter(): FunParameter {
        val type = typeSpec()
        val variable = variable()
        return FunParameter(type, variable)
    }

    private fun parameterList(): List<FunParameter> {
        if (currentToken.type == TokenType.RPAREN)
            return emptyList()
        val parameters = mutableListOf(funParameter())

        while (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA)
            parameters.add(funParameter())
        }

        return parameters
    }

    private fun mapElement(): MapElement {
        val key = logicalOr()
        eat(TokenType.TO)
        val value = logicalOr()
        return MapElement(key, value)
    }

    private fun mapElementList(): List<MapElement> {
        val elements = mutableListOf(mapElement())

        while (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA)
            elements.add(mapElement())
        }

        return elements
    }

    private fun variable(): Var {
        val token = currentToken
        eat(TokenType.ID)
        return Var(token)
    }

    private fun scalarType(): ScalarType {
        val token = currentToken
        if (token.type in setOf(TokenType.INT, TokenType.FLOAT, TokenType.STRING, TokenType.BOOL, TokenType.NULL)) {
            eat(token.type)
        }
        return ScalarType(token)
    }

    private fun listType(): ListType {
        eat(TokenType.LIST)
        eat(TokenType.LBRACKET)
        val type = typeSpec()
        eat(TokenType.RBRACKET)
        return ListType(type)
    }

    private fun mapType(): MapType {
        eat(TokenType.MAP)
        eat(TokenType.LBRACKET)
        val keyType = typeSpec()
        eat(TokenType.COMMA)
        val valueType = typeSpec()
        eat(TokenType.RBRACKET)
        return MapType(keyType, valueType)
    }

    private fun setType(): SetType {
        eat(TokenType.SET)
        eat(TokenType.LBRACKET)
        val type = typeSpec()
        eat(TokenType.RBRACKET)
        return SetType(type)
    }

    private fun typeSpec(): Type {
        return when (currentToken.type) {
            TokenType.LIST -> listType()
            TokenType.MAP -> mapType()
            TokenType.SET -> setType()
            else -> scalarType()
        }
    }

    private fun stringInterpolation(): StringInterpolation {
        val interpolation = StringInterpolation()

        val headToken = currentToken
        eat(TokenType.STRING_HEAD)

        interpolation.parts.add(StringLiteral(headToken))
        interpolation.parts.add(logicalOr())

        while (currentToken.type == TokenType.STRING_MID) {
            val midToken = currentToken
            eat(TokenType.STRING_MID)

            interpolation.parts.add(StringLiteral(midToken))
            interpolation.parts.add(logicalOr())
        }

        val tailToken = currentToken
        eat(TokenType.STRING_TAIL)

        interpolation.parts.add(StringLiteral(tailToken))

        return interpolation
    }

    private fun primary(): AST {
        val token = currentToken
        when (token.type) {
            in setOf(TokenType.INT_CONST, TokenType.FLOAT_CONST) -> {
                eat(token.type)
                return Num(token)
            }
            TokenType.BOOL_CONST -> {
                eat(TokenType.BOOL_CONST)
                return Bool(token)
            }
            TokenType.NULL -> {
                eat(TokenType.NULL)
                return Null
            }
            TokenType.LIST -> {
                val type = listType()
                eat(TokenType.LPAREN)
                var elements = emptyList<AST>()
                if (currentToken.type != TokenType.RPAREN) {
                    elements = argumentList()
                }
                eat(TokenType.RPAREN)
                return ListLiteral(type, elements)
            }
            TokenType.MAP -> {
                val type = mapType()
                eat(TokenType.LPAREN)
                var elements = emptyList<MapElement>()
                if (currentToken.type != TokenType.RPAREN)
                    elements = mapElementList()
                eat(TokenType.RPAREN)
                return MapLiteral(type, elements)
            }
            TokenType.SET -> {
                val type = setType()
                eat(TokenType.LPAREN)
                var elements = emptyList<AST>()
                if (currentToken.type != TokenType.RPAREN) {
                    elements = argumentList()
                }
                eat(TokenType.RPAREN)
                return SetLiteral(type, elements)
            }
            TokenType.STRING_CONST -> {
                eat(TokenType.STRING_CONST)
                return StringLiteral(token)
            }
            TokenType.STRING_HEAD -> {
                return stringInterpolation()
            }
            TokenType.LPAREN -> {
                eat(TokenType.LPAREN)
                val expr = logicalOr()
                eat(TokenType.RPAREN)
                return expr
            }
            else -> {
                return variable()
            }
        }
    }

    private fun factor(): AST {
        var node = primary()

        while (true) {
            when (currentToken.type) {
                // Indexing: arr[0]
                TokenType.LBRACKET -> {
                    eat(TokenType.LBRACKET)
                    val index = logicalOr()
                    eat(TokenType.RBRACKET)
                    node = IndexAccess(node, index)
                }

                // Member access or Method call: obj.prop OR obj.method(...)
                TokenType.DOT -> {
                    eat(TokenType.DOT)
                    val member = variable()

                    if (currentToken.type == TokenType.LPAREN) {
                        eat(TokenType.LPAREN)
                        var args = emptyList<AST>()
                        if (currentToken.type != TokenType.RPAREN) {
                            args = argumentList()
                        }
                        eat(TokenType.RPAREN)
                        node = MethodCall(node, member, args)
                    } else {
                        node = MemberAccess(node, member)
                    }
                }

                // Direct call on variable or expression: fn()
                TokenType.LPAREN -> {
                    eat(TokenType.LPAREN)
                    var args = emptyList<AST>()
                    if (currentToken.type != TokenType.RPAREN) {
                        args = argumentList()
                    }
                    eat(TokenType.RPAREN)
                    node = FunctionCall(node, args)
                }

                else -> break
            }
        }

        return node
    }

    private fun power(): AST {
        val node = factor()
        if (currentToken.type == TokenType.POW) {
            val token = currentToken
            eat(TokenType.POW)
            return BinOp(node, token, unary())
        }
        return node
    }

    private fun unary(): AST {
        val token = currentToken
        if (token.type in setOf(TokenType.PLUS, TokenType.MINUS, TokenType.BITWISE_NOT)) {
            eat(token.type)
            return UnaryOp(token, unary())
        }
        return power()
    }

    private fun term(): AST {
        var node = unary()

        while (currentToken.type in setOf(TokenType.MUL, TokenType.FLOAT_DIV, TokenType.INT_DIV, TokenType.MODULO)) {
            val token = currentToken
            eat(token.type)

            node = BinOp(node, token, unary())
        }

        return node
    }

    private fun expr(): AST {
        var node = term()

        while (currentToken.type in setOf(TokenType.PLUS, TokenType.MINUS)) {
            val token = currentToken
            eat(token.type)

            node = BinOp(node, token, term())
        }

        return node
    }

    private fun shift(): AST {
        var node = expr()

        while (currentToken.type in setOf(TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT)) {
            val token = currentToken
            eat(token.type)

            node = BinOp(node, token, expr())
        }

        return node
    }

    private fun bitwiseAnd(): AST {
        var node = shift()

        while (currentToken.type == TokenType.BITWISE_AND) {
            val token = currentToken
            eat(TokenType.BITWISE_AND)

            node = BinOp(node, token, shift())
        }

        return node
    }

    private fun bitwiseXor(): AST {
        var node = bitwiseAnd()

        while (currentToken.type == TokenType.BITWISE_XOR) {
            val token = currentToken
            eat(TokenType.BITWISE_XOR)

            node = BinOp(node, token, bitwiseAnd())
        }

        return node
    }

    private fun bitwiseOr(): AST {
        var node = bitwiseXor()

        while (currentToken.type == TokenType.BITWISE_OR) {
            val token = currentToken
            eat(TokenType.BITWISE_OR)

            node = BinOp(node, token, bitwiseXor())
        }

        return node
    }

    private fun comparison(): AST {
        var node = bitwiseOr()

        while (currentToken.type in setOf(TokenType.EQ, TokenType.NOT_EQ, TokenType.GT, TokenType.GTE, TokenType.LT, TokenType.LTE, TokenType.IN)) {
            val token = currentToken
            eat(token.type)

            node = BinOp(node, token, bitwiseOr())
        }

        return node
    }

    private fun logicalNot(): AST {
        val token = currentToken
        if (token.type == TokenType.NOT) {
            eat(TokenType.NOT)
            return UnaryOp(token, logicalNot())
        }
        return comparison()
    }

    private fun logicalAnd(): AST {
        var node = logicalNot()

        while (currentToken.type == TokenType.AND) {
            val token = currentToken
            eat(TokenType.AND)

            node = BinOp(node, token, logicalNot())
        }

        return node
    }

    private fun logicalOr(): AST {
        var node = logicalAnd()

        while (currentToken.type == TokenType.OR) {
            val token = currentToken
            eat(TokenType.OR)

            node = BinOp(node, token, logicalAnd())
        }

        return node
    }

    private fun comment() {
        eat(TokenType.QUESTION)

        while (currentToken.type != TokenType.QUESTION) {
            eat(currentToken.type)
        }

        eat(TokenType.QUESTION)
    }

    private fun declarationStatement(): VarDecl {
        eat(TokenType.VAR)
        val left = variable()
        eat(TokenType.ASSIGN)
        val expr = logicalOr()
        return VarDecl(left, expr)
    }

    private val assignmentOps = setOf(
        TokenType.ASSIGN,
        TokenType.PLUS_ASSIGN,
        TokenType.MINUS_ASSIGN,
        TokenType.MUL_ASSIGN,
        TokenType.FLOAT_DIV_ASSIGN,
        TokenType.INT_DIV_ASSIGN,
        TokenType.MODULO_ASSIGN,
        TokenType.POW_ASSIGN,
        TokenType.BITWISE_AND_ASSIGN,
        TokenType.BITWISE_OR_ASSIGN,
        TokenType.BITWISE_XOR_ASSIGN,
        TokenType.LEFT_SHIFT_ASSIGN,
        TokenType.RIGHT_SHIFT_ASSIGN
    )

    private fun expressionOrAssignmentStatement(): AST {
        val expr = logicalOr()

        if (currentToken.type in assignmentOps) {
            val opToken = currentToken
            eat(opToken.type)
            val right = logicalOr()

            if (expr !is Var && expr !is IndexAccess && expr !is MemberAccess) {
                throw Exception("Invalid assignment target")
            }

            return Assign(left = expr, op = opToken, right = right)
        }

        return ExpressionStatement(expr)
    }

    private fun inputStatement(): InputStatement {
        eat(TokenType.INPUT)
        val type = typeSpec()
        val variable = variable()
        return InputStatement(type, variable)
    }

    private fun printStatement(): PrintStatement {
        val token = currentToken

        if (token.type in setOf(TokenType.PRINT, TokenType.PRINTLN)) {
            eat(token.type)
        } else {
            error()
        }

        eat(TokenType.LPAREN)
        val expr = logicalOr()
        eat(TokenType.RPAREN)

        return PrintStatement(token, expr)
    }

    private fun exeStatement(): ExeStatement {
        eat(TokenType.EXE)
        eat(TokenType.LPAREN)

        val lines = getLines()

        eat(TokenType.RPAREN)
        return ExeStatement(lines)
    }

    private fun ifStatement(): IfStatement {
        eat(TokenType.IF)
        eat(TokenType.LPAREN)
        val condition = logicalOr()
        eat(TokenType.RPAREN)

        eat(TokenType.LBRACKET)
        val lines = getLines()
        eat(TokenType.RBRACKET)
        return IfStatement(condition, ExeStatement(lines))
    }

    private fun elifStatement(): ElifStatement {
        eat(TokenType.ELIF)
        eat(TokenType.LPAREN)
        val condition = logicalOr()
        eat(TokenType.RPAREN)

        eat(TokenType.LBRACKET)
        val lines = getLines()
        eat(TokenType.RBRACKET)
        return ElifStatement(condition, ExeStatement(lines))
    }

    private fun elseStatement(): ElseStatement {
        eat(TokenType.ELSE)
        eat(TokenType.LBRACKET)
        val lines = getLines()
        eat(TokenType.RBRACKET)
        return ElseStatement(ExeStatement(lines))
    }

    private fun whileStatement(): WhileStatement {
        eat(TokenType.WHILE)
        eat(TokenType.LPAREN)
        val condition = logicalOr()
        eat(TokenType.RPAREN)

        eat(TokenType.LBRACKET)
        val lines = getLines()
        eat(TokenType.RBRACKET)
        return WhileStatement(condition, ExeStatement(lines))
    }

    private fun forStatement(): AST {
        eat(TokenType.FOR)
        eat(TokenType.LPAREN)
        val variable = variable()

        if (currentToken.type == TokenType.FROM) {
            eat(TokenType.FROM)
            val start = logicalOr()
            var direction = ""
            when (currentToken.type) {
                TokenType.TO -> direction = "ASC"
                TokenType.DOWN_TO -> direction = "DESC"
                else -> error()
            }
            eat(currentToken.type)
            val end = logicalOr()
            eat(TokenType.RPAREN)
            eat(TokenType.LBRACKET)
            val lines = getLines()
            eat(TokenType.RBRACKET)
            return ForRangeStatement(variable, start, end, direction, ExeStatement(lines))
        } else if (currentToken.type == TokenType.IN) {
            eat(TokenType.IN)
            val list = logicalOr()
            eat(TokenType.RPAREN)
            eat(TokenType.LBRACKET)
            val lines = getLines()
            eat(TokenType.RBRACKET)
            return ForEachStatement(variable, list, ExeStatement(lines))
        }
        error()
        return NoOp
    }

    private fun funStatement(): FunStatement {
        eat(TokenType.FUN)
        val name = variable()
        eat(TokenType.LPAREN)
        val parameters = parameterList()
        eat(TokenType.RPAREN)
        eat(TokenType.COLON)
        val returnType = typeSpec()
        eat(TokenType.LBRACKET)
        val lines = getLines()
        eat(TokenType.RBRACKET)
        return FunStatement(name, parameters, returnType, ExeStatement(lines))
    }

    private fun returnStatement(): ReturnStatement {
        eat(TokenType.RETURN)
        val value = logicalOr()
        return ReturnStatement(value)
    }

    private fun empty(): NoOp {
        return NoOp
    }

    private fun statement(): Statement {
        val token = currentToken

        if (token.type in setOf(TokenType.ID, TokenType.MINUS, TokenType.QUESTION)) {
            eat(token.type)
        } else {
            error()
        }

        val statement = when (currentToken.type) {
            TokenType.VAR -> declarationStatement()
            TokenType.INPUT -> inputStatement()
            in setOf(TokenType.PRINT, TokenType.PRINTLN) -> printStatement()
            TokenType.EXE -> exeStatement()
            TokenType.IF -> ifStatement()
            TokenType.ELIF -> elifStatement()
            TokenType.ELSE -> elseStatement()
            TokenType.WHILE -> whileStatement()
            TokenType.FOR -> forStatement()
            TokenType.FUN -> funStatement()
            TokenType.RETURN -> returnStatement()
            else -> expressionOrAssignmentStatement()
        }

        eat(TokenType.SEMI)

        return Statement(token, statement)
    }

    private fun program(): List<Statement> {
        val statements = mutableListOf<Statement>()

        while (currentToken.type != TokenType.EOF) {
            if (currentToken.type == TokenType.QUESTION) {
                comment()
                continue
            }
            statements.add(statement())
        }

        return statements
    }

    fun parse(): List<Statement> {
        return program()
    }
}