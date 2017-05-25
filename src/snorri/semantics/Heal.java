package snorri.semantics;

import snorri.entities.Unit;
import snorri.events.SpellEvent;
import snorri.parser.Node;
import snorri.triggers.Trigger.TriggerType;

public class Heal extends TransVerbDef {

	private static final double AMOUNT = 10;
	
	public Heal() {
		super();
	}

	@Override
	public boolean exec(Node<Object> object, SpellEvent e) {
		
		Object obj = object.getMeaning(e);
		
		if (obj instanceof Unit) {
			((Unit) obj).heal(AMOUNT, e);
			TriggerType.HEAL.activate(((Unit) obj).getTag());
			return true;
		}
		return false;
	}

	@Override
	public boolean eval(Object subj, Object obj, SpellEvent e) {
		return false;
	}

	@Override
	public String toString() {
		return "heal";
	}

}
