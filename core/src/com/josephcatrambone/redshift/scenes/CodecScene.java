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
public class CodecScene extends Scene {

	String backgroundImageFilename;
	String leftImageFilename;
	String rightImageFilename;

	SpriteBatch batch;
	Camera camera;
	Texture bg;
	Texture leftImage;
	Texture rightImage;
	boolean keyWasPressed;

	public boolean clearBlack; // If false, clear color is white.

	public CodecScene(String codecSequenceName) {
		this.keyWasPressed = false;
	}

	@Override
	public void create() {
		bg = MainGame.assetManager.get(this.backgroundImageFilename);
		leftImage = MainGame.assetManager.get(this.leftImageFilename);
		rightImage = MainGame.assetManager.get(this.rightImageFilename);
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
		MainGame.assetManager.unload(rightImageFilename);
		MainGame.assetManager.unload(leftImageFilename);
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
		if(keyWasPressed && !Gdx.input.isKeyPressed(Input.Keys.ANY_KEY)) {
			keyWasPressed = false;
			advanceState();
		} else if(Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
			keyWasPressed = true;
		}
	}

	public void advanceState() {

	}
}
