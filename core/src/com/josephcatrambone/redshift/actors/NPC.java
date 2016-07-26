package com.josephcatrambone.redshift.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.josephcatrambone.redshift.Level;
import com.josephcatrambone.redshift.MainGame;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by josephcatrambone on 7/1/16.
 */
public class NPC extends Pawn {
	public static final float GRAB_DISTANCE = 24f;
	public static final float PATROL_SPEED = 3.0f;
	public static final float CHASE_SPEED = 7.0f;
	public static final float REACTION_TIME = 0.5f;
	public static final float ALERT_TIME = 5.0f;
	public static final float FOV = 35.0f;
	public static final float SIGHT_LIMIT = 6*16f;
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
		this.fov = FOV;
		this.sightLimit = SIGHT_LIMIT;
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
		// dot(A,B) = ||A|| * ||B|| * cos(theta)
		// theta = dot(A,B) / (||A|| * ||B||)
		Vector2 other = new Vector2(x-this.getX(), y-this.getY());
		float magnitude = other.len();
		if(magnitude > sightLimit) { return false; }
		other = other.scl(1.0f/magnitude);

		float halfpi = (float)Math.PI*0.5f;
		Vector2 thisSightVector = new Vector2((float)Math.cos(this.direction.ordinal()*halfpi), (float)Math.sin(this.direction.ordinal()*halfpi));

		double angle = Math.toDegrees(Math.acos(thisSightVector.dot(other)));
		System.out.println("Angle to player: " + angle);
		return angle < fov;
	}

	@Override
	public void draw(Batch spriteBatch, float alpha) {
		super.draw(spriteBatch, alpha);
	}

	public void drawFOV(ShapeRenderer batch) {
		batch.arc(this.getX(), this.getY(), this.sightLimit, (float)(this.direction.ordinal()*90.0f)-(this.fov), 2.0f*this.fov);
	}

	public void setMapReference(Level level) {
		// TODO: We need a way to get these things to pathfind without passing the level.
		this.levelReference = level;
	}

	public void seesPlayerAt(float x, float y) {
		stateStack.peek().seesPlayerAt(x, y);
	}

	public boolean isAlerted() {
		// TODO: This isn't a great way to handle this.
		return this.stateStack.peek() instanceof ChaseState;
	}

	class NPCState {
		public NPC self; // Reference to 'this'.

		public NPCState(NPC npc) {
			self = npc;
		}
		public void act(float deltaTime) {
			self.state = State.IDLE; // TODO: This could be bad if the NPC is dead.
		}

		public void seesPlayerAt(float x, float y) {
			// First push the return, then the pursuit.
			self.pushState(new GotoState(self, new Vector2(self.getX(), self.getY()))); // Return
			self.pushState(new ReturnFromAlert(self));
			self.pushState(new ChaseState(self, new Vector2(x, y))); // Go towards player.
		}
	}

	class ReturnFromAlert extends NPCState {
		public ReturnFromAlert(NPC npc) {
			super(npc);
		}

		public void act(float deltaTime) {
			self.walkSpeed = NPC.PATROL_SPEED; // Return to normal.
			// TODO: Play SFX here.
			self.popState(); // Pop this.
		}
	}

	/*** ChaseState
	 * Like GotoState, but won't add another chase state onto the stack if it sees player.
	 */
	class ChaseState extends GotoState {
		public ChaseState(NPC npc, Vector2 target) {
			super(npc, target);
		}

		@Override
		public void act(float deltaTime) {
			self.walkSpeed = CHASE_SPEED;
			super.act(deltaTime);
		}

		@Override
		public void seesPlayerAt(float x, float y) {
			// First push the return, then the pursuit.
			this.waypoints = self.levelReference.getPath(self.getX(), self.getY(), x, y);
			// Since map aliasing may cause the first waypoint to be behind us, try and start from the second.
			if(waypoints != null && waypoints.length > 1) {
				this.currentWaypointIndex = 1;
				this.currentWaypoint = waypoints[1];
			}
		}
	}

	/*** GotoState
	 * Calculates path to the target on first run, then moves towards it.
	 */
	class GotoState extends NPCState {
		Vector2 target; // Our ultimate goal.
		Vector2[] waypoints; // The path we are following now.
		Vector2 currentWaypoint; // The next place we'll try to get to.
		Vector2 previousPosition; // Where we were last frame to see if we're stuck.
		int currentWaypointIndex; // Index of where we are in the waypoint list.

		public GotoState(NPC npc, Vector2 target) {
			super(npc);
			this.target = target;
		}

		@Override
		public void act(float deltaTime) {
			// If we have not yet calculated a path, or if the last one was bad, make a new one.
			if(this.waypoints == null) {
				this.waypoints = self.levelReference.getPath(self.getX(), self.getY(), target.x, target.y);
				currentWaypointIndex = 0;
				if(this.waypoints == null) {
					// TODO: Log error.  No path.
					return;
				}
				currentWaypoint = null;
			}

			// Move towards the next waypoint.
			moveTowardsWaypoint();
			advanceToNextWaypoint();
		}

		void moveTowardsWaypoint() {
			if(currentWaypoint == null) { // If we have waypoints, move to our next one.
				self.state = State.IDLE;
				if(waypoints != null && waypoints.length > 0) {
					currentWaypoint = waypoints[currentWaypointIndex];
				} else {
					arrived();
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
			if(Math.abs(dx) < walkSpeed && Math.abs(dy) < walkSpeed) {
				currentWaypointIndex++;

				// Advance to next waypoint or, if there are none, set to null.
				if(currentWaypointIndex >= waypoints.length) {
					arrived();
					return;
				}
				currentWaypoint = waypoints[currentWaypointIndex];
			}
		}

		public void arrived() {
			self.popState();
		}
	}

	/*** PatrolState
	 * Note: Patrol state does NOT actually do anything. It just pushes the next path onto the stack when 'act' is called.
	 */
	class PatrolState extends NPCState {
		int currentRoutePoint;
		Vector2[] route;
		public PatrolState(NPC npc, Vector2[] route) {
			super(npc);
			this.currentRoutePoint = 0;
			this.route = route;
		}

		@Override
		public void act(float deltaTime) {
			self.walkSpeed = PATROL_SPEED;
			currentRoutePoint = (currentRoutePoint + 1)%route.length;
			self.pushState(new GotoState(self, route[currentRoutePoint]));
		}
	}
}