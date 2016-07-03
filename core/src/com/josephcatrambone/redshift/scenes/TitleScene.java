package com.josephcatrambone.redshift.scenes;

import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/16/2016.
 */
public class TitleScene extends KeyWaitScene {

	public static final String TITLE_BG = "title.png";
	public static boolean SEEN_ONCE = false; // Terrible ass-shit hat to do an instruction scene.

	public TitleScene() {
		super(TITLE_BG, new PlayScene());
		if(!SEEN_ONCE) {
			SEEN_ONCE = true;
			this.nextScene = new HowToPlayScene();
		}
		this.clearBlack = false;
	}
}
