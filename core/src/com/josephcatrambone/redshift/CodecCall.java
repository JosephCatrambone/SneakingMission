package com.josephcatrambone.redshift;

/**
 * Created by josephcatrambone on 7/3/16.
 */
public class CodecCall {
	String leftImage;
	String rightImage;
	String[] dialog;

	public void setLeftImage(String leftImageFilename) { leftImage = leftImageFilename; }
	public void setRightImage(String rightImageFilename) { rightImage = rightImageFilename; }
	public void setDialog(String[] dialog) { this.dialog = dialog; }
	public String getLeftImage() { return leftImage; }
	public String getRightImage() { return rightImage; }
	public String[] getDialog() { return dialog; }
}
