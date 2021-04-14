package Opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import Exc.OPT0_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.FunctionRef;
import Imm.AST.Expression.IDOfExpression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.InlineFunction;
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AST.Expression.Arith.BitXor;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.And;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.Expression.Boolean.Not;
import Imm.AST.Expression.Boolean.Or;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DirectASMStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AST.Typedef.EnumTypedef;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Opt.Util.CompoundStatementRules;
import Opt.Util.ExpressionMorpher;
import Opt.Util.Makros;
import Opt.Util.Matchers;
import Opt.Util.OPT_STRATEGY;
import Opt.Util.ProgramState;
import Opt.Util.ProgramState.VarState;
import Res.Setting;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Util;

public class ASTOptimizer {

	/**
	 * Stack of program contexts.
	 */
	private Stack<ProgramState> cStack = new Stack();
	
	/**
	 * The program context keeps track of variables and 
	 * other related information, such as if a variable has
	 * been overwritten or read at the current moment.
	 */
	private ProgramState state = null;
	
	/**
	 * Each time an optimization is done, this variable
	 * is set to true. The optimizer keeps starting new cycles,
	 * until this variable is false at the end of a cycle.
	 */
	private boolean OPT_DONE;
	
	/**
	 * How many optimization cycles the optimizer had to do
	 * for the given AST.
	 */
	public static int CYCLES = 0;
	
	/**
	 * Used to keep track of the current phase of the optimizer.
	 * There are two phases:
	 * 
	 * - In the first phase, the optimizer optimizes a duplicate AST.
	 * - In the second phase, the original AST is optimized. For each function
	 * 		in this AST, the function decides based on the current optimized counterpart,
	 * 		if it is worth it to optimize to the same point. If not, the function skips
	 * 		optimization until the counterpart has made optimizations that are worth
	 * 		adopting.
	 * 
	 * The duplicate ASTs are used since it is easier to parse and check a new
	 * AST than to clone the AST in a way that also keeps the references between the
	 * trees in a consistent state.
	 */
	private int phase = 1;
	
	/**
	 * The current substitution size. With each cycle, the size will grow exponentially.
	 * The size determines how large substituted expressions are allowed to be in this
	 * current iteration. The intuition behind this is to attempt to substitute small
	 * expressions first.
	 */
	private int CURR_SUBS_SIZE, subs_arg_n = 0;

	/**
	 * Sets the optimization-strategy. The optimization-strategy determines
	 * how and when optimizations are accepted. Different strategies may lead
	 * to under or over-optimized ASTs.
	 */
	public static OPT_STRATEGY STRATEGY = OPT_STRATEGY.ON_IMPROVEMENT;
	
	/**
	 * If set to true, the AST before and after the optimization will be
	 * print out in Snips-Code representation.
	 */
	private boolean PRINT_RESULT = false;

	private void OPT_DONE() {
		OPT_DONE = true;
	}
	
