package Res;

/**
 * Contains formatting strings, debug messages etc.
 */
public class Const {

			/* ---< CONTEXT CHECKING >--- */
	public static final String MAIN = "main";
	
	public static final String MISSING_MAIN_FUNCTION = "Missing main function";
	
	public static final String MISSING_DEFAULT_STATEMENT = "Missing default statement";
	
	public static final String MAIN_CANNOT_HOLD_PROVISOS = "Function main cannot hold proviso types";
	
	public static final String MAIN_CANNOT_SIGNAL = "Entry function 'main' cannot signal exceptions";
	
	public static final String PREDICATE_CANNOT_SIGNAL = "Predicates may not signal exceptions";
	
	public static final String CALL_DURING_INIT_CANNOT_SIGNAL = "Calls made during initial setup may not signal, but '%s' does";
	
	public static final String MUST_SIGNAL_AT_LEAST_ONE_TYPE = "Function must signal at least one exception type";
	
	public static final String WATCHED_EXCEPTION_NOT_THROWN_IN_FUNCTION = "Watched exception %s is not thrown in function '%s', %s";
	
	public static final String WATCHED_EXCEPTION_NOT_THROWN_IN_TRY = "Watched exception type %s is not thrown in try block, %s";
	
	public static final String MULTIPLE_WATCHPOINTS_FOR_EXCEPTION = "Found multiple watchpoints for exception %s";
	
	public static final String UNWATCHED_EXCEPTIONS_FOR_FUNCTION = "Unwatched exceptions for function %s: %s";
		
	public static final String DUPLICATE_PARAMETER_NAME = "Duplicate parameter name: %s in function: %s";
	
	public static final String DUPLICATE_FUNCTION_NAME = "Duplicate function name: %s";
	
	public static final String USED_RESERVED_NAME = "The identifier '%s' is reserved, %s";
	
	public static final String DUPLICATE_FIELD_NAME = "Duplicate field name: %s";
	
	public static final String UNCHECKED_TYPE_VOID = "Unchecked type %s, %s";
	
	public static final String UNKNOWN_VARIABLE = "Unknown variable: %s";
	
	public static final String UNKNOWN_PREDICATE = "Unknown predicate: %s";
	
	public static final String UNKNOWN_EXPRESSION = "Unknown expression: %s";
	
	public static final String UNKNOWN_REGISTER = "Unknown register: %s";
	
	public static final String UNDEFINED_FUNCTION_OR_PREDICATE = "Undefined function or predicate '%s'";
	
	public static final String EXPECTED_TYPE_ACTUAL = "Expected %s, actual %s";
	
	public static final String EXPECTED_RETURN_VALUE = "Expected return value from inline call";
	
	public static final String NO_RETURN_VALUE = "Return statement has no return value, expected %s";
	
	public static final String MULTIPLE_MATCHES_FOR_X = "Multiple matches for %s '%s': %s. Ensure namespace path is explicit and correct";
	
	public static final String VARIABLE_SHADOWED_BY = "Variable '%s' at %s shadowed by '%s' at %s";
	
	public static final String PREDICATE_SHADOWS_FUNCTION = "Predicate name shadows function name '%s'";
	
	public static final String MODIFIER_VIOLATION = "Modifier violation: %s from %s";
	
	public static final String MODIFIER_VIOLATION_AT = "Modifier violation: %s from %s at %s";
	
	public static final String MISSMATCHING_ARGUMENT_NUMBER = "Missmatching argument number: Expected %d, but got %d";
	
	public static final String CAN_ONLY_COVER_WITH_STRUCT = "Can only cover params with STRUCT type, actual %s";
	
	public static final String CANNOT_INVOKE_SUPER_NO_EXTENSION = "Cannot invoke super constructor, struct '%s' does not have an extension";
	
	public static final String CANNOT_INVOKE_SUPER_NO_CONSTRUCTOR = "Cannot invoke super constructor, struct '%s' does not have a constructor";
	
	public static final String OPERAND_TYPES_DO_NOT_MATCH = "Operand types do not match: %s vs. %s";
	
	public static final String PARAMETER_TYPE_DOES_NOT_MATCH = "Parameter type does not match expected type: %s vs %s";
	
