package SEEn.AnnotationPar;

import Exc.PARS_EXC;
import Imm.AST.Expression.OperatorExpression;
import Imm.AST.SyntaxElement;
import Par.Token;
import SEEn.Imm.Formula.*;
import SEEn.Imm.Term.DLTerm;
import SEEn.SEState;
import Util.ASTDirective;
import Util.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SEAnnotationParser {

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
                if (entry.getValue() instanceof ArrayList) {
                    this.tokens = (List<Token>) entry.getValue();

                    System.out.println(tokens.toString());
                }
            }

            if (!tokens.isEmpty()) current = tokens.remove(0);

            /* Start to parse and process the current annotation */
            if (current != null) this.parse();
        }
    }

    public void parse() throws PARS_EXC {
        accept(Token.TokenType.AT);

        Token identifier = accept(Token.TokenType.IDENTIFIER);

        /*
         * Requires annotation adds the parsed formula to the pathcondition
         * of the current state, since it is a precondition.
         */
        if (identifier.spelling().equals("requires")) {
            FAbstr formula = parseFormula();
            if (this.state.pathCondition instanceof FAnd and) and.operands.add(formula);
            else this.state.pathCondition = new FAnd(this.state.pathCondition, formula);
        }
        /*
         * The returns condition adds the parsed formula
         */
        else if (identifier.spelling().equals("returns")) {
            FAbstr formula = parseFormula();
            DLTerm term = parseTerm();



            if (this.state.pathCondition instanceof FAnd and) and.operands.add(formula);
            else this.state.pathCondition = new FAnd(this.state.pathCondition, formula);
        }
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

    public FAbstr parseFormula() throws PARS_EXC {
        return parseFOr();
    }

    public FAbstr parseFCmp() throws PARS_EXC {
        FAbstr left = this.parseFOr();

        if (current.type().group() == Token.TokenType.TokenGroup.COMPARE) {
            Token cmpT = current;

            FCmp cmp = null;

            Source source = current.source();
            if (current.type() == Token.TokenType.CMPEQ) {
                accept();
                cmp = new FCmp(left, this.parseFOr(), COMPARATOR.EQUAL, source);
            }
            else if (current.type() == Token.TokenType.CMPNE) {
                accept();
                cmp = new FCmp(left, this.parseFOr(), COMPARATOR.NOT_EQUAL, source);
            }
            else if (current.type() == Token.TokenType.CMPGE) {
                accept();
                cmp = new FCmp(left, this.parseFOr(), COMPARATOR.GREATER_SAME, source);
            }
            else if (current.type() == Token.TokenType.CMPGT) {
                accept();
                cmp = new FCmp(left, this.parseFOr(), COMPARATOR.GREATER_THAN, source);
            }
            else if (current.type() == Token.TokenType.CMPLE) {
                accept();
                cmp = new FCmp(left, this.parseFOr(), COMPARATOR.LESS_SAME, source);
            }
            else if (current.type() == Token.TokenType.CMPLT) {
                accept();
                cmp = new FCmp(left, this.parseFOr(), COMPARATOR.LESS_THAN, source);
            }

            left = cmp;
        }

        return left;
    }

    public FAbstr parseFOr() throws PARS_EXC {
        FAbstr left = this.parseFAnd();

        while (current.type() == Token.TokenType.OR) {
            accept();
            left = new FOr(left, this.parseFAnd());
        }

        return left;
    }

    public FAbstr parseFAnd() throws PARS_EXC {
        FAbstr left = this.parseFAtom();

        while (current.type() == Token.TokenType.AND) {
            accept();
            left = new FAnd(left, this.parseFAnd());
        }

        return left;
    }

    public FAbstr parseFAtom() throws PARS_EXC {
        if (current.type() == Token.TokenType.BOOLLIT) {
            return new FSingleton(Boolean.parseBoolean(accept().spelling()));
        }
        else if (current.type() == Token.TokenType.BACKSL) {
            accept();

            Token identifier = accept(Token.TokenType.IDENTIFIER);

            return new FBind(identifier.spelling());
        }
    }

    public DLTerm parseTerm() {

    }

}
