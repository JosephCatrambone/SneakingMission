package com.josephcatrambone.redshift;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.physics.box2d.World;
import com.josephcatrambone.redshift.actors.NPC;
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

		//switchState(new IntroScene());
		switchState(new PlayScene());
	}

	@Override
	public void render () {
		float dt = Gdx.graphics.getDeltaTime();
		scenes.peek().update(dt);
		scenes.peek().render(dt);
	}

	public void loadAllAssets() {
		assetManager.load("missing.png", Texture.class);
		assetManager.load("SneakingMission.ogg", Music.class);
		assetManager.load(CodecScene.CODEC_CALL_SFX_FILENAME, Sound.class);
		assetManager.load(CodecScene.CODEC_OPEN_SFX_FILENAME, Sound.class);
		assetManager.load(CodecScene.CODEC_CLOSE_SFX_FILENAME, Sound.class);
		assetManager.load("codec.png", Texture.class);
		assetManager.load("codec.fnt", BitmapFont.class);
		assetManager.load("radio.png", Texture.class);
		assetManager.load("grandpa2_codec.png", Texture.class);
		assetManager.load("grandma2_codec.png", Texture.class);
		assetManager.load(Player.SPRITESHEET, Texture.class);
		assetManager.load(NPC.SPRITESHEET, Texture.class);
		//assetManager.load(Player.PLAYER_COOLDOWN, Sound.class);
		assetManager.load(IntroScene.INTRO_BG, Texture.class);
		assetManager.load(TitleScene.TITLE_BG, Texture.class);
		assetManager.load(GameOverScene.GAME_OVER_BG, Texture.class);
		assetManager.load(WinScene.WIN_BG, Texture.class);
		assetManager.finishLoading();
	}

	public static void popState() {
		if(!scenes.isEmpty()) {
			scenes.pop().dispose();
		}
	}

	public static void pushState(Scene scene) {
		scene.create();
		scenes.push(scene);
	}

	public static void switchState(Scene scene) {
		if(!scenes.isEmpty()) {
			scenes.pop().dispose();
		}
		scene.create();
		scenes.push(scene);
	}
}
