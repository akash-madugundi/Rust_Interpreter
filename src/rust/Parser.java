package rust;

import java.util.*;

import static rust.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;
    int a;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    //    Expr parse() {
//        try {
//            return expression();
//        } catch (ParseError error) {
//            return null;
//        }
//    }
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }
    private Expr expression() {
        return assignment();
    }
    private Expr expression2() {
        return assign2();
    }
    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(PRINTLN)) return printLnStatement();
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(WHILE)) return whileStatement();
        if (match(LOOP)) return loopStatement();
        if(match(BREAK)) return breakStatement();
        if (match(FOR)) return forStatement();

        return expressionStatement();
    }
//    private Stmt forStatement() {
////        Token loopVariable = consume(IDENTIFIER, "Expect loop variable after 'for'");
////        consume(IN, "Expect 'in' after loop variable.");
////        Expr startExpr = primary2();
////        consume(DOT_DOT, "Expect '..' after the start of the range.");
////        Expr endExpr = primary2();
////
////        Stmt body = statement();
////
////        // Create a variable declaration for the loop variable
////        Stmt initializer = new Stmt.Let(loopVariable, startExpr);
////
////        // Create a condition using the loop variable and the upper limit of the range
////        Expr condition = new Expr.Binary(
////                new Expr.Variable(loopVariable),
////                new Token(LESS_EQUAL, "<=", null, 1),
////                endExpr
////        );
////
////        // Create a while loop with the condition and body
////        body = new Stmt.While(condition, body);
////
////        // Create an increment expression to increment the loop variable
////        Expr increment = new Expr.Assign2(
////                new Expr.Variable(loopVariable),
////                new Expr.Binary(
////                        new Expr.Variable(loopVariable),
////                        new Token(PLUS, "+", null, 1),
////                        new Expr.Literal(1)
////                )
////        );
////
////
////        // Combine initializer, while loop, and increment into a block
////        body = new Stmt.Block(Arrays.asList(initializer, body, new Stmt.Expression(increment)));
////
////        return body;
//    }
    Interpreter Inter=new Interpreter();
    private Stmt forStatement() {
        Stmt initial;

        initial = varDeclaration(1);
//            match(IN);
        Object condition=1 ;
//        if(match(INTEGER)) {
        condition =previous().literal;
        match(DOT_DOT);
//        }
        Object increment=1 ;
        if(match(INTEGER)) increment = previous().literal;

        Stmt body = statement();
        match(LEFT_BRACE);
        if (initial != null) {
            body = new Stmt.Block(Arrays.asList(initial, body));
        }
//        match(RIGHT_BRACE);
        for(int xx=(int)condition;xx<(int)increment-1;xx++)
        {
            Token name;
            name= Stmt.Let.name;
//            Inter.timepass((Stmt.Let) initial,xx);
//            Environment e=new Environment();
//            e.define(Stmt.Let.name.lexeme, xx);
            Inter.execute(body);
        }
        return body;
    }

    private Stmt ifStatement() {
        Expr condition = expression();

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt declaration() {
        try {
            if (match(FN)) return mainFunction();
            if (match(LET)){
                a=0;
                return varDeclaration(a);
            }
            if (match(MUT)){
                a=1;
                return mutDeclaration();
            }

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt printStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'print!'.");
        List<Expr> expressions = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                expressions.add(expression());
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after expressions.");
        consume(SEMICOLON, "Expect ';' after expressions.");

        return new Stmt.Print((ArrayList<Expr>) expressions);
    }
    private Stmt printLnStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'print!'.");
        List<Expr> expressions = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                expressions.add(expression());
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after expressions.");
        consume(SEMICOLON, "Expect ';' after expressions.");

        return new Stmt.PrintLn((ArrayList<Expr>) expressions);
    }
//    private Stmt varDeclaration() {
//        Token name = consume(IDENTIFIER, "Expect variable name.");
//        Expr initializer = null;
//        if (match(EQUAL)) {
//            initializer = expression2();
//        }
//        consume(SEMICOLON, "Expect ';' after variable declaration.");
//        return new Stmt.Let(name, initializer);
//    }
private Stmt varDeclaration(int a) {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(PLUS_EQUAL)) {
        initializer = assign();
    } else if (match(EQUAL)) {
        initializer = expression();
    } else if (a == 1) {
        match(IN);
        match(INTEGER);
        initializer = new Expr.Literal(previous().literal);
    }
    if (a == 0) consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Let(name, initializer);
}
    private Stmt mutDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Let(name, initializer);
    }
    private Stmt whileStatement() {
        Expr condition = expression();
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }
    private Stmt loopStatement() {
        consume(LEFT_BRACE, "Expect '{' after 'loop'.");
        List<Stmt> body = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            body.add(statement());
        }
        consume(RIGHT_BRACE, "Expect '}' after loop body.");
        Expr condition = null;
        if (match(IF)) {
            condition = expression();
            consume(LEFT_BRACE, "Expect '{' after 'if'.");
            consume(BREAK, "Expect 'break' after 'if'.");
            consume(SEMICOLON, "Expect ';' after 'break'.");
            consume(RIGHT_BRACE, "Expect '}' after 'break'.");
        }
        return new Stmt.Loop(body, condition);
    }
    private Stmt breakStatement(){
        return null;
    }
    private Stmt expressionStatement() {
        Expr expr = null;
        if(a==0) {
            expr = expression2();
        }
        else if(a==1){
            expr = expression();
        }
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
    //    private Stmt.Function function(String kind) {
//        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
//        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
//        List<Token> parameters = new ArrayList<>();
//        if (!check(RIGHT_PAREN)) {
//            do {
//                if (parameters.size() >= 255) {
//                    error(peek(), "Can't have more than 255 parameters.");
//                }
//
//                parameters.add(
//                        consume(IDENTIFIER, "Expect parameter name."));
//            } while (match(COMMA));
//        }
//        consume(RIGHT_PAREN, "Expect ')' after parameters.");
//        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
//        List<Stmt> body = block();
//        return new Stmt.Function(name, parameters, body);
//    }
    private Stmt.Function mainFunction() {
        Token name = consume(IDENTIFIER, "Expect 'main' name.");
        consume(LEFT_PAREN, "Expect '(' after 'main' name.");
        consume(RIGHT_PAREN, "Expect ')' after 'main' parameters.");
        consume(LEFT_BRACE, "Expect '{' before 'main' body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, Collections.emptyList(), body);
    }


    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }
    private Expr and() {
        Expr expr = assign();

        while (match(AND)) {
            Token operator = previous();
            Expr right = assign();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }
    private Expr assign(){
        Expr expr = equality();

        while (match(PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, MODULO_EQUAL)) {
            Token operator = previous();
            Expr value = assign();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                expr = new Expr.AssignPE(name, operator, value);
            }
        }
        return expr;
    }
    private Expr assign2(){
        Expr expr = equality();

        while (match(PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, MODULO_EQUAL)){
            Token operator = previous();
            Expr right = equality();
            expr=new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR, MODULO)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN,
                "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);

        if (match(INTEGER, FLOAT, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Rust.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case MUT:
                case FN:
                case LET:
                case FOR:
                case IF:
                case WHILE:
                case PRINTLN:
                case RETURN:
                case BREAK:
                    return;
            }

            advance();
        }
    }

}