	public static final String PARAMETER_TYPE_INDEX_DOES_NOT_MATCH = "Argument (%d) does not match parameter: %s vs %s";
	
	public static final String PARAMETER_TYPE_INDEX_DOES_NOT_MATCH_POLY = "Argument (%d) does not match parameter, polymorphism only via pointers, actual %s vs %s";
	
	public static final String EXPRESSION_TYPE_DOES_NOT_MATCH_DECLARATION = "Expression type does not match the declaration type: %s vs %s";
	
	public static final String EXPRESSION_TYPE_DOES_NOT_MATCH_VARIABLE = "Expression type does not match the variable type: %s vs %s";
	
	public static final String POLY_ONLY_VIA_POINTER = "Polymorphism only via pointers, actual %s vs %s";
	
	public static final String RETURN_TYPE_DOES_NOT_MATCH = "Return type does not match stated return type: %s vs %s";
	
	public static final String VARIABLE_DOES_NOT_MATCH_EXPRESSION_POLY = "Variable type does not match expression type, polymorphism only via pointers, actual %s vs %s";
	
	public static final String ARGUMENT_DOES_NOT_MATCH_STRUCT_FIELD_TYPE = "Argument type does not match struct field (%d) type: %s vs %s";
	
	public static final String FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE = "Function '%s' is not part of struct type %s";
	
	public static final String STRUCT_TYPEDEF_MUST_CONTAIN_FIELD = "Struct must contain at least one field";
	
	public static final String NESTED_FUNCTION_CANNOT_BE_ACCESSED = "Nested Function '%s' cannot be accessed";
	
	public static final String NESTED_CALL_BASE_IS_NOT_A_STRUCT = "Nested call base is not a STRUCT, actual %s";
	
	public static final String BASE_MUST_BE_VARIABLE_REFERENCE = "Base must be variable reference";
	
	public static final String SWITCH_COND_MUST_BE_VARIABLE = "Switch Condition has to be variable reference";
	
	public static final String CANNOT_DETERMINE_TYPE = "Cannot determine type";
	
	public static final String CANNOT_DEREF_NON_POINTER = "Cannot deref non pointer, actual %s";
	
	public static final String CANNOT_SELECT_FROM_NON_STRUCT = "Cannot select from non struct type, actual %s";
	
	public static final String CANNOT_SELECT_FROM_TYPE = "Cannot select from type %s";
	
	public static final String CANNOT_PERFORM_ARITH_ON_NULL = "Cannot perform arithmetic on null";
	
	public static final String CANNOT_CAST_TO = "Cannot cast %s to %s";
	
	public static final String CANNOT_DEREF_TYPE = "Cannot deref type %s";
	
	public static final String OPERAND_IS_NOT_A_POINTER = "Operand is not a pointer, may cause unexpected behaviour, %s";
	
	public static final String POINTER_ARITH_ONLY_SUPPORTED_FOR_TYPE = "Pointer arithmetic is only supported for %s, actual %s";
	
	public static final String CLASS_CANNOT_BE_SELECTOR = "%s cannot be a selector";
	
	public static final String CONDITION_TYPE_MUST_BE_32_BIT = "Condition types have to be 32 bit large";
	
	public static final String EXPRESSIONT_TYPE_NOT_APPLICABLE_FOR_TYPE = "Expression type %s is not applicable for comparison, must be 32 bit";
	
	public static final String EXPRESSIONT_TYPE_NOT_APPLICABLE_FOR_ASSIGN_OP = "Expression type %s is not applicable for assign operator";
	
	public static final String ONLY_APPLICABLE_FOR_ONE_WORD_TYPE = "Only applicable for 1-Word types";
	
	public static final String ONLY_APPLICABLE_FOR_ONE_WORD_TYPE_ACTUAL = "Only applicable for 1-Word types, got %s";
	
	public static final String ITERATOR_MUST_HAVE_INITIAL_VALUE = "Iterator must have initial value";
	
	public static final String POINTER_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE = "Pointer type does not match iterator type: %s vs %s";
	
	public static final String ARRAY_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE = "Array type does not match iterator type: %s vs %s";
	
