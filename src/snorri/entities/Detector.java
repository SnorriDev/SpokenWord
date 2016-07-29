package snorri.entities;

import snorri.events.CollisionEvent;
import snorri.triggers.Trigger;
import snorri.world.Vector;
import snorri.world.World;

public class Detector extends Entity {

	private static final long serialVersionUID = 1L;
	protected float age; //set age to -1 to make it not despawn
	protected boolean treeMember = false;
	
	public Detector(Vector pos, int r) {
		super(pos, r);
		age = 0;
	}
	
	public Detector(Entity e) {
		super(e);
		age = 0;
	}

	public void onCollision(CollisionEvent e) {
		
		if (tag != null) {
			Trigger.TriggerType.COLLISION.activate(this);
		}
		
	}
	
	@Override
	public void update(World world, double deltaTime) {
		
		if (age != -1) {
			age += deltaTime;
		}
		
		if (shouldDespawn()) {
			world.delete(this);
			return;
		}
		
		for (Entity hit : world.getEntityTree().getAllCollisions(this)) {
			if (hit != null) {
				onCollision(new CollisionEvent(this, hit, world));
			}
		}
		
	}
	
	protected double getLifeSpan() {
		return 4;
	}
	
	protected boolean shouldDespawn() {
		return age > getLifeSpan();
	}
	
	/**
	 * @return whether this entity should be stored in the entity tree or in the projectiles list
	 */
	public boolean isTreeMember() {
		return treeMember;
	}
		
}
