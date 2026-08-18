class Lexer(private val text: String) {
    private var pos = 0
    private var currentChar: Char? = text[pos]

    private var RESERVED_KEYWORDS = mapOf(
        "int" to Token(TokenType.INT),
        "float" to Token(TokenType.FLOAT),
        "string" to Token(TokenType.STRING),
        "bool" to Token(TokenType.BOOL),
        "list" to Token(TokenType.LIST),
        "map" to Token(TokenType.MAP),
        "set" to Token(TokenType.SET),
        "null" to Token(TokenType.NULL),
        "var" to Token(TokenType.VAR),
        "true" to Token(TokenType.BOOL_CONST, "true"),
        "false" to Token(TokenType.BOOL_CONST, "false"),
        "or" to Token(TokenType.OR),
        "and" to Token(TokenType.AND),
        "not" to Token(TokenType.NOT),
        "exe" to Token(TokenType.EXE),
        "print" to Token(TokenType.PRINT),
        "println" to Token(TokenType.PRINTLN),
        "input" to Token(TokenType.INPUT),
        "if" to Token(TokenType.IF),
        "elif" to Token(TokenType.ELIF),
        "else" to Token(TokenType.ELSE),
        "for" to Token(TokenType.FOR),
        "in" to Token(TokenType.IN),
        "from" to Token(TokenType.FROM),
        "to" to Token(TokenType.TO),
        "downTo" to Token(TokenType.DOWN_TO),
        "while" to Token(TokenType.WHILE),
        "break" to Token(TokenType.BREAK),
        "continue" to Token(TokenType.CONTINUE),
        "class" to Token(TokenType.CLASS),
        "fun" to Token(TokenType.FUN),
        "return" to Token(TokenType.RETURN)
    )

    private fun error(msg: String = "Invalid character: $currentChar"): Nothing {
        throw Exception(msg)
    }

    private fun advance(amount: Int = 1) {
        pos += amount
        currentChar = when {
            pos >= text.length -> null
            else -> text[pos]
        }
    }

    private fun peek(amount: Int = 1): Char? {
        val peekPos = pos + amount
        return when {
            peekPos >= text.length -> null
            else -> text[peekPos]
        }
    }

    private fun match(char: Char): Boolean {
        if (currentChar == char) {
            advance()
            return true
        }
        return false
    }

    private fun skipWhitespace() {
        while (currentChar != null && currentChar!!.isWhitespace())
            advance()
    }

    private fun makeNumber(): Token {
        var number = ""

        while (currentChar != null && currentChar!!.isDigit()) {
            number += currentChar
            advance()
        }

        if (currentChar == '.') {
            number += '.'
            advance()

            while (currentChar != null && currentChar!!.isDigit()) {
                number += currentChar
                advance()
            }

            return Token(TokenType.FLOAT_CONST, number)
        }

        return Token(TokenType.INT_CONST, number)
    }

    private fun makeKeywordOrID(): Token {
        var identifier = ""

        while (currentChar != null && (currentChar!!.isLetterOrDigit() || currentChar == '_')) {
            identifier += currentChar
            advance()
        }

        return RESERVED_KEYWORDS.getOrDefault(identifier, Token(TokenType.ID, identifier))
    }

    private fun makeString(): List<Token> {
        val tokens = mutableListOf<Token>()
        advance() // Consume opening quote '"'

        var hasInterpolation = false
        val currentText = StringBuilder()

        while (currentChar != null && currentChar != '"') {
            if (currentChar == '\\') {
                advance()
                when (currentChar) {
                    'n' -> currentText.append('\n')
                    't' -> currentText.append('\t')
                    'r' -> currentText.append('\r')
                    '\\' -> currentText.append('\\')
                    '"' -> currentText.append('"')
                    '$' -> currentText.append('$')
                    else -> {
                        currentText.append('\\')
                        if (currentChar != null) currentText.append(currentChar)
                    }
                }
                advance()
            } else if (currentChar == '$' && peek() == '{') {
                advance(2) // Consume '${'

                val textVal = currentText.toString()
                currentText.clear()

                if (!hasInterpolation) {
                    tokens.add(Token(TokenType.STRING_HEAD, textVal))
                    hasInterpolation = true
                } else {
                    tokens.add(Token(TokenType.STRING_MID, textVal))
                }

                // Tokenize expressions inside ${ ... }
                var braceDepth = 1
                while (currentChar != null && braceDepth > 0) {
                    if (currentChar!!.isWhitespace()) {
                        skipWhitespace()
                        continue
                    }

                    if (currentChar == '{') {
                        braceDepth++
                        advance()
                        tokens.add(Token(TokenType.LBRACKET))
                    } else if (currentChar == '}') {
                        braceDepth--
                        if (braceDepth == 0) {
                            advance() // Consume closing '}' of interpolation
                            break
                        } else {
                            advance()
                            tokens.add(Token(TokenType.RBRACKET))
                        }
                    } else {
                        tokens.addAll(makeNextToken())
                    }
                }
            } else {
                currentText.append(currentChar)
                advance()
            }
        }

        if (currentChar == null) {
            error("Unterminated string literal")
        }

        advance() // Consume closing quote '"'

        val finalVal = currentText.toString()
        if (hasInterpolation) {
            tokens.add(Token(TokenType.STRING_TAIL, finalVal))
        } else {
            tokens.add(Token(TokenType.STRING_CONST, finalVal))
        }

        return tokens
    }

    private fun makeNextToken(): List<Token> {
        val tokens = mutableListOf<Token>()

        if (currentChar == null) return tokens

        if (currentChar!!.isDigit()) {
            tokens.add(makeNumber())
        } else if (currentChar!!.isLetter() || currentChar == '_') {
            tokens.add(makeKeywordOrID())
        } else if (currentChar == '"') {
            tokens.addAll(makeString())
        } else if (currentChar == '+') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.PLUS_ASSIGN else TokenType.PLUS))
        } else if (currentChar == '-') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.MINUS_ASSIGN else TokenType.MINUS))
        } else if (currentChar == '*') {
            advance()
            if (match('*')) {
                tokens.add(Token(if (match('=')) TokenType.POW_ASSIGN else TokenType.POW))
            } else {
                tokens.add(Token(if (match('=')) TokenType.MUL_ASSIGN else TokenType.MUL))
            }
        } else if (currentChar == '/') {
            advance()
            if (match('/')) {
                tokens.add(Token(if (match('=')) TokenType.INT_DIV_ASSIGN else TokenType.INT_DIV))
            } else {
                tokens.add(Token(if (match('=')) TokenType.FLOAT_DIV_ASSIGN else TokenType.FLOAT_DIV))
            }
        } else if (currentChar == '%') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.MODULO_ASSIGN else TokenType.MODULO))
        } else if (currentChar == '~') {
            advance()
            tokens.add(Token(TokenType.BITWISE_NOT))
        } else if (currentChar == '&') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.BITWISE_AND_ASSIGN else TokenType.BITWISE_AND))
        } else if (currentChar == '|') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.BITWISE_OR_ASSIGN else TokenType.BITWISE_OR))
        } else if (currentChar == '^') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.BITWISE_XOR_ASSIGN else TokenType.BITWISE_XOR))
        } else if (currentChar == '=') {
            advance()
            tokens.add(Token(if (match('=')) TokenType.EQ else TokenType.ASSIGN))
        } else if (currentChar == '!' && peek() == '=') {
            advance(2)
            tokens.add(Token(TokenType.NOT_EQ))
        } else if (currentChar == '!') {
            advance()
            tokens.add(Token(TokenType.NOT))
        } else if (currentChar == '<') {
            advance()
            if (match('<')) {
                tokens.add(Token(if (match('=')) TokenType.LEFT_SHIFT_ASSIGN else TokenType.LEFT_SHIFT))
            } else {
                tokens.add(Token(if (match('=')) TokenType.LTE else TokenType.LT))
            }
        } else if (currentChar == '>') {
            advance()
            if (match('>')) {
                tokens.add(Token(if (match('=')) TokenType.RIGHT_SHIFT_ASSIGN else TokenType.RIGHT_SHIFT))
            } else {
                tokens.add(Token(if (match('=')) TokenType.GTE else TokenType.GT))
            }
        } else if (currentChar == '(') {
            advance()
            tokens.add(Token(TokenType.LPAREN))
        } else if (currentChar == ')') {
            advance()
            tokens.add(Token(TokenType.RPAREN))
        } else if (currentChar == '[') {
            advance()
            tokens.add(Token(TokenType.LBRACKET))
        } else if (currentChar == ']') {
            advance()
            tokens.add(Token(TokenType.RBRACKET))
        } else if (currentChar == '{') {
            advance()
            tokens.add(Token(TokenType.LBRACKET))
        } else if (currentChar == '}') {
            advance()
            tokens.add(Token(TokenType.RBRACKET))
        } else if (currentChar == '.') {
            advance()
            tokens.add(Token(TokenType.DOT))
        } else if (currentChar == ':') {
            advance()
            tokens.add(Token(TokenType.COLON))
        } else if (currentChar == ',') {
            advance()
            tokens.add(Token(TokenType.COMMA))
        } else if (currentChar == ';') {
            advance()
            tokens.add(Token(TokenType.SEMI))
        } else if (currentChar == '?') {
            advance()
            tokens.add(Token(TokenType.QUESTION))
        } else {
            error()
        }

        return tokens
    }

    fun makeTokens(): List<Token> {
        val tokens = mutableListOf<Token>()

        while (currentChar != null) {
            if (currentChar!!.isWhitespace()) {
                skipWhitespace()
            } else {
                tokens.addAll(makeNextToken())
            }
        }

        tokens.add(Token(TokenType.EOF))

        return tokens
    }
}