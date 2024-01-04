package rust;

enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, SEMICOLON,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    PLUS, PLUS_EQUAL,
    MINUS, MINUS_EQUAL,
    STAR, STAR_EQUAL,
    SLASH, SLASH_EQUAL,
    MODULO, MODULO_EQUAL,

    // Literals.
    IDENTIFIER, STRING, INTEGER, FLOAT,

    // Keywords.
    AND, OR, ELSE, FALSE, FN, FOR, IN, DOT_DOT, IF,
    PRINTLN , PRINT, RETURN, TRUE, LET, WHILE, LOOP, MUT, BREAK,

    EOF
}