package com.josephcatrambone.redshift.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.josephcatrambone.redshift.CodecCall;
import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/17/2016.
 */
public class CodecScene extends Scene {
	public static final String FONT_FILENAME = "codec.fnt";
	public static final String CODEC_CALL_SFX_FILENAME = "codec_call.ogg";
	public static final String CODEC_OPEN_SFX_FILENAME = "codec_open.wav";
	public static final String CODEC_CLOSE_SFX_FILENAME = "codec_close.wav";
	public static final String RADIO_FILENAME = "radio.png";

	String backgroundImageFilename;
	String leftImageFilename;
	String rightImageFilename;
	String[] dialog;
	int dialogPosition = 0;

	SpriteBatch batch;
	Camera camera;
	BitmapFont font;
	Texture radio;
	Texture leftImage;
	Texture rightImage;
	boolean keyWasPressed;

	Sound openSound;
	Sound closeSound;

	public boolean clearBlack; // If false, clear color is white.

	public CodecScene(String codecSequenceName) {
		this.keyWasPressed = false;
		// TODO: Replace the hard-coding with codec squence loading.
		backgroundImageFilename = RADIO_FILENAME;
		clearBlack = true;

		Json json = new Json();
		String jsonString = Gdx.files.internal(codecSequenceName).readString();

		CodecCall codecCall = json.fromJson(CodecCall.class, jsonString);
		leftImageFilename = codecCall.getLeftImage();
		rightImageFilename = codecCall.getRightImage();
		dialog = codecCall.getDialog();
		System.out.println("Read " + dialog.length + " lines.");
		dialogPosition = 0;
	}

	@Override
	public void create() {
		font = MainGame.assetManager.get(CodecScene.FONT_FILENAME, BitmapFont.class);
		radio = MainGame.assetManager.get(this.backgroundImageFilename);
		leftImage = MainGame.assetManager.get(this.leftImageFilename);
		rightImage = MainGame.assetManager.get(this.rightImageFilename);
		openSound = MainGame.assetManager.get(CODEC_OPEN_SFX_FILENAME);
		closeSound = MainGame.assetManager.get(CODEC_CLOSE_SFX_FILENAME);
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		batch = new SpriteBatch();
		camera.update();

		if(clearBlack) {
			Gdx.gl.glClearColor(0, 0, 0, 1.0f);
		} else {
			Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		}

		openSound.play();
	}

	@Override
	public void dispose() {
		//MainGame.assetManager.unload(backgroundImageFilename);
		//MainGame.assetManager.unload(rightImageFilename);
		//MainGame.assetManager.unload(leftImageFilename);
	}

	@Override
	public void render(float deltaTime) {
		Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

		batch.begin();
		batch.draw(radio, (Gdx.graphics.getWidth()-radio.getWidth())*0.5f, Gdx.graphics.getHeight()-radio.getHeight()-10f);
		batch.draw(leftImage, 10, Gdx.graphics.getHeight()-leftImage.getHeight()-10);
		batch.draw(rightImage, Gdx.graphics.getWidth()-rightImage.getWidth()-10, Gdx.graphics.getHeight()-rightImage.getHeight()-10);

		if(dialog != null && dialogPosition < dialog.length) { // We don't actually need this check any more.
			font.draw(batch, dialog[dialogPosition], 10, Gdx.graphics.getHeight()*0.5f, Gdx.graphics.getWidth()-10, Align.center, true);
		}
		batch.end();
	}

	@Override
	public void update(float deltaTime) {
		if(keyWasPressed && !Gdx.input.isKeyPressed(Input.Keys.ANY_KEY)) {
			keyWasPressed = false;
			advanceState();
		} else if(Gdx.input.isKeyPressed(Input.Keys.ANY_KEY)) {
			keyWasPressed = true;
		}
	}

	public void advanceState() {
		dialogPosition++;
		if(dialog == null || dialogPosition >= dialog.length) {
			closeSound.play();
			MainGame.popState();
		}
	}
}
