sealed interface AST

data class BinOp(val left: AST, val op: Token, val right: AST): AST
data class UnaryOp(val op: Token, val expr: AST): AST
data class Num(val token: Token, val value: String? = token.value): AST
data class String_(val token: Token, val value: String? = token.value): AST
data class StringInterpolation(val parts: MutableList<AST> = mutableListOf<AST>()): AST
data class Var(val token: Token, val value: String? = token.value): AST
data class Type(val token: Token, val value: String? = token.value): AST
data class VarDecl(val left: Var, val expr: AST): AST
data class Assign(val left: Var, val op: Token, val right: AST): AST
data class InputStatement(val type: Type, val variable: Var): AST
data class PrintStatement(val token: Token, val expr: AST): AST
data class ExeStatement(val lines: List<String>): AST
data class IfStatement(val expr: AST, val exeStatement: ExeStatement): AST
data class ElifStatement(val expr: AST, val exeStatement: ExeStatement): AST
data class ElseStatement(val exeStatement: ExeStatement): AST
data class WhileStatement(val expr: AST, val exeStatement: ExeStatement): AST
data class ForRangeStatement(val variable: Var, var start: AST, val end: AST, val direction: String, val exeStatement: ExeStatement): AST
data class ForEachStatement(val variable: Var, var list: AST, var exeStatement: ExeStatement): AST
data object NoOp : AST
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

    private fun variable(): Var {
        val token = currentToken
        eat(TokenType.ID)
        return Var(token)
    }

    private fun typeSpec(): Type {
        val token = currentToken
        if (token.type in setOf(TokenType.INT, TokenType.FLOAT, TokenType.STRING, TokenType.BOOL)) {
            eat(token.type)
        }
        return Type(token)
    }

    private fun stringInterpolation(): StringInterpolation {
        val interpolation = StringInterpolation()

        val headToken = currentToken
        eat(TokenType.STRING_HEAD)

        interpolation.parts.add(String_(headToken))
        interpolation.parts.add(logicalOr())

        while (currentToken.type == TokenType.STRING_MID) {
            val midToken = currentToken
            eat(TokenType.STRING_MID)

            interpolation.parts.add(String_(midToken))
            interpolation.parts.add(logicalOr())
        }

        val tailToken = currentToken
        eat(TokenType.STRING_TAIL)

        interpolation.parts.add(String_(tailToken))

        return interpolations
    }

    private fun factor(): AST {
        val token = currentToken
        when (token.type) {
            in setOf(TokenType.INT_CONST, TokenType.FLOAT_CONST) -> {
                eat(token.type)
                return Num(token)
            }
            TokenType.STRING_CONST -> {
                eat(TokenType.STRING_CONST)
                return String_(token)
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

    private fun assignmentStatement(): Assign {
        val left = variable()
        val token = currentToken

        if (token.type in setOf(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.MUL_ASSIGN, TokenType.FLOAT_DIV_ASSIGN, TokenType.INT_DIV_ASSIGN, TokenType.MODULO_ASSIGN, TokenType.POW_ASSIGN, TokenType.BITWISE_AND_ASSIGN, TokenType.BITWISE_OR_ASSIGN, TokenType.BITWISE_XOR_ASSIGN, TokenType.LEFT_SHIFT_ASSIGN, TokenType.RIGHT_SHIFT_ASSIGN)) {
            eat(token.type)
        } else {
            error()
        }

        return Assign(left, token, logicalOr())
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
            //TODO - logicalOr?
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
            TokenType.ID -> assignmentStatement()
            TokenType.INPUT -> inputStatement()
            in setOf(TokenType.PRINT, TokenType.PRINTLN) -> printStatement()
            TokenType.EXE -> exeStatement()
            TokenType.IF -> ifStatement()
            TokenType.ELIF -> elifStatement()
            TokenType.ELSE -> elseStatement()
            TokenType.WHILE -> whileStatement()
            TokenType.FOR -> forStatement()
            else -> empty()
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