	public Program optProgram(Program AST, Program AST0) throws OPT0_EXC {
		try {
			if (PRINT_RESULT) AST.codePrint(0).stream().forEach(System.out::println);

			/*
			 * Create 1-to-1 links between the functions in the original and duplicate
			 * AST for phase 2 cross-referencing.
			 */
			for (int i = 0; i < AST.programElements.size(); i++) {
				SyntaxElement s = AST.programElements.get(i);
				
				if (s instanceof Function) {
					Function f = (Function) s;
					Function f0 = (Function) AST0.programElements.get(i);
					f.ASTOptCounterpart = f0;
				}
			}
			
			/* Reset values and state */
			CYCLES = 0;
			OPT_DONE = true;
			subs_arg_n = 0;
			
			/*
			 * Do optimization cycles as long as optimizations
			 * are done. Enforce at least eight cycles. This is needed so that
			 * the CURR_SUBS_SIZE value can grow a bit before premature cancellation
			 * of the optimization algorithm.
			 */
			while (OPT_DONE || CYCLES < 8) {
				CYCLES++;
				OPT_DONE = false;
			
				/* Use fibonacci-sequence as substitution size. */
				CURR_SUBS_SIZE = Util.fib(subs_arg_n);
			
				/*
				 * Perform one optimization cycle on paralell AST.
				 */
				phase = 1;
				AST0 = this.optProgramStep(AST0);
				
				/*
				 * Perform optimizations on original AST. But since phase = 2, 
				 * the functions will decide wether to do optimizations or
				 * skip this optimization round.
				 */
				phase = 2;
				AST = this.optProgramStep(AST);
				
				subs_arg_n++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new OPT0_EXC("An error occurred during AST-Optimization, Phase " + phase + ": " + e.getMessage());
		}

		if (PRINT_RESULT) AST.codePrint(0).stream().forEach(System.out::println);
		
		return AST;
	}
	
	/**
	 * Optimizes the given AST once.
	 */
	public Program optProgramStep(Program AST) throws OPT0_EXC {
		
		/* Root program context. */
		this.state = new ProgramState(null, false);
		this.cStack.push(state);
		
		/* Add globally used symbols. */
		CompilerDriver.HEAP_START.opt(this);
		CompilerDriver.NULL_PTR.opt(this);
		
		/*
		 * Iterate over each program element. If the optimization
		 * returns null the program element can be removed. If it
		 * returns someting else, the element can be replaced.
		 */
		for (int i = 0; i < AST.programElements.size(); i++) {
			SyntaxElement s = AST.programElements.get(i);
			
			SyntaxElement s0 = s.opt(this);
			
			if (s0 == null) {
				AST.programElements.remove(i);
				i--;
			}
			else if (!s0.equals(s)) 
				AST.programElements.set(AST.programElements.indexOf(s), s0);
		}
		
		/* Reset state and scopes for next optimization round */
		this.cStack.pop().transferContextChangeToParent();
		this.state = null;
		
		return AST;
	}
	
	public Function optFunction(Function f) throws OPT0_EXC {
		
		/* Function was not called, wont be translated anyway */
		if (f.provisosCalls.isEmpty()) return f;
		
		/**
		 * If there is a counterpart available and we have selected the 
		 * 'ON_IMPROVEMENT'-Strategy, the function can decide here to accept
		 * the current optimizations or refuse them and wait for further optimizations.
		 */
		if (f.ASTOptCounterpart != null && STRATEGY == OPT_STRATEGY.ON_IMPROVEMENT) {
				
			/* Node # of optimized body */
			int opt_f0 = 0;
			for (Statement s : f.ASTOptCounterpart.body) opt_f0 += s.visit(x -> { return true; }).size();
			
			/* Node # of original body */
			int nodes_f = 0;
			for (Statement s : f.body) nodes_f += s.visit(x -> { return true; }).size();
			
			/**
			 * At this point, the AST-Counterpart-Function could not make an optimization that
			 * reduced its complexity below the current complexity of the original. So the
			 * original function skips this optimization round to prevent a rise in
			 * complexity.
			 */
			if (opt_f0 >= nodes_f) return f;
		
		}
		
		int repeat = 1;
		
		/*
		 * If this is the second phase, and the function has decided
		 * to do the optimizations to the current point as well, based
		 * on the last update, the amount of cycles required to get to the current
		 * point is calculated.
		 */
		if (phase == 2) {
			repeat = CYCLES - f.LAST_UPDATE;
			f.LAST_UPDATE = CYCLES;
		}
		
		/*
		 * Perform the set number of cycles to get the function up-to-date.
		 */
		for (int i = 0; i < repeat; i++) {
			this.pushContext();
		
			for (Declaration dec : f.parameters) dec.opt(this);
			f.body = this.optBody(f.body, false, false);
			
			this.popContext();
		}
		
		return f;
	}
	
	public List<Statement> optBody(List<Statement> body, boolean isLoopedScope, boolean pushContext) throws OPT0_EXC {
		if (pushContext) this.pushContext(isLoopedScope);
		
		for (int i = 0; i < body.size(); i++) {
			Statement s = body.get(i);
			Statement s0 = s.opt(this);
			
			if (s0 == null) {
				body.remove(i);
				i--;
			}
			else {
				if (s0 instanceof DefaultStatement) {
					DefaultStatement d = (DefaultStatement) s0;
					body.remove(i);
					body.addAll(i, d.body);
				}
				else body.set(body.indexOf(s), s0);
			}
		}
		
		/* Removes assignments to variables that are not used after this assignment */
		CompoundStatementRules.removeDanglingAssignments(body, this.state);
		
		List<Declaration> removed = new ArrayList();
		for (Entry<Declaration, VarState> entry : this.state.getCState().entrySet()) {
			Declaration dec = entry.getKey();
			
			if (!this.state.getRead(dec) && !this.state.getReferenced(dec) && !this.state.getWrite(dec)) {
				
				List<IDRefWriteback> idwb = new ArrayList();
				if (dec.value != null) idwb = dec.value.visit(x -> x instanceof IDRefWriteback);
				
				/*
				 * Get all writeback sub-expressions from the expression
				 * that is going to be removed. We have to wrap them in
				 * AssignWriteback statements and add them to the body
				 * in order to keep the program's functionality.
				 */
				List<AssignWriteback> awb = new ArrayList();
				for (IDRefWriteback idwb0 : idwb) 
					awb.add(new AssignWriteback(idwb0, idwb0.getSource()));
				
				if (body.contains(dec)) {
					int index = body.indexOf(dec);
					body.remove(dec);
					
					OPT_DONE();
					
					/* 
					 * Add all writeback operations of the removed expression
					 * as standalone statements at the location of the removed
					 * declaration.
					 */
					for (AssignWriteback awb0 : awb) 
						body.add(index++, awb0);
				}
				removed.add(dec);
			}
		}
		
		removed.stream().forEach(x -> {
			this.state.remove(x);
		});
		
		if (pushContext) this.popContext();
		return body;
	}
	
	public List<Expression> optExpressionList(List<Expression> ops) throws OPT0_EXC {
		for (int i = 0; i < ops.size(); i++) {
			Expression s = ops.get(i);
			Expression s0 = s.opt(this);
			ops.set(ops.indexOf(s), s0);
		}
		
		return ops;
	}

	public Expression optAddressOf(AddressOf aof) throws OPT0_EXC {
		this.state.disableSetting(Setting.SUBSTITUTION);
		
		Expression e0 = aof.expression.opt(this);
		
		if (e0 != null && (e0 instanceof IDRef || e0 instanceof IDRefWriteback || 
				e0 instanceof ArraySelect || e0 instanceof StructSelect || e0 instanceof StructureInit)) {
			
			if (e0 instanceof IDRef) {
				IDRef ref = (IDRef) e0;
				this.state.setReferenced(ref.origin);
			}
			
			if (e0 instanceof IDRefWriteback) {
				IDRefWriteback ref = (IDRefWriteback) e0;
				this.state.setReferenced(ref.idRef.origin);
			}
			
			aof.expression = e0;
		}
		
		this.state.enableSetting(Setting.SUBSTITUTION);
		
		List<IDRef> refs = aof.expression.visit(x -> x instanceof IDRef);
		refs.stream().forEach(x -> {
			this.state.setReferenced(x.origin);
		});
		
		return aof;
	}

	public Expression optArrayInit(ArrayInit a) throws OPT0_EXC {
		
		for (int i = 0; i < a.elements.size(); i++) {
			Expression e0 = a.elements.get(i).opt(this);
			if (e0 != null) 
				a.elements.set(i, e0);
		}
		
		return a;
	}

	public Expression optArraySelect(ArraySelect arraySelect) throws OPT0_EXC {
		
		this.state.setRead(arraySelect.idRef.origin);
		
		return arraySelect;
	}

	public Expression optAtom(Atom atom) throws OPT0_EXC {
		return atom;
	}

	public Expression optDeref(Deref deref) throws OPT0_EXC {
		deref.expression = deref.expression.opt(this);
		
		return deref;
	}

	public Expression optFunctionRef(FunctionRef functionRef) throws OPT0_EXC {
		return functionRef;
	}

	public Expression optIDOfExpression(IDOfExpression idOfExpression) throws OPT0_EXC {
		return idOfExpression;
	}

	public Expression optIDRef(IDRef idRef) throws OPT0_EXC {
		
		this.state.setRead(idRef.origin);
		
		/* Make sure declaration was registered */
		if (!this.state.getCState().containsKey(idRef.origin))
			throw new OPT0_EXC("Unknown declaration: " + idRef.path.build());
		
		/* 
		 * If variable has not been modified, and not been referenced,
		 * and has a value, we can attempt to forward substitute.
		 * The following criteria has to be given:
		 * 	- Not modified, referenced
		 *  - No variables in the value have been referenced or modified
		 *  - The type of the substituted value is primitive
		 *  - The value does not contain a function call
		 *  - We are not in a scope that is a looped scope
		 */
		if (this.state.getSetting(Setting.SUBSTITUTION) && 
				!this.state.getReferenced(idRef.origin) && 
				this.state.get(idRef.origin).getCurrentValue() != null) {
			
			boolean operationApplicable = true;
			
			/* 
			 * Check if there are any variables in the current value. If there
			 * are variables, forward-substitution of the expression may be incorrect,
			 * since the expression substitution would be theoretically correct, but since
			 * the values have changed already, the evaluation of this expression may be
			 * different.
			 */
			Expression cValue = this.state.get(idRef.origin).getCurrentValue();
			List<IDRef> varsInExpression = cValue.visit(x -> x instanceof IDRef);
			for (IDRef ref : varsInExpression) {
				if ((!ref.origin.equals(idRef.origin) && this.state.getWrite(ref.origin)) || ref.origin.equals(idRef.origin)) {
					operationApplicable = false;
					break;
				}
			}
			
			/*
			 * Only do substitution for primitive values. Pointers
			 * are to likely to have changed data that they point to.
			 * Structs and Array types may also have changed fields.
			 * Forwards-Substitution is not always valid.
			 */
			operationApplicable &= cValue.getType().isPrimitive();
			
			/*
			 * Make sure we do not forward substitute function calls.
			 * The calls could change global state variables.
			 */
			operationApplicable &= cValue.visit(x -> x instanceof InlineCall).isEmpty();
			
			/* 
			 * Check if the current path is within a looped scope.
			 * If yes, we cannot substitute since it is possible
			 * that the value of this changing per iteration.
			 */
			operationApplicable &= !this.state.isInLoopedScope(idRef.origin);
			
			/*
			 * Make sure we only substitute a different value, prevent
			 * unneessesary OPT_DONE().
			 */
			operationApplicable &= !idRef.codePrint().equals(cValue.codePrint());
			
			/*
			 * Make sure substituted value is smaller thatn current substitution size.
			 */
			operationApplicable &= cValue.size() <= CURR_SUBS_SIZE;
			
			/*
			 * No variables in the expression changed, safe
			 * to forward-substitute.
			 */
			if (operationApplicable) {
				OPT_DONE();
				return cValue.clone();
			}
		}
		
		return idRef;
	}

	public Expression optIDRefWriteback(IDRefWriteback wb) throws OPT0_EXC {
		this.state.disableSetting(Setting.SUBSTITUTION);
		wb.idRef.opt(this);
		this.state.enableSetting(Setting.SUBSTITUTION);
		
		this.state.setWrite(wb.idRef.origin, true);
		return wb;
	}

	public Expression optInlineCall(InlineCall inlineCall) throws OPT0_EXC {
		for (int i = 0; i < inlineCall.parameters.size(); i++) {
			Expression e = inlineCall.parameters.get(i);
			
			boolean isNestedFirst = inlineCall.isNestedCall && i == 0;
			
			if (isNestedFirst) this.state.disableSetting(Setting.SUBSTITUTION);
			Expression e0 = e.opt(this);
			if (isNestedFirst) {
				this.state.enableSetting(Setting.SUBSTITUTION);
				continue;
			}
			
			
			if (e0 != null) inlineCall.parameters.set(i, e0);
		}
		
		if (inlineCall.anonTarget != null)
			this.state.setRead(inlineCall.anonTarget);
		
		return inlineCall;
	}

	public Expression optInlineFunction(InlineFunction inlineFunction) throws OPT0_EXC {
		return inlineFunction;
	}

	public Expression optRegisterAtom(RegisterAtom registerAtom) throws OPT0_EXC {
		return registerAtom;
	}

	public Expression optSizeOfExpression(SizeOfExpression sizeOfExpression) throws OPT0_EXC {
		sizeOfExpression.expression = sizeOfExpression.expression.opt(this);
		
		/* If there are no proviso types in the type of the expression, size is static */
		if (!sizeOfExpression.expression.getType().hasProviso()) {
			Atom size = new Atom(new INT("" + sizeOfExpression.expression.getType().wordsize()), sizeOfExpression.getSource());
			OPT_DONE();
			return size;
		}
		
		return sizeOfExpression;
	}

	public Expression optSizeOfType(SizeOfType sizeOfType) throws OPT0_EXC {
		
		/* If there are no proviso types in the type, size is static */
		if (!sizeOfType.sizeType.hasProviso()) {
			Atom size = new Atom(new INT("" + sizeOfType.sizeType.wordsize()), sizeOfType.getSource());
			OPT_DONE();
			return size;
		}
		
		return sizeOfType;
	}

	public Expression optStructSelect(StructSelect structSelect) throws OPT0_EXC {
		if (structSelect.selector instanceof IDRef) {
			IDRef ref = (IDRef) structSelect.selector;
			this.state.setRead(ref.origin);
		}
		else structSelect.selector.opt(this);
		
		return structSelect;
	}

	public Expression optStructSelectWriteback(StructSelectWriteback structSelectWriteback) throws OPT0_EXC {
		return structSelectWriteback;
	}

	public Expression optStructureInit(StructureInit s) throws OPT0_EXC {
		
		for (int i = 0; i < s.elements.size(); i++) {
			Expression e0 = s.elements.get(i).opt(this);
			if (e0 != null) 
				s.elements.set(i, e0);
		}
		
		return s;
	}

	public Expression optTempAtom(TempAtom tempAtom) throws OPT0_EXC {
		if (tempAtom.base != null)
			tempAtom.base = tempAtom.base.opt(this);
		
		return tempAtom;
	}

	public Expression optTypeCast(TypeCast typeCast) throws OPT0_EXC {
		typeCast.expression = typeCast.expression.opt(this);
		
		return typeCast;
	}

	public Expression optAdd(Add add) throws OPT0_EXC {
		
		add.operands = this.optExpressionList(add.operands);
		
		/* Multiple equal operands, collapse into multiplication */
		for (int i = 0; i < add.operands.size(); i++) {
			Expression e0 = add.operands.get(i);
			
			/**
			 * We cannot fold sub-expressions that modify variables or the program state.
			 */
			boolean block = !e0.visit(x -> x instanceof InlineCall || x instanceof IDRefWriteback).isEmpty();
			
			if (!block) {
				List<Integer> indicies = new ArrayList();
				
				for (int a = i + 1; a < add.operands.size(); a++) {
					Expression e1 = add.operands.get(a);
					if (e0.codePrint().equals(e1.codePrint())) indicies.add(a);
				}
				
				if (!indicies.isEmpty()) {
					for (int a = indicies.size() - 1; a >= 0; a--) {
						int index = indicies.get(a);
						add.operands.remove(index);
					}
					
					Mul mul = new Mul(e0, new Atom(new INT("" + (indicies.size() + 1)), e0.getSource()), e0.getSource());
					add.operands.set(i, mul);
					mul.setType(e0.getType().clone());
					
					OPT_DONE();
				}
			}
		}
		
		/* Flatten Tree of Additions into one N-Fold Expression */
		for (int i = 0; i < add.operands.size(); i++) {
			if (add.operands.get(i) instanceof Add) {
				Add add0 = (Add) add.operands.remove(i);
				i--;
				add.operands.addAll(i + 1, add0.operands);
				OPT_DONE();
			}
		}
		
		/* Precalc */
		int sum = 0, c = 0;
		for (int i = 0; i < add.operands.size(); i++) {
			Expression e = add.operands.get(i);
			
			if (e instanceof Atom && e.getType().hasInt()) {
				sum += e.getType().toInt();
				add.operands.remove(i);
				i--;
				c++;
			}
		}
		if (sum != 0) {
			Atom atom = new Atom(new INT("" + sum), add.getSource());
			add.operands.add(atom);
			if (c > 1) OPT_DONE();
		}
		
		/* Only single operand -> Return operand */
		if (add.operands.size() == 1) {
			OPT_DONE();
			return add.operands.get(0);
		}
		
		return add;
	}

	public Expression optBitAnd(BitAnd b) throws OPT0_EXC {
		b.operands = this.optExpressionList(b.operands);
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : b.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = b.operands.get(0).getType().clone();
			
			for (Expression e : b.operands)
				if (!e.equals(b.operands.get(0)))
					t0.value = t0.toInt() & e.getType().toInt();
			
			Atom atom = new Atom(t0, b.getSource());
			OPT_DONE();
			return atom;
		}
		
		return b;
	}

