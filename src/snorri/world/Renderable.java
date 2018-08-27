package snorri.world;

import java.awt.Graphics2D;

import snorri.main.FocusedWindow;

/** A 2D plane that can be rendered and transformed.
 * @author lambdaviking
 * 
 * Applies to World, Level, etc.
 *
 */
public interface Renderable {
	
	/** The core logic that Renderable implements. */
	public void render(FocusedWindow<?> levelEditor, Graphics2D gr, double deltaTime, boolean b);

	public int getWidth();
	public int getHeight();
	
	default Vector getDimensions() {
		return new Vector(getWidth(), getHeight());
	}
		
	public Renderable getTransposed();
	public Renderable getXReflected();
	public Renderable getResized(int newWidth, int newHeight);
	
}
