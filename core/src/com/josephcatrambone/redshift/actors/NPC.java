package com.josephcatrambone.redshift.actors;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.josephcatrambone.redshift.Level;
import com.josephcatrambone.redshift.MainGame;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by josephcatrambone on 7/1/16.
 */
public class NPC extends Pawn {
	public static final float PATROL_SPEED = 3.0f;
	public static final float CHASE_SPEED = 7.0f;
	public static final float REACTION_TIME = 0.5f;
	public static final float ALERT_TIME = 5.0f;
	public static final String NPC_USER_DATA = "pawn";
	public float walkSpeed = 3.0f; // Our default.

	public static final String SPRITESHEET = "orderly.png";

	public static final String NPC_ALERT = "alert.wav";
	private Sound alert = null;

	// State stack.
	public Stack<NPCState> stateStack;

	// For looking.
	private float fov;
	// rotation = super.getRotation OR Direction.
	private float sightLimit;

	// For following waypoints.
	private Level levelReference;

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

		stateStack = new Stack<NPCState>();
		stateStack.push(new NPCState(this));
	}

	public void pushState(NPCState s) {
		stateStack.push(s);
	}

	public NPCState popState() {
		return stateStack.pop();
	}

	@Override
	public void act(float deltaTime) {
		super.act(deltaTime);

		stateStack.peek().act(deltaTime);

		updatePosition();
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

	public void setPatrolRoute(Vector2[] waypoints) {
		this.stateStack.push(new PatrolState(this, waypoints));
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

	public void setMapReference(Level level) {
		// TODO: We need a way to get these things to pathfind without passing the level.
		this.levelReference = level;
	}

	public void seesPlayerAt(float x, float y) {
		stateStack.peek().seesPlayerAt(x, y);
	}

	class NPCState {
		public NPC self; // Reference to 'this'.
		public NPCState(NPC npc) {
			self = npc;
		}
		public void act(float deltaTime) {}
		public void seesPlayerAt(float x, float y) {}
	}

	class PatrolState extends NPCState {
		Vector2[] waypoints;
		Vector2 currentWaypoint;
		Vector2 previousPosition;
		int currentWaypointIndex;

		public PatrolState(NPC npc, Vector2[] waypoints) {
			super(npc);
			this.waypoints = waypoints;
			currentWaypointIndex = 0;
			previousPosition = new Vector2(0f, 0f);
			currentWaypoint = waypoints[0];
		}

		@Override
		public void act(float deltaTime) {
			moveTowardsWaypoint();
			advanceToNextWaypoint();
		}

		void moveTowardsWaypoint() {
			if(currentWaypoint == null) { // If we have waypoints, move to our next one.
				self.state = State.IDLE;
				if(waypoints != null && waypoints.length > 0) {
					currentWaypoint = waypoints[currentWaypointIndex];
				}
			} else { // Select the next waypoint and decide to move towards it on the bigger axis.
				self.state = State.MOVING;
				double dx = 0;
				double dy = 0;
				try {
					dx = currentWaypoint.x - self.getX();
					dy = currentWaypoint.y - self.getY();
				} catch(ArrayIndexOutOfBoundsException aioob) {
					// TODO: Concurrent modification of the array list leads to an exception.  Fix the race condition.
					currentWaypoint = null;
					self.state = State.IDLE;
					return; // Give up the rest of this action.
				}
				// Check to see if we're stuck in the same place we were last round.
				Vector2 currentPosition = new Vector2(self.getX(), self.getY());
				if(currentPosition.epsilonEquals(previousPosition, 1e-6f)) {
					// DEBUG: NPC is stuck.
					System.out.println("Stuck!");
					currentWaypoint = new Vector2(currentWaypoint.x + (16.0f*MainGame.random.nextFloat()-8.0f), currentWaypoint.y + (16.0f*MainGame.random.nextFloat()-8.0f));
				}
				previousPosition = currentPosition;
				// Determine which way to go.
				if(Math.abs(dx) >= Math.abs(dy)) { // Move horizontally.
					if(dx > 0) {
						self.direction = Direction.RIGHT;
					} else if(dx < 0) {
						self.direction = Direction.LEFT;
					} else {
						// We should not see this.
					}
				} else { // Move vertically.
					if(dy > 0) {
						self.direction = Direction.UP;
					} else {
						self.direction = Direction.DOWN;
					}
				}
			}
		}

		public void advanceToNextWaypoint() {
			double dx = currentWaypoint.x - self.getX();
			double dy = currentWaypoint.y - self.getY();

			// Can we pop this waypoint?
			if(Math.abs(dx)+Math.abs(dy) < walkSpeed) {
				System.out.println("dx,dy: " + dx + "," + dy + ".  Next waypoint.");
				currentWaypointIndex++;

				// Advance to next waypoint or, if there are none, set to null.
				if(currentWaypointIndex >= waypoints.length) {
					currentWaypointIndex = 0;
				}
				currentWaypoint = waypoints[currentWaypointIndex];
			}
		}
	}
}