package snorri.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import snorri.main.Debug;
import snorri.main.FocusedWindow;
import snorri.masking.Mask;
import snorri.world.TileType;

public class TileLayer implements Editable, SavableLayer {

	public static final int MAX_SIZE = 1024;

	public static final int BACKGROUND = 0;
	public static final int MIDGROUND = 1;
	public static final int FOREGROUND = 2;
	
	public static final int CUSHION = 4;
	public static final int DOOR_WIDTH = 3;

	/** An array of tiles. Note that coordinates are Cartesian, not matrix-based. */
	private Tile[][] map;
	private Tile outsideTile;
	private RenderMode renderMode;
	
	private transient Map<BufferedImage, Area> textureMap;
	private transient BufferedImage bitmap;

	private enum RenderMode {
		BITMAP, GRID;
	}

	/** Create an incompletely initialized Layer.
	 *  
	 *  This method will leave null squares in the grid.
	 *  
	 *  @param width The width of the new World.
	 *  @param height. The height of the new World.
	 */
	private TileLayer(int width, int height) {
		map = new Tile[width][height];
		setRenderMode(RenderMode.GRID);
	}
	
	/** Initializes a new layer with background bg. */
	public TileLayer(int width, int height, TileType bg) {
		this(width, height);
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				map[i][j] = new Tile(bg);
			}
		}
