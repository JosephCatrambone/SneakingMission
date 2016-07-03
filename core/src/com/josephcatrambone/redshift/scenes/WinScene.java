package com.josephcatrambone.redshift.scenes;

import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/17/2016.
 */
public class WinScene extends KeyWaitScene {

	public static final String WIN_BG = "youwin.png";

	public WinScene() {
		super(WIN_BG, new TitleScene());
		this.clearBlack = true;
	}
}
