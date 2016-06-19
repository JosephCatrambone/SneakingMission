package com.josephcatrambone.metalskyarena.scenes;

import com.josephcatrambone.metalskyarena.MainGame;

/**
 * Created by Jo on 1/16/2016.
 */
public class TitleScene extends KeyWaitScene {

	public static final String TITLE_BG = "title.png";
	public static boolean SEEN_ONCE = false; // Terrible ass-shit hat to do an instruction scene.

	public TitleScene() {
		super(TITLE_BG, MainGame.GameState.PLAY);
		if(!SEEN_ONCE) {
			SEEN_ONCE = true;
			this.nextState = MainGame.GameState.HOW_TO_PLAY;
		}
		this.clearBlack = false;
	}
}
