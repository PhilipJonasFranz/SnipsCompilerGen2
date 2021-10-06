package SEEn.AnnotationPar;


import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import SEEn.Imm.DLTerm.*;
import SEEn.SEState;
import Util.ASTDirective;
import Util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SEAnnotationParser {

    private List<DLTerm> returns = new ArrayList<>();

    private SyntaxElement origin;

    private SEState state;

    private List<Token> tokens = new ArrayList();

    private Token current;

    public SEAnnotationParser(SyntaxElement origin, SEState state) {
        this.origin = origin;
        this.state = state;
    }

    public void parseAnnotations() throws PARS_EXC {
        List<ASTDirective> directives = origin.getDirectives(ASTDirective.DIRECTIVE.SE_PROPERTY);

        for (ASTDirective directive : directives) {
            Optional<Map.Entry<String, Object>> property = directive.properties().entrySet().stream().findFirst();

            /* Extract tokens from AST Annotation from SyntaxElement */
            if (property.isPresent()) {
                Map.Entry<String, Object> entry = property.get();
                if (entry.getValue() instanceof ArrayList)
                    this.tokens = (List<Token>) entry.getValue();
            }

            if (!tokens.isEmpty()) current = tokens.remove(0);

            /* Start to parse and process the current annotation */
            if (current != null) this.process();
        }

        /* One of the return statements is taken:
         *
         *      (cond1 -> result1) or (cond2 -> result2) or ...
         */
        if (!this.returns.isEmpty()) {
            this.state.addToPostCondition(new DLOr(this.returns));
        }
    }

    public void process() throws PARS_EXC {
        accept(Token.TokenType.AT);

        /* Was just a placeholder line */
        if (tokens.isEmpty()) return;

        Token identifier = accept(Token.TokenType.IDENTIFIER);

        /*
         * Requires annotation adds the parsed formula to the pathcondition
         * of the current state, since it is a precondition.
         */
        if (identifier.spelling().equals("requires")) {
            DLTerm formula = parse();
            this.state.addToPathCondition(formula);
            this.state.precondition.operands.add(formula);
        }
        /*
         * The returns condition adds the parsed formula
         */
        else if (identifier.spelling().equals("returns")) {
            DLTerm formula = parse();
            accept(Token.TokenType.COLON);
            DLTerm term = parse();

            this.state.returnCondition.add(new Pair<>(formula.clone(), term.clone()));

            /* formula -> term : If condition of returns holds the term must be equal to the returned value */
            this.returns.add(new DLOr(new DLNot(formula), new DLCmp(term, new DLBind("result"), COMPARATOR.EQUAL)));
        }
        /*
         * Ensures that the formula holds in the final state
         */
        else if (identifier.spelling().equals("ensures")) {
            DLTerm formula = parse();

            this.state.addToPostCondition(formula);
        }
        else if (identifier.spelling().equals("finally")) {
            DLTerm formula = parse();

            this.state.addToPostCondition(formula);
        }
        else if (identifier.spelling().equals("invariant")) {
            DLTerm formula = parse();

            this.state.invariantCondition = formula;
        }
        else throw new SNIPS_EXC("Unknown annotation: '" + identifier.spelling() + "'");
    }

    private Token accept(Token.TokenType tokenType) throws PARS_EXC {
        if (current.type() == tokenType) return accept();
        else throw new PARS_EXC(current.source(), current.type(), tokenType);
    }

    private Token accept() {
        Token old = current;
        if (!tokens.isEmpty()) {
            current = tokens.get(0);
            tokens.remove(0);
        }

        return old;
    }

    public DLTerm parse() throws PARS_EXC {
        return parseOr();
    }

    public DLTerm parseOr() throws PARS_EXC {
        DLTerm left = this.parseAnd();

        while (current.type() == Token.TokenType.OR) {
            accept();
            left = new DLOr(left, this.parseOr());
        }

        return left;
    }

    public DLTerm parseAnd() throws PARS_EXC {
        DLTerm left = this.parseCmp();

        while (current.type() == Token.TokenType.AND) {
            accept();
            left = new DLAnd(left, this.parseAnd());
        }

        return left;
    }

    public DLTerm parseCmp() throws PARS_EXC {
        DLTerm left = this.parseAddSub();

        if (current.type().group() == Token.TokenType.TokenGroup.COMPARE) {

            DLCmp cmp = null;

            if (current.type() == Token.TokenType.CMPEQ) {
                accept();
                cmp = new DLCmp(left, this.parseAddSub(), COMPARATOR.EQUAL);
            }
            else if (current.type() == Token.TokenType.CMPNE) {
                accept();
                cmp = new DLCmp(left, this.parseAddSub(), COMPARATOR.NOT_EQUAL);
            }
            else if (current.type() == Token.TokenType.CMPGE) {
                accept();
                cmp = new DLCmp(left, this.parseAddSub(), COMPARATOR.GREATER_SAME);
            }
            else if (current.type() == Token.TokenType.CMPGT) {
                accept();
                cmp = new DLCmp(left, this.parseAddSub(), COMPARATOR.GREATER_THAN);
            }
            else if (current.type() == Token.TokenType.CMPLE) {
                accept();
                cmp = new DLCmp(left, this.parseAddSub(), COMPARATOR.LESS_SAME);
            }
            else if (current.type() == Token.TokenType.CMPLT) {
                accept();
                cmp = new DLCmp(left, this.parseAddSub(), COMPARATOR.LESS_THAN);
            }

            left = cmp;
        }

        return left;
    }

    public DLTerm parseAddSub() throws PARS_EXC {
        DLTerm left = this.parseAtom();

        while (current.type() == Token.TokenType.ADD || current.type() == Token.TokenType.SUB) {
            if (current.type() == Token.TokenType.ADD) {
                accept();
                left = new DLAdd(left, this.parseAddSub());
            }
            else {
                accept();
                left = new DLSub(left, this.parseAddSub());
            }
        }

        return left;
    }

    public DLTerm parseAtom() throws PARS_EXC {
        if (current.type() == Token.TokenType.BOOLLIT) {
            return new DLAtom(new BOOL(accept().spelling()));
        }
        else if (current.type() == Token.TokenType.INTLIT) {
            return new DLAtom(new INT(accept().spelling()));
        }
        else if (current.type() == Token.TokenType.BACKSL) {
            accept();

            Token name = accept(Token.TokenType.IDENTIFIER);

            if (name.spelling().equals("sum")) {
                /* Sum Term */

                accept(Token.TokenType.LPAREN);
                DLTerm iterator = this.parse();
                accept(Token.TokenType.SEMICOLON);
                DLTerm condition = this.parse();
                accept(Token.TokenType.SEMICOLON);
                DLTerm operand = this.parse();
                accept(Token.TokenType.RPAREN);

                return new DLSum(iterator, condition, operand);
            }
            else {
                if (current.type() == Token.TokenType.LPAREN) {
                    accept();
                    Token identifier = accept(Token.TokenType.IDENTIFIER);
                    accept(Token.TokenType.RPAREN);

                    return new DLBind(name.spelling(), identifier.spelling());
                } else return new DLBind(name.spelling(), null);
            }
        }
        else if (current.type() == Token.TokenType.IDENTIFIER) {
            Token identifier = accept();
            return new DLVariable(identifier.spelling());
        }

        throw new PARS_EXC(current.source(), current.type(), Token.TokenType.BOOLLIT, Token.TokenType.IDENTIFIER);
    }

}
