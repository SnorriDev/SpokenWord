package snorri.semantics;

import snorri.entities.Projectile;
import snorri.world.Vector;
import snorri.world.World;

public class Go extends VerbDef {
	
	private static final double SPEED = 350;
	private static final double DELETE_MARGIN = 5;
	
	public interface Walker {
		
		public void walk(World world, Vector delta);
			
		default void walk(World world, Vector dir, double deltaTime) {
			walk(world, dir.copy().multiply(deltaTime));
		}
		
		default void walkNormalized(World world, Vector dir, double deltaTime) {
			walk(world, dir.copy().normalize(), deltaTime);
		}
		
	}
	
	public Go() {
		super(false);
	}

	@Override
	public boolean exec(Object obj) {
		if (e.getSecondPerson() instanceof Walker) {
			Vector trans = e.getDestination().copy().sub(e.getSecondPerson().getPos());
			if (trans.magnitude() < DELETE_MARGIN) {
				e.getSecondPerson().kill(e.getWorld());
				return false;
			}
			((Walker) e.getSecondPerson()).walkNormalized(e.getWorld(), trans, SPEED * e.getDeltaTime());
			return true;
		}
		return false;
	}
	
	//TODO: track movement better
	/**
	 * @return whether or not something is a projectile
	 */
	@Override
	public boolean eval(Object subj, Object obj) {
		return subj instanceof Projectile;
	}
	
	@Override
	public boolean altersMovement() {
		return true;
	}

	@Override
	public String toString() {
		return "go";
	}

}