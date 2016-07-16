package com.josephcatrambone.redshift.scenes;

import com.badlogic.gdx.audio.Sound;
import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/20/2016.
 */
public class IntroScene extends KeyWaitScene {

	public static final String INTRO_BG = "intro.png";

	public IntroScene() {
		super(INTRO_BG, new TitleScene());
		this.clearBlack = false;

		// Crappy hack.
		MainGame.assetManager.get(CodecScene.CODEC_CALL_SFX_FILENAME, Sound.class).play();
	}
}
