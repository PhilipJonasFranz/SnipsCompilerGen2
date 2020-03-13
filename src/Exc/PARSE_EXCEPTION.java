package Exc;

import Par.Token.TokenType;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.Message;

public class PARSE_EXCEPTION extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	TokenType actual;
	
	TokenType [] expected;
	
	Source location;
	
	public PARSE_EXCEPTION(Source source, TokenType actual, TokenType...expected) {
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
