package SEEn.AnnotationPar;


import Exc.PARS_EXC;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import SEEn.Imm.DLTerm.*;
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
                if (entry.getValue() instanceof ArrayList)
                    this.tokens = (List<Token>) entry.getValue();
            }

            if (!tokens.isEmpty()) current = tokens.remove(0);

            /* Start to parse and process the current annotation */
            if (current != null) this.process();
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
        }
        /*
         * The returns condition adds the parsed formula
         */
        else if (identifier.spelling().equals("returns")) {
            DLTerm formula = parse();
            accept(Token.TokenType.COLON);
            DLTerm term = parse();

            /* formula -> term : If condition of returns holds the term must be equal to the returned value */
            this.state.addToPostCondition(new DLOr(new DLNot(formula), new DLCmp(term, new DLBind("return"), COMPARATOR.EQUAL)));
        }
        /*
         * Ensures that the formula holds in the final state
         */
        else if (identifier.spelling().equals("ensures")) {
            DLTerm formula = parse();

            this.state.addToPostCondition(formula);
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

    public DLTerm parse() throws PARS_EXC {
        return parseCmp();
    }

    public DLTerm parseCmp() throws PARS_EXC {
        DLTerm left = this.parseOr();

        if (current.type().group() == Token.TokenType.TokenGroup.COMPARE) {
            Token cmpT = current;

            DLCmp cmp = null;

            Source source = current.source();
            if (current.type() == Token.TokenType.CMPEQ) {
                accept();
                cmp = new DLCmp(left, this.parseOr(), COMPARATOR.EQUAL);
            }
            else if (current.type() == Token.TokenType.CMPNE) {
                accept();
                cmp = new DLCmp(left, this.parseOr(), COMPARATOR.NOT_EQUAL);
            }
            else if (current.type() == Token.TokenType.CMPGE) {
                accept();
                cmp = new DLCmp(left, this.parseOr(), COMPARATOR.GREATER_SAME);
            }
            else if (current.type() == Token.TokenType.CMPGT) {
                accept();
                cmp = new DLCmp(left, this.parseOr(), COMPARATOR.GREATER_THAN);
            }
            else if (current.type() == Token.TokenType.CMPLE) {
                accept();
                cmp = new DLCmp(left, this.parseOr(), COMPARATOR.LESS_SAME);
            }
            else if (current.type() == Token.TokenType.CMPLT) {
                accept();
                cmp = new DLCmp(left, this.parseOr(), COMPARATOR.LESS_THAN);
            }

            left = cmp;
        }

        return left;
    }

    public DLTerm parseOr() throws PARS_EXC {
        DLTerm left = this.parseAnd();

        while (current.type() == Token.TokenType.OR) {
            accept();
            left = new DLOr(left, this.parseAnd());
        }

        return left;
    }

    public DLTerm parseAnd() throws PARS_EXC {
        DLTerm left = this.parseAtom();

        while (current.type() == Token.TokenType.AND) {
            accept();
            left = new DLAnd(left, this.parseAnd());
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

            if (current.type() == Token.TokenType.LPAREN) {
                accept();
                Token identifier = accept(Token.TokenType.IDENTIFIER);
                accept(Token.TokenType.RPAREN);

                return new DLBind(name.spelling(), identifier.spelling());
            }
            else return new DLBind(name.spelling(), null);
        }
        else if (current.type() == Token.TokenType.IDENTIFIER) {
            Token identifier = accept();
            return new DLVariable(identifier.spelling());
        }

        throw new PARS_EXC(current.source(), current.type(), Token.TokenType.BOOLLIT, Token.TokenType.IDENTIFIER);
    }

}
