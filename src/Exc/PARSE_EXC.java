package Exc;

import Par.Token.TokenType;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.Message;

/**
 * Thrown when the parser encounters a token that is not expected by the Snips Language Grammar.
 * This error can only occur in combination with a bad user input.
 */
public class PARSE_EXC extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	TokenType actual;
	
	TokenType [] expected;
	
	Source location;
	
	public PARSE_EXC(Source source, TokenType actual, TokenType...expected) {
		this.location = source;
		this.actual = actual;
		this.expected = expected;
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public String getMessage() {
		String message = "Got " + this.actual + ", expected ";
		for (TokenType t : this.expected) {
			message += t + ", ";
		}
		return message + this.location.getSourceMarker();
	}
	
} 
