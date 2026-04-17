import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Main {

    // ======================= TOKEN =======================
    enum TipoToken {
        PR_NUMERO, PR_DECIMAL, PR_LETRA,
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

    // ======================= MAIN =======================
    public static void main(String[] args) throws Exception {

        String fuente = leerArchivo("src/codigo.txt");

        List<Token> tokens = analizarLexico(fuente);

        System.out.println("=== TOKENS ===");
        tokens.forEach(System.out::println);

        System.out.println("\n=== ÁRBOL Y CUÁDRUPLOS ===");

        analizarSintactico(tokens);
    }

    // ======================= LÉXICO =======================
    static List<Token> analizarLexico(String fuente) {

        List<Token> tokens = new ArrayList<>();

        Map<String, TipoToken> reservadas = new HashMap<>();
        reservadas.put("numero", TipoToken.PR_NUMERO);
        reservadas.put("decimal", TipoToken.PR_DECIMAL);
        reservadas.put("letra", TipoToken.PR_LETRA);

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

            if (tokens.get(i).tipo == TipoToken.ID &&
                    tokens.get(i + 1).tipo == TipoToken.OP_ASIG) {

                String variable = tokens.get(i).valor;
                i += 2;

                List<Token> expr = new ArrayList<>();

                while (i < tokens.size() && tokens.get(i).tipo != TipoToken.PUNTO_COMA) {
                    expr.add(tokens.get(i));
                    i++;
                }

                // Validar expresión
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

    // ======================= LECTURA =======================
    static String leerArchivo(String ruta) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(ruta));
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = br.readLine()) != null)
            sb.append(linea).append("\n");
        br.close();
        return sb.toString();
    }
}