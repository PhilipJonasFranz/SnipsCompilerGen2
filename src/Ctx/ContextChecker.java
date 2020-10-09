package Ctx;

import java.util.*;

import Exc.CTX_EXC;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.*;
import Imm.AST.Expression.*;
import Imm.AST.Expression.Arith.*;
import Imm.AST.Expression.Boolean.*;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Statement.*;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.*;
import Imm.TYPE.COMPOSIT.*;
import Imm.TYPE.PRIMITIVES.*;
import Par.Token;
import Par.Token.TokenType;
import Res.Const;
import Snips.CompilerDriver;
import Util.*;
import Util.Logging.*;

public class ContextChecker {

			/* --- FIELDS --- */
	/**
	 * Set to the current Context Checker instance.
	 */
	public static ContextChecker checker;
	
	/**
	 * Set to the current progress message instance for 
	 * the context checking process.
	 */
	public static ProgressMessage progress;
	
	/**
	 * The Root AST Syntax Element.
	 */
	protected Program AST;
	
	/**
	 * Contains all functions in the AST, as well as
	 * struct nested functions. During the context checking
	 * process, this list is filled. This way the total 
	 * dependency order of the functions can be ensured.
	 */
	protected List<Function> functions = new ArrayList();
	
	/**
	 * This stack is filled with functions when a program
	 * is checked recursiveley. A new function is pushed on
	 * the stack either if a new function is checked or, within
	 * this function, a function is called. In this case, this
	 * function is pushed. Since then this function is checked,
	 * the cycle may continue.
	 */
	protected Stack<Function> currentFunction = new Stack();
	
	/**
	 * This list contains all struct nested functions. The list is used,
	 * when a function is called, to determine wether the function is a nested
	 * function or not, and thus can be accessed or not.
	 */
	protected List<Function> nestedFunctions = new ArrayList();
	
	/**
	 * This stack is filled with compound statements when
	 * a program structure is recursiveley checked. The
	 * stack is used to determine the next highest super-loop
	 * when checking break and continue statements.
	 */
	protected Stack<CompoundStatement> compoundStack = new Stack();
	
	/**
	 * This stack is filled with scopes when a program is
	 * recursiveley checked. Each scope will contain program
	 * variables that are defined in the current scope. The
	 * stack is used to localize the highest scoped variable
	 * at any point, to link origins and to check for duplicate
	 * variable names.
	 */
	protected Stack<Scope> scopes = new Stack();
	
	/**
	 * This stack is filled with exception type lists when
	 * a program is recursiveley checked. Each list corresponds
	 * to an exception scope. This is either a function scope
	 * or a scope opened by a try statement. The stack is used
	 * to determine where which exception types can be signaled
	 * and to ensure the correct 'signals' annotations are present.
	 * This is done by adding non-watched exceptions in a higher scope
	 * to the next scope until the base scope is reached. Each
	 * of the remaining exception types needs to be signaled in
	 * the function header.
	 */
	protected Stack<List<TYPE>> signalStack = new Stack();
	
	/**
	 * This stack is filled with syntax elements when a program
	 * is recursiveley checked. Each syntax element is either a
	 * function or a try-statement. The stack is used to determine
	 * the highest scoped watchpoint. When checking inline or function
	 * calls or a signal statement, the watchpoint is set to the 
	 * highest scoped watchpoint.
	 */
	protected Stack<SyntaxElement> watchpointStack = new Stack();
	
	/**
	 * This statement is always set to the statement that is 
	 * currently being checked. When checking IDRefs, the last
	 * statement may be determined, so that during ASM casting,
	 * at one point where the IDRef is not being used anymore, 
	 * the register can be freed.
	 */
	protected Statement currentStatement = null;
	
	/**
	 * Contains a collection of all declarations in the AST.
	 */
	protected List<Declaration> declarations = new ArrayList();
	
	/**
	 * All generated (WARN) messages. These messages will
	 * be flushed after the context checking process is over.
	 */
	protected List<Message> messages = new ArrayList();
	
	/**
	 *  Contains the structs that have no extensions.
	 */
	protected List<StructTypedef> tLStructs = new ArrayList();
	
	
			/* --- CONSTRUCTORS --- */
	public ContextChecker(SyntaxElement AST, ProgressMessage progress) {
		this.AST = (Program) AST;
		ContextChecker.progress = progress;
		checker = this;
	}
	
	
			/* --- AST Check Methods --- */
	public TYPE check() throws CTX_EXC {
		this.checkProgram((Program) AST);
		
		/* Assigns each struct typedef a unique SID */
		this.setupSIDs();
		
		/* Flush warn messages */
		for (Message m : this.messages) m.flush();
		
		return null;
	}
	
	public void checkProgram(Program p) throws CTX_EXC {
		scopes.push(new Scope(null));
		
		/* Add global reserved declarations */
		scopes.peek().addDeclaration(CompilerDriver.HEAP_START);
		
		boolean gotMain = false;
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Function) {
				Function f = (Function) s;
				
				if (f.path.build().equals(Const.MAIN)) gotMain = true;
				
				/* Check main function as entrypoint, if a function is called, 
				 * context is provided and then checked */
				if (f.path.build().equals(Const.MAIN) && !f.provisosTypes.isEmpty()) 
					throw new CTX_EXC(f.getSource(), Const.MAIN_CANNOT_HOLD_PROVISOS);
				
				/* Check for duplicate function name */
				for (Function f0 : p.functions) {
					if (f0.path.build().equals(f.path.build())) 
						throw new CTX_EXC(f.getSource(), Const.DUPLICATE_FUNCTION_NAME, f.path.build());
				}
				
				/* 
				 * Add the function to the function pool here already, 
				 * since through recursion the same function may be called. 
				 */
				this.functions.add(f);
				
				/* Check only functions with no provisos, proviso functions will be hot checked. */
				if (f.provisosTypes.isEmpty()) f.check(this);
			}
			else if (s instanceof Namespace) {
				p.namespaces.add((Namespace) s);
				s.check(this);
			}
			else s.check(this);
			
