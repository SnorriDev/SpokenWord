package snorri.entities;

import java.awt.Graphics;

import snorri.events.CollisionEvent;
import snorri.triggers.TriggerType;
import snorri.windows.FocusedWindow;
import snorri.windows.LevelEditor;
import snorri.world.Vector;

public class Listener extends Detector {

	/**
	 * TODO different types of listeners for different entities
	 */
	private static final long serialVersionUID = 1L;
	
	private Entity target;

	public Listener(Vector pos, int r, String msg) {
		super(pos, r);
		tag = msg;
		age = -1;
		ignoreCollisions = true;
	}
	
	public Listener(Vector pos, int r, String msg, Entity e) {
		this(pos, r, msg);
		target = e;
	}

	@Override
	public void onCollision(CollisionEvent e) {

		if (tag != null && (target == e.getTarget() || (target == null && e.getTarget() instanceof Player))) {
			TriggerType.BROADCAST.activate(tag);
		}

	}
	
	@Override
	public void renderAround(FocusedWindow<?> g, Graphics gr, double deltaTime) {
		if (g instanceof LevelEditor) {
			super.renderAround(g, gr, deltaTime);
		}
	}
	
}