	public static final String ONLY_AVAILABLE_FOR_POINTERS_AND_ARRAYS = "Only available for pointers and arrays, actual %s";
	
	public static final String CANNOT_ITERATE_WITHOUT_RANGE = "Cannot iterate over reference without range";
	
	public static final String STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT = "Structure Init can only be a sub expression of structure init";
	
	public static final String ARRAY_INIT_MUST_HAVE_ONE_FIELD = "Array init must have at least one element";
	
	public static final String ARRAY_SELECT_MUST_HAVE_SELECTION = "Element select must have at least one element (how did we even get here?)";
	
	public static final String ARRAY_SELECTION_HAS_TO_BE_OF_TYPE = "Selection has to be of type INT, actual %s";
	
	public static final String ARRAY_ELEMENTS_MUST_HAVE_SAME_TYPE = "Structure init elements have to have same type: %s vs %s";
	
	public static final String ARRAY_OUT_OF_BOUNDS = "Array out of bounds: %s, type: %s";
	
	public static final String CAN_ONLY_SELECT_ONCE_FROM_POINTER_OR_VOID = "Can only select once from pointer or void type";
	
	public static final String CAN_ONLY_SELECT_FROM_VARIABLE_REF = "Can only select from variable reference";
	
	public static final String EXPECTED_IDREF_ACTUAL = "Expected IDRef, got %s";
	
	public static final String MULTIPLE_ELSE_STATEMENTS = "If Statement can only have one else statement";
	
	public static final String CAN_ONLY_BREAK_OUT_OF_LOOP = "Can only break out of the scope of a loop";
	
	public static final String CAN_ONLY_CONTINUE_IN_LOOP = "Can only continue in the scope of a loop";
	
	public static final String SID_DISABLED_NO_INSTANCEOF = "SID headers are disabled, instanceof is not available";
	
	public static final String SID_DISABLED_NO_INTERFACES = "SID headers are disabled, interfaces are not available";
	
	public static final String FIELD_NOT_IN_STRUCT = "The selected field %s in the structure %s does not exist";
	
	public static final String PREDICATE_IS_ANONYMOUS = "Unsafe operation, predicate '%s' is anonymous, %s";
	
	public static final String USING_IMPLICIT_ANONYMOUS_TYPE = "Using implicit anonymous type %s, %s";
	
	public static final String CAN_ONLY_GET_ADDRESS_OF_VARIABLE_REF_OR_ARRAY_SELECT = "Can only get address of variable reference or array select";
	
	public static final String TYPE_CANNOT_BE_ALIGNED_TO =  "%s cannot be aligned to %s";
	
	public static final String DIRECT_ASM_HAS_NO_OUTPUTS = "Direct ASM Operation has no explicit outputs, %s";
	
	public static final String FAILED_TO_CHECK_PARAMETER = "Failed to check parameter %d or function call %s";
	
	
			/* ---< PROVISO RELATED >--- */
	public static final String NO_MAPPING_EQUAL_TO_GIVEN_MAPPING = "No mapping is equal to the given mapping!";

	public static final String RECIEVED_MAPPING_LENGTH_NOT_EQUAL = "Recieved proviso mapping length is not equal to expected length, expected %d, but got %d";

	public static final String MISSMATCHING_NUMBER_OF_PROVISOS = "Missmatching number of provided provisos, expected %d, but got %d";
	
	public static final String MISSING_PROVISOS = "Operation '%s' is mission proviso types, %s";
	
	public static final String MISSMATCHING_NUMBER_OF_PROVISOS_EXTENSION = "Incorrect number of proviso for extension %s, expected %d, got %d";
	
	public static final String NON_PROVISO_TYPE_IN_HEADER = "Found non proviso type in proviso header: %s";
	
	public static final String IMPLEMENTED_FUNCTION_MISSING = "Implemented function '%s' from interface '%s' is missing";
	
	public static final String FUNCTION_MISSING_REQUIRED_PROVISOS = "Function '%s' in struct '%s' is missing provisos: %s";
	
	public static final String CANNOT_MAP_TYPE_TO_PROVISO = "Cannot map %s to %s";
	