			if (progress != null) 
				progress.incProgress((double) i / p.programElements.size());
		}
		
		/* Did not find a function that matches main signature */
		if (!gotMain) 
			throw new CTX_EXC(p.getSource(), Const.MISSING_MAIN_FUNCTION);
		
		if (progress != null) 
			progress.finish();

		/* If the declarations is not used, add itself to the free list. */
		for (Declaration d : declarations) 
			if (d.last != null) 
				d.last.free.add(d);
		
		p.print(0, true);
	}
	
	public TYPE checkFunction(Function f) throws CTX_EXC {
		/* Proviso Types are already set at this point */
		
		scopes.push(new Scope(scopes.peek()));
		
		this.signalStack.push(new ArrayList());
		this.watchpointStack.push(f);
		
		if (f.path.build().equals(Const.MAIN) && f.signals()) 
			throw new CTX_EXC(f.getSource(), Const.MAIN_CANNOT_SIGNAL);
		
		/* Check for duplicate function parameters */
		if (f.parameters.size() > 1) {
			for (int i = 0; i < f.parameters.size(); i++) {
				for (int a = i + 1; a < f.parameters.size(); a++) {
					if (f.parameters.get(i).path.build().equals(f.parameters.get(a).path.build())) 
						throw new CTX_EXC(f.getSource(), Const.DUPLICATE_PARAMETER_NAME, f.parameters.get(i).path.build(), f.path.build());
				}
			}
		}
		
		for (Declaration d : f.parameters) {
			d.check(this);
			if (d.getType().getCoreType() instanceof VOID && !CompilerDriver.disableWarnings) 
				messages.add(new Message(String.format(Const.UNCHECKED_TYPE_VOID, new VOID().typeString(), d.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		if (f.signals() && f.signalsTypes.isEmpty()) 
			throw new CTX_EXC(f.getSource(), Const.MUST_SIGNAL_AT_LEAST_ONE_TYPE);
		
		/* Check body */
		this.currentFunction.push(f);
		for (Statement s : f.body) {
			currentStatement = s;
			s.check(this);
		}
		this.currentFunction.pop();
		
		/* Check for signaled types that are not thrown */
		for (TYPE t : f.signalsTypes) {
			boolean contains = false;
			for (int i = 0; i < this.signalStack.peek().size(); i++) 
				contains |= this.signalStack.peek().get(i).isEqual(t);
			
			if (!contains) 
				messages.add(new Message(String.format(Const.WATCHED_EXCEPTION_NOT_THROWN_IN_FUNCTION, t.provisoFree().typeString(), f.path.build(), f.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		/* Remove function signaled exceptions */
		for (TYPE t : f.signalsTypes) {
			for (int i = 0; i < this.signalStack.peek().size(); i++) {
				if (this.signalStack.peek().get(i).isEqual(t)) {
					this.signalStack.peek().remove(i);
					break;
				}
			}
		}
		
		/* Exception types are not watched or signaled */
		if (!this.signalStack.peek().isEmpty()) {
			String unwatched = "";
			for (TYPE t : this.signalStack.peek()) unwatched += t.provisoFree().typeString() + ", ";
			unwatched = unwatched.substring(0, unwatched.length() - 2);
			throw new CTX_EXC(f.getSource(), Const.UNWATCHED_EXCEPTIONS_FOR_FUNCTION, f.path.build(), unwatched);
		}
		
		this.watchpointStack.pop();
		this.signalStack.pop();
		scopes.pop();
		
		return f.getReturnType().clone();
	}
	
	public TYPE checkStructTypedef(StructTypedef e) throws CTX_EXC {
		
		/* Make sure at least one field is in the struct */
		if (e.getFields().isEmpty())
			throw new CTX_EXC(e.getSource(), Const.STRUCT_TYPEDEF_MUST_CONTAIN_FIELD);
		
		for (Function f : e.functions) {
			if (f.modifier != MODIFIER.STATIC) {
				/* Add to a pool of nested functions */
				this.nestedFunctions.add(f);
			 
				/* Check for duplicate function name */
				for (Function f0 : this.functions) {
					if (f0.path.build().equals(f.path.build()))
						throw new CTX_EXC(f.getSource(), Const.DUPLICATE_FUNCTION_NAME, f.path.build());
				}
			}
			
			/* 
			 * Add the function to the function pool here already, 
			 * since through recursion the same function may be called. 
			 */
			this.functions.add(f);
			
			if (f.provisosTypes.isEmpty()) 
				f.check(this);
			
			if (f.modifier != MODIFIER.STATIC) {
				/* Check if all required provisos are present */
				List<TYPE> missing = new ArrayList();
				
				for (TYPE t : e.proviso) 
					missing.add(t.clone());
				
				for (int i = 0; i < missing.size(); i++) {
					for (int a = 0; a < f.provisosTypes.size(); a++) {
						if (((PROVISO) missing.get(i)).placeholderName.equals(((PROVISO) f.provisosTypes.get(a)).placeholderName)) {
							missing.remove(i);
							i--;
							break;
						}
					}
				}
				
				/* 
				 * There are provisos missing and the function is not inherited, throw an error. 
				 * If the function is inherited, the same check has been done to the function by
				 * the parent, so we dont need to check it here.
				 */
				if (!missing.isEmpty() && !e.inheritedFunctions.contains(f)) {
					String s = "";
					for (TYPE t : missing) s += t.typeString() + ", ";
					s = s.substring(0, s.length() - 2);
					
					throw new CTX_EXC(e.getSource(), Const.FUNCTION_MISSING_REQUIRED_PROVISOS, f.path.getLast(), e.path.build(), s);
				}
			}
		}
		
		Optional<TYPE> opt = e.proviso.stream().filter(x -> !(x instanceof PROVISO)).findFirst();
		
		if (opt.isPresent())
			throw new CTX_EXC(e.getSource(), Const.NON_PROVISO_TYPE_IN_HEADER, opt.get().provisoFree().typeString());
		
		if (e.extension != null && e.extension.proviso.size() != e.extProviso.size()) 
			throw new CTX_EXC(e.getSource(), Const.MISSMATCHING_NUMBER_OF_PROVISOS_EXTENSION, e.extension.self.provisoFree().typeString(), e.extension.proviso.size(), e.extProviso.size());
		
		/* 
		 * Add to topLevelStructExtenders, since this typedef is the root
		 * of an extension tree, and is used to assign SIDs.
		 */
		if (e.extension == null)
			this.tLStructs.add(e);
		
		/* Set the declarations in the struct type */
		return new VOID();
	}
	
	public TYPE checkInterfaceTypedef(InterfaceTypedef e) throws CTX_EXC {
		for (Function f : e.functions) {
			if (f.modifier != MODIFIER.STATIC) {
				/* Add to a pool of nested functions */
				this.nestedFunctions.add(f);
			 
				/* Check for duplicate function name */
				for (Function f0 : this.functions) {
					if (f0.path.build().equals(f.path.build()))
						throw new CTX_EXC(f.getSource(), Const.DUPLICATE_FUNCTION_NAME, f.path.build());
				}
			}
			
			/* 
			 * Add the function to the function pool here already, 
			 * since through recursion the same function may be called. 
			 */
			this.functions.add(f);
			
			if (f.provisosTypes.isEmpty()) 
				f.check(this);
			
			if (f.modifier != MODIFIER.STATIC) {
				/* Check if all required provisos are present */
				List<TYPE> missing = new ArrayList();
				
				for (TYPE t : e.proviso) 
					missing.add(t.clone());
				
				for (int i = 0; i < missing.size(); i++) {
					for (int a = 0; a < f.provisosTypes.size(); a++) {
						if (((PROVISO) missing.get(i)).placeholderName.equals(((PROVISO) f.provisosTypes.get(a)).placeholderName)) {
							missing.remove(i);
							i--;
							break;
						}
					}
				}
				
				/* 
				 * There are provisos missing and the function is not inherited, throw an error. 
				 * If the function is inherited, the same check has been done to the function by
				 * the parent, so we dont need to check it here.
				 */
				if (!missing.isEmpty()) {
					String s = "";
					for (TYPE t : missing) s += t.typeString() + ", ";
					s = s.substring(0, s.length() - 2);
					
					throw new CTX_EXC(e.getSource(), Const.FUNCTION_MISSING_REQUIRED_PROVISOS, f.path.getLast(), e.path.build(), s);
				}
			}
		}
		
		Optional<TYPE> opt = e.proviso.stream().filter(x -> !(x instanceof PROVISO)).findFirst();
		
		if (opt.isPresent())
			throw new CTX_EXC(e.getSource(), Const.NON_PROVISO_TYPE_IN_HEADER, opt.get().provisoFree().typeString());
		
		/* Set the declarations in the struct type */
		return new VOID();
	}
	
	public TYPE checkSignal(SignalStatement e) throws CTX_EXC {
		TYPE exc = e.exceptionInit.check(this);
		e.watchpoint = this.watchpointStack.peek();
		
		/* Add to signal stack */
		if (!this.signalStackContains(exc)) 
			this.signalStack.peek().add(exc);
		
		return new VOID();
	}
	
	public TYPE checkTryStatement(TryStatement e) throws CTX_EXC {
		this.scopes.push(new Scope(this.scopes.peek()));
		this.signalStack.push(new ArrayList());
		
		/* If exception is thrown that is not watched by this statement, relay to this watchpoint */
		e.watchpoint = this.watchpointStack.peek();
		
		/* Setup new watchpoint target */
		this.watchpointStack.push(e);
		
		for (Statement s : e.body) {
			currentStatement = s;
			s.check(this);
		}
		
		for (int i = 0; i < e.watchpoints.size(); i++) {
			for (int a = i + 1; a < e.watchpoints.size(); a++) {
				if (e.watchpoints.get(i).watched.getType().isEqual(e.watchpoints.get(a).watched.getType())) 
					throw new CTX_EXC(e.getSource(), Const.MULTIPLE_WATCHPOINTS_FOR_EXCEPTION, e.watchpoints.get(i).watched.getType().provisoFree().typeString());
			}
		}
		
		this.scopes.pop();
		this.watchpointStack.pop();
		
		for (WatchStatement w : e.watchpoints) {
			w.check(this);
			
			for (TYPE t : this.signalStack.peek()) {
				if (t.isEqual(w.watched.getType())) {
					w.hasTarget = true;
					this.signalStack.peek().remove(t);
					break;
				}
			}
			
			if (!w.hasTarget) 
				messages.add(new Message(String.format(Const.WATCHED_EXCEPTION_NOT_THROWN_IN_TRY, w.watched.getType().provisoFree().typeString(), e.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		/* Add all unwatched to the previous signal level */
		List<TYPE> unwatched = this.signalStack.pop();
		e.unwatched = unwatched;
		for (TYPE t : unwatched) {
			boolean contains = this.signalStack.peek().stream().filter(x -> x.isEqual(t)).count() > 0;
			if (!contains) this.signalStack.peek().add(t);
		}
		
		return new VOID();
	}
	
	public TYPE checkWatchStatement(WatchStatement e) throws CTX_EXC {
		this.scopes.push(new Scope(this.scopes.peek()));
		
		e.watched.check(this);
		
		for (Statement s : e.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		return new VOID();
	}
	
	public TYPE checkStructureInit(StructureInit e) throws CTX_EXC {
		e.setType(e.structType);
		
		StructTypedef extension = e.structType.getTypedef().extension;
		
		boolean covered = false;
		
		/* Make sure the correct number of provisos are supplied. If the provided provisos are empty, an auto mapping still can be created. */
		if (e.structType.proviso.size() != e.structType.getTypedef().proviso.size() && !e.structType.proviso.isEmpty())
			throw new CTX_EXC(e.getSource(), Const.MISSMATCHING_NUMBER_OF_PROVISOS, e.structType.getTypedef().proviso.size(), e.structType.proviso.size());
		
		/* Check if the first element of the call is the super constructor */
		if (e.elements.get(0) instanceof InlineCall) {
			InlineCall call = (InlineCall) e.elements.get(0);
			
			/* Calls to super constructor */
			if (call.path.build().equals("super")) {
				
				/* Calls to super, but no extension */
				if (e.structType.getTypedef().extension == null)
					throw new CTX_EXC(e.getSource(), Const.CANNOT_INVOKE_SUPER_NO_EXTENSION, e.structType.typeString());
				
				/* Search for constructor of extension */
				for (Function f : e.structType.getTypedef().extension.functions) 
					if (f.path.build().endsWith("create") && f.modifier == MODIFIER.STATIC) 
						/* Found static constructor, switch out 'super' with path to constructor */
						call.path = f.path.clone();
				
				/* No super constructor was found */
				if (call.path.build().equals("super"))
					throw new CTX_EXC(e.getSource(), Const.CANNOT_INVOKE_SUPER_NO_CONSTRUCTOR, e.structType.getTypedef().extension.self.typeString());
				else
					covered = true;
			}
			/* Does not call directley to super constructor, but calls the super constructor manually */
			else if (call.path.path.size() > 1 && (call.path.path.get(call.path.path.size() - 2) + ".create").equals(extension.path.getLast() + ".create")) {
				covered = true;
			}
		}
		/* Creates new instance of extension manually */
		else if (extension != null && !(e.elements.get(0) instanceof TempAtom))
			covered = e.elements.get(0).check(this).isEqual(extension.self);
		
		/* No provisos were supplied, but provisos are expected. Attempt to auto map. */
		if (e.structType.proviso.isEmpty() && !e.structType.getTypedef().proviso.isEmpty()) {
			/* Attempt to find auto-provisos */
			List<TYPE> expected = new ArrayList();
			for (int i = 0; i < e.structType.getNumberOfFields(); i++) 
				expected.add(e.structType.getFieldNumberDirect(i).getRawType());
			
			List<TYPE> provided = new ArrayList();
			for (Expression x : e.elements) 
				provided.add(x.check(this));
			
			e.structType.proviso = this.autoProviso(e.structType.getTypedef().proviso, expected, provided, e.getSource());
		}
		
		/* Map the current function provisos to the resulting struct type */
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(e.getType(), this.currentFunction.peek().provisosTypes);
		
		/* 
		 * It it is not a temp atom, check the first element here so in the 
		 * line after this line getType() can be called safely 
		 */
		if (!(e.elements.get(0) instanceof TempAtom))
			e.elements.get(0).check(this);
		
		/* Check that type that is covering is a struct type */
		if (extension != null && covered && !(e.elements.get(0).getType() instanceof STRUCT)) 
				throw new CTX_EXC(e.getSource(), Const.CAN_ONLY_COVER_WITH_STRUCT, e.elements.get(0).getType().typeString());
		
		/* Absolute placeholder case */
		if (e.elements.size() == 1 && e.elements.size() != e.structType.getNumberOfFields() && e.elements.get(0) instanceof TempAtom && ((TempAtom) e.elements.get(0)).base == null) {
			TempAtom a = (TempAtom) e.elements.get(0);
			a.inheritType = e.structType;
			a.check(this);
		}
		/* Covered parameter case */
		else if (covered) {
			e.hasCoveredParam = true;
			
			int expected = e.structType.getTypedef().getFields().size() - extension.getFields().size() + 1;
			
			/* Make sure the correct number or parameters is supplied */
			if (e.elements.size() != expected)
				throw new CTX_EXC(e.getSource(), Const.MISSMATCHING_ARGUMENT_NUMBER, expected, e.elements.size());
			
			for (int i = 1; i < e.elements.size(); i++) {
				TYPE strType = e.structType.getField(e.structType.getTypedef().getFields().get(i + extension.getFields().size() - 1).path).getType();
				
				/* Single placeholder case */
				if (e.elements.get(i) instanceof TempAtom) {
					TempAtom a = (TempAtom) e.elements.get(i);
					a.inheritType = strType;
				}
				
				TYPE valType = e.elements.get(i).check(this);
				
				if (e.elements.get(i) instanceof StructureInit) {
					StructureInit init = (StructureInit) e.elements.get(i);
					init.isTopLevelExpression = false;
				}
				
				if (!valType.isEqual(strType) && !(e.elements.get(i) instanceof TempAtom)) {
					if (valType instanceof POINTER || strType instanceof POINTER) 
						CompilerDriver.printProvisoTypes = true;
					
					throw new CTX_EXC(e.getSource(), Const.ARGUMENT_DOES_NOT_MATCH_STRUCT_FIELD_TYPE, i + 1, valType.provisoFree().typeString(), strType.provisoFree().typeString());
				}
			}
		}
		else {
			/* Make sure the correct number or parameters is supplied, at this point we can strictly compare */
			if (e.elements.size() != e.structType.getTypedef().getFields().size() && e.elements.size() > 1) 
				throw new CTX_EXC(e.getSource(), Const.MISSMATCHING_ARGUMENT_NUMBER, e.structType.getTypedef().getFields().size(), e.elements.size());
			
			/* Make sure that all field types are equal to the expected types */
			for (int i = 0; i < e.elements.size(); i++) {
				
				TYPE strType = e.structType.getField(e.structType.getTypedef().getFields().get(i).path).getType();
				
				/* Single placeholder case */
				if (e.elements.get(i) instanceof TempAtom) {
					TempAtom a = (TempAtom) e.elements.get(i);
					a.inheritType = strType;
				}
				
				TYPE valType = e.elements.get(i).check(this);
				
				if (e.elements.get(i) instanceof StructureInit) {
					StructureInit init = (StructureInit) e.elements.get(i);
					init.isTopLevelExpression = false;
				}
				
				if (!valType.isEqual(strType) && !(e.elements.get(i) instanceof TempAtom)) {
					if (valType instanceof POINTER || strType instanceof POINTER) 
						CompilerDriver.printProvisoTypes = true;
					
					throw new CTX_EXC(e.getSource(), Const.ARGUMENT_DOES_NOT_MATCH_STRUCT_FIELD_TYPE, i + 1, valType.provisoFree().typeString(), strType.provisoFree().typeString());
				}
			}
		}
		
		/* Struct may have modifier restrictions */
		this.checkModifier(e.structType.getTypedef().modifier, e.structType.getTypedef().path, e.getSource());
		
		return e.getType();
	}
	
	public TYPE checkStructSelect(StructSelect e) throws CTX_EXC {
		/* 
		 * This method links origins and types manually. This is partially due to the fact
		 * that for example, a field in a struct does not count as a field identifier in the
		 * current scope when referencing it. If it was to the check() method, it would report
		 * an duplicate identifier. 
		 */
		
		TYPE type = null;
		
		/* Get Base Type */
		if (e.selector instanceof IDRef) {
			IDRef sel = (IDRef) e.selector;
			/* Link automatically, identifier is local */
			type = sel.check(this);
		}
		else if (e.selector instanceof TypeCast) {
			TypeCast tc = (TypeCast) e.selector;
			
			if (!(tc.expression instanceof IDRef))
				throw new CTX_EXC(e.getSource(), Const.BASE_MUST_BE_VARIABLE_REFERENCE);
			
			type = tc.check(this);
		}
		else if (e.selector instanceof ArraySelect) {
			ArraySelect arr = (ArraySelect) e.selector;
			type = arr.check(this);
		}
		else throw new CTX_EXC(e.getSource(), Const.BASE_MUST_BE_VARIABLE_REFERENCE);
		
		if (type == null) 
			throw new CTX_EXC(e.getSource(), Const.CANNOT_DETERMINE_TYPE);
		
		/* First selections does deref, this means that the base must be a pointer */
		if (e.deref) {
			if (type instanceof POINTER) {
				POINTER p0 = (POINTER) type;
				type = p0.targetType;
			}
			else throw new CTX_EXC(e.selector.getSource(), Const.CANNOT_DEREF_NON_POINTER, type.provisoFree().typeString());
		}
		
		Expression selection = e.selection;
		
		while (true) {
			selection.setType(type.clone());
			
			if (type instanceof STRUCT) {
				STRUCT struct = (STRUCT) type;
					
				if (selection instanceof StructSelect) {
					StructSelect sel0 = (StructSelect) selection;
					
					if (sel0.selector instanceof IDRef) {
						IDRef ref = (IDRef) sel0.selector;
						
						type = findAndLinkField(struct, ref);
						
						if (sel0.deref) {
							if (!(type instanceof POINTER)) 
								throw new CTX_EXC(selection.getSource(), Const.CANNOT_DEREF_NON_POINTER, type.provisoFree().typeString());
							else {
								/* Unwrap pointer, selection does dereference */
								POINTER p0 = (POINTER) type;
								type = p0.targetType;
							}
						}
						
						ref.setType(type.clone());
					}
					else if (sel0.selector instanceof ArraySelect) {
						/* Push new scope to house the struct fields */
						this.scopes.push(new Scope(this.scopes.peek()));
						
						/* Add declarations for struct */
						for (int i = 0; i < struct.getNumberOfFields(); i++) 
							/* 
							 * Add the struct fields to the current scope, so that the select expresssion
							 * from the array select can be checked and finds the field its selecting from.
							 * The fields are added without checking for duplicates. This is not a big problem,
							 * since the same scope is instantly popped afterwards.
							 */
							this.scopes.peek().addDeclaration(struct.getFieldNumber(i), false);
						
						ArraySelect arr = (ArraySelect) sel0.selector;
						type = arr.check(this);
						
						arr.setType(type.clone());
						
						this.scopes.pop();
					}
					else throw new CTX_EXC(selection.getSource(), Const.CLASS_CANNOT_BE_SELECTOR, sel0.selector.getClass().getName());
					
					/* Next selection in chain */
					selection = sel0.selection;
				}
				else if (selection instanceof IDRef) {
					IDRef ref = (IDRef) selection;
					
					/* Last selection */
					type = findAndLinkField(struct, ref);
					
					TYPE type0 = type;
					if (type0 instanceof POINTER) 
						type0 = ((POINTER) type0).targetType;
					
					ref.setType(type.clone());
					
					break;
				}
				else if (selection instanceof ArraySelect) {
					/* Push new scope to house the struct fields */
					this.scopes.push(new Scope(this.scopes.peek()));

					/* Add declarations for struct */
					for (int i = 0; i < struct.getNumberOfFields(); i++) 
						/* 
						 * Add the struct fields to the current scope, so that the select expresssion
						 * from the array select can be checked and finds the field its selecting from.
						 * The fields are added without checking for duplicates. This is not a big problem,
						 * since the same scope is instantly popped afterwards.
						 */
						this.scopes.peek().addDeclaration(struct.getFieldNumber(i), false);
					
					ArraySelect arr = (ArraySelect) selection;
					type = arr.check(this);
					
					this.scopes.pop();
					
					arr.setType(type.clone());
					
					break;
				}
				else throw new CTX_EXC(e.getSource(), Const.CLASS_CANNOT_BE_SELECTOR, selection.getClass().getName());
			}
			else throw new CTX_EXC(e.getSource(), Const.CANNOT_SELECT_FROM_NON_STRUCT, type.provisoFree().typeString());
			
		}
		
		e.setType(type.clone());
		return e.getType();
	}
	
	public TYPE checkWhileStatement(WhileStatement w) throws CTX_EXC {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (!(cond instanceof BOOL)) 
			throw new CTX_EXC(w.getSource(), Const.CONDITION_NOT_BOOLEAN);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : w.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkDoWhileStatement(DoWhileStatement w) throws CTX_EXC {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (!(cond instanceof BOOL)) 
			throw new CTX_EXC(w.getSource(), Const.CONDITION_NOT_BOOLEAN);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : w.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForStatement(ForStatement f) throws CTX_EXC {
		this.compoundStack.push(f);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		f.iterator.check(this);
		if (f.iterator.value == null) 
			throw new CTX_EXC(f.getSource(), Const.ITERATOR_MUST_HAVE_INITIAL_VALUE);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		TYPE cond = f.condition.check(this);
		if (!(cond instanceof BOOL)) 
			throw new CTX_EXC(f.getSource(), Const.CONDITION_NOT_BOOLEAN);
		
		f.increment.check(this);
		
		for (Statement s : f.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForEachStatement(ForEachStatement f) throws CTX_EXC {
		this.compoundStack.push(f);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		TYPE itType = f.iterator.check(this);
		TYPE refType = f.shadowRef.check(this);
		
		f.counter.check(this);
		f.counterRef.check(this);
		
		if (refType instanceof POINTER) {
			POINTER p = (POINTER) refType;
			
			if (!p.targetType.isEqual(itType) && !p.targetType.getCoreType().isEqual(itType))
				throw new CTX_EXC(f.getSource(), Const.POINTER_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE, p.targetType.provisoFree().typeString(), itType.provisoFree().typeString());
			
			/* Construct expression to calculate address based on address of the shadowRef, counter and the size of the type */
			Expression add = new Add(f.shadowRef, f.counterRef, f.shadowRef.getSource());
			
			/* Set as new shadowRef, will be casted during code generation */
			f.shadowRef = new Deref(add, f.shadowRef.getSource());
			f.shadowRef.check(this);
			
			if (f.range == null)
				throw new CTX_EXC(f.getSource(), Const.CANNOT_ITERATE_WITHOUT_RANGE);
			
			f.range = new Mul(f.range, new Atom(new INT("" + itType.wordsize()), new Token(TokenType.INTLIT, null, ""), f.shadowRef.getSource()), f.range.getSource());
			f.range.check(this);
		}
		else if (refType instanceof ARRAY) {
			ARRAY a = (ARRAY) refType;
			
			if (!a.elementType.isEqual(itType))
				throw new CTX_EXC(f.getSource(), Const.ARRAY_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE, a.elementType.provisoFree().typeString(), itType.provisoFree().typeString());
			
			/* Select first value from array */
			List<Expression> select = new ArrayList();
			select.add(f.counterRef);
			f.select = new ArraySelect(f.shadowRef, select, f.shadowRef.getSource());
			
			f.select.check(this);
		}
		else throw new CTX_EXC(f.getSource(), Const.ONLY_AVAILABLE_FOR_POINTERS_AND_ARRAYS, refType.provisoFree().typeString());
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		for (Statement s : f.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkIfStatement(IfStatement i) throws CTX_EXC {
		/* Since else statement is directley checked, we need to set this explicitly here */
		this.currentStatement = i;
		
		if (i.condition != null) {
			TYPE cond = i.condition.check(this);
			if (!(cond instanceof BOOL)) 
				throw new CTX_EXC(i.getSource(), Const.CONDITION_NOT_BOOLEAN);
		}
		else {
			if (i.elseStatement != null) 
				throw new CTX_EXC(i.getSource(), Const.MULTIPLE_ELSE_STATEMENTS);
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		
		for (Statement s : i.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		
		if (i.elseStatement != null) 
			i.elseStatement.check(this);
		
		return null;
	}
	
	public TYPE checkDeclaration(Declaration d) throws CTX_EXC {
		
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(d.getType(), this.currentFunction.peek().provisosTypes);
		
		/* Set self as last, implicitly unused */
		d.last = d;
		
		if (d.value != null) {
			/* Apply parameter type if atom is placeholder */
			if (d.value instanceof TempAtom) {
				TempAtom a = (TempAtom) d.value;
				a.inheritType = d.getType();
			}
			
			TYPE t = d.value.check(this);
			
			if (t instanceof FUNC) {
				FUNC d0 = (FUNC) d.getType();
				FUNC t0 = (FUNC) t;
				
				if (!t.isEqual(d.getType())) 
					throw d0.getInequality(t0, d.getSource());
				
				/* 
				 * If func head provided by declaration is anonymous, 
				 * replace with func head of value. May still be null.
				 */
				if (d0.funcHead == null) {
					d0.funcHead = t0.funcHead;
					d0.proviso = t0.proviso;
				}
				
				/* Wont be able to check, set to null */
				d.last = null;
			}
			
			if (!t.isEqual(d.getType()) || d.getType().wordsize() != t.wordsize()) {
				if (t instanceof POINTER || d.getType() instanceof POINTER) 
					CompilerDriver.printProvisoTypes = true;
				
				if (this.checkPolymorphViolation(t, d.getType())) 
					throw new CTX_EXC(d.getSource(), Const.POLY_ONLY_VIA_POINTER, t.provisoFree().typeString(), d.getType().provisoFree().typeString());
				
				throw new CTX_EXC(d.getSource(), Const.EXPRESSION_TYPE_DOES_NOT_MATCH_DECLARATION, t.provisoFree().typeString(), d.getType().provisoFree().typeString());
			}
		}
		
		/* When function type, can not only collide with over vars, but also function names */
		if (d.getType() instanceof FUNC) {
			for (Function f0 : this.functions) {
				if (f0.path.getLast().equals(d.path.getLast()) && f0.path.path.size() == 1) 
					throw new CTX_EXC(d.getSource(), Const.PREDICATE_SHADOWS_FUNCTION, d.path.build());
			}
		}
		
		Message m = scopes.peek().addDeclaration(d);
		if (m != null) this.messages.add(m);
		
		this.declarations.add(d);
		
		/* No need to set type here, is done while parsing */
		return d.getType();
	}
	
	public TYPE checkAssignment(Assignment a) throws CTX_EXC {
		TYPE targetType = a.lhsId.check(this);
		
		if (a.lhsId instanceof PointerLhsId) targetType = new POINTER(targetType);
		
		NamespacePath path = a.lhsId.getFieldName();
		
		Declaration dec = null;
		if (path != null) scopes.peek().getField(path, a.getSource());
		a.origin = dec;
		
		/* Override for placeholder atom */
		if (a.value instanceof TempAtom) {
			TempAtom atom = (TempAtom) a.value;
			atom.inheritType = targetType;
		}
		
		TYPE t = a.value.check(this);
		TYPE ctype = t;
		
		/* If target type is a pointer, only the core types have to match */
		if (!targetType.isEqual(t) || (targetType.wordsize() != t.wordsize() && a.lhsId instanceof SimpleLhsId)) {
			if (targetType instanceof POINTER || t instanceof POINTER) 
				CompilerDriver.printProvisoTypes = true;
			
			if (this.checkPolymorphViolation(t, targetType)) 
				throw new CTX_EXC(a.getSource(), Const.VARIABLE_DOES_NOT_MATCH_EXPRESSION_POLY, t.provisoFree().typeString(), targetType.provisoFree().typeString());
			
			throw new CTX_EXC(a.getSource(), Const.EXPRESSION_TYPE_DOES_NOT_MATCH_VARIABLE, t.provisoFree().typeString(), targetType.provisoFree().typeString());
		}
		
		if (a.assignArith != ASSIGN_ARITH.NONE) {
			if (t.wordsize() > 1) 
				throw new CTX_EXC(a.getSource(), Const.ONLY_APPLICABLE_FOR_ONE_WORD_TYPE);
			
			if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN || a.assignArith == ASSIGN_ARITH.ORR_ASSIGN || a.assignArith == ASSIGN_ARITH.BIT_XOR_ASSIGN) {
				if (!(ctype instanceof BOOL)) 
					throw new CTX_EXC(a.getSource(), Const.EXPRESSIONT_TYPE_NOT_APPLICABLE_FOR_BOOL, t.provisoFree().typeString());
			}
			else if (a.assignArith != ASSIGN_ARITH.NONE) {
				if (!(ctype instanceof INT)) 
					throw new CTX_EXC(a.getSource(), Const.EXPRESSIONT_TYPE_NOT_APPLICABLE_FOR_ASSIGN_OP, t.provisoFree().typeString());
			}
		}
		
		a.lhsId.expressionType = t;
		return null;
	}
	
	public TYPE checkBreak(BreakStatement b) throws CTX_EXC {
		if (this.compoundStack.isEmpty()) 
			throw new CTX_EXC(b.getSource(), Const.CAN_ONLY_BREAK_OUT_OF_LOOP);
		else b.superLoop = this.compoundStack.peek();
		return null;
	}
	
	public TYPE checkContinue(ContinueStatement c) throws CTX_EXC {
		if (this.compoundStack.isEmpty()) 
			throw new CTX_EXC(c.getSource(), Const.CAN_ONLY_CONTINUE_IN_LOOP);
		else c.superLoop = this.compoundStack.peek();
		return null;
	}
	
	public TYPE checkSwitchStatement(SwitchStatement s) throws CTX_EXC {
		if (!(s.condition instanceof IDRef)) 
			throw new CTX_EXC(s.condition.getSource(), Const.SWITCH_COND_MUST_BE_VARIABLE);
		
		TYPE type = s.condition.check(this);
		if (!(type instanceof PRIMITIVE)) 
			throw new CTX_EXC(s.condition.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE);
		
		if (s.defaultStatement == null) 
			throw new CTX_EXC(s.getSource(), Const.MISSING_DEFAULT_STATEMENT);
		
		for (CaseStatement c : s.cases) c.check(this);
		s.defaultStatement.check(this);
		return null;
	}
	
	public TYPE checkCaseStatement(CaseStatement c) throws CTX_EXC {
		TYPE type = c.condition.check(this);
		
		if (!type.isEqual(c.superStatement.condition.getType())) 
			throw new CTX_EXC(c.condition.getSource(), Const.EXPRESSION_TYPE_DOES_NOT_MATCH_VARIABLE, type.provisoFree().typeString(), c.superStatement.condition.getType().provisoFree().typeString());
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : c.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();
		return null;
	}
	
	public TYPE checkDefaultStatement(DefaultStatement d) throws CTX_EXC {
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : d.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();
		return null;
	}
	
	public TYPE checkReturn(ReturnStatement r) throws CTX_EXC {
		if (r.value != null) {
			TYPE t = r.value.check(this);

			this.currentFunction.peek().hasReturn = true;
			
			/* There was a return statement with no return value previously */
			if (this.currentFunction.peek().noReturn != null) 
				throw new CTX_EXC(this.currentFunction.peek().noReturn.getSource(), Const.NO_RETURN_VALUE, this.currentFunction.peek().getReturnType().provisoFree().typeString());
			
			if (t.isEqual(this.currentFunction.peek().getReturnType())) 
				return t;
			else throw new CTX_EXC(r.getSource(), Const.RETURN_TYPE_DOES_NOT_MATCH, t.provisoFree().typeString(), this.currentFunction.peek().getReturnType().provisoFree().typeString());
		}
		else {
			if (this.currentFunction.peek().hasReturn) 
				throw new CTX_EXC(r.getSource(), Const.NO_RETURN_VALUE, this.currentFunction.peek().getReturnType().provisoFree().typeString());
			else 
				this.currentFunction.peek().noReturn = r;
			
			if (!(currentFunction.peek().getReturnType() instanceof VOID)) 
				throw new CTX_EXC(r.getSource(), Const.RETURN_TYPE_DOES_NOT_MATCH, new VOID().typeString(), currentFunction.peek().getReturnType().provisoFree().typeString());
			
			return new VOID();
		}
	}
	
	public TYPE checkTernary(Ternary t) throws CTX_EXC {
		TYPE type = t.condition.check(this);
		if (!(type instanceof BOOL)) 
			throw new CTX_EXC(t.condition.getSource(), Const.CONDITION_NOT_BOOLEAN, type.provisoFree().typeString());
		
		if (t.condition instanceof ArrayInit) 
			throw new CTX_EXC(t.condition.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		TYPE t0 = t.leftOperand.check(this);
		TYPE t1 = t.rightOperand.check(this);
		
		if (t.leftOperand instanceof ArrayInit) 
			throw new CTX_EXC(t.leftOperand.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (t.rightOperand instanceof ArrayInit) 
			throw new CTX_EXC(t.rightOperand.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (!t0.isEqual(t1)) 
			throw new CTX_EXC(t.condition.getSource(), Const.OPERAND_TYPES_DO_NOT_MATCH, t0.provisoFree().typeString(), t1.provisoFree().typeString());
		
		t.setType(t0);
		return t.getType();
	}
	
	public TYPE checkBinaryExpression(BinaryExpression b) throws CTX_EXC {
		TYPE left = b.getLeft().check(this);
		TYPE right = b.getRight().check(this);
		
		if (left instanceof NULL) 
			throw new CTX_EXC(b.left.getSource(), Const.CANNOT_PERFORM_ARITH_ON_NULL);
		
		if (right instanceof NULL) 
			throw new CTX_EXC(b.right.getSource(), Const.CANNOT_PERFORM_ARITH_ON_NULL);
		
		if (b.left instanceof ArrayInit) 
			throw new CTX_EXC(b.left.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (b.right instanceof ArrayInit) 
			throw new CTX_EXC(b.right.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (left.wordsize() > 1) 
			throw new CTX_EXC(b.left.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE_OR_POINTER, left.provisoFree().typeString());
		
		if (right.wordsize() > 1) {
			throw new CTX_EXC(b.left.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE_OR_POINTER, right.provisoFree().typeString());
		}
		
		if (left instanceof POINTER) {
			if (!(right.getCoreType() instanceof INT)) 
				throw new CTX_EXC(b.getSource(), Const.POINTER_ARITH_ONLY_SUPPORTED_FOR_TYPE, new INT().typeString(), right.provisoFree().typeString());
			
			b.setType(left);
		}
		else if (right instanceof POINTER) {
			if (!(left.getCoreType() instanceof INT)) 
				throw new CTX_EXC(b.getSource(), Const.POINTER_ARITH_ONLY_SUPPORTED_FOR_TYPE, new INT().typeString(), left.provisoFree().typeString());
			
			b.setType(left);
		}
		else if (left.isEqual(right)) 
			b.setType(left);
		else throw new CTX_EXC(b.getSource(), Const.OPERAND_TYPES_DO_NOT_MATCH, left.provisoFree().typeString(), right.provisoFree().typeString());
	
		return b.getType();
	}
	
	/**
	 * Overrides binary expression.<br>
	 * Checks for:<br>
	 * - Both operand types have to be of type BOOL<br>
	 */
	public TYPE checkBoolBinaryExpression(BoolBinaryExpression b) throws CTX_EXC {
		TYPE left = b.getLeft().check(this);
		TYPE right = b.getRight().check(this);
		
		if (!(left instanceof BOOL)) 
			throw new CTX_EXC(b.left.getSource(), Const.EXPECTED_TYPE_ACTUAL, new BOOL().typeString(), left.provisoFree().typeString());
		
		if (!(right instanceof BOOL)) 
			throw new CTX_EXC(b.right.getSource(), Const.EXPECTED_TYPE_ACTUAL, new BOOL().typeString(), right.provisoFree().typeString());
		
		b.setType(left);
		return b.getType();
	}
	
	/**
	 * Overrides unary expression.<br>
	 * Checks for:<br>
	 * - Operand type has to be of type BOOL<br>
	 */
	public TYPE checkBoolUnaryExpression(BoolUnaryExpression b) throws CTX_EXC {
		TYPE t = b.getOperand().check(this);
		
		if (!(t instanceof BOOL)) 
			throw new CTX_EXC(b.getOperand().getSource(), Const.EXPECTED_TYPE_ACTUAL, new BOOL().typeString(), t.provisoFree().typeString());
		
		b.setType(t);
		return b.getType();
	}
	
	public TYPE checkUnaryExpression(UnaryExpression u) throws CTX_EXC {
		TYPE op = u.getOperand().check(this);
		
		if (op instanceof NULL) 
			throw new CTX_EXC(u.getOperand().getSource(), Const.CANNOT_PERFORM_ARITH_ON_NULL);
		
		if (u.getOperand() instanceof ArrayInit) 
			throw new CTX_EXC(u.getOperand().getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (u instanceof BitNot && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else if (u instanceof UnaryMinus && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else throw new CTX_EXC(u.getSource(), Const.UNKNOWN_EXPRESSION, u.getClass().getName());
	}
	
	public TYPE checkCompare(Compare c) throws CTX_EXC {
		TYPE left = c.getLeft().check(this);
		TYPE right = c.getRight().check(this);
		
		if (c.left instanceof ArrayInit) 
			throw new CTX_EXC(c.left.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (c.right instanceof ArrayInit) 
			throw new CTX_EXC(c.right.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (left.isEqual(right)) {
			c.setType(new BOOL());
			return c.getType();
		}
		else throw new CTX_EXC(c.getSource(), Const.OPERAND_TYPES_DO_NOT_MATCH, left.provisoFree().typeString(), right.provisoFree().typeString());
	}
	
	public TYPE checkInlineCall(InlineCall i) throws CTX_EXC {
		String prefix = "";
		
		if (i.isNestedCall) {
			if (i.parameters.get(0).check(this).getCoreType() instanceof STRUCT) {
				STRUCT s = (STRUCT) i.parameters.get(0).check(this).getCoreType();
				prefix = s.getTypedef().path.build();
			}
			else {
				INTERFACE s = (INTERFACE) i.parameters.get(0).check(this).getCoreType();
				prefix = s.getTypedef().path.build();
			}
		}
		
		Function f = this.linkFunction(i.path, i, i.getSource(), prefix);
		
		if (i.isNestedCall) {
			if (i.parameters.get(0).check(this).getCoreType() instanceof STRUCT) {
				STRUCT s = (STRUCT) i.parameters.get(0).check(this).getCoreType();
				
				boolean found = false;
				for (Function nested : s.getTypedef().functions) {
					if (nested.equals(f)) {
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new CTX_EXC(i.getSource(), Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path.build(), s.getTypedef().path.build());
			}
			else {
				INTERFACE s = (INTERFACE) i.parameters.get(0).check(this).getCoreType();
				
				boolean found = false;
				for (Function nested : s.getTypedef().functions) {
					if (nested.equals(f)) {
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new CTX_EXC(i.getSource(), Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path.build(), s.getTypedef().path.build());
			}
		}
		else {
			if (this.nestedFunctions.contains(f))
				throw new CTX_EXC(i.getSource(), Const.NESTED_FUNCTION_CANNOT_BE_ACCESSED, f.path.build());
		}
		
		i.calledFunction = f;
		
		if (!this.watchpointStack.isEmpty()) i.watchpoint = this.watchpointStack.peek();
		
		if (f != null) {
			/* Inline calls made during global setup may not signal, since exception cannot be watched */
			if (f.signals() && this.scopes.size() == 1) 
				throw new CTX_EXC(i.getSource(), Const.CALL_DURING_INIT_CANNOT_SIGNAL, f.path.build());
			
			checkModifier(f.modifier, f.path, i.getSource());
			
			/* Add signaled types */
			if (f.signals()) {
				for (TYPE s : f.signalsTypes) 
					if (!this.signalStackContains(s)) 
						this.signalStack.peek().add(s);
			}
			
			if (!f.provisosTypes.isEmpty()) {
				if (i.proviso.isEmpty() || i.hasAutoProviso) {
					i.hasAutoProviso = true;
					
					/* Attempt to find auto-proviso mapping */
					List<TYPE> iParamTypes = new ArrayList();
					
					for (int a = 0; a < i.parameters.size(); a++) {
						/* Apply parameter type if atom is placeholder */
						if (i.parameters.get(a) instanceof TempAtom) {
							TempAtom atom = (TempAtom) i.parameters.get(a);
							atom.inheritType = f.parameters.get(a).getType();
						}
						
						iParamTypes.add(i.parameters.get(a).check(this));
					}	
					
					List<TYPE> functionTypes = new ArrayList();
					
					for (Declaration d : f.parameters) 
						functionTypes.add(d.getRawType());
					
					i.proviso = this.autoProviso(f.provisosTypes, functionTypes, iParamTypes, i.getSource());
				}
				
				if (f.containsMapping(i.proviso)) {
					/* Mapping already exists, just return return type of this specific mapping */
					f.setContext(i.proviso);
					
					i.setType(f.getReturnType());
				}
				else {
					/* Create a new context, check function for this specific context */
					f.setContext(i.proviso);
					
					this.scopes.push(new Scope(this.scopes.get(0)));
					f.check(this);
					this.scopes.pop();
					i.setType(f.getReturnType());
				}
			}
			else 
				/* 
				 * Add default proviso mapping, so mapping is present,
				 * function was called and will be compiled.
				 */
				f.addProvisoMapping(f.getReturnType(), new ArrayList());
			
			if (i.parameters.size() != f.parameters.size()) 
				throw new CTX_EXC(i.getSource(), Const.MISSMATCHING_ARGUMENT_NUMBER, f.parameters.size(), i.parameters.size());
			
			for (int a = 0; a < f.parameters.size(); a++) {
				TYPE functionParamType = f.parameters.get(a).getType();
				
				/* Apply parameter type if atom is placeholder */
				if (i.parameters.get(a) instanceof TempAtom) {
					TempAtom atom = (TempAtom) i.parameters.get(a);
					atom.inheritType = functionParamType;
				}
				
				TYPE paramType = i.parameters.get(a).check(this);
				
				if (!paramType.isEqual(functionParamType)) {
					if (paramType instanceof POINTER || functionParamType instanceof POINTER) 
						CompilerDriver.printProvisoTypes = true;
					
					int paramNumber = a + 1;
					if (i.isNestedCall)
						a -= 1;
					
					if (this.checkPolymorphViolation(paramType, functionParamType))
						throw new CTX_EXC(i.parameters.get(a).getSource(), Const.PARAMETER_TYPE_INDEX_DOES_NOT_MATCH_POLY, paramNumber, paramType.provisoFree().typeString(), functionParamType.provisoFree().typeString());
					
					throw new CTX_EXC(i.parameters.get(a).getSource(), Const.PARAMETER_TYPE_INDEX_DOES_NOT_MATCH, paramNumber, paramType.provisoFree().typeString(), functionParamType.provisoFree().typeString());
				}
			}
			
			if (f.provisosTypes.isEmpty() || !f.containsMapping(i.proviso)) 
				i.setType(f.getReturnType().clone());
			
			if (i.getType() instanceof VOID && !f.hasReturn) 
				throw new CTX_EXC(i.getSource(), Const.EXPECTED_RETURN_VALUE);
		}
		else {
			/* Set void as return type */
			i.setType(new VOID());
			
			for (int a = 0; a < i.parameters.size(); a++) 
				i.parameters.get(a).check(this);
		}
		
		return i.getType();
	}
	
	public TYPE checkFunctionCall(FunctionCall i) throws CTX_EXC {
		String prefix = "";
		
		if (i.isNestedCall) {
			STRUCT s = (STRUCT) i.parameters.get(0).check(this).getCoreType();
			prefix = s.getTypedef().path.build();
		}
		
		Function f = this.linkFunction(i.path, i, i.getSource(), prefix);
		
		if (i.isNestedCall) {
			STRUCT s = (STRUCT) i.parameters.get(0).check(this).getCoreType();
			
			boolean found = false;
			for (Function nested : s.getTypedef().functions) {
				if (nested.equals(f)) {
					found = true;
					break;
				}
			}
			
			if (!found)
				throw new CTX_EXC(i.getSource(), Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path.build(), s.getTypedef().path.build());
		}
		else {
			if (this.nestedFunctions.contains(f))
				throw new CTX_EXC(i.getSource(), Const.NESTED_FUNCTION_CANNOT_BE_ACCESSED, f.path.build());
		}
		
		i.calledFunction = f;
		i.watchpoint = this.watchpointStack.peek();
		
		if (f != null) {
			
			if (i.baseRef != null) {
				TYPE t = i.baseRef.check(this);
				
				if (!(t.getCoreType() instanceof STRUCT)) 
					throw new CTX_EXC(i.getSource(), Const.NESTED_CALL_BASE_IS_NOT_A_STRUCT, t.getCoreType().typeString());
				
				STRUCT s = (STRUCT) t.getCoreType();
				
				boolean found = false;
				for (Function f0 : s.getTypedef().functions) {
					if (f0.path.build().equals(f.path.build())) {
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new CTX_EXC(i.getSource(), Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path.build(), s.typeString());
			}
			
			checkModifier(f.modifier, f.path, i.getSource());
			
			/* Add signaled types */
			if (f.signals()) {
				for (TYPE s : f.signalsTypes) {
					if (!this.signalStackContains(s)) 
						this.signalStack.peek().add(s);
				}
			}
			
			if (!f.provisosTypes.isEmpty()) {
				
				if (i.proviso.isEmpty() || i.hasAutoProviso) {
					i.hasAutoProviso = true;
					
					/* Attempt to find auto-proviso mapping */
					List<TYPE> iParamTypes = new ArrayList();

					for (int a = 0; a < i.parameters.size(); a++) {
						/* Apply parameter type if atom is placeholder */
						if (i.parameters.get(a) instanceof TempAtom) {
							TempAtom atom = (TempAtom) i.parameters.get(a);
							atom.inheritType = f.parameters.get(a).getType();
						}
						
						iParamTypes.add(i.parameters.get(a).check(this));
					}	
					
					List<TYPE> functionTypes = new ArrayList();
					
					for (Declaration d : f.parameters) 
						functionTypes.add(d.getRawType());
					
					i.proviso = this.autoProviso(f.provisosTypes, functionTypes, iParamTypes, i.getSource());
				}
				
				if (!f.containsMapping(i.proviso)) {
					/* Create new scope that points to the global scope */
					f.setContext(i.proviso);
					
					this.scopes.push(new Scope(this.scopes.get(0)));
					f.check(this);
					this.scopes.pop();
				}
				else f.setContext(i.proviso);
			}
			else 
				/* 
				 * Add default proviso mapping, so mapping is present,
				 * function was called and will be compiled.
				 */
				f.addProvisoMapping(f.getReturnType(), new ArrayList());
			
			if (i.parameters.size() != f.parameters.size()) 
				throw new CTX_EXC(i.getSource(), Const.MISSMATCHING_ARGUMENT_NUMBER, f.parameters.size(), i.parameters.size());
			
			for (int a = 0; a < f.parameters.size(); a++) {
				
				/* Apply parameter type if atom is placeholder */
				if (i.parameters.get(a) instanceof TempAtom) {
					TempAtom atom = (TempAtom) i.parameters.get(a);
					atom.inheritType = f.parameters.get(a).getType();
				}
				
				TYPE paramType = i.parameters.get(a).check(this);
				
				if (!paramType.isEqual(f.parameters.get(a).getType())) {
					if (paramType instanceof POINTER || f.parameters.get(a).getType() instanceof POINTER) 
						CompilerDriver.printProvisoTypes = true;
					
					int paramNumber = a + 1;
					if (i.isNestedCall)
						a -= 1;
					
					if (this.checkPolymorphViolation(paramType, f.parameters.get(a).getType()))
						throw new CTX_EXC(i.parameters.get(a).getSource(), Const.PARAMETER_TYPE_INDEX_DOES_NOT_MATCH_POLY, paramNumber, paramType.provisoFree().typeString(), f.parameters.get(a).getType().provisoFree().typeString());
					
					throw new CTX_EXC(i.parameters.get(a).getSource(), Const.PARAMETER_TYPE_INDEX_DOES_NOT_MATCH, paramNumber, paramType.provisoFree().typeString(), f.parameters.get(a).getType().provisoFree().typeString());
				}
			}
		}
		else {
			for (int a = 0; a < i.parameters.size(); a++) {
				if (i.parameters.get(a) instanceof ArrayInit) 
					throw new CTX_EXC(i.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
				
				i.parameters.get(a).check(this);
			}
		}
		
		return new VOID();
	}
	
	/**
	 * Checks for:<br>
	 * - Sets the origin of the reference<br>
	 * - Sets the type of the reference
	 */
	public TYPE checkIDRef(IDRef i) throws CTX_EXC {
		/* Search for the declaration in the scopes */
		Declaration d = this.scopes.peek().getField(i.path, i.getSource());
		
		if (d != null) {
			/* Link origin */
			i.origin = d;
			
			/* Apply type */
			i.setType(d.getType());
			
			/* Check for modifier restrictions */
			this.checkModifier(i.origin.modifier, i.origin.path, i.getSource());

			boolean contains = false;
			for (int a = this.scopes.size() - 1; a >= 0; a--) {
				if (this.scopes.get(a).isLoopedScope) break;
				
				if (this.scopes.get(a).declarations.containsKey(i.path.build())) {
					contains = true;
					break;
				}
			}
			
			if (contains && !(i.origin.getType() instanceof FUNC)) 
				/* Set where the last statement accesses the IDRef. */
				i.origin.last = currentStatement;
			else 
				i.origin.last = null;
			
			return i.getType();
		}
		else throw new CTX_EXC(i.getSource(), Const.UNKNOWN_VARIABLE, i.path.build());
	}
	
	public TYPE checkFunctionRef(FunctionRef r) throws CTX_EXC {
		/* If not already linked, find referenced function */
		Function lambda = null;
		if (r.origin != null)
			lambda = r.origin;
		else {
			lambda = this.findFunction(r.path, r.getSource(), true, "");
			
			if (r.base != null) {
				STRUCT s = (STRUCT) r.base.check(this).getCoreType();
				
				boolean found = false;
				for (Function nested : s.getTypedef().functions) {
					if (nested.equals(lambda)) {
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new CTX_EXC(r.getSource(), Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, lambda.path.build(), s.getTypedef().path.build());
			}
			else {
				if (this.nestedFunctions.contains(lambda))
					throw new CTX_EXC(r.getSource(), Const.NESTED_FUNCTION_CANNOT_BE_ACCESSED, lambda.path.build());
			
			}
		}
		
		if (lambda == null) 
			throw new CTX_EXC(r.getSource(), Const.UNKNOWN_PREDICATE, r.path.build());
		
		/* Provided number of provisos does not match number of provisos of lambda */
		if (lambda.provisosTypes.size() != r.proviso.size()) 
			throw new CTX_EXC(r.getSource(), Const.MISSMATCHING_NUMBER_OF_PROVISOS, lambda.provisosTypes.size(), r.proviso.size());
		
		/* A lambda cannot signal exceptions, since it may become anonymous */
		if (lambda.signals()) 
			throw new CTX_EXC(r.getSource(), Const.PREDICATE_CANNOT_SIGNAL);
		
		/* Set context and add mapping */
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNToNMaybe(r.proviso, this.currentFunction.peek().provisosTypes);
		
		lambda.setContext(r.proviso);
		lambda.addProvisoMapping(lambda.getReturnType(), r.proviso);
		
		/* Make sure function is check, may only be called through anonymous predicate */
		this.scopes.push(new Scope(this.scopes.get(0)));
		lambda.check(this);
		this.scopes.pop();
		
		r.origin = lambda;
		
		/* Set flag that this function was targeted as a lambda */
		lambda.isLambdaTarget = true;
		
		r.setType(new FUNC(lambda, r.proviso));
		return r.getType();
	}
	
	public TYPE checkArrayInit(ArrayInit init) throws CTX_EXC {
		/* Array must at least contain one element */
		if (init.elements.isEmpty()) 
			throw new CTX_EXC(init.getSource(), Const.ARRAY_INIT_MUST_HAVE_ONE_FIELD);
		
		TYPE type0 = init.elements.get(0).check(this);
		
		int dontCareSize = 0;
		
		if (init.elements.size() > 1 || init.dontCareTypes) {
			for (int i = 0; i < init.elements.size(); i++) {
				TYPE typeX = init.elements.get(i).check(this);
				
				if (init.dontCareTypes) 
					dontCareSize += typeX.wordsize();
				else {
					if (!typeX.isEqual(type0)) 
						throw new CTX_EXC(init.getSource(), Const.ARRAY_ELEMENTS_MUST_HAVE_SAME_TYPE, type0.provisoFree().typeString(), typeX.provisoFree().typeString());
				}
			}
		}
		
		init.setType(new ARRAY((init.dontCareTypes)? new VOID() : type0, (init.dontCareTypes)? dontCareSize : init.elements.size()));
		return init.getType();
	}
	
	public TYPE checkInstanceofExpression(InstanceofExpression iof) throws CTX_EXC {
		iof.expression.check(this);
		
		if (CompilerDriver.disableStructSIDHeaders) 
			throw new CTX_EXC(iof.getSource(), Const.SID_DISABLED_NO_INSTANCEOF);
		
		if (!(iof.instanceType instanceof STRUCT)) 
			throw new CTX_EXC(iof.getSource(), Const.EXPECTED_STRUCT_TYPE, iof.instanceType.provisoFree().typeString());
		
		iof.setType(new BOOL());
		return iof.getType();
	}
	
	public TYPE checkSizeOfType(SizeOfType sot) throws CTX_EXC {
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(sot.sizeType, this.currentFunction.peek().provisosTypes);
		
		sot.setType(new INT());
		return sot.getType();
	}
	
	public TYPE checkSizeOfExpression(SizeOfExpression soe) throws CTX_EXC {
		soe.sizeType = soe.expression.check(this);
		soe.setType(new INT());
		return soe.getType();
	}
	
	public TYPE checkAddressOf(AddressOf aof) throws CTX_EXC {
		TYPE t = aof.expression.check(this);
		
		if (!(aof.expression instanceof IDRef || aof.expression instanceof IDRefWriteback || aof.expression instanceof ArraySelect || aof.expression instanceof StructSelect || aof.expression instanceof StructureInit)) 
			throw new CTX_EXC(aof.getSource(), Const.CAN_ONLY_GET_ADDRESS_OF_VARIABLE_REF_OR_ARRAY_SELECT);
		
		aof.setType(new POINTER(t.getCoreType()));
		
		return aof.getType();
	}
	
	/**
	 * Checks for:<br>
	 * - Operand type is pointer.y
	 */
	public TYPE checkDeref(Deref deref) throws CTX_EXC {
		TYPE t = deref.expression.check(this);
		
		/* Dereference pointer or primitive type */
		if (t instanceof PRIMITIVE) 
			/* Set to core type */
			deref.setType(t.getCoreType());
		else if (t instanceof POINTER) {
			POINTER p = (POINTER) t;
			deref.setType(p.targetType);
		}
		else throw new CTX_EXC(deref.expression.getSource(), Const.CANNOT_DEREF_TYPE, t.provisoFree().typeString());
		
		/* Dereferencing a primitive can be a valid statement, but it can be unsafe. A pointer would be safer. */
		if (t instanceof PRIMITIVE) {
			if (!CompilerDriver.disableWarnings) 
				this.messages.add(new Message(String.format(Const.OPERAND_IS_NOT_A_POINTER, deref.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		return deref.getType();
	}
	
	public TYPE checkTypeCast(TypeCast tc) throws CTX_EXC {
		TYPE t = tc.expression.check(this);
		
		if (tc.expression instanceof InlineCall) {
			InlineCall ic = (InlineCall) tc.expression;
			/* Anonymous inline call */
			if (ic.calledFunction == null) {
				ic.setType(tc.castType);
				t = tc.castType;
				
				if (!CompilerDriver.disableWarnings) 
					messages.add(new Message(String.format(Const.USING_IMPLICIT_ANONYMOUS_TYPE, tc.castType.provisoFree().typeString(), tc.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
			}
		}
		
		/* Allow only casting to equal word sizes or from or to void types */
		if ((t != null && t.wordsize() != tc.castType.wordsize()) && !(tc.castType.getCoreType() instanceof VOID || t instanceof VOID)) 
			throw new CTX_EXC(tc.getSource(), Const.CANNOT_CAST_TO, t.provisoFree().typeString(), tc.castType.provisoFree().typeString());
		
		tc.setType(tc.castType);
		return tc.castType;
	}
	
	public TYPE checkIDRefWriteback(IDRefWriteback i) throws CTX_EXC {
		if (i.getShadowRef() instanceof IDRef) {
			IDRef ref = (IDRef) i.getShadowRef();
			i.idRef = ref;
			
			TYPE t = ref.check(this);
			
			if (!(t instanceof PRIMITIVE)) throw new CTX_EXC(i.idRef.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE);
			
			i.setType(t);
		}
		else throw new CTX_EXC(i.getSource(), Const.CAN_ONLY_APPLY_TO_IDREF);
		
		return i.getType();
	}
	
	public TYPE checkStructSelectWriteback(StructSelectWriteback i) throws CTX_EXC {
		if (i.getShadowSelect() instanceof StructSelect) {
			StructSelect ref = (StructSelect) i.getShadowSelect();
			i.select = ref;
			
			TYPE t = ref.check(this);
			
			if (!(t instanceof PRIMITIVE)) throw new CTX_EXC(i.select.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE);
			
			i.setType(t);
		}
		else throw new CTX_EXC(i.getSource(), Const.CAN_ONLY_APPLY_TO_IDREF);
		
		return i.getType();
	}
	
	public TYPE checkAssignWriteback(AssignWriteback i) throws CTX_EXC {
		if (i.reference instanceof IDRefWriteback || i.reference instanceof StructSelectWriteback) i.reference.check(this);
		else throw new CTX_EXC(i.getSource(), Const.CAN_ONLY_APPLY_TO_IDREF);
		return null;
	}
	
	/**
	 * Checks for:<br>
	 * - At least one selection has to be made<br>
	 * - Checks that the shadow ref is an IDRef<br>
	 * - Checks that the types of the selection are of type int<br>
	 * - Amount of selections does not exceed structure dimension<br>
	 */
	public TYPE checkArraySelect(ArraySelect select) throws CTX_EXC {
		if (select.selection.isEmpty()) 
			throw new CTX_EXC(select.getSource(), Const.ARRAY_SELECT_MUST_HAVE_SELECTION);
		
		Expression ref = null;
		
		if (select.getShadowRef() instanceof IDRef) {
			ref = (IDRef) select.getShadowRef();
			select.idRef = (IDRef) ref;
		}
		else if (select.getShadowRef() instanceof StructSelect) {
			ref = (StructSelect) select.getShadowRef();
		}
		else throw new CTX_EXC(select.getShadowRef().getSource(), Const.CAN_ONLY_SELECT_FROM_VARIABLE_REF);
		
		TYPE type0 = ref.check(this);
		
		/* If pointer, unwrap it */
		TYPE chain = (type0 instanceof POINTER)? ((POINTER) type0).targetType : type0;
		
		/* Check selection chain */
		for (int i = 0; i < select.selection.size(); i++) {
			TYPE stype = select.selection.get(i).check(this);
			if (!(stype instanceof INT)) 
				throw new CTX_EXC(select.selection.get(i).getSource(), Const.ARRAY_SELECTION_HAS_TO_BE_OF_TYPE, stype.provisoFree().typeString());
			else {
				/* Allow to select from array but only in the first selection, since pointer 'flattens' the array structure */
				if (!(chain instanceof ARRAY || (i == 0 && (type0 instanceof POINTER || chain instanceof VOID)))) 
					throw new CTX_EXC(select.selection.get(i).getSource(), Const.CANNOT_SELECT_FROM_TYPE, type0.provisoFree().typeString());
				else if (chain instanceof ARRAY) {
					ARRAY arr = (ARRAY) chain;
					
					if (select.selection.get(i) instanceof Atom) {
						Atom a = (Atom) select.selection.get(i);
						int value = (int) a.getType().getValue();
						if (value < 0 || value >= arr.getLength()) 
							throw new CTX_EXC(select.selection.get(i).getSource(), Const.ARRAY_OUT_OF_BOUNDS, value, chain.provisoFree().typeString());
					}
					
					chain = arr.elementType;
				}
				else {
					if (type0 instanceof POINTER) {
						POINTER p = (POINTER) type0;
						chain = p.targetType;
					}
					else {
						/* When selecting from void, type will stay void */
						VOID v = (VOID) chain;
						chain = v;
					}
					
					if (select.selection.size() > 1) 
						throw new CTX_EXC(select.getShadowRef().getSource(), Const.CAN_ONLY_SELECT_ONCE_FROM_POINTER_OR_VOID);
				}
			}
		}
		
		if (type0 instanceof POINTER) chain = new POINTER(chain);
		
		select.setType(chain);
		return select.getType();
	}
	
	public TYPE checkAtom(Atom a) throws CTX_EXC {
		return a.getType();
	}
	
	public TYPE checkPlaceholderAtom(TempAtom a) throws CTX_EXC {
		
		/* Override the type type of the base */
		a.setType(a.inheritType);
		
		if (a.base != null) {
			TYPE t = a.base.check(this);
			
			if (a.inheritType.wordsize() % t.wordsize() != 0 || t.wordsize() > a.inheritType.wordsize()) 
				throw new CTX_EXC(a.getSource(), Const.TYPE_CANNOT_BE_ALIGNED_TO, t.provisoFree().typeString(), a.inheritType.provisoFree().typeString());
		}
		
		return a.getType();
	}
	
	public TYPE checkRegisterAtom(RegisterAtom a) throws CTX_EXC {
		String reg = a.spelling.toLowerCase();
		
		REG reg0 = RegOp.convertStringToReg(reg);
		
		if (reg0 == null) 
			throw new CTX_EXC(a.getSource(), Const.UNKNOWN_REGISTER, reg);
		else a.reg = reg0;
		
		return a.getType();
	}
	
	public TYPE checkDirectASMStatement(DirectASMStatement d) throws CTX_EXC {
		for (Pair<Expression, REG> p : d.dataIn) {
			TYPE t = p.first.check(this);
			
			if (t.wordsize() > 1) 
				throw new CTX_EXC(p.first.getSource(), Const.ONLY_APPLICABLE_FOR_ONE_WORD_TYPE_ACTUAL, t.provisoFree().typeString());
		}
		
		for (Pair<Expression, REG> p : d.dataOut) {
			TYPE t = p.first.check(this);
			
			if (!(p.first instanceof IDRef)) 
				throw new CTX_EXC(p.first.getSource(), Const.EXPECTED_IDREF_ACTUAL, p.first.getClass().getName());
			
			if (t.wordsize() > 1) 
				throw new CTX_EXC(p.first.getSource(), Const.ONLY_APPLICABLE_FOR_ONE_WORD_TYPE_ACTUAL, t.provisoFree().typeString());
		}
		
		if (d.dataOut.isEmpty()) {
			if (!CompilerDriver.disableWarnings) 
				messages.add(new Message(String.format(Const.DIRECT_ASM_HAS_NO_OUTPUTS, d.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		return new VOID();
	}
	
	
			/* --- HELPER METHODS --- */
	/**
	 * Attempts to figure out the provided proviso.
	 * 
	 * @param targetProviso The proviso header of the target.
	 * @param expectedTypes The parameter types of the target.
	 * @param providedTypes The provided parameter types.
	 * @return The missing proviso mapping for the target.
	 * @throws CTX_EXC If the mapping is not clear or a mapping cannot be determined.
	 */
	public List<TYPE> autoProviso(List<TYPE> targetProviso, List<TYPE> expectedTypes, List<TYPE> providedTypes, Source source) throws CTX_EXC {
		List<TYPE> foundMapping = new ArrayList();
		
		if (expectedTypes.size() != providedTypes.size())
			throw new CTX_EXC(source, Const.MISSMATCHING_NUMBER_OF_PROVISOS, expectedTypes.size(), providedTypes.size());
			
		for (int i = 0; i < targetProviso.size(); i++) {
			PROVISO prov = (PROVISO) targetProviso.get(i);
			prov.releaseContext();
			
			int ind = -1;
			TYPE mapped = null;
			
			for (int a = 0; a < expectedTypes.size(); a++) {
				/* Found a parameter type that holds the searched proviso */
				TYPE map0 = expectedTypes.get(a).mappable(providedTypes.get(a), prov.placeholderName);
				
				if (map0 != null) {
					if (mapped == null) {
						mapped = map0;
						ind = a;
					}
					else {
						if (!mapped.typeString().equals(map0.typeString())) 
							/* Found two possible types for proviso, abort */
							throw new CTX_EXC(source, Const.MULTIPLE_AUTO_MAPS_FOR_PROVISO, prov.placeholderName, mapped.provisoFree().typeString(), ind + 1, map0.provisoFree().typeString(), a + 1);
					}
				}
			}
			
			if (mapped == null) 
				/* None of the types held the searched proviso, proviso cannot be auto-ed, abort. */
				throw new CTX_EXC(source, Const.CANNOT_AUTO_MAP_PROVISO, prov.typeString());
			
			foundMapping.add(mapped.provisoFree().clone());
		}
		
		return foundMapping;
	}
	
	/**
	 * Attempts to find a field in the given struct type where the name matches the name specified by
	 * the IDRef. If such a field is found, the origin of the IDRef is set to the declaration of this
	 * field. Additionally, the type of the IDRef is set to the field type. Then the field type is returned.
	 * 
	 * @param struct The struct type to search in.
	 * @param ref0 The IDRef that specifies the field name.
	 * @return The type of the field that was found.
	 * @throws CTX_EXC Thrown when the field name does not exist.
	 */
	private TYPE findAndLinkField(STRUCT struct, IDRef ref0) throws CTX_EXC {
		Declaration field = struct.getField(ref0.path);
		
		/* The ID the current selection targets */
		if (field != null) {
			/* Link manually, identifier is not part of current scope */
			ref0.origin = field;
			ref0.setType(ref0.origin.getType());
			
			/* Next type in chain */
			return ref0.getType();
		}
		else throw new CTX_EXC(ref0.getSource(), Const.FIELD_NOT_IN_STRUCT, ref0.path.build(), struct.provisoFree().typeString());
	}
	
	/**
	 * Checks for a modifier violation. This is done by constructing the path of the current function and
	 * comparing it to the given path. If the modifier is SHARED or STATIC, no checks are required. If it is 
	 * RESTRICTED, check if the path of the current function is contained in the subtree of the given namespace
	 * path. If it is EXCLUSIVE, check for strict equality.
	 * 
	 * @param mod The modifier to check.
	 * @param path The path of the ressource accessed.
	 * @param source The source of the AST node that initiated the check.
	 * @throws CTX_EXC Thrown if a modifier violation is detected.
	 */
	public void checkModifier(MODIFIER mod, NamespacePath path, Source source) throws CTX_EXC {
		String currentPath = (this.currentFunction.isEmpty())? "" : this.currentFunction.peek().path.buildPathOnly();
		
		if (mod == MODIFIER.SHARED || mod == MODIFIER.STATIC) return;
		else if (mod == MODIFIER.RESTRICTED) {
			if (!currentPath.startsWith(path.buildPathOnly())) {
				if (CompilerDriver.disableModifiers) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message(String.format(Const.MODIFIER_VIOLATION_AT, path.build(), this.currentFunction.peek().path.build(), source.getSourceMarker()), LogPoint.Type.WARN, true));
				}
				else throw new CTX_EXC(source, Const.MODIFIER_VIOLATION, path.build(), this.currentFunction.peek().path.build());
			}
		}
		else if (mod == MODIFIER.EXCLUSIVE) {
			if (!currentPath.equals(path.buildPathOnly())) {
				if (CompilerDriver.disableModifiers) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message(String.format(Const.MODIFIER_VIOLATION_AT, path.build(), this.currentFunction.peek().path.build(), source.getSourceMarker()), LogPoint.Type.WARN, true));
				}
				else throw new CTX_EXC(source, Const.MODIFIER_VIOLATION, path.build(), this.currentFunction.peek().path.build());
			}
		}
	}
	
	/**
	 * Check if given child is a polymorph child of the target. Return true if this is the case,
	 * return false in any other case or if the target is not a struct.
	 * 
	 * @param child The Type that is checked to be a child of target.
	 * @param target The Type that acts as the parent of the child.
	 * @throws CTX_EXC
	 */
	public boolean checkPolymorphViolation(TYPE child, TYPE target) {
		if (!(target instanceof STRUCT)) return false;
		
		if (child.getCoreType() instanceof STRUCT) {
			if (((STRUCT) child.getCoreType()).isPolymorphTo(target) && 
				!((STRUCT) child).getTypedef().equals(((STRUCT) target).getTypedef())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the top layer of the signal stack contains given type.
	 * 
	 * @param newSignal The type to check if it is contained in the signal stack.
	 * @return True if it is contained, false if not.
	 */
	public boolean signalStackContains(TYPE newSignal) {
		for (TYPE t : this.signalStack.peek()) 
			if (t.isEqual(newSignal)) return true;
		
		return false;
	}
	
	/**
	 * Attempts to find the function that has the given namespace path. Also checks func 
	 * predicate declarations. If more than one match is found, an exception is thrown. If
	 * no match is found, an exception is thrown. If exactly one match is found, the found
	 * function is returned.
	 * 
	 * @param path The namespace path of the searched function.
	 * @param source The location of the AST node that initiated the check.
	 * @param isPredicate Set to true when checking a function ref. Variates the error message.
	 * @return The found function.
	 * @throws CTX_EXC Thrown if no or multiple matches for the function are found.
	 */
	public Function findFunction(NamespacePath path, Source source, boolean isPredicate, String prefix) throws CTX_EXC {
		/* Search through registered functions, match entire path */
		for (Function f0 : this.functions) 
			if (f0.path.build().equals(path.build())) 
				return f0;
		
		/* Collect functions that match this namespace path. */
		List<Function> funcs = new ArrayList();
		
		/* Search through the registered function declarations, but only match the end of the namespace path */
		for (Function f0 : this.functions) 
			if (f0.path.build().endsWith(path.build())) 
				funcs.add(f0);
		
		/* Search through predicate declarations */
		for (Declaration d : this.currentFunction.peek().parameters) {
			if (d.getType() instanceof FUNC) {
				FUNC f0 = (FUNC) d.getType();
				
				if (f0.funcHead != null) 
					f0.funcHead.lambdaDeclaration = d;
				
				if (d.path.getLast().equals(path.getLast())) 
					funcs.add(f0.funcHead);
			}
		}
		
		if (funcs.isEmpty()) 
			/* Return if there is only one result */
			return null;
		else if (funcs.size() == 1) 
			/* Found one match, return this match */
			return funcs.get(0);
		else {
			/* Multiple results, cannot determine correct one, throw an exception */
			String s = "";
			
			/* Check for match with prefix */
			for (Function f0 : funcs) 
				if (f0.path.build().equals(prefix + "." + path.build()))
					return f0;
			
			for (Function f0 : funcs) s += f0.path.build() + ", ";
			s = s.substring(0, s.length() - 2);
			
			throw new CTX_EXC(source, Const.MULTIPLE_MATCHES_FOR_X, ((isPredicate)? "predicate" : "function"), path.build(), s);
		}
	}
	
	/**
	 * Attempts to find the function with the name of given namespace path. If the function is not found,
	 * it is searched as a predicate. If the called function is a predicate, and the predicate is not anonymous,
	 * the provisos are overridden by the provisos by the predicate. Also, the anonTarget field is set.
	 * 
	 * @param path The path that specifies the function name.
	 * @param i The callee, should be either an InlineCall or FunctionCall.
	 * @param source The source of the AST node that initiated the check.
	 * @return The found function.
	 * @throws CTX_EXC Thrown if the callee has provisos in a predicate call, or if the function cannot be found.
	 */
	public Function linkFunction(NamespacePath path, SyntaxElement i, Source source, String prefix) throws CTX_EXC {
		List<TYPE> proviso = null;
		
		assert i instanceof InlineCall || i instanceof FunctionCall : "Given SyntaxElement is neither an InlineCall or FunctionCall!";
		
		/* Extract the proviso from the callee */
		if (i instanceof InlineCall) {
			InlineCall i0 = (InlineCall) i;
			proviso = i0.proviso;
		}
		else {
			FunctionCall i0 = (FunctionCall) i;
			proviso = i0.proviso;
		}
		
		/* Find the called function */
		Function f = this.findFunction(path, source, false, prefix);
		
		Declaration anonTarget = null;
		
		/* Function not found, may be a lambda call */
		if (f == null) {
			anonTarget = this.scopes.peek().getFieldNull(path, source);
			
			/* Found target as predicate, predicate is not anonymous */
			if (anonTarget != null && anonTarget.getType() instanceof FUNC) {
				FUNC f0 = (FUNC) anonTarget.getType();
				
				/* Provisos of call must be empty in case of predicate. */
				if (!proviso.isEmpty()) 
					throw new CTX_EXC(source, Const.PROVISO_ARE_PROVIDED_BY_PREDICATE, anonTarget.path.build());
				
				/* Proviso types are provided through lambda */
				proviso = f0.proviso;
				
				/* Set found function to function head */
				f = f0.funcHead;
				
				if (f == null) {
					/* Anonymous function head */
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message(String.format(Const.PREDICATE_IS_ANONYMOUS, path.build(), source.getSourceMarker()), LogPoint.Type.WARN, true));
				}
			}
		}
		
		/* Neither regular function or predicate was found, undefined */
		if (f == null && anonTarget == null) 
			throw new CTX_EXC(source, Const.UNDEFINED_FUNCTION_OR_PREDICATE, path.build());
		
		/* Write back anon target and provisos */
		if (i instanceof InlineCall) {
			InlineCall i0 = (InlineCall) i;
			i0.anonTarget = anonTarget;
			i0.proviso = proviso;
		}
		else {
			FunctionCall i0 = (FunctionCall) i;
			i0.anonTarget = anonTarget;
			i0.proviso = proviso;
		}
		
		return f;
	}
	
	/* 
	 * Initiates the SID propagation process. This process will
	 * assign each struct typedef a unique ID, in a way that makes
	 * it easy to check if a struct type is a child of another one.
	 */
	public void setupSIDs() {
		if (!this.tLStructs.isEmpty()) {
			int SIDStart = 1;
			if (this.tLStructs.size() == 1) 
				this.tLStructs.get(0).propagateSIDs(SIDStart, null);
			else {
				/* Apply to first n - 1 */
				for (int i = 1; i < this.tLStructs.size(); i++) { 
					SIDStart = this.tLStructs.get(i - 1).propagateSIDs(SIDStart, this.tLStructs.get(i));
					
					/* Set neighbour of n - 1 to n */
					this.tLStructs.get(i - 1).SIDNeighbour = this.tLStructs.get(i);
				}
				
				/* Apply to last */
				this.tLStructs.get(this.tLStructs.size() - 1).propagateSIDs(SIDStart, null);
			}
		}
	}
	
} 
