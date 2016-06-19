package com.josephcatrambone.metalskyarena.scenes;

import com.josephcatrambone.metalskyarena.MainGame;

/**
 * Created by Jo on 1/16/2016.
 */
public class GameOverScene extends KeyWaitScene {

	public static final String GAME_OVER_BG = "gameover.png";

	public GameOverScene() {
		super(GAME_OVER_BG, MainGame.GameState.TITLE);
		this.clearBlack = true;
	}
}
