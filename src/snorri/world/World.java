package snorri.world;

import java.awt.FileDialog;
import java.awt.Graphics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.yamlbeans.YamlException;
import snorri.entities.Mummy;
import snorri.entities.Entity;
import snorri.entities.Player;
import snorri.entities.QuadTree;
import snorri.entities.Spawn;
import snorri.entities.Unit;
import snorri.entities.LongRangeAIUnit.ShootAttempt;
import snorri.main.Debug;
import snorri.main.FocusedWindow;
import snorri.main.Main;
import snorri.pathfinding.Pathfinding;
import snorri.pathfinding.Team;
import snorri.triggers.Trigger;
import snorri.triggers.TriggerMap;
import snorri.world.TileType;
import snorri.world.BackgroundElement;
import snorri.world.MidgroundElement;
import snorri.world.ForegroundElement;

public class World implements Playable, Editable {

	public static final int DEFAULT_LEVEL_SIZE = 144;
	public static final Vector DEFAULT_SPAWN = new Vector(100, 100);
	private static final int RANDOM_SPAWN_ATTEMPTS = 100;
	public static final int UPDATE_RADIUS = 4000;
	private static final int SPAWN_SEARCH_RADIUS = 12;
	private static final int EDGE_TP_THRESHOLD = 10;

	private String path;
	
	private Level background;
	private Level midground;
	private Level foreground;

	private final Pathfinding pathfinding;
	private EntityGroup col;

	private List<Team> teams;
	private TriggerMap triggers;
	
	private WorldGraph universe;
	private World[] neighbors = new World[4];

	public World() {
		this(DEFAULT_LEVEL_SIZE, DEFAULT_LEVEL_SIZE);
	}

	/**
	 * constructor to create a blank world in the level editor
	 * 
	 * @param width
	 *            width of the new world
	 * @param height
	 *            height of the new world
	 */
	public World(int width, int height) {

		this(new Level(width, height, Level.BACKGROUND), new Level(width, height, Level.MIDGROUND),
				new Level(width, height, Level.FOREGROUND));

		// temporary
		add(new Player(DEFAULT_SPAWN.copy()));
		add(new Mummy(new Vector(600, 600), computeFocus()));

	}

	public World(String folderName) throws FileNotFoundException, IOException {
		this(folderName, null);
	}
	
	public World(String folderName, Player p) throws FileNotFoundException, IOException {
		this(new File(folderName), p);
	}

	public World(File file, Player p) throws FileNotFoundException, IOException {

		load(file);
		if (p != null) {
			spawnPlayer(p);
		}

		pathfinding = new Pathfinding(getPathfindingLevels());
		
		if (computeFocus() == null) {
			Debug.warning("world without player detected");
		}

	}
		
	public World(File file) throws FileNotFoundException, IOException {
		this(file, null);
	}

	public World(Level l0, Level l1, Level l2) {

		background = l0;
		midground = l1;
		foreground = l2;

		col = QuadTree.coverLevel(background);

		pathfinding = new Pathfinding(getPathfindingLevels());

	}

	/**
	 * @return a list of levels that will be taken into account for pathfinding
	 */
	private List<Level> getPathfindingLevels() {
		List<Level> p = new ArrayList<>();
		p.add(background);
		p.add(midground);
		return p;
	}

	// TODO input the unit as an arg?
	public Vector getRandomSpawnPos(int radius) {
		for (int i = 0; i < RANDOM_SPAWN_ATTEMPTS; i++) {
			Vector pos = getGoodSpawn(background.getDimensions().random()); // FIXME:
																						// good
																						// spawn?
			if (pos != null && col.getFirstCollision(new Entity(pos, radius)) == null) {
				return pos;
			}
		}
		Debug.warning("could not find suitable spawn");
		return null;
	}

	public Vector getRandomSpawnPos() {
		return getRandomSpawnPos(Unit.RADIUS);
	}

	public static File wrapLoad() {
		File file = Main.getFileDialog("Select file to load", FileDialog.LOAD);
		return file == null ? null : file;
	}

	public synchronized void update(Entity focus, double d) {

		if (Debug.LOG_WORLD) { //TODO make these into methods in Debug
			Debug.log("world update");
		}

		col.updateAround(this, d, focus);
		
		if (touchingRight(focus)) {
			universe.crossInto(getRightNeighbor(), EDGE_TP_THRESHOLD, focus.getPos().getY());
		} else if (touchingBottom(focus)) {
			universe.crossInto(getBottomNeighbor(), focus.getPos().getX(), EDGE_TP_THRESHOLD);
		} else if (touchingLeft(focus)) {
			World left = getLeftNeighbor();
			universe.crossInto(left, left.getWidth() - EDGE_TP_THRESHOLD, focus.getPos().getY());
		} else if (touchingTop(focus)) {
			World top = getTopNeighbor();
			universe.crossInto(top, focus.getPos().getX(), top.getHeight() - EDGE_TP_THRESHOLD);
		}

	}

