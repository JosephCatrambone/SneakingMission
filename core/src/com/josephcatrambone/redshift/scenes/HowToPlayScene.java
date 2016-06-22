package com.josephcatrambone.redshift.scenes;

import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/20/2016.
 */
public class HowToPlayScene extends KeyWaitScene {
	public static final String HOW_TO_PLAY = "instructions.png";

	public HowToPlayScene() {
		super(HOW_TO_PLAY, MainGame.GameState.PLAY);
		this.clearBlack = false;
	}
}
