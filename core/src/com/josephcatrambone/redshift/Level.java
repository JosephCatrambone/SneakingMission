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
import com.badlogic.gdx.physics.box2d.*;

import java.util.HashMap;
import java.util.Iterator;

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
	public static final String MAP_HEIGHT_PROPERTY = "height";
	public static final String TILE_WIDTH_PROPERTY = "tilewidth";
	public static final String TILE_HEIGHT_PROPERTY = "tileheight";
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
		//TiledMapTileLayer mapLayer = (TiledMapTileLayer) // TODO: Error checking.

		// Merge the collision layers into larger objects.
		TiledMapTileLayer collisionLayer = (TiledMapTileLayer)map.getLayers().get(COLLISION_LAYER);
		// TODO: Start here.

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
		// y-down, so flip.
		return
			(map.getProperties().get(MAP_HEIGHT_PROPERTY, 0, Integer.class) * map.getProperties().get(TILE_HEIGHT_PROPERTY, 0, Integer.class)
					- Integer.parseInt(map.getProperties().get(PLAYER_START_Y_PROPERTY, "0", String.class)));
	}
}
