package com.josephcatrambone.redshift.scenes;

import com.josephcatrambone.redshift.MainGame;

/**
 * Created by Jo on 1/16/2016.
 */
public class TitleScene extends KeyWaitScene {

	public static final String TITLE_BG = "title.png";
	public static boolean seenIntroCodec = false;

	public TitleScene() {
		super(TITLE_BG, new PlayScene());
		this.clearBlack = false;
	}

	@Override
	public void nextScene() {
		if(seenIntroCodec == false) {
			seenIntroCodec = true;
			MainGame.pushState(new PlayScene());
			MainGame.pushState(new CodecScene("codec_intro.json"));
		} else {
			MainGame.pushState(new PlayScene());
		}
	}
}
