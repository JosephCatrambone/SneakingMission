package com.josephcatrambone.redshift.actors;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.josephcatrambone.redshift.MainGame;
import com.josephcatrambone.redshift.scenes.PlayScene;

import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

/**
 * Created by josephcatrambone on 7/1/16.
 */
public class NPC extends Pawn {
	public static final float PATROL_SPEED = 3.0f;
	public static final float INVESTIGATE_SPEED = 4.0f;
	public static final float CHASE_SPEED = 7.0f;
	public static final float REACTION_TIME = 0.5f;
	public static final float INVESTIGATE_TIME = 10.0f;
	public static final float ALERT_TIME = 5.0f;
	public static final String NPC_USER_DATA = "pawn";
	public float walkSpeed = 3.0f; // Our default.

	public static final String SPRITESHEET = "orderly.png";

	public static final String NPC_ALERT = "alert.wav";
	private Sound alert = null;

	// For looking.
	private float fov;
	// rotation = super.getRotation OR Direction.
	private float sightLimit;

	// For following waypoints.
	private boolean moveHorizontalFirst = false; // If we're moving towards an obstacle, do we move horizontal first?
	private Vector2 previousPosition; // If we are in the same position as last frame and we have waypoints, we're stuck.
	private Vector2[] waypoints;
	private Vector2[] previousWaypoints; // If we go into investigation mode, push our previous waypoints here.
	private int currentWaypoint;
	private boolean stopAtEnd;

	// For handling alert and pursuit.
	private boolean sawPlayerFrameBeforeLast = false;
	private boolean sawPlayerLastFrame = false;
	private Vector2 lastPlayerLocation = null;
	private float spotTimer = 0;
	private float investigateTimer = 0;
	private float alertTimer = 0;

	public NPC(int x, int y) {
		this.fov = (float)Math.toRadians(35);
		this.sightLimit = 200;
		create(x, y, 4, 4, 1.0f, SPRITESHEET); // TINY hitbox.

		createDefaultAnimations();

		// Always use first fixture for labelling contact data.
		HashMap<String, String> fixtureData = new HashMap<String, String>();
		fixtureData.put("type", NPC_USER_DATA);
		this.getBody().getFixtureList().get(0).setUserData(fixtureData);
		//this.getBody().setUserData(PLAYER_USER_DATA);
	}

	@Override
	public void act(float deltaTime) {
		super.act(deltaTime);

		moveTowardsWaypoint();

		updatePosition();

		// Logic for transitioning between states and seeking player.
		if(sawPlayerFrameBeforeLast) {
			if(alertTimer > 0) { // Are we in alert mode?  If so and we saw the player, lock this to the alert time.
				alertTimer = ALERT_TIME;

			} else if(investigateTimer > 0) {
				spotTimer += deltaTime;
				if(spotTimer > ALERT_TIME) {
					alertTimer = ALERT_TIME;
				}
			} else {
				spotTimer += deltaTime; //
				if(spotTimer >= REACTION_TIME) { // We are in alert mode and have spotted the player.
					alertTimer = ALERT_TIME;
				} else { // We spotted the player but didn't react.  Investigate.
					investigateTimer = INVESTIGATE_TIME; // Start investigating.
				}
			}
		} else { // Player moved out of our cone of vision.
			// If we are alerted, reduce our alert time.
			if(alertTimer > 0) { alertTimer -= deltaTime; }
			if(investigateTimer > 0) { investigateTimer -= deltaTime; }
			spotTimer = 0;
			if(investigateTimer <= 0 && alertTimer <= 0) {
				alertTimer = 0;
				investigateTimer = 0;
				// Return to our previous state.
				// TODO: Return to state.
			}
		}

		// Now that we've figured out our timers, the 'see player' method will determine our action.

		// Use for state tracking.
		sawPlayerFrameBeforeLast = sawPlayerLastFrame;
		sawPlayerLastFrame = false; // seesPlayerAt is called _after_ this already resolves, so it gets change to the correct value for this frame.
	}

