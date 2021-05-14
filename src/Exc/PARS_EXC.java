package Exc;

import Par.Token.TokenType;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.LogPoint;
import Util.Logging.Message;

import java.io.Serial;

/**
 * Thrown when the parser encounters a token that is not expected by the Snips Language Grammar.
 * This error can only occur in combination with a bad user input.
 */
public class PARS_EXC extends Exception {

	@Serial
	private static final long serialVersionUID = 765217464625890214L;

	private TokenType actual;
	
	private TokenType [] expected;
	
	private Source location;
	
	public PARS_EXC(Source source, TokenType actual, TokenType...expected) {
		this.location = source;
		this.actual = actual;
		this.expected = expected;
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}

	public String getExcFieldName() {
		return "UNKNOWN_FIELD";
	}
	
	public String getMessage() {
		String message = "Got " + this.actual + ", expected ";
		for (TokenType t : this.expected) {
			message += t + ", ";
		}
		return message + this.location.getSourceMarker();
	}
	
} 
