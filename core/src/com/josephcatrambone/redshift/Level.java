package com.josephcatrambone.redshift;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.josephcatrambone.redshift.actors.Pawn;

import java.util.*;

import static com.josephcatrambone.redshift.PhysicsConstants.PPM;

/**
 * Created by Jo on 12/22/2015.
 */
public class Level {
	public static final int MAX_RAYCAST_DISTANCE = 100;
	public static final int[] BACKGROUND_LAYERS = new int[]{0, 1}; // Background + foreground.  No overlay yet.
	public static final int[] OVERLAY_LAYERS = new int[]{2};
	public static final String COLLISION_LAYER = "collision";
	public static final String TRIGGER_LAYER = "trigger";
	public static final String TELEPORT_TYPE = "teleport";
	public static final String NPC_TYPE = "npc";
	public static final String WAYPOINT_TYPE = "waypoint";
	public static final String GOAL_TYPE = "goal";
	public static final String CODEC_TYPE = "codec";
	public static final String PLAYER_START_X_PROPERTY = "playerStartX";
	public static final String PLAYER_START_Y_PROPERTY = "playerStartY";
	TiledMap map;
	TiledMapRenderer renderer;
	Body collision;

	// TODO: This NPC stuff is a little messy.  We might be better with explicit classes.
	private ArrayList<NPCData> npcData; // Used to store spawn points with metadata.
	private HashMap<String,ArrayList<Vector2>> waypointSets; // Name -> Waypoints.

	public Level() {}
	public Level(String filename) {
		load(filename);
	}

	public void load(String filename) {
		// Use asset manager to resolve the asset loading.
		// See the extra setup in MainGame.
		//assetManager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
		//assetManager.load("level1.tmx", TiledMap.class);
		// TiledMap map = assetManager.get("level1.tmx");
		// For external maps:
		// map = new TmxMapLoader(new ExternalFileHandleResolver()).load(filename);
		map = new TmxMapLoader().load(filename);
		renderer = new OrthogonalTiledMapRenderer(map);

		// Create the rigid bodies for this map for collisions.
		BodyDef bdef = new BodyDef();
		bdef.type = BodyDef.BodyType.StaticBody;
		collision = MainGame.world.createBody(bdef);

		// Merge the collision layers into larger objects.
		TiledMapTileLayer collisionLayer = (TiledMapTileLayer)map.getLayers().get(COLLISION_LAYER);
		for(Rectangle r : optimizeCollisionLayer(collisionLayer)) {
			// Boxes come back in pixel coordinates, not map coordinates.
			PolygonShape shape = new PolygonShape();
			float w = r.getWidth();
			float h = r.getHeight();
			float x = r.getX();
			float y = r.getY();
			shape.setAsBox(w/PPM, h/PPM);
			shape.set(new float[]{ // Assumes exterior is right hand of each line in XY order.  CCW winding.
					x/PPM, y/PPM, (x+w)/PPM, (y)/PPM, (x+w)/PPM, (y+h)/PPM, (x)/PPM, (y+h)/PPM
			});
			FixtureDef fdef = new FixtureDef();
			fdef.shape = shape;
			Fixture f = collision.createFixture(fdef);
			//collision.setUserData(type);
		}

		// Build the triggers.
		npcData = new ArrayList<NPCData>();
		waypointSets = new HashMap< String,ArrayList<Vector2> >();
		MapObjects mapObjects = map.getLayers().get(TRIGGER_LAYER).getObjects();
		// First, make the rectangle colliders.
		for(RectangleMapObject rob : mapObjects.getByType(RectangleMapObject.class)) {
			boolean createFixture = true;
			// Boxes come back in pixel coordinates, not map coordinates.
			PolygonShape shape = new PolygonShape();
			Rectangle r = rob.getRectangle(); // Rekt? Not rekt?
			float w = r.getWidth();
			float h = r.getHeight();
			float x = r.getX();
			float y = r.getY();
			shape.setAsBox(w/PPM, h/PPM);
			shape.set(new float[]{ // Assumes exterior is right hand of each line in XY order.  CCW winding.
				x/PPM, y/PPM, (x+w)/PPM, (y)/PPM, (x+w)/PPM, (y+h)/PPM, (x)/PPM, (y+h)/PPM
			});

			// Is this a cooling area or a collision or a teleporter or a trigger?
			HashMap <String, String> fixtureData = new HashMap<String,String>();
			Iterator<String> stringIterator = rob.getProperties().getKeys();
			// TODO: Fucking bullshit for(String key : rob.getProperties() doesn't work despite MapProperties implementing iter.
			while(stringIterator.hasNext()) {
				String key = stringIterator.next();
				String value = rob.getProperties().get(key).toString(); // TODO: Actually get the class type.
				fixtureData.put(key, (String)value);
				if(value.equals(TELEPORT_TYPE) || value.equals(GOAL_TYPE) || value.equals(CODEC_TYPE)) {
					createFixture = true; // In this part, if we're creating a fixture, we're creating a sensor.
				} else if(value.equals(NPC_TYPE)) {
					createFixture = false;
					// Create the rest of the NPC data.
					NPCData npcd = new NPCData();
					// Remap to world space.
					npcd.position = new Vector2(rob.getProperties().get("x", Float.class), rob.getProperties().get("y", Float.class));
					npcd.direction = Pawn.Direction.values()[Integer.parseInt(rob.getProperties().get("direction", "0", String.class))];
					npcd.waypointSet = rob.getProperties().get("waypoint_set_name", "", String.class);
					npcData.add(npcd);
				} else if(value.equals(WAYPOINT_TYPE)) {
					String waypointSetName = rob.getProperties().get("waypoint_set_name", String.class);
					int id = Integer.parseInt(rob.getProperties().get("waypoint_id", String.class));
					Vector2 pos = new Vector2(rob.getProperties().get("x", Float.class), rob.getProperties().get("y", Float.class));
					ArrayList<Vector2> waypoints = null;
					if(!waypointSets.containsKey(waypointSetName)) {
						waypoints = new ArrayList<Vector2>();
						waypointSets.put(waypointSetName, waypoints);
					} else {
						waypoints = waypointSets.get(waypointSetName);
					}
					waypoints.add(id, pos); // TODO: We should set so we don't push around IDs by accident.
				}
			}

			// If we're making a trigger, actually create the fixture definition.
			if(createFixture) {
				FixtureDef fdef = new FixtureDef();
				fdef.shape = shape;
				fdef.isSensor = true;
				Fixture f = collision.createFixture(fdef);
				//collision.setUserData(type);
				f.setUserData(fixtureData);
			}
		}
		for(PolygonMapObject pob : mapObjects.getByType(PolygonMapObject.class)) {
			// TODO: Poly support.
		}
	}

