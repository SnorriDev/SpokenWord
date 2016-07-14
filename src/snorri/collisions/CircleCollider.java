package snorri.collisions;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

import snorri.entities.Entity;
import snorri.main.FocusedWindow;
import snorri.world.Vector;

public class CircleCollider extends Collider {

	private static final long serialVersionUID = 1L;
	private int r;

	public CircleCollider(Vector pos, int r) {
		super(pos);
		this.r = r;
	}
	
	public void setRadius(int r) {
		this.r = r;
	}
	
	public void increaseRadius(int incr) {
		r += incr;
	}
	
	public int getRadius() {
		return r;
	}
	
	public Rectangle getBoundingRect() {
		return new Rectangle(pos.getX() - r, pos.getY() - r, 2 * r, 2 * r);
	}
	
	@Override
	public Ellipse2D getShape() {
		return new Ellipse2D.Double(pos.x - r, pos.y - r, 2 * r, 2 * r);
	}

	@Override
	public void render(FocusedWindow g, Graphics gr) {
		if (pos == null || g.getFocus().getPos() == null) {
			return;
		}
		Vector rel = pos.copy().sub(g.getFocus().getPos());
		gr.drawOval(rel.getX() - r + g.getBounds().width / 2, rel.getY() - r + g.getBounds().height / 2, 2 * r, 2 * r);
	}

	@Override
	public Collider cloneOnto(Entity root) {
		return new CircleCollider(root.getPos(), getRadius());
	}

	@Override
	public int getMaxRadius() {
		return r;
	}
	
	@Override
	public String toString() {
		return "r" + r;
	}
	
	@Override
	public CircleCollider copy() {
		return new CircleCollider(pos, r);
	}

}
