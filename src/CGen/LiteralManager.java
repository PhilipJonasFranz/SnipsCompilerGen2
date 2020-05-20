package CGen;

import java.util.HashMap;
import java.util.Map.Entry;

import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.Memory.MemoryWordOperand;
import Imm.AsN.AsNNode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LiteralManager {

			/* --- FIELDS --- */
	/* The stored literals, and the corresponding data label */
	private HashMap<Integer, ASMDataLabel> storedLiterals = new HashMap();
	
	
			/* --- METHODS --- */
	/**
	 * Check if a label already exists that contains the given value, if yes,
	 * return this label. If not, create a new label and add it to the stored
	 * ones and return the new label.
	 */
	public ASMDataLabel requestLabel(int value) {
		if (this.storedLiterals.containsKey(value)) {
			return this.storedLiterals.get(value);
		}
		else {
			ASMDataLabel newLabel = new ASMDataLabel("LIT_" + value, new MemoryWordOperand(value));
			this.storedLiterals.put(value, newLabel);
			return newLabel;
		}
	}
	
	/**
	 * Inject a small routine in the given node that moves given value into the given
	 * target reg. If the value is too large ( > 255), the routine will be to load it
	 * from an external literal pool.
	 */
	public void loadValue(AsNNode node, int value, int target) {
		if (value > 255) {
			ASMDataLabel label = requestLabel(value);
			
			ASMLdrLabel ldr = new ASMLdrLabel(new RegOperand(target), new LabelOperand(label), null);
			ldr.comment = new ASMComment("Literal is too large, load from literal pool");
			
			node.instructions.add(ldr);
		}
		else node.instructions.add(new ASMMov(new RegOperand(target), new ImmOperand(value)));
	}
	
	/**
	 * Returns the data label where the data label name matches the name of the given label.
	 */
	public ASMDataLabel getValue(LabelOperand l) {
		for (Entry<Integer, ASMDataLabel> entry : this.storedLiterals.entrySet()) {
			if (entry.getValue().name.equals(l.label.name)) return entry.getValue();
		}
		return null;
	}
	
}
