package snorri.semantics;

import snorri.events.SpellEvent;
import snorri.main.GameWindow;
import snorri.main.Main;
import snorri.parser.Node;
import snorri.triggers.Trigger.TriggerType;

public class Write extends TransVerbDef {

	protected static final String NONE = "null";
	
	public Write() {
		super();
	}
	
	@Override
	public boolean exec(Node<Object> object, SpellEvent e) {
		Object obj = object.getMeaning(e);
		if (Main.getWindow() instanceof GameWindow && obj != null) {
			String objString = obj.toString();
			String text = objString != null ? objString : NONE;
			TriggerType.WRITE.activate(text);
			((GameWindow) Main.getWindow()).showMessage(text);
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
		return "write";
	}

}