package rust;

import java.util.List;

interface RustCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}