	public Rectangle[] optimizeCollisionLayer(TiledMapTileLayer collisionLayer) {
		// Rather than merge into the fewest rectangles (which can be done by picking the minimum separable set of the
		// bipartite graph made from the edges of the intersections of the interior verts), we will take the stupid
		// solution and greedily select the largest fitting rectangle, then merge them down.  This isn't guaranteed to
		// be optimal, but it will
		// collisionLayer.getCell(0, 0).tile.id == 0
		// collisionLayer.getCell(0, 0) == null for no tile
		// collisionLayer.y is inverted.  0 is bottom of map.

		// We're basically storing the origin of the rectangle inside a grid of the same size as the map.
		// The position is given by widths[x,y] and the width the the value at that location.  Repeat for height, but merging if widths match.
		int[] widths = new int[collisionLayer.getWidth()*collisionLayer.getHeight()];
		int[] heights = new int[collisionLayer.getWidth()*collisionLayer.getHeight()];
		int startX = -1;
		int startY = -1;
		int previousWidth = -1;

		for(int y=0; y < collisionLayer.getHeight(); y++) {
			for(int x=0; x < collisionLayer.getWidth(); x++) {
				if(collisionLayer.getCell(x, y) == null) {
					if(startX == -1) { // Do nothing.  We weren't started.
					} else { // We've come to the end.
						// First, for all records in this range, record the start and stop.
						startX = -1; // End this run.
					}
				} else {
					if(startX == -1) { // We had a starting edge and we're still in a tile.
						startX = x;
						widths[startX + y*collisionLayer.getWidth()] = 1; // Increment the width.
					} else { // We are continuing and already had a start edge.
						widths[startX + y*collisionLayer.getWidth()]++; // Increment the width.
					}
				}
			}
		}

		for(int x=0; x < collisionLayer.getWidth(); x++) {
			for(int y=0; y < collisionLayer.getHeight(); y++) {
				int currentBlockWidth = widths[x+y*collisionLayer.getWidth()];
				if(currentBlockWidth == 0) { // No rectangle starts here.  We've come to the end.
					startY = -1; // End this run.
					startX = -1;
					previousWidth = -1;
				} else if(currentBlockWidth == previousWidth) { // Continuing an edge.  CurrentBlockWidth > 0.
					heights[startX + startY*collisionLayer.getWidth()]++; // Increment the width.
				} else { // New edge.  currentBlockWidth > 0 && currentBlockWidth != previousBlockWidth.
					startX = x;
					startY = y;
					previousWidth = currentBlockWidth;
					heights[startX + startY*collisionLayer.getWidth()] = 1; // Increment the height.
				}
			}
		}

		ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();
		for(int y=0; y < collisionLayer.getHeight(); y++) {
			for(int x=0; x < collisionLayer.getWidth(); x++) {
				// If the width is nonzero and the height is nonzero, make rect.
				Rectangle r =
					new Rectangle(
						x*collisionLayer.getTileWidth(),
						y*collisionLayer.getTileHeight(),
						widths[x+y*collisionLayer.getWidth()]*collisionLayer.getTileWidth(),
						heights[x+y*collisionLayer.getWidth()]*collisionLayer.getTileHeight()
					);
				if(r.getWidth() > 0 && r.getHeight() > 0) {
					System.out.println("Made rectangle: " + r.toString());
					rectangles.add(r);
				}
			}
		}
		Rectangle[] finalRectangles = new Rectangle[rectangles.size()];
		rectangles.toArray(finalRectangles);
		System.out.println("Made " + finalRectangles.length + " rectangles.");
		return finalRectangles;
	}

