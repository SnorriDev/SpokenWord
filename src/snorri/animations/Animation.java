package snorri.animations;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import snorri.main.Debug;
import snorri.main.Main;
import snorri.util.Util;
import snorri.world.Vector;

/**
 * Stores all the animation frames as Images (PNGS).
 * To convert SWFs to PNGs, use the swfrender utility.
 * <code>getSprite()</code> should be used to get an animation's current frame.
 */

public class Animation implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static final double FRAME_RATE = 8;

	protected BufferedImage[] frames;
	protected BufferedImage[] flippedFrames;
	private String path;
	
	private double currentTime = 0;
	private double frameRate = FRAME_RATE;
	private boolean hasCycled = false;
	private boolean flipped = false;
	
	/**
	 * Loads an animation from a path (not package name).
	 * @param path
	 * 	The path from which to load
	 * @throws URISyntaxException
	 */
	public Animation(String str) {
		if (str.endsWith(".png")) {
			loadImage(Main.getImage(str));
		} else {
			loadFolder(Main.getFile(str));
		}
		computeFlipped();
		path = str;
	}
	
	public Animation(BufferedImage image) {
		loadImage(image);
		computeFlipped();
	}

	/**
	 * Shallow copy of the specified animation.
	 * @param other The animation to copy.
	 */
	public Animation(Animation other) {
		set(other);
	}

	private void loadFolder(File folder) {

		if (!folder.isDirectory()) {
			return;
		}

		File[] frames = folder.listFiles();
		Arrays.sort(frames, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});

		ArrayList<BufferedImage> tempFrames = new ArrayList<>();
		for (int i = 0; i < frames.length; i++) {

			if (!frames[i].getName().endsWith(".png")) {
				continue;
			}
			
			try {
				tempFrames.add(ImageIO.read(frames[i]));
			} catch (IOException e) {
				Debug.logger.log(Level.SEVERE, "Animation frame " + frames[i].getName() + " could not be loaded.", e);
			}
		}

		if (tempFrames.size() == 0) {
			Debug.logger.warning("Animation " + folder.getName() + " has zero frames.");
		}
		
		this.frames = new BufferedImage[tempFrames.size()];
		int i = 0;
		for (BufferedImage im : tempFrames) {
			this.frames[i] = im;
			i++;
		}

	}
	
	protected Animation(int numFrames) {
		frames = new BufferedImage[numFrames];
	}

	private void loadImage(BufferedImage image) {
		if (image == null) {
			return;
		}
		frames = new BufferedImage[1];
		frames[0] = image;
	}

	// only save the path to the animation folder/image
	private void writeObject(ObjectOutputStream out) throws IOException {
		
		out.writeObject(getPath() == null ? "..." : getPath());
		
		// for image animations that were not read from a file
		if (path == null) {
			ImageIO.write(frames[0], "png", out);
		}
		
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
				
		String read = (String) in.readObject();
		
		if (read.equals("...")) { // Signifies nowhere.
			BufferedImage im = ImageIO.read(in);
			if (im != null) {
				set(new Animation(im));
			} else {
				Debug.logger.warning("Could not load custom frame in animation.");
			}
			return;
		}
		
		set(new Animation((String) read));
		
	}
	
	/**
	 * set the animation back to the initial frame
	 */
	public void restart() {
		currentTime = 0;
		hasCycled = true;
	}

	private void set(Animation animation) {
		path = animation.path;
		frames = animation.frames;
		flippedFrames = animation.flippedFrames;
		hasCycled = animation.hasCycled;
		currentTime = animation.currentTime;
		setFrameRate(animation.getFrameRate());
	}

	public String getPath() {
		return path;
	}

	/**
	 * updates the frame counter to move to the next frame
	 * 
	 * @return the current image
	 */
	public synchronized BufferedImage getSprite(double timeDelta) {
		hasCycled |= (currentTime + timeDelta) >= (frames.length / getFrameRate());
		currentTime = (currentTime + timeDelta) % (frames.length / getFrameRate());		
		return (flipped ? flippedFrames : frames)[getFrameIndex()];
	}
	
	public int getFrameIndex() {
		return (int) (currentTime * getFrameRate());
	}

	/**
	 * @return whether the animation loop has been completed at least once
	 */
	public boolean hasCycled() {
		return hasCycled;
	}
	
	@Override
	public String toString() {
		return "Animation{n: " + frames.length + ", i: " + getFrameIndex() + "}";
	}
	
	public void computeFlipped() {
		assert frames != null;
		flippedFrames = new BufferedImage[frames.length];
		for (int i = 0; i < frames.length; i++) {
			flippedFrames[i] = Util.getFlipped(frames[i], true, false);
		}
	}
	
	/**
	 * Returns an animation rotated with respect to <code>dir</code>.
	 * The original animation is unchanged.
	 * @param dir
	 * 	The direction to rotate, as a vector.
	 * @return
	 * 	The new rotated animation.
	 */
	public Animation getRotated(Vector dir) {
		Animation other = new Animation(frames.length);
		for (int i = 0; i < frames.length; i++) {
			other.frames[i] = Util.getRotated(frames[i], dir);
		}
		other.computeFlipped();
		return other;
	}
	
	public void flip() {
		flipped = !flipped;
	}
	
	public void flip(boolean facingLeft) {
		flipped = facingLeft;
	}
	
	public int getWidth() {
		return frames[0].getWidth();
	}
	
	public int getHeight() {
		return frames[0].getHeight();
	}
	
	protected final void setFrameRate(double frameRate) {
		this.frameRate = frameRate;
	}
	
	protected final double getFrameRate() {
		return frameRate;
	}

}
