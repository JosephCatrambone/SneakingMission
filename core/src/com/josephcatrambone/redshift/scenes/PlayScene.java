package com.josephcatrambone.redshift.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.josephcatrambone.redshift.Level;
import com.josephcatrambone.redshift.MainGame;
import com.josephcatrambone.redshift.actors.NPC;
import com.josephcatrambone.redshift.actors.Pawn;
import com.josephcatrambone.redshift.actors.Player;
import com.josephcatrambone.redshift.handlers.RegionContactListener;

import java.util.ArrayList;

/**
 * Created by Jo on 12/20/2015.
 */
public class PlayScene extends Scene {
	public final int PIXEL_DISPLAY_WIDTH = 160; // Ten pixels on a side?
	Stage stage;
	Camera camera;
	Level level;
	Music backgroundMusic;
	Player player;
	ArrayList<NPC> npcs;
	float sceneChangeDelay = 2.5f;

	RegionContactListener regionContactListener;

	@Override
	public void create() {
		MainGame.world = new World(new Vector2(0, 0), true);

		// Set up drawing area.
		stage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())); // Fit viewport = black bars.

		// Set up work physics.
		regionContactListener = new RegionContactListener();
		MainGame.world.setContactListener(regionContactListener);

		// Setup camera.  Enforce y-up.
		float invAspectRatio = stage.getHeight()/stage.getWidth();
		camera = stage.getCamera();
		((OrthographicCamera)camera).setToOrtho(false, PIXEL_DISPLAY_WIDTH, PIXEL_DISPLAY_WIDTH*invAspectRatio);
		camera.update(true);

		// Load map and add actors.
		level = new Level("test.tmx");
		player = new Player(level.getPlayerStartX(), level.getPlayerStartY());
		stage.addActor(player);
		// Spawn NPCs.
		npcs = new ArrayList<NPC>();
		for(int i=0; i < level.getNPCCount(); i++) {
			Vector2 pos = level.getNPCSpawnPoint(i);
			NPC npc = new NPC((int)pos.x, (int)pos.y);
			npc.direction = level.getNPCDirection(i);
			Vector2[] waypoints = level.getNPCWaypoints(i);
			if(waypoints != null && waypoints.length > 0) {
				npc.setPatrolRoute(waypoints);
			}
			stage.addActor(npc);
			npcs.add(npc);
		}

		// If the background music is playing and it's not our default music, use that.
		if(backgroundMusic == null) {
			backgroundMusic = MainGame.assetManager.get("SneakingMission.ogg", Music.class);
		}
		if(backgroundMusic.isPlaying()) {
			backgroundMusic.stop();
		}
		backgroundMusic.setLooping(true);
		backgroundMusic.play();

		// Global input listener if needed.
		stage.addListener(player.getInputListener());

		// TODO: When resuming, restore input processors.
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void dispose() {
		level.dispose();
		stage.dispose();
		MainGame.world.dispose();
	}

	@Override
	public void render(float deltaTime) {
		Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
		level.drawBG(camera);
		stage.draw();
		//debugRenderer.render(MainGame.world, camera.combined);
		level.drawOverlay(camera);
	}

	@Override
	public void update(float deltaTime) {
		MainGame.world.step(deltaTime, 8, 3);
		stage.act(deltaTime);

		// Reached the goal?
		if(regionContactListener.reachedGoal) {
			MainGame.switchState(new WinScene());
		}

		// Touch teleporter?
		if(regionContactListener.playerTeleport) {
			regionContactListener.playerTeleport = false;
			if(regionContactListener.teleportMap != null) {
				level.load(regionContactListener.teleportMap);
			}
			player.teleportTo(regionContactListener.teleportX, regionContactListener.teleportY);
		}

		// How long has the player been dead?
		if(player.state == Pawn.State.DEAD) {
			sceneChangeDelay -= deltaTime;
			if(sceneChangeDelay < 0) {
				MainGame.switchState(new GameOverScene());
			}
		}

		// Does any NPC see the player?
		for(NPC npc : npcs) {
			if(npc.inConeOfVision(player.getX(), player.getY())) { // Rough cone of vision.
				if(level.isClearPath(npc.getX(), npc.getY(), player.getX(), player.getY())) {
					npc.seesPlayerAt(player.getX(), player.getY());
				}
			}
		}

		// Camera follows player?
		camera.position.set(player.getX(), player.getY(), camera.position.z);
		camera.update();
	}

}
