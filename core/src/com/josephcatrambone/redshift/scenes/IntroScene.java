package com.josephcatrambone.metalskyarena.scenes;

import com.josephcatrambone.metalskyarena.MainGame;

/**
 * Created by Jo on 1/20/2016.
 */
public class IntroScene extends KeyWaitScene {

	public static final String INTRO_BG = "intro.png";

	public IntroScene() {
		super(INTRO_BG, MainGame.GameState.TITLE);
		this.clearBlack = false;
	}
}
