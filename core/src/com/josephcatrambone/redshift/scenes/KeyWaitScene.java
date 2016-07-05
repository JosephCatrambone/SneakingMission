package com.josephcatrambone.redshift.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/17/2016.
 */
public class KeyWaitScene extends Scene {

	String backgroundImageFilename;
	Scene nextScene;
	SpriteBatch batch;
	Camera camera;
	Texture bg;

	public boolean clearBlack; // If false, clear color is white.

	public KeyWaitScene(String backgroundImageFilename, Scene nextScene) {
		this.backgroundImageFilename = backgroundImageFilename;
		this.nextScene = nextScene;
	}

	@Override
	public void create() {
		bg = MainGame.assetManager.get(this.backgroundImageFilename);
		camera = new OrthographicCamera(bg.getWidth(), bg.getHeight());
		batch = new SpriteBatch();
		camera.update();

		if(clearBlack) {
			Gdx.gl.glClearColor(0, 0, 0, 1.0f);
		} else {
			Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	@Override
	public void dispose() {
		MainGame.assetManager.unload(backgroundImageFilename);
	}

	@Override
	public void render(float deltaTime) {
		Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

		batch.begin();
		batch.draw(bg, 0, 0, bg.getWidth() * 4, bg.getHeight() * 4);
		batch.end();
	}

	@Override
	public void update(float deltaTime) {
		if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
			nextScene();
		}
	}

	public void nextScene() {
		MainGame.switchState(nextScene);
	}
}
