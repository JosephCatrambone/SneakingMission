package com.josephcatrambone.redshift.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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
	Player player;
	ArrayList<NPC> npcs;
	float sceneChangeDelay = 2.5f;

	RegionContactListener regionContactListener;

	Box2DDebugRenderer debugRenderer;

	@Override
	public void create() {
		MainGame.world = new World(new Vector2(0, 0), true);

		stage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())); // Fit viewport = black bars.
		debugRenderer = new Box2DDebugRenderer();

		regionContactListener = new RegionContactListener();
		MainGame.world.setContactListener(regionContactListener);

		// Setup camera.  Enforce y-up.
		float invAspectRatio = stage.getHeight()/stage.getWidth();
		camera = stage.getCamera();
		((OrthographicCamera)camera).setToOrtho(false, PIXEL_DISPLAY_WIDTH, PIXEL_DISPLAY_WIDTH*invAspectRatio);
		camera.update(true);

		level = new Level("test.tmx");

		player = new Player(level.getPlayerStartX(), level.getPlayerStartY());
		stage.addActor(player);

		// Spawn an NPC
		npcs = new ArrayList<NPC>();
		NPC npc = new NPC(150, 150);
		stage.addActor(npc);
		npcs.add(npc);

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
			if(npc.inConeOfVision(player.getX(), player.getY())) {
				System.out.println("NPC sees you! " + System.currentTimeMillis());
			}
		}

		// Camera follows player?
		camera.position.set(player.getX(), player.getY(), camera.position.z);
		camera.update();
	}

}
