package com.josephcatrambone.redshift;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.World;
import com.josephcatrambone.redshift.actors.Player;
import com.josephcatrambone.redshift.scenes.*;

import java.util.Random;
import java.util.Stack;

public class MainGame extends ApplicationAdapter {
	public enum GameState {INTRO, TITLE, HOW_TO_PLAY, PLAY, GAME_OVER, WIN, NUM_STATES};
	public static final Random random;
	public static World world;
	public static final AssetManager assetManager;
	public static final Stack<Scene> scenes;

	static {
		random = new Random();
		world = null;
		assetManager = new AssetManager();
		scenes = new Stack<Scene>();
	}
	
	@Override
	public void create () {
		loadAllAssets();

		switchState(GameState.INTRO);
	}

	@Override
	public void render () {
		float dt = Gdx.graphics.getDeltaTime();
		scenes.peek().update(dt);
		scenes.peek().render(dt);
	}

	public void loadAllAssets() {
		assetManager.load("missing.png", Texture.class);
		assetManager.load(Player.PLAYER_SPRITESHEET, Texture.class);
		assetManager.load(Player.PLAYER_COOLDOWN, Sound.class);
		assetManager.load(Player.PLAYER_OVERHEAT, Sound.class);
		assetManager.load(IntroScene.INTRO_BG, Texture.class);
		assetManager.load(TitleScene.TITLE_BG, Texture.class);
		assetManager.load(HowToPlayScene.HOW_TO_PLAY, Texture.class);
		assetManager.load(GameOverScene.GAME_OVER_BG, Texture.class);
		assetManager.load(WinScene.WIN_BG, Texture.class);
		assetManager.finishLoading();
	}

	public static void switchState(GameState newState) {
		Scene newScene = null;
		switch(newState) {
			case INTRO:
				newScene = new IntroScene();
				break;
			case TITLE:
				newScene = new TitleScene();
				break;
			case HOW_TO_PLAY:
				newScene = new HowToPlayScene();
				break;
			case PLAY:
				newScene = new PlayScene();
				break;
			case GAME_OVER:
				newScene = new GameOverScene();
				break;
			case WIN:
				newScene = new WinScene();
				break;
		}
		if(!scenes.empty()) {
			scenes.pop().dispose();
		}
		newScene.create();
		scenes.push(newScene);
	}
}
