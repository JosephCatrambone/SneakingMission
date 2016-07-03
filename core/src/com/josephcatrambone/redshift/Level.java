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

import java.util.*;

import static com.josephcatrambone.redshift.PhysicsConstants.PPM;

/**
 * Created by Jo on 12/22/2015.
 */
public class Level {
	public static final int[] BACKGROUND_LAYERS = new int[]{0, 1}; // Background + foreground.  No overlay yet.
	public static final int[] OVERLAY_LAYERS = new int[]{2};
	public static final String COLLISION_LAYER = "collision";
	public static final String TRIGGER_LAYER = "trigger";
	public static final String COOL_TYPE = "cool";
	public static final String TELEPORT_TYPE = "teleport";
	public static final String GOAL_TYPE = "goal";
	public static final String PLAYER_START_X_PROPERTY = "playerStartX";
	public static final String PLAYER_START_Y_PROPERTY = "playerStartY";
	TiledMap map;
	TiledMapRenderer renderer;
	Body collision;

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
		MapObjects mapObjects = map.getLayers().get(TRIGGER_LAYER).getObjects();
		// First, make the rectangle colliders.
		for(RectangleMapObject rob : mapObjects.getByType(RectangleMapObject.class)) {
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
			FixtureDef fdef = new FixtureDef();
			fdef.shape = shape;

			// Is this a cooling area or a collision or a teleporter or a trigger?
			HashMap <String, String> fixtureData = new HashMap<String,String>();
			Iterator<String> stringIterator = rob.getProperties().getKeys();
			// TODO: Fucking bullshit for(String key : rob.getProperties() doesn't work despite MapProperties implementing iter.
			while(stringIterator.hasNext()) {
				String key = stringIterator.next();
				fixtureData.put(key, (String)rob.getProperties().get(key).toString());
				if(key.equals(COOL_TYPE) || key.equals(TELEPORT_TYPE) || key.equals(GOAL_TYPE)) {
					fdef.isSensor = true;
				}
			}

			Fixture f = collision.createFixture(fdef);
			//collision.setUserData(type);
			f.setUserData(fixtureData);
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

	public Vector2[] getPath(float startX, float startY, float goalX, float goalY) {
		// Returns an unoptimized list of squares in world coordinates which don't touch any blocked cells.
		// Internally, this function will convert the start and goal to local squares, then solve with A*.
		TiledMapTileLayer collisionLayer = (TiledMapTileLayer)map.getLayers().get(COLLISION_LAYER);
		int mapWidth = collisionLayer.getWidth();
		int sx = (int)(startX / collisionLayer.getTileWidth());
		int sy = (int)(startY / collisionLayer.getTileHeight());
		int gx = (int)(goalX / collisionLayer.getTileWidth());
		int gy = (int)(goalY / collisionLayer.getTileHeight());

		// Run A*.
		// TODO: We're doing an O(n) search through the list of candidates. Switch to priority queue and write comparator.
		LinkedList<MapCandidatePoint> candidates = new LinkedList<MapCandidatePoint>();
		int[] parents = new int[collisionLayer.getWidth()*collisionLayer.getHeight()]; // -1 means no parent.
		float[] costs = new float[collisionLayer.getWidth()*collisionLayer.getHeight()];
		int previousPoint = -1;
		int startId = sx+sy*mapWidth;
		int goalId = gx+gy*mapWidth;

		// Push the start onto the stack.
		parents[startId] = -1;
		candidates.add(new MapCandidatePoint(startId, 0, Math.abs(sx-gx)+Math.abs(sy-gy)));

		while(!candidates.isEmpty()) {
			// Go through and get the best candidate.
			// TODO: See above.  Use priority queue to speed this up.
			float bestDistance = Float.POSITIVE_INFINITY;
			MapCandidatePoint bestCandidate = null;
			for(MapCandidatePoint c : candidates) {
				if(c.cost + c.minDistanceToGoal < bestDistance) {
					bestCandidate = c;
					bestDistance = c.cost + c.minDistanceToGoal;
				}
			}
			candidates.remove(bestCandidate); // Pop.

			// Mark our parent and cost so far.
			parents[bestCandidate.id] = previousPoint;
			previousPoint = bestCandidate.id;
			costs[bestCandidate.id] = bestCandidate.cost;

			// Are we at the goal?
			if(bestCandidate.id == goalId) {
				// Handle it.
				break;
			}

			// Add the neighbors of this position with their costs.
			// Right up left down.
			int nextId = 0;
			int x = bestCandidate.id%mapWidth;
			int y = bestCandidate.id/mapWidth;
			// Don't revisit these.
			nextId = (x+1)+y*mapWidth;
			if(parents[nextId] == 0 && collisionLayer.getCell(x+1, y) == null) {
				candidates.push(new MapCandidatePoint(nextId, bestCandidate.cost + 1, Math.abs(x + 1 - gx) + Math.abs(y - gy)));
			}
			nextId = x+(y+1)*mapWidth;
			if(parents[nextId] == 0 && collisionLayer.getCell(x, y+1) == null) {
				candidates.push(new MapCandidatePoint(nextId, bestCandidate.cost + 1, Math.abs(x - gx) + Math.abs(y + 1 - gy)));
			}
			nextId = (x-1)+y*mapWidth;
			if(parents[nextId] == 0 && collisionLayer.getCell(x-1, y) == null) {
				candidates.push(new MapCandidatePoint(nextId, bestCandidate.cost + 1, Math.abs(x - 1 - gx) + Math.abs(y - gy)));
			}
			nextId = x+(y-1)*mapWidth;
			if(parents[nextId] == 0 && collisionLayer.getCell(x, y-1) == null) {
				candidates.push(new MapCandidatePoint(nextId, bestCandidate.cost + 1, Math.abs(x - gx) + Math.abs(y - 1 - gy)));
			}
		}

		// If the goal has no parent, there's no path.
		if(parents[goalId] == -1) {
			return null;
		} else {
			// Convert each of the parents to real-world coordinates, then push it onto the stack.
			// Reverse at the end.
			Vector2[] path = null;

			LinkedList<Vector2> tempPath = new LinkedList<Vector2>(); // A double-linked list.
			int currentId = goalId;
			while(currentId != -1) { // TODO: Should we check for -1 here?
				// Convert to world space.
				int x = currentId%mapWidth;
				int y = currentId/mapWidth;
				// The 0.5 is a half-tile width so we go to the center of the tile.
				tempPath.push(new Vector2(x*collisionLayer.getTileWidth() + (collisionLayer.getTileWidth()*0.5f), y*collisionLayer.getTileHeight() + (collisionLayer.getTileHeight()*0.5f)));
				currentId = parents[currentId];
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

	private class MapCandidatePoint {
		public int id;
		public float cost;
		public float minDistanceToGoal;
		MapCandidatePoint(int id, float cost, float dist){
			this.id = id; this.cost = cost; this.minDistanceToGoal = dist;
		}
	}
}
