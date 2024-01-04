package rust;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import static rust.TokenType.EQUAL;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    Interpreter() {
        globals.define("clock", new RustCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Rust.runtimeError(error);
        }
    }
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -((Number)right).doubleValue();
        }

        // Unreachable.
        return null;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Number) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Number && right instanceof Number) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    private boolean isTruthy(Object object) {
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }
    private String stringify(Object object) {
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    void execute(Stmt stmt) {
        stmt.accept(this);
    }
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        RustFunction function = new RustFunction(stmt);
        environment.define(stmt.name.lexeme, function);
        return null;
    }
//    public void visitForStmt(Stmt.For stmt){
//        try {
//            Object iterableValue = evaluate(stmt.iterable);
//            if (iterableValue instanceof Iterator<?>) {
//                for (Object element : (Iterable<?>) iterableValue) {
//                    environment.define(stmt.variable.lexeme, element);
//                    for (Stmt statement : stmt.body) {
//                        execute(statement);
//                    }
//                }
//            } else {
//
//            }
//        }
//        catch(BreakException breakException){
//
//        }
//    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Iterator<Expr> expressionsIterator = stmt.expression.iterator();
        while (expressionsIterator.hasNext()) {
            Expr expression = expressionsIterator.next();
            if (expression instanceof Expr.Literal) {
                // Literal text
                if(((String)(((Expr.Literal) expression).value)).contains("{}"))
                {
                    String s=((String)(((Expr.Literal) expression).value));
                    int a=s.length();
                    String s2="";
                    for(int i=0;i<a;i++)
                    {
                        if(s.charAt(i)=='}' || s.charAt(i)=='{') continue;
                        s2+=s.charAt(i);
                    }
                    System.out.print(s2);
                } else
                    System.out.print(((Expr.Literal) expression).value);
            } else {
                // Value inside braces
                Object value = evaluate(expression);
                System.out.print(stringify(value));
            }

            // Print a comma if there are more expressions
            if (expressionsIterator.hasNext()) {
                //System.out.print(", ");
            }
        }
        return null;
    }
    @Override
    public Void visitPrintLnStmt(Stmt.PrintLn stmt) {
        Iterator<Expr> expressionsIterator = stmt.expression.iterator();
        while (expressionsIterator.hasNext()) {
            Expr expression = expressionsIterator.next();
            if (expression instanceof Expr.Literal) {
                // Literal text
                if(((String)(((Expr.Literal) expression).value)).contains("{}"))
                {
                    String s=((String)(((Expr.Literal) expression).value));
                    int a=s.length();
                    String s2="";
                    for(int i=0;i<a;i++)
                    {
                        if(s.charAt(i)=='}' || s.charAt(i)=='{') continue;
                        s2+=s.charAt(i);
                    }
                    System.out.print(s2);
                } else
                    System.out.println(((Expr.Literal) expression).value);
            } else {
                // Value inside braces
                Object value = evaluate(expression);
                System.out.println(stringify(value));
            }

            // Print a comma if there are more expressions
            if (expressionsIterator.hasNext()) {
                //System.out.print(", ");
            }
        }
        return null;
    }
    @Override
    public Void visitLetStmt(Stmt.Let stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    @Override
    public Void visitLoopStmt(Stmt.Loop stmt) {
        while (true) {
            for (Stmt bodyStmt : stmt.body) {
                execute(bodyStmt);
            }
            if (stmt.condition != null && !isTruthy(evaluate(stmt.condition))) {
                break;
            }
        }
        return null;
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }
    @Override
    public Object visitAssignPEExpr(Expr.AssignPE expr){
        Object left=(expr.operator.type==EQUAL)?expr.name.lexeme:environment.get(expr.name);
        Object right=evaluate(expr.value);
        Object value=null;
        switch (expr.operator.type) {
            case PLUS_EQUAL:
                if(left instanceof Integer && right instanceof  Integer)
                {
                    value = ((Integer) left).doubleValue() + ((Integer) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                if(left instanceof Float && right instanceof  Float)
                {
                    value = ((Float) left).doubleValue() + ((Float) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                if(left instanceof String && right instanceof  String)
                {
                    value = (String) left + (String) right;
                    environment.assign(expr.name, value);
                }
                break;
            case MINUS_EQUAL:
                if(left instanceof Integer && right instanceof  Integer)
                {
                    value = ((Integer) left).doubleValue() - ((Integer) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                if(left instanceof Float && right instanceof  Float)
                {
                    value = ((Float) left).doubleValue() - ((Float) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                break;
            case STAR_EQUAL:
                if(left instanceof Integer && right instanceof  Integer)
                {
                    value = ((Integer) left).doubleValue() * ((Integer) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                if(left instanceof Float && right instanceof  Float)
                {
                    value = ((Float) left).doubleValue() * ((Float) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                break;
            case SLASH_EQUAL:
                if(left instanceof Integer && right instanceof  Integer)
                {
                    value = ((Integer) left).doubleValue() / ((Integer) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                if(left instanceof Float && right instanceof  Float)
                {
                    value = ((Float) left).doubleValue() / ((Float) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                break;
            case MODULO_EQUAL:
                if(left instanceof Integer && right instanceof  Integer)
                {
                    value = ((Integer) left).doubleValue() % ((Integer) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                if(left instanceof Float && right instanceof  Float)
                {
                    value = ((Float) left).doubleValue() % ((Float) right).doubleValue();
                    environment.assign(expr.name, value);
                }
                break;
        }
        return value;
    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() - ((Number) right).doubleValue();
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() * ((Number) right).doubleValue();
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() / ((Number) right).doubleValue();
            case MODULO:
                return ((Number) left).doubleValue() % ((Number) right).doubleValue();
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() > ((Number) right).doubleValue();
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() < ((Number) right).doubleValue();
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
        }
        // Unreachable.
        return null;
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof RustCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }
        RustCallable function = (RustCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }
    public void executeProgram(List<Stmt> statements) {
        for (Stmt statement : statements) {
            if (statement instanceof Stmt.Function) {
                Stmt.Function function = (Stmt.Function) statement;
                if ("main".equals(function.name.lexeme)) {
                    executeFunction(function, globals);
                    return; // exit after executing main
                }
            }
        }
        throw new RuntimeException("No main function found.");
    }

    private void executeFunction(Stmt.Function function, Environment environment) {
        Environment functionEnvironment = new Environment(environment);
        for (int i = 0; i < function.params.size(); i++) {
            functionEnvironment.define(function.params.get(i).lexeme, null); // initialize to null for simplicity
        }
        executeBlock(function.body, functionEnvironment);
    }
}