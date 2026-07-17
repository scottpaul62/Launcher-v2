package fr.heroesworld.titlescreen.gek;

import java.util.ArrayList;
import java.util.List;

/** Conditions gek-lite : always | queries booleennes | comparaisons numeriques | ! && || ( ). */
public final class HWGekCond {

    /** Contexte d'evaluation (rempli par l'appelant : preview ou jeu). */
    public static final class Ctx {
        public boolean inGui, isFlying, isElytra, isSwimming, isSneaking, onGround = true;
        public float speed, yVelocity, lifeTime;
    }

    private interface Node { boolean eval(Ctx c); }

    private final Node root;
    private HWGekCond(Node root) { this.root = root; }
    public boolean eval(Ctx c) { try { return root.eval(c); } catch (Throwable t) { return false; } }

    public static HWGekCond parse(String s) {
        try { return new HWGekCond(new Parser(s).parseExpr()); }
        catch (Throwable t) {
            HWGekRegistry.LOG.warn("[GEK] condition invalide, repli false : {}", s);
            return new HWGekCond(c -> false);
        }
    }

    // ---------------- mini-parser recursif ----------------
    private static final class Parser {
        private final List<String> toks = new ArrayList<>();
        private int i = 0;
        Parser(String s) { tokenize(s); }

        private void tokenize(String s) {
            int n = s.length(), p = 0;
            while (p < n) {
                char c = s.charAt(p);
                if (Character.isWhitespace(c)) { p++; continue; }
                if (c == '(' || c == ')' || c == '!') {
                    if (c == '!' && p + 1 < n && s.charAt(p + 1) == '=') { toks.add("!="); p += 2; continue; }
                    toks.add(String.valueOf(c)); p++; continue;
                }
                if (c == '&' || c == '|') { toks.add(s.substring(p, Math.min(n, p + 2))); p += 2; continue; }
                if (c == '>' || c == '<' || c == '=') {
                    if (p + 1 < n && s.charAt(p + 1) == '=') { toks.add(s.substring(p, p + 2)); p += 2; }
                    else { toks.add(String.valueOf(c)); p++; }
                    continue;
                }
                int q = p;
                while (q < n && (Character.isLetterOrDigit(s.charAt(q)) || s.charAt(q) == '_' || s.charAt(q) == '.' || s.charAt(q) == '-')) q++;
                if (q == p) throw new IllegalArgumentException("caractere inattendu: " + c);
                toks.add(s.substring(p, q)); p = q;
            }
        }

        private String peek() { return i < toks.size() ? toks.get(i) : null; }
        private String next() { return toks.get(i++); }

        Node parseExpr() { Node n = parseOr(); if (i < toks.size()) throw new IllegalArgumentException("reste: " + peek()); return n; }

        private Node parseOr() {
            Node left = parseAnd();
            while ("||".equals(peek())) { next(); Node r = parseAnd(); Node l = left; left = c -> l.eval(c) || r.eval(c); }
            return left;
        }
        private Node parseAnd() {
            Node left = parseUnary();
            while ("&&".equals(peek())) { next(); Node r = parseUnary(); Node l = left; left = c -> l.eval(c) && r.eval(c); }
            return left;
        }
        private Node parseUnary() {
            if ("!".equals(peek())) { next(); Node n = parseUnary(); return c -> !n.eval(c); }
            return parsePrimary();
        }
        private Node parsePrimary() {
            String t = next();
            if ("(".equals(t)) { Node n = parseOr(); if (!")".equals(next())) throw new IllegalArgumentException(") attendu"); return n; }
            // comparaison numerique ?
            String op = peek();
            if (op != null && (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=") || op.equals("==") || op.equals("!="))) {
                next();
                float val = Float.parseFloat(next());
                final String q = t, o = op;
                return c -> {
                    float v = num(q, c);
                    switch (o) {
                        case ">": return v > val;
                        case "<": return v < val;
                        case ">=": return v >= val;
                        case "<=": return v <= val;
                        case "==": return v == val;
                        default: return v != val;
                    }
                };
            }
            final String q = t;
            return c -> bool(q, c);
        }

        private static float num(String q, Ctx c) {
            switch (q) {
                case "speed": return c.speed;
                case "y_velocity": return c.yVelocity;
                case "life_time": return c.lifeTime;
                default: return bool(q, c) ? 1f : 0f;
            }
        }
        private static boolean bool(String q, Ctx c) {
            switch (q) {
                case "always": case "1": case "true": return true;
                case "in_gui": return c.inGui;
                case "is_flying": return c.isFlying;
                case "is_elytra": return c.isElytra;
                case "is_swimming": return c.isSwimming;
                case "is_sneaking": return c.isSneaking;
                case "on_ground": return c.onGround;
                default:
                    HWGekRegistry.LOG.warn("[GEK] query inconnue (false) : {}", q);
                    return false;
            }
        }
    }
}
