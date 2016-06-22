package com.josephcatrambone.redshift.scenes;

/**
 * Created by Jo on 12/20/2015.
 */
public abstract class Scene {
	public abstract void create();
	public abstract void dispose();
	public abstract void render(float deltaTime);
	public abstract void update(float deltaTime);

}
