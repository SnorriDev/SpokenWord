package snorri.world;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import snorri.main.Main;
import snorri.world.Tile.TileType;



public class Level {
	
	private Tile[][]	map;
	private Vector		dim;
						
	//TODO: load from file, not empty grid with parameters
	//not that indexing conventions are Cartesian, not matrix-based
	
	public Level(int width, int height) {
		map = new Tile[width][height];
		dim = new Vector(width, height);
		
		for (int i = 0; i < dim.getX(); i++ ) {
			for (int j = 0; j < dim.getY(); j++ ) {
				map[i][j] = new Tile(TileType.SAND);
			}
		}
	}
	
	public Level(String levelFileName) {
		load(levelFileName);
	}
	
	public void setTile(int x, int y, Tile t) {
		map[x / Tile.WIDTH][y / Tile.WIDTH] = t;
	}
	
	public void setTileRaw(int x, int y, Tile t) {
		map[x][y] = t;
	}
	
	public Tile getTile(int x, int y) {
		return map[x / Tile.WIDTH][y / Tile.WIDTH];
	}
	
	public Tile getTile(Vector v) {
		return getTile(v.getX(), v.getY());
	}
	
	public Tile getTileRaw(int x, int y) {
		return map[x][y];
	}
	
	public Vector getDimensions() {
		return dim;
	}
	
	public void load(String fileName) {
		Main.log("loading " + fileName + "...");
		try {
			byte[] b = new byte[4];
			
			FileInputStream is = new FileInputStream(fileName);
			
			is.read(b);
			int width = ByteBuffer.wrap(b).getInt();
			is.read(b);
			int height = ByteBuffer.wrap(b).getInt();
			
			dim = new Vector(width, height);
			map = new Tile[width][height];
			
			byte[] b2 = new byte[2];
			for (int i = 0; i < width; i++ ) {
				for (int j = 0; j < height; j++ ) {
					is.read(b2);
					map[i][j] = new Tile(((Byte) b2[0]).intValue(), ((Byte) b2[1]).intValue());
				}
			}
			
			is.close();
		}
		catch (FileNotFoundException ex) {
			Main.error("Unable to open file '" + fileName + "'");
		}
		catch (IOException ex) {
			Main.error("Error reading file '" + fileName + "'");
		}
		Main.log("Load Complete!");
	}
	
	public void save(String fileName) {
		Main.log("saving " + fileName + "...");
		try {
			FileOutputStream os = new FileOutputStream(fileName);
			ByteBuffer b1 = ByteBuffer.allocate(4);
			ByteBuffer b2 = ByteBuffer.allocate(4);
			
			byte[] buffer = b1.putInt(dim.getX()).array();
			os.write(buffer);
			buffer = b2.putInt(dim.getY()).array();
			os.write(buffer);
			
			for (int i = 0; i < dim.getX(); i++ ) {
				for (int j = 0; j < dim.getY(); j++ ) {
					os.write(((byte) map[i][j].getType().getId()) & 0xFF);
					os.write(((byte) map[i][j].getStyle()) & 0xFF);
				}
			}
			
			os.close();
		}
		catch (IOException ex) {
			Main.error("Error writing file '" + fileName + "'");
		}
		Main.log("Save Complete!");
	}
}