	public static final String MULTIPLE_AUTO_MAPS_FOR_PROVISO = "Multiple auto-maps for proviso '%s': %s, provided by arg %d vs %s, provided by arg %d";
	
	public static final String CANNOT_AUTO_MAP_PROVISO = "Cannot auto-map proviso '%s', not used by parameter";
	
	public static final String ATTEMPTED_TO_GET_WORDSIZE_OF_PROVISO_WITHOUT_CONTEXT = "INTERNAL : Attempted to get word size of PROVISO %s without context!";
	
	public static final String CANNOT_FREE_CONTEXTLESS_PROVISO = "Cannot free contextless proviso %s at %s";
	
	public static final String CAN_ONLY_APPLY_TO_IDREF = "Can only apply to id reference";
	
	public static final String CAN_ONLY_APPLY_TO_PRIMITIVE = "Can only be applied to primitive types";
	
	public static final String CAN_ONLY_APPLY_TO_PRIMITIVE_OR_POINTER = "Can only be applied to primitive or pointer, actual %s";
	
	public static final String PROVISO_ARE_PROVIDED_BY_PREDICATE = "Proviso for inline call are provided by predicate '%s', cannot provide proviso at this location";
			
	
			/* ---< SCANNING >--- */
	public static final String BAD_END_STATE = "Bad Syntax, Lexer finished in state: %s";
	
	public static final String BAD_HEX_LITERAL = "Bad HEX literal, %s";
	
	public static final String BAD_BIN_LITERAL = "Bad BIN literal, %s";
	
	
			/* ---< PARSING >--- */
	public static final String TOKENS_ARE_NULL = "SNIPS_PARSE -> Tokens are null!";
	
	public static final String CHECK_FOR_MISSPELLED_TYPES = "Got '%s', check for misspelled types or tokens, %s";
	
	public static final String EXPECTED_STRUCT_TYPE = "Expected STRUCT type, got %s";
	
	public static final String UNKNOWN_ENUM = "Unknown enum type: %s, %s";
			
	public static final String UNKNOWN_ENUM_FIELD = "The expression '%s' is not a known field of the enum %s, %s";
	
	public static final String UNKNOWN_STRUCT_OR_ENUM_OR_INTERFACE = "Unknown struct, interface or enum type '%s', %s";
	
	public static final String MULTIPLE_MATCHES_FOR_STRUCT_TYPE = "Multiple matches for struct type '%s': %s. Ensure namespace path is explicit and correct, %s";
	
	public static final String MULTIPLE_MATCHES_FOR_ENUM_TYPE = "Multiple matches for enum type '%s': %s. Ensure namespace path is explicit and correct, %s";
	
	
			/* ---< PRE-PROCESSING >--- */
	public static final String CANNOT_FLATTEN = "Cannot flatten %s";
	
	public static final String CANNOT_RESOLVE_IMPORT = "PRE0 -> Cannot resolve import %s, %s";
	
	
			/* ---< CODE GENERATION >--- */
	public static final String UNABLE_TO_POP_X_WORDS = "Unable to pop %d Words from the stack, could only pop %d";
	
	public static final String CANNOT_PATCH_NON_PATCHABLE_IMM_OP = "Cannot patch non-patchable imm operand!";
	
	public static final String NO_INJECTION_CAST_AVAILABLE = "No injection cast available for %s";
	
	public static final String FUNCTION_UNDEFINED_AT_THIS_POINT = "Function %s is undefined at this point, %s";
	
	public static final String OPERATION_NOT_IMPLEMENTED = "Operation not implemented!";
	
	public static final String CANNOT_CHECK_REFERENCES = "Cannot check references for %s";
	
	public static final String UNKNOWN_WATCHPOINT_TYPE = "Unknown watchpoint type %s, %s";
	
	
			/* ---< INTERNAL >--- */
	public static final String UNKNOWN_MODIFIER = "Unknown Modifier '%s', defaulting to SHARED";
	
	public static final String CANNOT_GET_SOURCE_CODE_REPRESENTATION = "Cannot get source code representation of type %s";
	
	public static final String CANNOT_SET_VALUE_OF_TYPE = "Cannot set value of type %s";
	
}