	public void dispose() {
		// TODO: Unload sprite sheet.
		MainGame.world.destroyBody(collision);
	}

	public void drawBG(Camera camera) {
		renderer.setView((OrthographicCamera)camera);
		renderer.render(BACKGROUND_LAYERS);
	}

	public void drawOverlay(Camera camera) {
		renderer.setView((OrthographicCamera)camera);
		renderer.render(OVERLAY_LAYERS);
	}

	public void act(float deltatime) {

	}

	public int getPlayerStartX() {
		return Integer.parseInt(map.getProperties().get(PLAYER_START_X_PROPERTY, "0", String.class));
			// *map.getProperties().get(TILE_WIDTH_PROPERTY, 0, Integer.class);
	}

	public int getPlayerStartY() {
		// y-down in the map, so flip.
		return Integer.parseInt(map.getProperties().get(PLAYER_START_Y_PROPERTY, "0", String.class));
		//(map.getProperties().get(MAP_HEIGHT_PROPERTY, 0, Integer.class) * map.getProperties().get(TILE_HEIGHT_PROPERTY, 0, Integer.class) - Integer.parseInt(map.getProperties().get(PLAYER_START_Y_PROPERTY, "0", String.class)));
	}

	public int getNPCCount() {
		return npcData.size();
	}

	public Vector2 getNPCSpawnPoint(int id) {
		return npcData.get(id).position;
	}

	public Pawn.Direction getNPCDirection(int id) {
		return npcData.get(id).direction;
	}

	public Vector2[] getNPCWaypoints(int id) {
		String waypointSetName = npcData.get(id).waypointSet;
		if(waypointSetName.equals("") || !waypointSets.containsKey(waypointSetName)) {
			return null;
		}
		ArrayList<Vector2> waypointArrayList = waypointSets.get(waypointSetName);
		Vector2[] waypoints = new Vector2[waypointArrayList.size()];
		waypointArrayList.toArray(waypoints);
		return waypoints;
	}

	private class NPCData {
		public Vector2 position;
		public Pawn.Direction direction;
		public String waypointSet; // wayspoint_set_name.  Picked because I don't want to get confused with 'path'.
	}

