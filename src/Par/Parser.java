package Par;

import java.util.Deque;
import java.util.HashMap;

import Exc.PARSE_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.TYPE.COMPOSIT.STRUCT;
import Par.Token.TokenType;
import Par.Token.TokenType.TokenGroup;

public class Parser {

	protected Deque<Token> tokenStream;
	
	Token current;
	
	HashMap<String, STRUCT> structs = new HashMap();
	
	public Parser(Deque tokens) throws SNIPS_EXCEPTION {
		if (tokens == null) throw new SNIPS_EXCEPTION("SNIPS_PARSE -> Tokens are null!");
		tokenStream = tokens;
		current = tokenStream.pop();
	}
	
	/**
	 * Accept a token based on its type.
	 * @param tokenType The type the token should match.
	 * @return The accepted Token.
	 * @throws PARSE_EXCEPTION Thrown then the token does not have the given type.
	 */
	protected Token accept(TokenType tokenType) throws PARSE_EXCEPTION {
		if (current.type() == tokenType)return accept();
		else throw new PARSE_EXCEPTION(current.source, current.type(), tokenType);
	}
	
	/**
	 * Accept a token based on its token group.
	 * @param group The group the token should match.
	 * @return The accepted Token.
	 * @throws PARSE_EXCEPTION Thrown when the token does not have the given token group.
	 */
	protected Token accept(TokenGroup group) throws PARSE_EXCEPTION {
		if (current.type().group == group)return accept();
		else throw new PARSE_EXCEPTION(current.source, current.type());
	}
	
	/**
	 * Accept a token without any checks.
	 * @return The accepted token.
	 */
	protected Token accept() {
		Token old = current;
		current = tokenStream.pop();
		return old;
	}
	
	public SyntaxElement parse() {
		try {
			return parseProgram();
		} catch (PARSE_EXCEPTION e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	public Program parseProgram() throws PARSE_EXCEPTION {
		return null;
	}
	
}