//		updateAllMasksAndBitmap();
	}

	public TileLayer(Vector v, TileType bg) {
		this(v.getX(), v.getY(), bg);
	}

	public TileLayer(File file) throws IOException {
		load(file);
		setRenderMode(RenderMode.GRID);
//		updateAllMasksAndBitmap();
	}
	
	public static TileLayer fromYAML(World world, Map<String, Object> params) throws IOException {
		File file = new File(world.getDirectory(), (String) params.get("path"));
		return new TileLayer(file);
	}
	
	/** The same as fromYAML, but wrapped to catch exceptions.
	 * 
	 * This is useful for passing the function into an enum in Layer.LayerType.
	 * 
	 */
	public static TileLayer wrappedFromYAML(World world, Map<String, Object> params) {
		try {
			return fromYAML(world, params);
		} catch (IOException e) {
			Debug.logger.log(java.util.logging.Level.SEVERE, "Could not load load TileLayer from YAML.", e);
			return null;
		}
	}
	
	/** Copy a TileLayer. */
	public TileLayer copy() {
		Vector dims = getDimensions();
		TileLayer layer = new TileLayer(dims.getX(), dims.getY());
		for (int x = 0; x < dims.getX(); x++) {
			for (int y = 0; y < dims.getY(); y++) {
				layer.map[x][y] = map[x][y];
			}
		}
		layer.outsideTile = outsideTile;
		layer.renderMode = renderMode;
		return layer;
	}

	public TileLayer getTransposed() {
		TileLayer t = new TileLayer(getDimensions(), outsideTile.getType());
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				t.setTileGrid(y, x, getNewTileGrid(x, y));
			}
		}
		return t;
	}

	/**
	 * Flip the level on the x axis. Using this method and
	 * <code>getTransposed()</code>, one can produce levels with a door facing
	 * out from all four sides if a door exists.
	 */
	public TileLayer getXReflected() {
		TileLayer f = new TileLayer(getDimensions(), outsideTile.getType());
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				f.setTileGrid(getWidth() - 1 - x, y, getNewTileGrid(x, y));
			}
		}
		return f;
	}

	private void resize(int newWidth, int newHeight) {

		Tile[][] newMap = new Tile[newWidth][newHeight];
		Vector newDim = new Vector(newWidth, newHeight);

		for (int i = 0; i < newDim.getX(); i++) {
			for (int j = 0; j < newDim.getY(); j++) {
				newMap[i][j] = new Tile(UnifiedTileType.SAND);
			}
		}
		Debug.logger.info("Resizing Level from\t" + getWidth() + "\tx\t" + getHeight() + "\tto\t" + newDim.getX() + "\tx\t" + newDim.getY() +"\tusing resize function.");
		for (int i = 0; i < newDim.getX() && i < getWidth(); i++) {
			for (int j = 0; j < newDim.getY() && j < getHeight(); j++) {
				newMap[i][j] = map[i][j];
			}
		}

		map = newMap;
		Debug.logger.info("New Level Size:\t" + getWidth() + "\tx\t" + getHeight() + ".");		
	}

	public TileLayer getResized(int newWidth, int newHeight) {
		TileLayer level = copy();
		level.resize(newWidth, newHeight);
		return level;
	}

	public void setTile(int x, int y, Tile t) {
		setTileGrid(x / Tile.WIDTH, y / Tile.WIDTH, t);
	}

	public void setTileGrid(int x, int y, Tile t) {
		if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
			return;
		}
		Tile old = map[x][y];
		map[x][y] = t;
		computeMasks(x, y);
		updateMasksAndBitmap(new Vector(x, y), old, t);
	}

	public Tile getTile(int x, int y) {
		return getTileGrid(x / Tile.WIDTH, y / Tile.WIDTH);
	}

	public Tile getTile(Vector v) {
		return getTile(v.getX(), v.getY());
	}

	public Tile getNewTileGrid(int x, int y) {
		if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
			return null;
		}
		if (getTileGrid(x, y) == null) {
			return null;
		}
		return new Tile(getTileGrid(x, y));
	}

	public Tile getTileGrid(int x, int y) {
		if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
			return null;
		}
		return map[x][y];
	}

	public Tile getTileGrid(Vector v) {
		return getTileGrid(v.getX(), v.getY());
	}

	public Vector getDimensions() {
		return new Vector(getWidth(), getHeight());
	}

	@Override
	public void render(FocusedWindow<?> g, Graphics2D gr, double deltaTime, boolean renderOutside) {
		int minX, maxX, minY, maxY;
		Vector center = g.getCenterObject().getPos();
		Vector dim = g.getDimensions();

		if (!Debug.maskingDisabled() && getRenderMode() == RenderMode.BITMAP) {
			if (bitmap == null) {
				return;
			}
			minX = center.getX() - dim.getX() / 2;
			minY = center.getY() - dim.getY() / 2;
			int adjMinX = Math.max(0, minX), adjMinY = Math.max(0, minY);
			BufferedImage image = bitmap.getSubimage(adjMinX, adjMinY,
					Math.min(bitmap.getWidth() - adjMinX, dim.getX()),
					Math.min(bitmap.getHeight() - adjMinY, dim.getY()));
				
			gr.drawImage(image, Math.max(0, -minX), Math.max(0, -minY), null);
			return;
		}

		minX = (center.getX() - dim.getX() / 2) / Tile.WIDTH - CUSHION;
		maxX = (center.getX() + dim.getX() / 2) / Tile.WIDTH + CUSHION;
		minY = (center.getY() - dim.getY() / 2) / Tile.WIDTH - CUSHION;
		maxY = (center.getY() + dim.getY() / 2) / Tile.WIDTH + CUSHION;

		for (int i = minX; i < maxX; i++) {
			for (int j = minY; j < maxY; j++) {
				if (i >= 0 && i < map.length && j >= 0 && j < map[i].length) {
					map[i][j].drawTile(g, gr, new Vector(i, j));
				} else if (renderOutside) {
					getOutsideTile().drawTile(g,  gr, new Vector(i, j));
				}
			}
		}
	}

	public void load(File file) throws FileNotFoundException, IOException {
		Debug.logger.info("Loading " + file + "...");
		byte[] b = new byte[4];
		FileInputStream is = new FileInputStream(file);

		is.read(b);
		int width = ByteBuffer.wrap(b).getInt();
		is.read(b);
		int height = ByteBuffer.wrap(b).getInt();

		map = new Tile[width][height];
		byte[] b2 = new byte[2];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				is.read(b2);
				int id = ((Byte) b2[0]).intValue();
				TileType type = UnifiedTileType.values()[id];
				int style = ((Byte) b2[1]).intValue();
				map[i][j] = new Tile(type, style);
			}
		}
		is.close();
	}

	public void save(File file) throws IOException {
		save(file, true);
	}

	public void save(File file, boolean saveGraphs) throws IOException {
		FileOutputStream os = new FileOutputStream(file);
		ByteBuffer b1 = ByteBuffer.allocate(4);
		ByteBuffer b2 = ByteBuffer.allocate(4);

		byte[] buffer = b1.putInt(getWidth()).array();
		os.write(buffer);
		buffer = b2.putInt(getHeight()).array();
		os.write(buffer);

		for (int i = 0; i < getWidth(); i++) {
			for (int j = 0; j < getHeight(); j++) {
				os.write(((byte) map[i][j].getType().getId()) & 0xFF);
				os.write(((byte) map[i][j].getStyle()) & 0xFF);
			}
		}

		os.close();
	}

	/**
	 * Check if a position is pathable.
	 * @param x The x coordinate of the position.
	 * @param y The y coordinate of the position.
	 * @return Whether the tile at <code>(x, y)</code> is pathable.
	 */
	public boolean isPathable(int x, int y) {
		return getTileGrid(x, y) != null && getTileGrid(x, y).isPathable();
	}

	public boolean isPathable(Vector pos) {
		return isPathable(pos.getX(), pos.getY());
	}

	public void setTileGrid(Vector v, Tile newTile) {
		setTileGrid(v.getX(), v.getY(), newTile);
	}

	@Override
	public TileLayer getTileLayer() {
		return this;
	}

	/**
	 * Helper function for fill door which might have broader utility.
	 */
	public boolean isBg(int x, int y, TileType wall, TileType floor) {
		return getTileGrid(x, y) == null
				|| (getTileGrid(x, y).getType() != floor && getTileGrid(x, y).getType() != wall);
	}

	/**
	 * For use in world generation. Fill in a door in a dungeon.
	 * 
	 * @param pos
	 *            The position of the door
	 * @param floor
	 *            The background tile type (used for orientation)
	 */
	public void fillDoor(Vector pos, Tile fill, TileType floor) {

		Vector dir;

		if (isBg(pos.getX() + 1, pos.getY(), fill.getType(), floor)
				|| isBg(pos.getX() - 1, pos.getY(), fill.getType(), floor)) {
			dir = new Vector(0, 1);
		} else if (isBg(pos.getX(), pos.getY() + 1, fill.getType(), floor)
				|| isBg(pos.getX(), pos.getY() - 1, fill.getType(), floor)) {
			dir = new Vector(1, 0);
		} else {
			return;
		}

		for (int i = -DOOR_WIDTH / 2; i <= DOOR_WIDTH / 2; i++) {
			setTileGrid(dir.copy().multiply_(i).add_(pos), new Tile(fill));
		}

	}

	public void setTile(Vector pos, Tile tile) {
		setTile(pos.getX(), pos.getY(), tile);
	}

	private void updateMasksAndBitmap(Vector pos, Tile o, Tile n) {
		
		Graphics2D g = (getRenderMode() == RenderMode.BITMAP) ? bitmap.createGraphics() : null;
		if (o.equals(n)) {
			return;
		}
		
		int i = 0;
		for (Vector trans : Mask.NEIGHBORS_AND_CORNERS) {
			Tile tile = getTileGrid(trans.copy().add_(pos));
			if (tile == null) {
				i++;
				continue;
			}
			boolean isCorner = i >= 4;
			byte b = Mask.getNeighborValue(Mask.getComplement(i));
			Mask added = null, subbed = null;
			for (Mask mask : tile.getMasks()) {
				
				if (mask.isCorner() != isCorner) {
					continue;
				}
				
				if (mask.getTile().equals(o)) { // if this starts bugging: mask.getBitVal() >= b
					mask.sub(b);
					subbed = mask;
				} else if (mask.getTile().equals(n) && n.compareTo(tile) < 0) {
					mask.add(b);
					added = mask;
				}
			}
			if (added == null && n.compareTo(tile) < 0) {
				added = new Mask(n, false);
				added.add(b);
				tile.addMask(added);
			}
			if (subbed != null && subbed.getBitVal() == 0) {
				tile.removeMask(subbed);
			}
			if (g != null) {
				// FIXME add border
				tile.drawTileAbs(g, trans.copy().add_(pos).globalPos_(), true);
			}
			i++;

		}
		if (g != null) {
			n.drawTileAbs(g, pos.copy().globalPos_(), true);
			g.dispose();
		}
	}

	/**
	 * Enqueues all bitmasks onto a tile. Uses very small integer fields so as
	 * not to run out of memory
	 * 
	 * @return true if masks were updated
	 */
	public boolean computeMasks(int x, int y) {

		// TODO(#49): Is this stuff worth keeping?

		Tile tile = getTileGrid(x, y);
		if (tile == null || tile.getType().isAtTop()) {
			return false;
		}
		
		Mask[] masks = new Mask[8];

		byte bitVal = 1;
		byte j = 0;
		for (Vector v : Mask.NEIGHBORS) {
			Tile t = getTileGrid(v.copy().add_(x, y));
			if (t != null && !t.getType().isAtTop() && tile.compareTo(t) > 0) {
				for (j = 0; j < masks.length; j++) {
					if (masks[j] == null) {
						masks[j] = new Mask(t, false);
					}
					if (masks[j].hasTile(t)) {
						masks[j].add(bitVal);
						break;
					}
				}
			}
			bitVal <<= 1;
		}

		bitVal = 1;
		for (Vector v : Mask.CORNERS) {
			Tile t = getTileGrid(v.copy().add_(x, y));
			if (t != null && !t.getType().isAtTop() && tile.compareTo(t) > 0) {
				for (j = 0; j < masks.length; j++) {
					if (masks[j] == null) {
						masks[j] = new Mask(t, true);
					}
					if (masks[j].hasTile(t) && masks[j].isCorner()) {
						masks[j].add(bitVal);
						break;
					}
				}
			}
			bitVal *= 2;
		}
		
		// TODO make this more efficient in general
		// TODO just bitshift existing masks when a neighbor is modified

		tile.clearMasks();
		for (Mask mask : masks) {
			if (mask == null) {
				break;
			}
			tile.addMask(mask);
		}
		return masks[0] != null;
	}

	public boolean computeMasks(Vector v) {
		return computeMasks(v.getX(), v.getY());
	}

	public void updateAllMasksAndBitmap() {

		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				computeMasks(x, y);
			}
		}

		if (getRenderMode() == RenderMode.BITMAP) {
			computeTextureMap();
			renderBitmap();
		}

	}

	public int getWidth() {
		return map.length;
	}

	public int getHeight() {
		return map[0].length;
	}

	public boolean canShootOver(Vector pos) {
		Tile t = getTileGrid(pos);
		return t != null && t.canShootOver();
	}

	/**
	 * Builds a mapping from each texture in the level to the area where that
	 * texture should be drawn.
	 * 
	 * @return the computed mapping
	 */
	public void computeTextureMap() {
		textureMap = new HashMap<>();
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {

				if (map[x][y].getBaseTexture() != null) {
					if (!textureMap.containsKey(map[x][y].getBaseTexture())) {
						textureMap.put(map[x][y].getBaseTexture(), new Area());
					}
					textureMap.get(map[x][y].getBaseTexture())
							.add(new Area(new Rectangle(x * Tile.WIDTH, y * Tile.WIDTH, Tile.WIDTH, Tile.WIDTH)));
				}

				AffineTransform transform = AffineTransform.getTranslateInstance(x * Tile.WIDTH, y * Tile.WIDTH);
				for (Mask m : map[x][y].getMasks()) {
					BufferedImage texture = m.getBaseTexture();
					if (!textureMap.containsKey(texture)) {
						textureMap.put(texture, new Area());
					}
					Area area = m.getArea().createTransformedArea(transform);
					textureMap.get(texture).add(area);
				}

			}
		}
	}

	// see
	// https://gamedev.stackexchange.com/questions/72924/how-do-i-add-an-image-inside-a-rectangle-or-a-circle-in-javafx
	// see
	// https://www.cse.iitb.ac.in/~paragc/teaching/2012/cs775/assignments/A3/groups/jai+ahana/Depixelizing%20pixelArt.pdf
	// TODO want to implement this second paper for blending nearby stuff

	// TODO general path:
	// https://docs.oracle.com/javase/tutorial/2d/geometry/arbitrary.html

	// TODO maybe cut the area, draw images directly
	// confirmed: can draw non-Rectangles onto the other shape
	// solution: make a path2d through all the border points

	/**
	 * Render the full bitmap of this level. Should only be called if
	 * <code>this.getRenderMode() == RenderMode.BITMAP</code>
	 */
	public void renderBitmap() {
		bitmap = new BufferedImage(getWidth() * Tile.WIDTH, getHeight() * Tile.WIDTH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bitmap.createGraphics();
		for (Tile tile : Tile.getBlendOrdering()) {

			BufferedImage texture = tile.getBaseTexture();
			if (!textureMap.containsKey(texture)) {
				continue;
			}

			Rectangle r = new Rectangle(0, 0, texture.getWidth(), texture.getHeight());
			g.setPaint(new TexturePaint(texture, r));
			g.fill(textureMap.get(texture));

			g.setColor(Color.BLACK);
			Path2D path = new Path2D.Double(textureMap.get(texture));
			g.draw(path);

		}
		g.dispose();
	}
	
	public void setRenderMode(RenderMode renderMode) {
		// TODO(lambdaviking): Set the RenderMode correctly for the layer type.
		this.renderMode = renderMode;
	}

	public RenderMode getRenderMode() {
		return renderMode;
	}

	@Override
	public WorldGraph getWorldGraph() {
		return null;
	}

	public Tile getOutsideTile() {
		return outsideTile != null ? outsideTile : map[0][0];
	}

	public void setOutsideTile(Tile outsideTile) {
		this.outsideTile = outsideTile;
	}

	@Override
	public String getFilename() {
		return "tile.layer";
	}

}