	public Expression optBitNot(BitNot bitNot) throws OPT0_EXC {
		bitNot.operand = bitNot.operand.opt(this);
		
		/* Precalc */
		if (bitNot.operand instanceof Atom && bitNot.operand.getType().hasInt()) {
			int value = bitNot.operand.getType().toInt();
			
			TYPE t0 = bitNot.operand.getType().clone();
			t0.value = ~value;
			
			Atom atom = new Atom(t0, bitNot.getSource());
			OPT_DONE();
			return atom;
		}
		
		return bitNot;
	}

	public Expression optBitOr(BitOr b) throws OPT0_EXC {
		b.operands = this.optExpressionList(b.operands);
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : b.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = b.operands.get(0).getType().clone();
			
			for (Expression e : b.operands)
				if (!e.equals(b.operands.get(0)))
					t0.value = t0.toInt() | e.getType().toInt();
			
			Atom atom = new Atom(t0, b.getSource());
			OPT_DONE();
			return atom;
		}
		
		return b;
	}

	public Expression optBitXor(BitXor b) throws OPT0_EXC {
		b.operands = this.optExpressionList(b.operands);
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : b.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = b.operands.get(0).getType().clone();
			
			for (Expression e : b.operands)
				if (!e.equals(b.operands.get(0)))
					t0.value = t0.toInt() ^ e.getType().toInt();
			
			Atom atom = new Atom(t0, b.getSource());
			OPT_DONE();
			return atom;
		}
		
		return b;
	}

	public Expression optLsl(Lsl lsl) throws OPT0_EXC {
		lsl.operands = this.optExpressionList(lsl.operands);
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : lsl.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = lsl.operands.get(0).getType().clone();
			
			for (Expression e : lsl.operands)
				if (!e.equals(lsl.operands.get(0)))
					t0.value = t0.toInt() << e.getType().toInt();
			
			Atom atom = new Atom(t0, lsl.getSource());
			OPT_DONE();
			return atom;
		}
		
		return lsl;
	}

	public Expression optLsr(Lsr lsr) throws OPT0_EXC {
		lsr.operands = this.optExpressionList(lsr.operands);
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : lsr.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = lsr.operands.get(0).getType().clone();
			
			for (Expression e : lsr.operands)
				if (!e.equals(lsr.operands.get(0)))
					t0.value = t0.toInt() >> e.getType().toInt();
			
			Atom atom = new Atom(t0, lsr.getSource());
			OPT_DONE();
			return atom;
		}
		
		return lsr;
	}

	public Expression optMul(Mul mul) throws OPT0_EXC {
		mul.operands = this.optExpressionList(mul.operands);
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : mul.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = mul.operands.get(0).getType().clone();
			
			for (Expression e : mul.operands)
				if (!e.equals(mul.operands.get(0)))
					t0.value = t0.toInt() * e.getType().toInt();
			
			Atom atom = new Atom(t0, mul.getSource());
			OPT_DONE();
			return atom;
		}
		
		return mul;
	}
	
	public Expression optSub(Sub s) throws OPT0_EXC {
		s.operands = this.optExpressionList(s.operands);
		
		if (s.operands.size() == 2) {
			/* -4 - -5 = 5 - 4 = 1 */
			if (s.operands.get(0) instanceof UnaryMinus && s.operands.get(1) instanceof UnaryMinus) {
				UnaryMinus left = (UnaryMinus) s.operands.get(0);
				UnaryMinus right = (UnaryMinus) s.operands.get(1);
				
				s.operands.set(0, right.getOperand());
				s.operands.set(1, left.getOperand());
				
				OPT_DONE();
			}
			
			/* 4 - -5 = 4 + 5 = 9 */
			if (s.operands.get(1) instanceof UnaryMinus) {
				UnaryMinus right = (UnaryMinus) s.operands.get(1);
				OPT_DONE();
				return new Add(s.operands.get(0), right.getOperand(), s.getSource());
			}
		}
		
		/* Precalc */
		boolean allAtom = true;
		for (Expression e : s.operands) allAtom &= e instanceof Atom && e.getType().hasInt();
		if (allAtom) {
			TYPE t0 = s.operands.get(0).getType().clone();
			
			for (Expression e : s.operands)
				if (!e.equals(s.operands.get(0)))
					t0.value = t0.toInt() - e.getType().toInt();
			
			Atom atom = new Atom(t0, s.getSource());
			OPT_DONE();
			return atom;
		}
		
		return s;
	}
	
	public Expression optUnaryMinus(UnaryMinus m) throws OPT0_EXC {
		m.operand = m.operand.opt(this);
		
		/* --5 = 5 */
		if (m.getOperand() instanceof UnaryMinus) {
			UnaryMinus m0 = (UnaryMinus) m.getOperand();
			OPT_DONE();
			return m0.getOperand();
		}
		
		/* - -5 = 5 */
		if (m.getOperand() instanceof Atom && m.getOperand().getType().hasInt()) {
			int value = m.getOperand().getType().toInt();
			if (value < 0) {
				Atom atom = (Atom) m.getOperand();
				atom.getType().value = -value;
				OPT_DONE();
				return atom;
			}
		}
		
		return m;
	}

	public Expression optAnd(And and) throws OPT0_EXC {
		and.operands = this.optExpressionList(and.operands);
		
		/* false || value = false */
		boolean hasFalseAtom = false;
		for (Expression e : and.operands) hasFalseAtom |= e instanceof Atom && e.getType().hasInt() && e.getType().toInt() == 0;
		if (hasFalseAtom) {
			Atom atom = new Atom(new BOOL("" + false), and.operands.get(0).getSource());		
			OPT_DONE();
			return atom;
		}
		
		return and;
	}

	public Expression optCompare(Compare c) throws OPT0_EXC {
		c.operands = this.optExpressionList(c.operands);
		
		/*
		 * If we get an equal operator, we can retrieve some information about the current
		 * value of a variable under certain circumstances. This information is active within
		 * the current scope.
		 * 
		 * a == 20 -> if true, a is 20 currently
		 */
		if (c.comparator == COMPARATOR.EQUAL) {
			if (c.operands.get(0) instanceof IDRef) {
				IDRef ref = (IDRef) c.operands.get(0);
				this.state.get(ref.origin).setCurrentValue(c.operands.get(1).clone());
			}
			else if (c.operands.get(1) instanceof IDRef) {
				IDRef ref = (IDRef) c.operands.get(1);
				this.state.get(ref.origin).setCurrentValue(c.operands.get(0).clone());
			}
		}
		
		Expression op0 = c.operands.get(0);
		Expression op1 = c.operands.get(1);
		
		/*
		 * Pre-Compute comparison and replace with direct value.
		 */
		if  (op0 instanceof Atom && op0.getType().hasInt() && op1 instanceof Atom && op1.getType().hasInt()) {
			int val0 = op0.getType().toInt(), val1 = op1.getType().toInt();
			
			boolean res = false;
			if (c.comparator == COMPARATOR.EQUAL) res = val0 == val1;
			if (c.comparator == COMPARATOR.GREATER_SAME) res = val0 >= val1;
			if (c.comparator == COMPARATOR.GREATER_THAN) res = val0 > val1;
			if (c.comparator == COMPARATOR.LESS_SAME) res = val0 <= val1;
			if (c.comparator == COMPARATOR.LESS_THAN) res = val0 < val1;
			if (c.comparator == COMPARATOR.NOT_EQUAL) res = val0 != val1;
			
			OPT_DONE();
			return new Atom(new BOOL("" + res), op0.getSource());
		}
		
		return c;
	}

	public Expression optNot(Not not) throws OPT0_EXC {
		not.operand = not.operand.opt(this);
		
		/* !!value = value */
		if (not.getOperand() instanceof Not) {
			Not m0 = (Not) not.getOperand();
			OPT_DONE();
			return m0.getOperand();
		}
		
		/* !true = false */
		if (not.getOperand() instanceof Atom && not.getOperand().getType().hasInt()) {
			int value = not.getOperand().getType().toInt();
			
			Atom atom = (Atom) not.getOperand();
			
			if (value == 0) atom.getType().value = true;
			else atom.getType().value = false;
			
			OPT_DONE();
			return atom;
		}
		
		return not;
	}

	public Expression optOr(Or or) throws OPT0_EXC {
		or.operands = this.optExpressionList(or.operands);
		
		/* true || value = true */
		boolean hasTrueAtom = false;
		for (Expression e : or.operands) hasTrueAtom |= e instanceof Atom && e.getType().hasInt() && e.getType().toInt() != 0;
		if (hasTrueAtom) {
			Atom atom = new Atom(new BOOL("" + true), or.operands.get(0).getSource());		
			OPT_DONE();
			return atom;
		}
		
		return or;
	}

	public Expression optTernary(Ternary ternary) throws OPT0_EXC {
		
		this.pushContext();
		ternary.condition = ternary.condition.opt(this);
		this.popContext();
		
		ternary.left = ternary.left.opt(this);
		ternary.right = ternary.right.opt(this);
		
		/* (true)? a : b = a, (false)? a : b = b */
		if (ternary.condition instanceof Atom && ternary.condition.getType().hasInt()) {
			int value = ternary.condition.getType().toInt();
			OPT_DONE();
			return (value == 0)? ternary.right : ternary.left;
		}
		
		return ternary;
	}

	public LhsId optArraySelectLhsId(ArraySelectLhsId arraySelectLhsId) throws OPT0_EXC {
		
		this.state.setWrite(arraySelectLhsId.selection.idRef.origin, true);
		
		return arraySelectLhsId;
	}

	public LhsId optPointerLhsId(PointerLhsId pointerLhsId) throws OPT0_EXC {
		
		/* 
		 * Cannot say for sure which variable is targeted, mark all occurring variables as
		 * potential candidates. For each found reference, mark the variable as written to.
		 */
		List<IDRef> refs = pointerLhsId.deref.visit(x -> x instanceof IDRef);
		refs.stream().forEach(x -> {
			this.state.setWrite(x.origin, true);
		});
		
		if (refs.isEmpty()) {
			
			/* No variables are used, this must be an unsafe operation. */
			// TODO: Enable unsafe operation from here
			
		}
		
		return pointerLhsId;
	}

	public LhsId optSimpleLhsId(SimpleLhsId simpleLhsId) throws OPT0_EXC {
		
		if (simpleLhsId.origin != null)
			this.state.setWrite(simpleLhsId.origin, true);
		
		return simpleLhsId;
	}

	public LhsId optStructSelectLhsId(StructSelectLhsId structSelectLhsId) throws OPT0_EXC {
		structSelectLhsId.select.opt(this);
		
		return structSelectLhsId;
	}

	public Statement optAssignment(Assignment assignment) throws OPT0_EXC {
		
		Declaration origin = null;
		
		/*
		 * Extract data origin if it is a SimpleLhsId.
		 */
		if (assignment.lhsId instanceof SimpleLhsId) {
			SimpleLhsId lhs = (SimpleLhsId) assignment.lhsId;
			
			if (lhs.origin != null) {
				origin = lhs.origin;
			}
		}
		
		assignment.value = assignment.value.opt(this);
		assignment.lhsId = assignment.lhsId.opt(this);
		
		if (origin != null && assignment.assignArith == ASSIGN_ARITH.NONE) {
			
			Expression currentValue = this.state.get(origin).getCurrentValue();
			
			/**
			 * Make sure upward propagation of this assignment is side-effect free.
			 * We need to make sure that in this expression, no inline functions are called, 
			 * since they may modify the state. Also, no writeback operations may take place.
			 * Additionally, the assignment must be in the same scope as the declaration - otherwise
			 * we risk that the value is propagated through a loop or condition.
			 * Finally, the assigned value has to be morphable in order to propagate the value
			 * back up.
			 */
			if (!Matchers.containsStateDependentSubExpression(assignment.value, this.state) &&
				this.state.isDeclarationScope(origin) && currentValue != null &&
				Matchers.isMorphable(assignment.value, origin, this.state)) {
				
				Expression toMorph = assignment.value.clone();
					
				/**
				 * Morph the assigned value into the exisiting, current value
				 * of the variable.
				 */
				final Declaration origin0 = origin;
				ExpressionMorpher.morphExpression(toMorph, x -> {
					if (x instanceof IDRef) {
						IDRef ref = (IDRef) x;
						return ref.origin.equals(origin0);
					}
					
					return false;
				}, currentValue);
				
				/* 
				 * Set the new current value both in program state 
				 * and to actual declaration in code 
				 */
				this.state.get(origin).setCurrentValue(toMorph);
				origin.value = toMorph;
			
				/* 
				 * We can safely delete the assignment here, since the value change 
				 * has been propagated upwards.
				 */
				OPT_DONE();
				return null;
			}
			
			/**
			 * Set value that was assigned to variable in program context
			 */
			this.state.get(origin).setCurrentValue(assignment.value);
		}
		
		return assignment;
	}

	public Statement optAssignWriteback(AssignWriteback wb) throws OPT0_EXC {
		wb.reference.opt(this);
		
		// TODO Check if incremented value is in same scope, and has atom as value. If yes, write back changes to current value
		
		/*
		 * Signal in the current program context that the target of the writeback
		 * operations has been modified.
		 */
		if (wb.reference instanceof IDRefWriteback) {
			IDRefWriteback idwb = (IDRefWriteback) wb.reference;
			this.state.setWrite(idwb.idRef.origin, true);
			this.state.get(idwb.idRef.origin).clearCurrentValue();
		}
		else {
			StructSelectWriteback sswb = (StructSelectWriteback) wb.reference;
			if (sswb.select.selector instanceof IDRef) {
				IDRef ref = (IDRef) sswb.select.selector;
				this.state.setWrite(ref.origin, true);
				this.state.get(ref.origin).clearCurrentValue();
			}
		}
		
		return wb;
	}

	public Statement optBreakStatement(BreakStatement breakStatement) throws OPT0_EXC {
		return breakStatement;
	}

	public Statement optCaseStatement(CaseStatement c) throws OPT0_EXC {
		c.body = this.optBody(c.body, true, true);
		c.condition = c.condition.opt(this);
		
		/* Case is not satisfiable */
		if (Makros.isAlwaysFalse(c.condition)) return null;
		
		return c;
	}

	public Statement optContinueStatement(ContinueStatement continueStatement) throws OPT0_EXC {
		return continueStatement;
	}

	public Statement optDeclaration(Declaration declaration) throws OPT0_EXC {
		if (declaration.value != null) 
			declaration.value = declaration.value.opt(this);
		
		this.state.register(declaration);
		
		return declaration;
	}

	public Statement optDefaultStatement(DefaultStatement defaultStatement) throws OPT0_EXC {
		defaultStatement.body = this.optBody(defaultStatement.body, false, true);
		
		return defaultStatement;
	}

	public Statement optDirectASMStatement(DirectASMStatement d) throws OPT0_EXC {
		for (Pair<Expression, REG> pair : d.dataOut) {
			IDRef ref = (IDRef) pair.first;
			
			this.state.disableSetting(Setting.SUBSTITUTION);
			pair.first.opt(this);
			this.state.enableSetting(Setting.SUBSTITUTION);
			
			this.state.setWrite(ref.origin, true);
		}
		
		for (Pair<Expression, REG> pair : d.dataIn) 
			pair.first = pair.first.opt(this);
		
		return d;
	}

	public Statement optDoWhileStatement(DoWhileStatement doWhileStatement) throws OPT0_EXC {
		doWhileStatement.condition = doWhileStatement.condition.opt(this);
		doWhileStatement.body = this.optBody(doWhileStatement.body, true, true);
		
		return doWhileStatement;
	}

	public Statement optForEachStatement(ForEachStatement forEachStatement) throws OPT0_EXC {
		this.pushContext();
		forEachStatement.iterator.opt(this);
		forEachStatement.counter.opt(this);
		
		this.state.disableSetting(Setting.SUBSTITUTION);
		forEachStatement.shadowRef.opt(this);
		this.state.enableSetting(Setting.SUBSTITUTION);
		
		forEachStatement.body = this.optBody(forEachStatement.body, true, true);
		
		this.popContext();
		return forEachStatement;
	}

	public Statement optForStatement(ForStatement f) throws OPT0_EXC {
		this.pushContext();
		f.iterator.opt(this);
		this.state.setWrite(f.iterator, true);
		
		f.body = this.optBody(f.body, true, true);
		
		f.increment = f.increment.opt(this);
		f.condition = f.condition.opt(this);
		
		this.popContext();
		return f;
	}

	public Statement optFunctionCall(FunctionCall functionCall) throws OPT0_EXC {
		for (int i = 0; i < functionCall.parameters.size(); i++) {
			Expression e = functionCall.parameters.get(i);
			
			boolean isNestedFirst = functionCall.isNestedCall && i == 0;
			
			if (isNestedFirst) this.state.disableSetting(Setting.SUBSTITUTION);
			Expression e0 = e.opt(this);
			if (isNestedFirst) {
				this.state.enableSetting(Setting.SUBSTITUTION);
				continue;
			}
			
			
			if (e0 != null) functionCall.parameters.set(i, e0);
		}
		
		if (functionCall.anonTarget != null)
			this.state.setRead(functionCall.anonTarget);
		
		return functionCall;
	}

	public Statement optIfStatement(IfStatement i) throws OPT0_EXC {
		if (i.condition != null)
			i.condition = i.condition.opt(this);
		
		i.body = this.optBody(i.body, false, true);
		
		/*
		 * Condition is always true, prune else statement.
		 */
		if (i.condition != null && Makros.isAlwaysTrue(i.condition) && i.elseStatement != null) {
			i.elseStatement = null;
			OPT_DONE();
		}
		
		/*
		 * Condition is always false, remove from chain.
		 */
		if (i.condition != null && Makros.isAlwaysFalse(i.condition)) {
			OPT_DONE();
			return i.elseStatement;
		}
		
		if (i.elseStatement != null) {
			Statement s = i.elseStatement.opt(this);
			if (s instanceof IfStatement)
				i.elseStatement = (IfStatement) s;
		}
		
		/*
		 * Body is empty, check if it is safe to remove this if-statement
		 * from the if-else chain or from the statement block.
		 */
		if (i.body.isEmpty()) {
			if (i.condition == null) {
				OPT_DONE();
				return null;
			}
			else {
				if (!Matchers.containsStateChangingSubExpression(i.condition, this.state)) {
					OPT_DONE();
					if (i.elseStatement != null) return i.elseStatement;
					else return null;
				}
			}
		}
		
		return i;
	}

	public Statement optReturnStatement(ReturnStatement returnStatement) throws OPT0_EXC {
		if (returnStatement.value != null)
			returnStatement.value = returnStatement.value.opt(this);
		
		return returnStatement;
	}

	public Statement optSignalStatement(SignalStatement signalStatement) throws OPT0_EXC {
		signalStatement.exceptionInit = (StructureInit) signalStatement.exceptionInit.opt(this);
		
		return signalStatement;
	}

	public Statement optSwitchStatement(SwitchStatement s) throws OPT0_EXC {
		
		this.state.disableSetting(Setting.SUBSTITUTION);
		s.condition.opt(this);
		this.state.enableSetting(Setting.SUBSTITUTION);
		
		IDRef ref = (IDRef) s.condition;
		Expression value = this.state.get(ref.origin).getCurrentValue();
		
		/*
		 * Condition of switch statement has value, we can attempt to match
		 * it against case conditions to determine always-false cases.
		 */
		if (value != null) {
			for (int i = 0; i < s.cases.size(); i++) {
				CaseStatement c0 = s.cases.get(i);
				c0.opt(this);
				
				/*
				 * Value of switch statement condition and condition of case 
				 * do not match, case will never be executed.
				 */
				if (Makros.atomComparable(value, c0.condition) && !Makros.compareAtoms(value, c0.condition)) {
					s.cases.remove(i);
					i--;
					OPT_DONE();
				}
			}
		}
		
		s.defaultStatement.opt(this);
		
		/*
		 * No cases left, return contents of default statement.
		 */
		if (s.cases.isEmpty()) {
			OPT_DONE();
			return s.defaultStatement;
		}
		
		return s;
	}

	public Statement optTryStatement(TryStatement tryStatement) throws OPT0_EXC {
		tryStatement.body = this.optBody(tryStatement.body, false, true);
		
		for (int i = 0; i < tryStatement.watchpoints.size(); i++)
			tryStatement.watchpoints.set(i, (WatchStatement) tryStatement.watchpoints.get(i).opt(this));
		
		return tryStatement;
	}

	public Statement optWatchStatement(WatchStatement watchStatement) throws OPT0_EXC {
		watchStatement.body = this.optBody(watchStatement.body, false, true);
		
		return watchStatement;
	}

	public Statement optWhileStatement(WhileStatement whileStatement) throws OPT0_EXC {
		whileStatement.body = this.optBody(whileStatement.body, true, true);
		whileStatement.condition = whileStatement.condition.opt(this);
		
		return whileStatement;
	}

	public SyntaxElement optEnumTypedef(EnumTypedef enumTypedef) throws OPT0_EXC {
		return enumTypedef;
	}

	public SyntaxElement optInterfaceTypedef(InterfaceTypedef interfaceTypedef) throws OPT0_EXC {
		return interfaceTypedef;
	}

	public SyntaxElement optStructTypedef(StructTypedef structTypedef) throws OPT0_EXC {
		for (Function f : structTypedef.functions) f.opt(this);
		
		return structTypedef;
	}
	
	private void pushContext(boolean isLoopedContext) {
		this.state = new ProgramState(this.cStack.peek(), isLoopedContext);
		this.cStack.push(this.state);
	}
	
	private void pushContext() {
		this.state = new ProgramState(this.cStack.peek(), false);
		this.cStack.push(this.state);
	}
	
	private void popContext() {
		this.cStack.pop().transferContextChangeToParent();
		this.state = this.cStack.peek();
	}
	
}
