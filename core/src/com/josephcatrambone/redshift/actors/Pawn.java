package com.josephcatrambone.metalskyarena.actors;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Align;
import com.josephcatrambone.metalskyarena.MainGame;

import static com.josephcatrambone.metalskyarena.PhysicsConstants.PPM;

/**
 * Created by Jo on 12/20/2015.
 */
public class Pawn extends Actor {
	public enum State {IDLE, MOVING, DEAD, NUM_STATES};
	public enum Direction {RIGHT, UP, LEFT, DOWN, NUM_DIRECTIONS}; // Purely graphical, doesn't impact physics body.

	// Sounds


	// Animation
	public final String SPRITE_SHEET_FILENAME = "missing.png";
	public Texture spriteSheet; // Don't use Image subclass or Sprite.
	public State state;
	public Direction direction;
	public Animation[][] animations; // Cartesian product of state and direction.
	public float stateTime; // How long have we been in this anim state?

	// Physics
	public Body physicsBody;

	public Pawn() {
		// DO NOTHING.
		// Don't even init graphics.  Let the inheriting class call all the setup + create.
	}

	/*** Pawn
	 * Given a reference to the Box2D world and pixel coordinates in SCREEN SPACE, create a new player.
	 * @param x
	 * @param y
	 */
	public Pawn(int x, int y) {
		create(x, y, 8, 8, 1.0f, SPRITE_SHEET_FILENAME);
	}

	public void create(float x, float y, int hw, int hh, float mass, String spriteSheetFilename) {
		// Load graphics.
		spriteSheet = MainGame.assetManager.get(spriteSheetFilename);
		animations = new Animation[State.NUM_STATES.ordinal()][Direction.NUM_DIRECTIONS.ordinal()];

		// Set game state.
		state = State.IDLE;
		direction = Direction.RIGHT;

		// Create visible bounds.
		this.setPosition(x, y, Align.center);
		this.setBounds(0, 0, hw*2, hh*2);
		this.setOrigin(Align.center);

		// Create physics body.
		World world = MainGame.world;
		BodyDef bdef = new BodyDef();
		bdef.fixedRotation = true;
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyDef.BodyType.DynamicBody;
		physicsBody = world.createBody(bdef);
		MassData md = physicsBody.getMassData();
		md.mass = mass;
		physicsBody.setMassData(md);
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(hw/PPM, hh/PPM);
		FixtureDef fdef = new FixtureDef();
		fdef.shape = shape;
		physicsBody.createFixture(fdef);
	}

	public void dispose() {
		MainGame.assetManager.unload(SPRITE_SHEET_FILENAME);
		MainGame.world.destroyBody(this.physicsBody);
	}

	@Override
	public void act(float deltaTime) {
		// Update position based on rigit body.
		Vector2 pos = physicsBody.getPosition();
		this.setX(pos.x*PPM);
		this.setY(pos.y*PPM);

		// Handle anim
		stateTime += deltaTime;
		Animation anim = animations[this.state.ordinal()][this.direction.ordinal()];
		if(anim != null) {
			stateTime %= anim.getAnimationDuration();
		}
	}

	@Override
	public void draw(Batch spriteBatch, float alpha) {
		// TODO: Won't be using getWidth() forever.  Use animation frame size around origin.
		Animation anim = animations[this.state.ordinal()][this.direction.ordinal()];
		if(anim != null) {
			spriteBatch.draw(anim.getKeyFrame(stateTime), this.getX() - this.getOriginX(), this.getY() - this.getOriginY());
		} else {
			spriteBatch.draw(spriteSheet, this.getX()-this.getOriginX(), this.getY()-this.getOriginY());
		}
	}

	public void teleportTo(float x, float y) {
		this.getBody().setTransform(x/PPM, y/PPM, 0);
	}

	public Body getBody() {
		return physicsBody;
	}

}