	@Override
	public synchronized void render(FocusedWindow<?> g, Graphics gr, double deltaTime, boolean showOutlands) {
		background.render(g, gr, deltaTime, showOutlands);
		midground.render(g, gr, deltaTime, false);
		col.renderAround(g, gr, deltaTime);
		foreground.render(g, gr, deltaTime, false);
	}

	public EntityGroup getEntityTree() {
		return col;
	}

	public Level getLevel() {
		return background;
	}

	/**
	 * @see <code>getLevel(Class<? extends TileType> c)</code>
	 */
	@Deprecated
	public Level getLevel(int layer) {	
		switch(layer) {
		case Level.BACKGROUND:
			return background;
		case Level.MIDGROUND:
			return midground;
		case Level.FOREGROUND:
			return foreground;
		}	
		return null;	
	}

	public Level getLevel(Class<? extends TileType> c) {
		if (c == BackgroundElement.class) {
			return background;
		} else if (c == MidgroundElement.class) {
			return midground;
		} else {
			return foreground;
		}
	}

	public Level[] getLevels() {
		return new Level[] { background, midground, foreground };
	}

	public boolean add(Entity e) {
		return col.insert(e, pathfinding);
	}

	/**
	 * Add a bunch of things to the world
	 */
	public void addAll(List<Entity> ents) {
		for (Entity e : ents) {
			add(e);
		}
	}

	/**
	 * @param e the entity to delete
	 * @return whether the delete was successful
	 */
	public synchronized boolean delete(Entity e) {
		if (e == null) {
			return false;
		}
		e.onDelete(this);
		return col.delete(e);
	}

	@Override
	public void save(File f, boolean recomputeGraphs) throws IOException {

		if (f.exists() && !f.isDirectory()) {
			throw new IOException("tried to save world to non-directory");
		}

		if (!f.exists()) {
			Debug.log("creating new world directory...");
			f.mkdir();
		}
		
		Playable.tryCreatingDefaultConfig(f, "world");

		String path = (f == null) ? this.path : f.getPath();
		col.saveEntities(new File(path, "entities.dat"));
		background.save(new File(path, "background.lvl"), recomputeGraphs);
		midground.save(new File(path, "midground.lvl"), recomputeGraphs);
		foreground.save(new File(path, "foreground.lvl"), recomputeGraphs);
		Team.save(new File(path, "teams.dat"), teams);
	}

	@Override @SuppressWarnings("unchecked")
	public void load(File f, Map<String, Object> yaml) throws FileNotFoundException, IOException, YamlException {
		
		if (!f.exists()) {
			throw new FileNotFoundException("no world called " + f.getName());
		}

		if (!f.isDirectory()) {
			throw new IOException("world file " + f.getName() + " is not a directory");
		}
		
		path = f.getName();
		
		if (yaml == null) {
			yaml = Playable.getConfig(f, "world");
		}

		background = new Level(new File(f, "background.lvl"), BackgroundElement.class);
		midground = new Level(new File(f, "midground.lvl"), MidgroundElement.class);
		foreground = new Level(new File(f, "foreground.lvl"), ForegroundElement.class);
		col = QuadTree.coverLevel(background);
		col.loadEntities(new File(f, "entities.dat"), pathfinding);

		triggers = Trigger.load((Map<String, Object>) yaml.get("triggers"), this);

		File teamsFile = new File(f, "teams.dat");
		if (teamsFile.exists()) {
			teams = Team.load(teamsFile);
		}

	}

	/**
	 * search through all the entities to find the first player
	 */
	@Override
	public Player computeFocus() {
		return col.getFirst(Player.class);
	}

	@Override
	public World getCurrentWorld() {
		return this;
	}

	public void resize(int newWidth, int newHeight) {
		background = background.getResized(newWidth, newHeight);
	}

	@Override
	public World getTransposed() {
		World w = new World(background.getTransposed(), midground.getTransposed(), foreground.getTransposed());
		col.mapOverEntities(e -> {
			Entity e2 = e.copy();
			e2.getPos().invert();
			w.add(e2);
		});
		return w;
	}

	@Override
	public World getXReflected() {
		World w = new World(background.getXReflected(), midground.getXReflected(), foreground.getXReflected());
		col.mapOverEntities(e -> {
			Entity e2 = e.copy();
			e2.setPos(e2.getPos().getXReflected(background.getDimensions().copy().toGlobalPos()));
			w.add(e2);
		});
		return w;
	}

	@Override @Deprecated
	public List<Entity> getEntities() {
		return col.getAllEntities();
	}

	/**
	 * Check if a tile is occupied by any entity
	 * 
	 * @param pos
	 *            The tile in grid coordinates
	 */
	public boolean tileHasEntity(Vector pos) {
		Entity hit = getEntityTree().getFirstCollision(Tile.getRectangle(pos.getX(), pos.getY()), true);
		return hit != null;
	}

	public TriggerMap getTriggerMap() {
		return triggers;
	}

	public List<Team> getTeams() {
		return teams;
	}

	public void addTeam(Team team) {
		if (teams == null) {
			teams = new ArrayList<>();
		}
		teams.add(team);
	}
	
