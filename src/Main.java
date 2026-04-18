import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Main {

    // ======================= TOKEN =======================
    enum TipoToken {
        PR_NUMERO, PR_DECIMAL, PR_LETRA,
        PR_TILIN, PR_LEER,
        ID, VAL_INT, VAL_DEC, VAL_STR,
        OP_ARIT, OP_REL, OP_ASIG,
        PARENTESIS_A, PARENTESIS_C,
        PUNTO_COMA
    }

    static class Token {
        TipoToken tipo;
        String valor;

        Token(TipoToken tipo, String valor) {
            this.tipo = tipo;
            this.valor = valor;
        }

        public String toString() {
            return "[" + tipo + ": " + valor + "]";
        }
    }

    // ======================= NODO =======================
    static class Nodo {
        String valor;
        Nodo izquierda, derecha;

        Nodo(String valor) {
            this.valor = valor;
        }
    }

    static int tempCount = 1;

    // ======================= MEMORIA DE EJECUCIÓN (NUEVO) =======================
    static Map<String, Object> variables = new HashMap<>();
    static Scanner scannerEntrada = new Scanner(System.in);

    // ======================= MAIN =======================
    public static void main(String[] args) throws Exception {

        String fuente = leerArchivo("src/codigo.txt");

        // Quitar comentarios de línea para que no interfieran (NUEVO)
        fuente = fuente.replaceAll("//[^\n]*", "");

        List<Token> tokens = analizarLexico(fuente);

        System.out.println("=== TOKENS ===");
        tokens.forEach(System.out::println);

        System.out.println("\n=== ÁRBOL Y CUÁDRUPLOS ===");
        analizarSintactico(tokens);

        System.out.println("\n=== SALIDA ===");     // <-- NUEVO
        ejecutar(tokens);                           // <-- NUEVO
    }

    // ======================= LÉXICO =======================
    static List<Token> analizarLexico(String fuente) {

        List<Token> tokens = new ArrayList<>();

        Map<String, TipoToken> reservadas = new HashMap<>();
        reservadas.put("numero",  TipoToken.PR_NUMERO);
        reservadas.put("decimal", TipoToken.PR_DECIMAL);
        reservadas.put("letra",   TipoToken.PR_LETRA);
        reservadas.put("tilin",   TipoToken.PR_TILIN);   // <-- NUEVO
        reservadas.put("leer",    TipoToken.PR_LEER);    // <-- NUEVO

        String regex = "\\s*(?:(\".*?\")|([a-zA-Z_][a-zA-Z0-9_]*)|(\\d+\\.\\d+)|(\\d+)|(==|!=|<=|>=|>|<)|([\\+\\-\\*/])|(=)|(\\(|\\)|;))";
        Matcher m = Pattern.compile(regex).matcher(fuente);

        while (m.find()) {
            if (m.group(1) != null)
                tokens.add(new Token(TipoToken.VAL_STR, m.group(1)));

            else if (m.group(2) != null) {
                String val = m.group(2);
                tokens.add(new Token(reservadas.getOrDefault(val, TipoToken.ID), val));
            }

            else if (m.group(3) != null)
                tokens.add(new Token(TipoToken.VAL_DEC, m.group(3)));

            else if (m.group(4) != null)
                tokens.add(new Token(TipoToken.VAL_INT, m.group(4)));

            else if (m.group(5) != null)
                tokens.add(new Token(TipoToken.OP_REL, m.group(5)));

            else if (m.group(6) != null)
                tokens.add(new Token(TipoToken.OP_ARIT, m.group(6)));

            else if (m.group(7) != null)
                tokens.add(new Token(TipoToken.OP_ASIG, m.group(7)));

            else if (m.group(8) != null) {
                String s = m.group(8);
                if (s.equals("(")) tokens.add(new Token(TipoToken.PARENTESIS_A, s));
                else if (s.equals(")")) tokens.add(new Token(TipoToken.PARENTESIS_C, s));
                else if (s.equals(";")) tokens.add(new Token(TipoToken.PUNTO_COMA, s));
            }
        }

        return tokens;
    }

    // ======================= SINTÁCTICO =======================
    static void analizarSintactico(List<Token> tokens) {

        int i = 0;

        while (i < tokens.size() - 1) {

            // --- Declaración con asignación: tipo id = expr ;
            if ((tokens.get(i).tipo == TipoToken.PR_NUMERO ||
                 tokens.get(i).tipo == TipoToken.PR_DECIMAL ||
                 tokens.get(i).tipo == TipoToken.PR_LETRA)
                && i + 2 < tokens.size()
                && tokens.get(i + 1).tipo == TipoToken.ID
                && tokens.get(i + 2).tipo == TipoToken.OP_ASIG) {

                String tipo     = tokens.get(i).valor;
                String variable = tokens.get(i + 1).valor;
                i += 3;

                List<Token> expr = new ArrayList<>();
                while (i < tokens.size() && tokens.get(i).tipo != TipoToken.PUNTO_COMA) {
                    expr.add(tokens.get(i++));
                }

                if (!expr.isEmpty()) {
                    // leer en la expresión → cuádruplo especial
                    if (expr.get(0).tipo == TipoToken.PR_LEER) {
                        String prompt = "";
                        for (Token t : expr)
                            if (t.tipo == TipoToken.VAL_STR) { prompt = t.valor; break; }
                        System.out.println("\nCuádruplos: (declaración " + tipo + " " + variable + ")");
                        System.out.println("leer\t" + prompt + "\t-\t" + variable);
                    } else {
                        boolean valida = true;
                        for (Token t : expr) {
                            if (!(t.tipo == TipoToken.ID || t.tipo == TipoToken.VAL_INT ||
                                  t.tipo == TipoToken.VAL_DEC || t.tipo == TipoToken.OP_ARIT ||
                                  t.tipo == TipoToken.PARENTESIS_A || t.tipo == TipoToken.PARENTESIS_C)) {
                                valida = false; break;
                            }
                        }
                        if (valida) {
                            Nodo raiz = construirArbol(expr);
                            System.out.println("\nÁrbol de: " + variable + " (declaración " + tipo + ")");
                            imprimirArbol(raiz, 0);
                            System.out.println("\nCuádruplos:");
                            generarCuadruplos(raiz, variable);
                        }
                    }
                }
                continue;
            }

            // --- Asignación: id = expr ;
            if (tokens.get(i).tipo == TipoToken.ID &&
                    tokens.get(i + 1).tipo == TipoToken.OP_ASIG) {

                String variable = tokens.get(i).valor;
                i += 2;

                List<Token> expr = new ArrayList<>();
                while (i < tokens.size() && tokens.get(i).tipo != TipoToken.PUNTO_COMA) {
                    expr.add(tokens.get(i));
                    i++;
                }

                if (expr.isEmpty()) continue;

                boolean valida = true;
                for (Token t : expr) {
                    if (!(t.tipo == TipoToken.ID ||
                            t.tipo == TipoToken.VAL_INT ||
                            t.tipo == TipoToken.VAL_DEC ||
                            t.tipo == TipoToken.OP_ARIT ||
                            t.tipo == TipoToken.PARENTESIS_A ||
                            t.tipo == TipoToken.PARENTESIS_C)) {
                        valida = false;
                        break;
                    }
                }

                if (!valida) continue;

                Nodo raiz = construirArbol(expr);

                System.out.println("\nÁrbol de: " + variable);
                imprimirArbol(raiz, 0);

                System.out.println("\nCuádruplos:");
                generarCuadruplos(raiz, variable);
            }

            // --- tilin(expr) ;
            else if (tokens.get(i).tipo == TipoToken.PR_TILIN
                     && i + 1 < tokens.size()
                     && tokens.get(i + 1).tipo == TipoToken.PARENTESIS_A) {

                i += 2; // saltar tilin y (
                List<Token> expr = new ArrayList<>();
                int depth = 0;
                while (i < tokens.size()) {
                    Token t = tokens.get(i);
                    if (t.tipo == TipoToken.PARENTESIS_A) { depth++; expr.add(t); i++; }
                    else if (t.tipo == TipoToken.PARENTESIS_C) {
                        if (depth == 0) { i++; break; }
                        depth--; expr.add(t); i++;
                    } else { expr.add(t); i++; }
                }

                String temp = "T" + tempCount++;
                System.out.println("\nCuádruplos: (tilin)");
                if (expr.size() == 1 && (expr.get(0).tipo == TipoToken.VAL_STR
                        || expr.get(0).tipo == TipoToken.ID
                        || expr.get(0).tipo == TipoToken.VAL_INT
                        || expr.get(0).tipo == TipoToken.VAL_DEC)) {
                    System.out.println("tilin\t" + expr.get(0).valor + "\t-\t-");
                } else {
                    boolean valida = true;
                    for (Token t : expr)
                        if (!(t.tipo == TipoToken.ID || t.tipo == TipoToken.VAL_INT ||
                              t.tipo == TipoToken.VAL_DEC || t.tipo == TipoToken.OP_ARIT ||
                              t.tipo == TipoToken.PARENTESIS_A || t.tipo == TipoToken.PARENTESIS_C))
                            { valida = false; break; }
                    if (valida && !expr.isEmpty()) {
                        Nodo raiz = construirArbol(expr);
                        String res = generarCuadruplos(raiz, null);
                        System.out.println("tilin\t" + res + "\t-\t-");
                    }
                }
            }

            i++;
        }
    }

    // ======================= ÁRBOL =======================
    static Nodo construirArbol(List<Token> tokens) {

        Stack<Nodo> valores = new Stack<>();
        Stack<String> ops = new Stack<>();

        for (Token t : tokens) {

            if (t.tipo == TipoToken.VAL_INT || t.tipo == TipoToken.VAL_DEC || t.tipo == TipoToken.ID) {
                valores.push(new Nodo(t.valor));
            }

            else if (t.tipo == TipoToken.OP_ARIT) {

                while (!ops.isEmpty() && prioridad(ops.peek()) >= prioridad(t.valor)) {

                    if (valores.size() < 2) return new Nodo("ERROR");

                    Nodo der = valores.pop();
                    Nodo izq = valores.pop();
                    Nodo op = new Nodo(ops.pop());
                    op.izquierda = izq;
                    op.derecha = der;
                    valores.push(op);
                }

                ops.push(t.valor);
            }

            else if (t.tipo == TipoToken.PARENTESIS_A) {
                ops.push("(");
            }

            else if (t.tipo == TipoToken.PARENTESIS_C) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {

                    if (valores.size() < 2) return new Nodo("ERROR");

                    Nodo der = valores.pop();
                    Nodo izq = valores.pop();
                    Nodo op = new Nodo(ops.pop());
                    op.izquierda = izq;
                    op.derecha = der;
                    valores.push(op);
                }
                if (!ops.isEmpty()) ops.pop();
            }
        }

        while (!ops.isEmpty()) {

            if (valores.size() < 2) return new Nodo("ERROR");

            Nodo der = valores.pop();
            Nodo izq = valores.pop();
            Nodo op = new Nodo(ops.pop());
            op.izquierda = izq;
            op.derecha = der;
            valores.push(op);
        }

        return valores.isEmpty() ? new Nodo("ERROR") : valores.pop();
    }

    static int prioridad(String op) {
        if (op.equals("*") || op.equals("/")) return 2;
        if (op.equals("+") || op.equals("-")) return 1;
        return 0;
    }

    // ======================= CUÁDRUPLOS =======================
    static String generarCuadruplos(Nodo nodo, String resultadoFinal) {

        if (nodo == null) return "";

        if (nodo.izquierda == null && nodo.derecha == null)
            return nodo.valor;

        String izq = generarCuadruplos(nodo.izquierda, null);
        String der = generarCuadruplos(nodo.derecha, null);

        String temp = "T" + tempCount++;

        System.out.println(nodo.valor + "\t" + izq + "\t" + der + "\t" + temp);

        if (resultadoFinal != null) {
            System.out.println("=\t" + temp + "\t-\t" + resultadoFinal);
        }

        return temp;
    }

    // ======================= IMPRIMIR =======================
    static void imprimirArbol(Nodo nodo, int nivel) {
        if (nodo == null) return;

        imprimirArbol(nodo.derecha, nivel + 1);

        for (int i = 0; i < nivel; i++) System.out.print("   ");
        System.out.println(nodo.valor);

        imprimirArbol(nodo.izquierda, nivel + 1);
    }

    // ======================= EJECUTAR =======================

    static void ejecutar(List<Token> tokens) {
        int i = 0;
        while (i < tokens.size()) {
            Token tk = tokens.get(i);

            if ((tk.tipo == TipoToken.PR_NUMERO ||
                 tk.tipo == TipoToken.PR_DECIMAL ||
                 tk.tipo == TipoToken.PR_LETRA)
                && i + 2 < tokens.size()
                && tokens.get(i + 1).tipo == TipoToken.ID
                && tokens.get(i + 2).tipo == TipoToken.OP_ASIG) {

                String nombre = tokens.get(i + 1).valor;
                i += 3;
                List<Token> expr = recolectarHastaPuntoComa(tokens, i);
                i += expr.size() + 1;
                Object val = evaluarExpresion(expr);
                variables.put(nombre, val);
                continue;
            }

            // Asignación: id = expr ;
            if (tk.tipo == TipoToken.ID
                && i + 1 < tokens.size()
                && tokens.get(i + 1).tipo == TipoToken.OP_ASIG) {

                String nombre = tk.valor;
                i += 2;
                List<Token> expr = recolectarHastaPuntoComa(tokens, i);
                i += expr.size() + 1;
                Object val = evaluarExpresion(expr);
                variables.put(nombre, val);
                continue;
            }

            // tilin(expr) ;
            if (tk.tipo == TipoToken.PR_TILIN
                && i + 1 < tokens.size()
                && tokens.get(i + 1).tipo == TipoToken.PARENTESIS_A) {

                i += 2; // saltar 'tilin' y '('
                List<Token> expr = recolectarHastaParentesisC(tokens, i);
                i += expr.size() + 1; // +1 por ')'
                Object val = evaluarExpresion(expr);
                System.out.println(formatear(val));
                eatSemicolon(tokens, i); if (i < tokens.size() && tokens.get(i).tipo == TipoToken.PUNTO_COMA) i++;
                continue;
            }

            i++;
        }
    }

    static List<Token> recolectarHastaPuntoComa(List<Token> tokens, int desde) {
        List<Token> expr = new ArrayList<>();
        int i = desde;
        while (i < tokens.size() && tokens.get(i).tipo != TipoToken.PUNTO_COMA) {
            expr.add(tokens.get(i++));
        }
        return expr;
    }

    static List<Token> recolectarHastaParentesisC(List<Token> tokens, int desde) {
        List<Token> expr = new ArrayList<>();
        int depth = 0, i = desde;
        while (i < tokens.size()) {
            Token t = tokens.get(i);
            if (t.tipo == TipoToken.PARENTESIS_A) { depth++; expr.add(t); }
            else if (t.tipo == TipoToken.PARENTESIS_C) {
                if (depth == 0) break;
                depth--; expr.add(t);
            } else { expr.add(t); }
            i++;
        }
        return expr;
    }

    static void eatSemicolon(List<Token> tokens, int i) { /* solo para claridad */ }

    static Object evaluarExpresion(List<Token> expr) {

        // Caso especial: leer("prompt")
        if (!expr.isEmpty() && expr.get(0).tipo == TipoToken.PR_LEER) {
            String prompt = "";
            for (Token t : expr) {
                if (t.tipo == TipoToken.VAL_STR) {
                    prompt = t.valor.replace("\"", "");
                    break;
                }
            }
            System.out.print(prompt);
            return scannerEntrada.nextLine();
        }

        // Expresión con un solo elemento
        if (expr.size() == 1) {
            Token t = expr.get(0);
            if (t.tipo == TipoToken.VAL_INT) return Integer.parseInt(t.valor);
            if (t.tipo == TipoToken.VAL_DEC) return Double.parseDouble(t.valor);
            if (t.tipo == TipoToken.VAL_STR) return t.valor.substring(1, t.valor.length() - 1);
            if (t.tipo == TipoToken.ID)      return variables.getOrDefault(t.valor, 0);
        }

        // Expresión aritmética: shunting-yard
        Stack<Object>  vals = new Stack<>();
        Stack<String>  ops  = new Stack<>();

        for (Token t : expr) {
            if (t.tipo == TipoToken.VAL_INT) {
                vals.push(Integer.parseInt(t.valor));
            } else if (t.tipo == TipoToken.VAL_DEC) {
                vals.push(Double.parseDouble(t.valor));
            } else if (t.tipo == TipoToken.VAL_STR) {
                vals.push(t.valor.substring(1, t.valor.length() - 1));
            } else if (t.tipo == TipoToken.ID) {
                vals.push(variables.getOrDefault(t.valor, 0));
            } else if (t.tipo == TipoToken.OP_ARIT) {
                while (!ops.isEmpty() && prioridad(ops.peek()) >= prioridad(t.valor))
                    aplicarOperacion(vals, ops.pop());
                ops.push(t.valor);
            } else if (t.tipo == TipoToken.PARENTESIS_A) {
                ops.push("(");
            } else if (t.tipo == TipoToken.PARENTESIS_C) {
                while (!ops.isEmpty() && !ops.peek().equals("("))
                    aplicarOperacion(vals, ops.pop());
                if (!ops.isEmpty()) ops.pop();
            }
        }
        while (!ops.isEmpty()) aplicarOperacion(vals, ops.pop());

        return vals.isEmpty() ? 0 : vals.pop();
    }

    static void aplicarOperacion(Stack<Object> vals, String op) {
        if (vals.size() < 2) return;
        Object b = vals.pop(), a = vals.pop();
        if (a instanceof String || b instanceof String) {
            vals.push(op.equals("+") ? formatear(a) + formatear(b) : formatear(a));
            return;
        }
        double da = toDouble(a), db = toDouble(b), r;
        switch (op) {
            case "+": r = da + db; break;
            case "-": r = da - db; break;
            case "*": r = da * db; break;
            case "/": r = db != 0 ? da / db : 0; break;
            default:  r = da;
        }
        vals.push((a instanceof Integer && b instanceof Integer && !op.equals("/")) ? (int) r : r);
    }

    static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    static String formatear(Object o) {
        if (o instanceof Double) {
            double d = (Double) o;
            return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        return String.valueOf(o);
    }

    // ======================= LECTURA =======================
    static String leerArchivo(String ruta) throws Exception {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(new FileInputStream(ruta), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = br.readLine()) != null)
            sb.append(linea).append("\n");
        br.close();
        return sb.toString();
    }
}