package Res;

/**
 * Contains formatting strings, debug messages etc.
 */
public class Const {

			/* --- CONTEXT CHECKING --- */
	public static final String MAIN = "main";
	
	public static final String MISSING_MAIN_FUNCTION = "Missing main function";
	
	public static final String MAIN_CANNOT_HOLD_PROVISOS = "Function main cannot hold proviso types";
	
	public static final String MAIN_CANNOT_SIGNAL = "Entry function 'main' cannot signal exceptions";
	
	public static final String MUST_SIGNAL_AT_LEAST_ONE_TYPE = "Function must signal at least one exception type";
	
	public static final String WATCHED_EXCEPTION_NOT_THROWN_IN_FUNCTION = "Watched exception %s is not thrown in function '%s', %s";
	
	public static final String WATCHED_EXCEPTION_NOT_THROWN_IN_TRY = "Watched exception type %s is not thrown in try block, %s";
	
	public static final String MULTIPLE_WATCHPOINTS_FOR_EXCEPTION = "Found multiple watchpoints for exception %s";
		
	public static final String DUPLICATE_PARAMETER_NAME = "Duplicate parameter name: %s in function: %s";
	
	public static final String DUPLICATE_FUNCTION_NAME = "Duplicate function name: %s";
	
	public static final String DUPLICATE_FIELD_NAME = "Duplicate field name: %s";
	
	public static final String UNCHECKED_TYPE_VOID = "Unchecked type %s, %s";
	
	public static final String UNKNOWN_VARIABLE = "Unknown variable: %s";
	
	public static final String MULTIPLE_MATCHES_FOR_FIELD = "Multiple matches for field '%s': %s. Ensure namespace path is explicit and correct";
	
	public static final String VARIABLE_SHADOWED_BY = "Variable '%s' at %s shadowed by '%s' at %s";
	
	public static final String MISSMATCHING_ARGUMENT_COUNT = "Missmatching argument count: Expected %d but got %d";
	
	public static final String ARGUMENT_DOES_NOT_MATCH_STRUCT_FIELD_TYPE = "Argument type does not match struct field (%d) type: %s vs %s";
	
	public static final String BASE_MUST_BE_VARIABLE_REFERENCE = "Base must be variable reference";
	
	public static final String CANNOT_DETERMINE_TYPE = "Cannot determine type";
	
	public static final String CANNOT_DEREF_NON_POINTER = "Cannot deref non pointer, actual %s";
	
	public static final String CANNOT_SELECT_FROM_NON_STRUCT = "Cannot select from non struct type, actual %s";
	
	public static final String CLASS_CANNOT_BE_SELECTOR = "%s cannot be a selector";
	
	public static final String CONDITION_NOT_BOOLEAN = "Condition is not boolean";
	
	public static final String ITERATOR_MUST_HAVE_INITIAL_VALUE = "Iterator must have initial value";
	
	public static final String POINTER_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE = "Pointer type does not match iterator type: %s vs %s";
	
	public static final String ARRAY_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE = "Array type does not match iterator type: %s vs %s";
	
	public static final String ONLY_AVAILABLE_FOR_POINTERS_AND_ARRAYS = "Only available for pointers and arrays, actual %s";
	
	public static final String CANNOT_ITERATE_WITHOUT_RANGE = "Cannot iterate over reference without range";
	
	public static final String MULTIPLE_ELSE_STATEMENTS = "If Statement can only have one else statement";
	
	
			/* --- PROVISO RELATED --- */
	public static final String NO_MAPPING_EQUAL_TO_GIVEN_MAPPING = "No mapping is equal to the given mapping!";

	public static final String RECIEVED_MAPPING_LENGTH_NOT_EQUAL = "Recieved proviso mapping length is not equal to expected length, expected %d, but got %d";

	public static final String MISSMATCHING_NUMBER_OF_PROVISOS = "Missmatching number of provided provisos, expected %d, got %d";
	
	public static final String MISSMATCHING_NUMBER_OF_PROVISOS_EXTENSION = "Incorrect number of proviso for extension %s, expected %d, got %d";
	
	public static final String NON_PROVISO_TYPE_IN_HEADER = "Found non proviso type in proviso header: %s";
	
	public static final String CANNOT_MAP_TYPE_TO_PROVISO = "Cannot map %s to %s";
	
	
			/* --- CODE GENERATION --- */
	public static final String UNABLE_TO_POP_X_WORDS = "Unable to pop %d Words from the stack, could only pop %d";
	
}
