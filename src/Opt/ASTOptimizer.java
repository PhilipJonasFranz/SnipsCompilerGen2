package Opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import Exc.OPT0_EXC;
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
import Imm.AST.Typedef.EnumTypedef;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.TYPE;
import Opt.Util.ProgramContext;
import Opt.Util.ProgramContext.VarState;
import Res.Setting;
import Snips.CompilerDriver;

public class ASTOptimizer {

	private Stack<ProgramContext> cStack = new Stack();
	
	private ProgramContext state = null;
	
	private boolean OPT_DONE = true;
	
	private void OPT_DONE() {
		OPT_DONE = true;
	}
	
	public Program optProgram(Program AST) throws OPT0_EXC {
		try {
			while (OPT_DONE) {
				OPT_DONE = false;
				
				this.state = new ProgramContext(null);
				this.cStack.push(state);
				
				CompilerDriver.HEAP_START.opt(this);
				CompilerDriver.NULL_PTR.opt(this);
				
				for (int i = 0; i < AST.programElements.size(); i++) {
					SyntaxElement s = AST.programElements.get(i);
					
					SyntaxElement s0 = s.opt(this);
					
					if (s0 == null) {
						AST.programElements.remove(i);
						i--;
					}
					else {
						AST.programElements.set(AST.programElements.indexOf(s), s0);
					}
				}
				
				this.cStack.pop().transferContextChangeToParent();
				this.state = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new OPT0_EXC("An error occurred during AST-Optimization: " + e.getMessage());
		}
		
		return AST;
	}
	
	public Function optFunction(Function f) throws OPT0_EXC {
		
		/* Function was not called, wont be translated anyway */
		if (f.provisosCalls.isEmpty()) return f;
		
		this.pushContext();
		
		/* Register declarations */
		for (Declaration dec : f.parameters) dec.opt(this);
		
		f.body = this.optBody(f.body);
		
		this.popContext();
		return f;
	}
	
	public List<Statement> optBody(List<Statement> body) throws OPT0_EXC {
		this.pushContext();
		
		for (int i = 0; i < body.size(); i++) {
			Statement s = body.get(i);
			Statement s0 = s.opt(this);
			
			if (s0 == null) {
				body.remove(i);
				i--;
			}
			else {
				body.set(body.indexOf(s), s0);
			}
		}
		
		List<Declaration> removed = new ArrayList();
		for (Entry<Declaration, VarState> entry : this.state.cState.entrySet()) {
			Declaration dec = entry.getKey();
			
			if (false && !this.state.getRead(dec) && !this.state.getReferenced(dec) && !this.state.getWrite(dec)) {
				if (body.contains(dec)) body.remove(dec);
				removed.add(dec);
			}
		}
		
		removed.stream().forEach(x -> {
			this.state.cState.remove(x);
		});
		
		this.popContext();
		return body;
	}

	public Expression optAddressOf(AddressOf addressOf) throws OPT0_EXC {
		this.state.disableSetting(Setting.SUBSTITUTION);
		
		Expression e0 = addressOf.expression.opt(this);
		
		if (e0 != null && (e0 instanceof IDRef || e0 instanceof IDRefWriteback || 
				e0 instanceof ArraySelect || e0 instanceof StructSelect || e0 instanceof StructureInit)) {
			addressOf.expression = e0;
		}
		
		this.state.enableSetting(Setting.SUBSTITUTION);
		return addressOf;
	}

	public Expression optArrayInit(ArrayInit arrayInit) throws OPT0_EXC {
		return arrayInit;
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
		
		/* If variable has not been overwritten, substitute */
		if (false && this.state.getSetting(Setting.SUBSTITUTION) && !this.state.getWrite(idRef.origin) && 
				this.state.cState.get(idRef.origin).currentValue != null) {
			OPT_DONE();
			return this.state.cState.get(idRef.origin).currentValue.clone();
		}
		
		return idRef;
	}

	public Expression optIDRefWriteback(IDRefWriteback idRefWriteback) throws OPT0_EXC {
		return idRefWriteback;
	}

	public Expression optInlineCall(InlineCall inlineCall) throws OPT0_EXC {
		for (int i = 0; i < inlineCall.parameters.size(); i++) {
			Expression e = inlineCall.parameters.get(i);
			Expression e0 = e.opt(this);
			
			if (e0 != null) inlineCall.parameters.set(i, e0);
		}
		
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
		
		if (!sizeOfExpression.expression.getType().hasProviso()) {
			Atom size = new Atom(new INT("" + sizeOfExpression.expression.getType().wordsize()), sizeOfExpression.getSource());
			OPT_DONE();
			return size;
		}
		
		return sizeOfExpression;
	}

	public Expression optSizeOfType(SizeOfType sizeOfType) throws OPT0_EXC {
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

	public Expression optStructureInit(StructureInit structureInit) throws OPT0_EXC {
		return structureInit;
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
		add.left = add.left.opt(this);
		add.right = add.right.opt(this);
		
		/* Precalc */
		if (add.left instanceof Atom && add.right instanceof Atom && 
				add.left.getType().hasInt() && add.right.getType().hasInt()) {
			int left = add.left.getType().toInt();
			int right = add.right.getType().toInt();
			
			TYPE t0 = add.left.getType().clone();
			t0.value = left + right;
			
			Atom atom = new Atom(t0, add.getSource());
			OPT_DONE();
			return atom;
		}
		
		return add;
	}

	public Expression optBitAnd(BitAnd bitAnd) throws OPT0_EXC {
		bitAnd.left = bitAnd.left.opt(this);
		bitAnd.right = bitAnd.right.opt(this);
		
		/* Precalc */
		if (bitAnd.left instanceof Atom && bitAnd.right instanceof Atom && 
				bitAnd.left.getType().hasInt() && bitAnd.right.getType().hasInt()) {
			int left = bitAnd.left.getType().toInt();
			int right = bitAnd.right.getType().toInt();
			
			TYPE t0 = bitAnd.left.getType().clone();
			t0.value = left & right;
			
			Atom atom = new Atom(t0, bitAnd.getSource());
			OPT_DONE();
			return atom;
		}
		
		return bitAnd;
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

	public Expression optBitOr(BitOr bitOr) throws OPT0_EXC {
		bitOr.left = bitOr.left.opt(this);
		bitOr.right = bitOr.right.opt(this);
		
		/* Precalc */
		if (bitOr.left instanceof Atom && bitOr.right instanceof Atom && 
				bitOr.left.getType().hasInt() && bitOr.right.getType().hasInt()) {
			int left = bitOr.left.getType().toInt();
			int right = bitOr.right.getType().toInt();
			
			TYPE t0 = bitOr.left.getType().clone();
			t0.value = left | right;
			
			Atom atom = new Atom(t0, bitOr.getSource());
			OPT_DONE();
			return atom;
		}
		
		return bitOr;
	}

	public Expression optBitXor(BitXor bitXor) throws OPT0_EXC {
		bitXor.left = bitXor.left.opt(this);
		bitXor.right = bitXor.right.opt(this);
		
		/* Precalc */
		if (bitXor.left instanceof Atom && bitXor.right instanceof Atom && 
				bitXor.left.getType().hasInt() && bitXor.right.getType().hasInt()) {
			int left = bitXor.left.getType().toInt();
			int right = bitXor.right.getType().toInt();
			
			TYPE t0 = bitXor.left.getType().clone();
			t0.value = left ^ right;
			
			Atom atom = new Atom(t0, bitXor.getSource());
			OPT_DONE();
			return atom;
		}
		
		return bitXor;
	}

	public Expression optLsl(Lsl lsl) throws OPT0_EXC {
		lsl.left = lsl.left.opt(this);
		lsl.right = lsl.right.opt(this);
		
		/* Precalc */
		if (lsl.left instanceof Atom && lsl.right instanceof Atom && 
				lsl.left.getType().hasInt() && lsl.right.getType().hasInt()) {
			int left = lsl.left.getType().toInt();
			int right = lsl.right.getType().toInt();
			
			TYPE t0 = lsl.left.getType().clone();
			t0.value = left << right;
			
			Atom atom = new Atom(t0, lsl.getSource());
			OPT_DONE();
			return atom;
		}
		
		return lsl;
	}

	public Expression optLsr(Lsr lsr) throws OPT0_EXC {
		lsr.left = lsr.left.opt(this);
		lsr.right = lsr.right.opt(this);
		
		/* Precalc */
		if (lsr.left instanceof Atom && lsr.right instanceof Atom && 
				lsr.left.getType().hasInt() && lsr.right.getType().hasInt()) {
			int left = lsr.left.getType().toInt();
			int right = lsr.right.getType().toInt();
			
			TYPE t0 = lsr.left.getType().clone();
			t0.value = left >> right;
			
			Atom atom = new Atom(t0, lsr.getSource());
			OPT_DONE();
			return atom;
		}
		
		return lsr;
	}

	public Expression optMul(Mul mul) throws OPT0_EXC {
		mul.left = mul.left.opt(this);
		mul.right = mul.right.opt(this);
		
		/* Precalc */
		if (mul.left instanceof Atom && mul.right instanceof Atom && 
				mul.left.getType().hasInt() && mul.right.getType().hasInt()) {
			int left = mul.left.getType().toInt();
			int right = mul.right.getType().toInt();
			
			TYPE t0 = mul.left.getType().clone();
			t0.value = left * right;
			
			Atom atom = new Atom(t0, mul.getSource());
			OPT_DONE();
			return atom;
		}
		
		return mul;
	}
	
	public Expression optSub(Sub s) throws OPT0_EXC {
		s.left = s.left.opt(this);
		s.right = s.right.opt(this);
		
		/* -4 - -5 = 5 - 4 = 1 */
		if (s.left instanceof UnaryMinus && s.right instanceof UnaryMinus) {
			UnaryMinus left = (UnaryMinus) s.left;
			UnaryMinus right = (UnaryMinus) s.right;
			
			s.left = right.getOperand();
			s.right = left.getOperand();
			
			OPT_DONE();
		}
		
		/* 4 - -5 = 4 + 5 = 9 */
		if (s.right instanceof UnaryMinus) {
			UnaryMinus right = (UnaryMinus) s.right;
			OPT_DONE();
			return new Add(s.left, right.getOperand(), s.getSource());
		}
		
		/* Precalc */
		if (s.left instanceof Atom && s.right instanceof Atom && 
				s.left.getType().hasInt() && s.right.getType().hasInt()) {
			int left = s.left.getType().toInt();
			int right = s.right.getType().toInt();
			
			TYPE t0 = s.left.getType().clone();
			t0.value = left - right;
			
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
		and.left = and.left.opt(this);
		and.right = and.right.opt(this);
		
		/* false || value = false */
		if (and.left instanceof Atom && and.left.getType().hasInt()) {
			int value = and.left.getType().toInt();
			
			if (value == 0) {
				Atom atom = (Atom) and.left;
				atom.getType().value = false;
				OPT_DONE();
				return atom;
			}
		}
		
		/* value || false = false */
		if (and.right instanceof Atom && and.right.getType().hasInt()) {
			int value = and.right.getType().toInt();
			
			if (value == 0) {
				Atom atom = (Atom) and.right;
				atom.getType().value = false;
				OPT_DONE();
				return atom;
			}
		}
		
		return and;
	}

	public Expression optCompare(Compare compare) throws OPT0_EXC {
		compare.left = compare.left.opt(this);
		compare.right = compare.right.opt(this);
		
		return compare;
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
		or.left = or.left.opt(this);
		or.right = or.right.opt(this);
		
		/* true || value = true */
		if (or.left instanceof Atom && or.left.getType().hasInt()) {
			int value = or.left.getType().toInt();
			
			if (value != 0) {
				Atom atom = (Atom) or.left;
				atom.getType().value = true;
				OPT_DONE();
				return atom;
			}
		}
		
		/* value || true = true */
		if (or.right instanceof Atom && or.right.getType().hasInt()) {
			int value = or.right.getType().toInt();
			
			if (value != 0) {
				Atom atom = (Atom) or.right;
				atom.getType().value = true;
				OPT_DONE();
				return atom;
			}
		}
		
		return or;
	}

	public Expression optTernary(Ternary ternary) throws OPT0_EXC {
		ternary.condition = ternary.condition.opt(this);
		
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
		arraySelectLhsId.selection.opt(this);
		
		return arraySelectLhsId;
	}

	public LhsId optPointerLhsId(PointerLhsId pointerLhsId) throws OPT0_EXC {
		pointerLhsId.deref.opt(this);
		
		return pointerLhsId;
	}

	public LhsId optSimpleLhsId(SimpleLhsId simpleLhsId) throws OPT0_EXC {
		
		if (simpleLhsId.origin != null)
			this.state.setWrite(simpleLhsId.origin);
		
		return simpleLhsId;
	}

	public LhsId optStructSelectLhsId(StructSelectLhsId structSelectLhsId) throws OPT0_EXC {
		structSelectLhsId.select.opt(this);
		
		return structSelectLhsId;
	}

	public Statement optAssignment(Assignment assignment) throws OPT0_EXC {
		
		Declaration origin = null;
		
		if (assignment.lhsId instanceof SimpleLhsId) {
			SimpleLhsId lhs = (SimpleLhsId) assignment.lhsId;
			
			if (lhs.origin != null) {
				origin = lhs.origin;
			}
		}
		
		boolean read = true;
		if (origin != null) 
			read = this.state.getRead(origin);
		
		assignment.lhsId = assignment.lhsId.opt(this);
		assignment.value = assignment.value.opt(this);
		
		if (false && origin != null) {
			// TODO: Need to morph current expression into this expression, for example: int a = 5; a = a + 4;
			this.state.cState.get(origin).currentValue = assignment.value.clone();
		
			if (!read) {
				/* 
				 * Variable value has not been read at this point. This means
				 * we can write the new value directly to the declaration without
				 * consequences. We return null to remove the assignment.
				 */
				return null;
			}
		}
		
		return assignment;
	}

	public Statement optAssignWriteback(AssignWriteback assignWriteback) throws OPT0_EXC {
		return assignWriteback;
	}

	public Statement optBreakStatement(BreakStatement breakStatement) throws OPT0_EXC {
		return breakStatement;
	}

	public Statement optCaseStatement(CaseStatement caseStatement) throws OPT0_EXC {
		return caseStatement;
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
		return defaultStatement;
	}

	public Statement optDirectASMStatement(DirectASMStatement directASMStatement) throws OPT0_EXC {
		return directASMStatement;
	}

	public Statement optDoWhileStatement(DoWhileStatement doWhileStatement) throws OPT0_EXC {
		doWhileStatement.body = this.optBody(doWhileStatement.body);
		doWhileStatement.condition = doWhileStatement.condition.opt(this);
		
		return doWhileStatement;
	}

	public Statement optForEachStatement(ForEachStatement forEachStatement) throws OPT0_EXC {
		forEachStatement.iterator.opt(this);
		forEachStatement.counter.opt(this);
		
		forEachStatement.body = this.optBody(forEachStatement.body);
		
		return forEachStatement;
	}

	public Statement optForStatement(ForStatement forStatement) throws OPT0_EXC {
		forStatement.iterator.opt(this);
		forStatement.condition = forStatement.condition.opt(this);
		forStatement.increment = forStatement.increment.opt(this);
		
		forStatement.body = this.optBody(forStatement.body);
		
		return forStatement;
	}

	public Statement optFunctionCall(FunctionCall functionCall) throws OPT0_EXC {
		for (int i = 0; i < functionCall.parameters.size(); i++) {
			Expression e = functionCall.parameters.get(i);
			Expression e0 = e.opt(this);
			
			if (e0 != null) functionCall.parameters.set(i, e0);
		}
		
		if (functionCall.anonTarget != null)
			this.state.setRead(functionCall.anonTarget);
		
		return functionCall;
	}

	public Statement optIfStatement(IfStatement ifStatement) throws OPT0_EXC {
		if (ifStatement.condition != null)
			ifStatement.condition = ifStatement.condition.opt(this);
		
		ifStatement.body = this.optBody(ifStatement.body);
		
		if (ifStatement.elseStatement != null) 
			ifStatement.elseStatement.opt(this);
		
		return ifStatement;
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

	public Statement optSwitchStatement(SwitchStatement switchStatement) throws OPT0_EXC {
		return switchStatement;
	}

	public Statement optTryStatement(TryStatement tryStatement) throws OPT0_EXC {
		tryStatement.body = this.optBody(tryStatement.body);
		
		for (int i = 0; i < tryStatement.watchpoints.size(); i++)
			tryStatement.watchpoints.set(i, (WatchStatement) tryStatement.watchpoints.get(i).opt(this));
		
		return tryStatement;
	}

	public Statement optWatchStatement(WatchStatement watchStatement) throws OPT0_EXC {
		watchStatement.body = this.optBody(watchStatement.body);
		
		return watchStatement;
	}

	public Statement optWhileStatement(WhileStatement whileStatement) throws OPT0_EXC {
		whileStatement.condition = whileStatement.condition.opt(this);
		whileStatement.body = this.optBody(whileStatement.body);
		
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
	
	private void pushContext() {
		this.state = new ProgramContext(this.cStack.peek());
		this.cStack.push(this.state);
	}
	
	private void popContext() {
		this.cStack.pop().transferContextChangeToParent();
		this.state = this.cStack.peek();
	}
	
}