	public Vector2[] getPath(float startX, float startY, float goalX, float goalY) {
		// Returns an unoptimized list of squares in world coordinates which don't touch any blocked cells.
		// Internally, this function will convert the start and goal to local squares, then solve with A*.
		TiledMapTileLayer collisionLayer = (TiledMapTileLayer)map.getLayers().get(COLLISION_LAYER);
		int mapWidth = collisionLayer.getWidth();
		int sx = (int)(startX / collisionLayer.getTileWidth());
		int sy = (int)(startY / collisionLayer.getTileHeight());
		int gx = (int)(goalX / collisionLayer.getTileWidth());
		int gy = (int)(goalY / collisionLayer.getTileHeight());
		boolean[] visited = new boolean[collisionLayer.getWidth()*collisionLayer.getHeight()];
		MapPoint goal = null;

		// Run A*.
		PriorityQueue<MapPoint> candidates = new PriorityQueue<MapPoint>(new Comparator<MapPoint>() {
			@Override
			public int compare(MapPoint o1, MapPoint o2) {
				if(o1.cost+o1.minDistanceToGoal < o2.cost+o2.minDistanceToGoal) {
					return -1;
				} else if(o1.cost+o1.minDistanceToGoal > o2.cost+o2.minDistanceToGoal) {
					return +1;
				} else {
					return 0;
				}
			}
		});

		// Push the start onto the stack.
		candidates.add(new MapPoint(sx, sy, 0, Math.abs(sx-gx)+Math.abs(sy-gy), null));
		visited[sx+sy*mapWidth] = true;

		while(!candidates.isEmpty()) {
			// Go through and get the best candidate.
			MapPoint bestCandidate = candidates.poll();
			visited[bestCandidate.x+bestCandidate.y*mapWidth] = true;

			// Are we at the goal?
			if(bestCandidate.x == gx && bestCandidate.y == gy) {
				// Handle it.
				goal = bestCandidate;
				break;
			}

			// Add the neighbors of this position with their costs.
			// Right up left down.
			int[] cos = new int[]{1, 0, -1, 0};
			int[] sin = new int[]{0, 1, 0, -1};
			// Don't revisit these.
			for(int i=0; i < Pawn.Direction.NUM_DIRECTIONS.ordinal(); i++) {
				int dx = bestCandidate.x+cos[i];
				int dy = bestCandidate.y+sin[i];
				if(dx < 0 || dy < 0 || dx >= collisionLayer.getWidth() || dy >= collisionLayer.getHeight()) {
					continue;
				}
				if(!visited[dx+dy*mapWidth] && collisionLayer.getCell(dx, dy) == null) {
					candidates.add(new MapPoint(dx, dy, bestCandidate.cost + 1, Math.abs(gx-dx) + Math.abs(gy-dy), bestCandidate));
				}
			}
		}

		// If the goal has no parent, there's no path.
		if(goal == null) {
			return null;
		} else {
			// Convert each of the parents to real-world coordinates, then push it onto the stack.
			// Reverse at the end.
			Vector2[] path = null;

			LinkedList<Vector2> tempPath = new LinkedList<Vector2>(); // A double-linked list.
			MapPoint current = goal;
			float halfTileWidth = collisionLayer.getTileWidth()*0.5f;
			float halfTileHeight = collisionLayer.getTileHeight()*0.5f;
			while(current != null) {
				// Convert to world space.
				int x = current.x;
				int y = current.y;
				// The 0.5 is a half-tile width so we go to the center of the tile.
				tempPath.push(new Vector2(x*collisionLayer.getTileWidth() + halfTileWidth, y*collisionLayer.getTileHeight() + halfTileHeight));
				current = current.parent;
			}
			// Convert our list into an array and reverse it.
			path = new Vector2[tempPath.size()];
			int i=0;
			// Since we did 'push' on our linked list, this path is already reversed.
			//java.util.Iterator<Vector2> iter = tempPath.descendingIterator();
			for(Vector2 v : tempPath) {
				path[i++] = v;
			}

			return path;
		}
	}

	private class MapPoint {
		public int x;
		public int y;
		public float cost;
		public float minDistanceToGoal;
		public MapPoint parent;
		MapPoint(int x, int y, float cost, float dist, MapPoint parent){
			this.x = x; this.y = y; this.cost = cost; this.minDistanceToGoal = dist; this.parent = parent;
		}
	}

	public boolean isClearPath(float startX, float startY, float goalX, float goalY) {
		// Returns true if there are no obstacles between the start and end, as determined by a conservative ray-cast.
		// This will likely report NO sight more often than sight because we want to make it easier for the player.
		TiledMapTileLayer collisionLayer = (TiledMapTileLayer)map.getLayers().get(COLLISION_LAYER);
		int mapWidth = collisionLayer.getWidth();
		int sx = (int)(startX / collisionLayer.getTileWidth());
		int sy = (int)(startY / collisionLayer.getTileHeight());
		int gx = (int)(goalX / collisionLayer.getTileWidth());
		int gy = (int)(goalY / collisionLayer.getTileHeight());

		int dx = gx-sx;
		int dy = gy-sy;

		if(dx == 0) { // Handle straight vertical.
			if(dy == 0) { // Exact same tile.
				return true;
			} else { // Most cases of vertical lines.
				dy /= Math.abs(dy); // Force dy to be +1 or -1.
				while(sy != gy) {
					if(collisionLayer.getCell(sx, sy) != null) {
						return false;
					}
					sy += dy;
				}
				return true;
			}
		} else { // Handle remaining cases.
			int iterationCount = 0; // Our safety in case our floats drift.  We're better off picking a different algo, but I'm lazy.
			// y = mx+b.  We have y and x at the start.  We derive m.  Calculate b.  y-mx = b
			float m = (float)dy/(float)dx;
			float b = gy - m*gx;
			while(sx != gx && sy != gy && iterationCount++ < MAX_RAYCAST_DISTANCE) {
				sx += 1;
				sy = (int)(m*sx + b);
				if(collisionLayer.getCell(sx, sy) != null) {
					return false;
				}
			}
			return true;
		}
	}
}