	public Vector getGoodSpawn(Vector start) {
		return getGoodSpawn(start, new Vector(1, 2));
	}

	public Vector getGoodSpawn(Vector start, Vector gridSize) {

		for (int r = 0; r < SPAWN_SEARCH_RADIUS; r++) {
			changeStart: for (Vector v : start.getSquareAround(r)) {

				int x = v.getX();
				int y = v.getY();

				for (int x1 = (x * Tile.WIDTH - 2 * Unit.RADIUS_X)
						/ Tile.WIDTH; x1 <= (x * Tile.WIDTH + 2 * Unit.RADIUS_X) / Tile.WIDTH; x1++) {
					for (int y1 = (y * Tile.WIDTH - 2 * Unit.RADIUS_Y)
							/ Tile.WIDTH; y1 <= (y * Tile.WIDTH + 2 * Unit.RADIUS_Y) / Tile.WIDTH; y1++) {
						if (!pathfinding.getGraph(gridSize).isContextPathable(x1, y1)) {
							continue changeStart;
						}
					}
				}

				return v.copy().toGlobalPos();

			}
		}

		return null;

	}

	public Vector getGoodSpawn(int x, int y) {
		return getGoodSpawn(new Vector(x, y));
	}

	public void wrapUpdate(Vector pos, Tile tile) {
		wrapGridUpdate(pos.copy().toGridPos(), tile);
	}
	
	//TODO add wrapGridUpdates
	public synchronized void wrapGridUpdate(Vector posGrid, Tile tile) {
				
		Level l = getLevel(tile.getType().getClass());
		Tile oldTile = l.getTileGrid(posGrid);

		if (oldTile == null || pathfinding.isOccupied(posGrid)) {
			return;
		}

		l.setTileGrid(posGrid, tile);
		pathfinding.wrapPathingUpdate(posGrid, oldTile, tile);
		ShootAttempt.reset();
	
	}

	public Pathfinding getPathfinding() {
		return pathfinding;
	}
	
	/**
	 * @param v grid coordinates
	 * @return whether or not the tile at <code>v</code> is pathable
	 */
	public boolean isPathable(Vector v) {
		return isPathable(v.getX(), v.getY());
	}
	
	/**
	 * @param pos global coordinates
	 * @return whether or not bullets can pass over pos
	 */
	public boolean canShootOver(Vector pos) {
		Vector g = pos.copy().toGridPos();
		return background.canShootOver(g) && midground.canShootOver(g) && foreground.canShootOver(g);
	}

	/**
	 * @param x grid coordinate
	 * @param y grid coordinate
	 * @return whether the tile at x, y is pathable
	 */
	public boolean isPathable(int x, int y) {
		return pathfinding.isPathable(x, y);
	}

	public void wrapGridUpdate(int x, int y, Tile tile) {
		wrapGridUpdate(new Vector(x, y), tile);
	}

	@Override
	public Vector getDimensions() {
		return getLevel().getDimensions();
	}
	
	/**
	 * Search for a spawn marker, and put the player at that location
	 * @param player
	 * 	The player to spawn
	 * @return
	 * 	Whether or not the spawn marker was found
	 */
	public boolean spawnPlayer(Player player) {
		Spawn spawn = getEntityTree().getFirst(Spawn.class);
		if (spawn == null) {
			return false;
		}
		player.setPos(spawn.getPos());
		add(player);
		return true;
	}
	
	//TODO just need to implement the cross-world panning mechanic
	
	public World getRightNeighbor() {
		return neighbors[0];
	}
	
	public World getBottomNeighbor() {
		return neighbors[1];
	}
	
	public World getLeftNeighbor() {
		return neighbors[2];
	}
	
	public World getTopNeighbor() {
		return neighbors[3];
	}
	
	public void setRightNeighbor(World world) {
		neighbors[0] = world;
	}
	
	public void setBottomNeighbor(World world) {
		neighbors[1] = world;
	}
	
	public void setLeftNeighbor(World world) {
		neighbors[2] = world;
	}
	
	public void setTopNeighbor(World world) {
		neighbors[3] = world;
	}
	
	public boolean touchingRight(Entity e) {
		return getWidth() - e.getPos().getX() < EDGE_TP_THRESHOLD;
	}
	
	public boolean touchingBottom(Entity e) {
		return getHeight() - e.getPos().getY() < EDGE_TP_THRESHOLD;
	}
	
	public boolean touchingLeft(Entity e) {
		return e.getPos().getX() < EDGE_TP_THRESHOLD;
	}
	
	public boolean touchingTop(Entity e) {
		return e.getPos().getY() < EDGE_TP_THRESHOLD;
	}
	
	public void setUniverse(WorldGraph universe) {
		this.universe = universe;
	}
	
	public Playable getUniverse() {
		return universe;
	}
	
	public int getWidth() {
		return getLevel().getWidth() * Tile.WIDTH;
	}
	
	public int getHeight() {
		return getLevel().getHeight() * Tile.WIDTH;
	}
	
	@Override
	public String getFilePath() {
		return path;
	}
	
}