	private void moveTowardsWaypoint() {
		if(waypoints == null || waypoints.length == 0) { // If we have waypoints, move to our next one.
			this.state = State.IDLE;
		} else { // Select the next waypoint and decide to move towards it on the bigger axis.
			this.state = State.MOVING;
			double dx = 0;
			double dy = 0;
			try {
				dx = waypoints[currentWaypoint].x - this.getX();
				dy = waypoints[currentWaypoint].y - this.getY();
			} catch(ArrayIndexOutOfBoundsException aioob) {
				// TODO: Concurrent modification of the array list leads to an exception.  Fix the race condition.
				currentWaypoint = 0;
				this.state = State.IDLE;
				return; // Give up the rest of this action.
			}
			// Check to see if we're stuck in the same place we were last round.
			Vector2 currentPosition = new Vector2(this.getX(), this.getY());
			if(currentPosition.epsilonEquals(previousPosition, 1e-6f)) {
				// DEBUG: NPC is stuck.
				moveHorizontalFirst = !moveHorizontalFirst; // Try to unstick ourselves.
			}
			previousPosition = currentPosition;
			// Determine which way to go.
			if(moveHorizontalFirst || Math.abs(dx) >= Math.abs(dy)) { // Move horizontally.
				if(dx > 0) {
					this.direction = Direction.RIGHT;
				} else if(dx < 0) {
					this.direction = Direction.LEFT;
				} else {
					// We should not see this.
				}
			} else { // Move vertically.
				if(dy > 0) {
					this.direction = Direction.UP;
				} else {
					this.direction = Direction.DOWN;
				}
			}

			// Can we pop this waypoint?
			if(Math.abs(dx)+Math.abs(dy) < walkSpeed) {
				currentWaypoint++;
				if(currentWaypoint >= waypoints.length) {
					if(stopAtEnd) {
						waypoints = null;
					}
					currentWaypoint = 0;
				}
			}
		}
	}

	private void updatePosition() {
		if(this.state == State.MOVING) {
			float dx = 0;
			float dy = 0;
			if (this.direction == Direction.RIGHT) { dx = this.walkSpeed; dy = 0; }
			if (this.direction == Direction.UP) { dx = 0; dy = this.walkSpeed; }
			if (this.direction == Direction.LEFT) { dx = -this.walkSpeed; dy = 0;	}
			if (this.direction == Direction.DOWN) { dx = 0; dy = -this.walkSpeed; }
			this.getBody().setLinearVelocity(dx, dy);
		} else {
			this.getBody().setLinearVelocity(0, 0);
		}
	}

	public void kill() {
		this.state = State.DEAD;
	}

	public boolean inConeOfVision(float x, float y) {
		// Triangle ABC for cone of vision.
		// dx is not delta x, but the value of D remapped to the origin.
		double dx = x - this.getX();
		double dy = y - this.getY();

		double rot = Math.toRadians(this.direction.ordinal()*90);

		double bx = Math.cos(rot+fov)*this.sightLimit;
		double by = Math.sin(rot+fov)*this.sightLimit;
		double cx = Math.cos(rot-fov)*this.sightLimit;
		double cy = Math.sin(rot-fov)*this.sightLimit;

		// bx cx | t1 = dx
		// by cy | t2   dy

		double determinant = bx*cy-cx*by;
		if(determinant == 0) {
			return false;
		}

		// bx dx
		// by dy | t2

		double t1 = (dx*cy - cx*dy) / determinant;
		double t2 = (bx*dy - dx*by) / determinant;

		return (t1 >= 0 && t1 <= 1 && t2 >= 0 && t2 <= 1 && t2+t2 <= 1);
	}

	@Override
	public void draw(Batch spriteBatch, float alpha) {
		super.draw(spriteBatch, alpha);
	}

	public void setWaypoints(Vector2[] wp, boolean stopAtEnd) {
		this.currentWaypoint = 0;
		this.waypoints = wp;
		this.stopAtEnd = stopAtEnd;
	}

	public void seesPlayerAt(float x, float y) {
		sawPlayerLastFrame = true;
		if(lastPlayerLocation == null || x != lastPlayerLocation.x || y != lastPlayerLocation.y) {
			lastPlayerLocation = new Vector2(x, y); // Recalculate path, too.
		}
	}
}