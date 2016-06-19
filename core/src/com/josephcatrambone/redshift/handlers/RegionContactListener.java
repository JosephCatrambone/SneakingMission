package com.josephcatrambone.metalskyarena.handlers;

import com.badlogic.gdx.physics.box2d.*;
import com.josephcatrambone.metalskyarena.Level;
import com.josephcatrambone.metalskyarena.actors.Player;

import java.util.Map;

/**
 * Created by Jo on 1/12/2016.
 */
public class RegionContactListener implements ContactListener {

	// If these are set by a collision with a trigger, we'll do something.
	public boolean playerCooling = false;

	public boolean playerTeleport = false;
	public int teleportX, teleportY;
	public String teleportMap;

	public boolean reachedGoal = false;

	@Override
	public void beginContact(Contact contact) {
		Fixture fa = contact.getFixtureA();
		Fixture fb = contact.getFixtureB();

		Map<String, String> ad = (Map<String, String>)fa.getUserData();
		Map<String, String> bd = (Map<String, String>)fb.getUserData();

		if(ad != null && bd != null && ad.containsKey("type") && bd.containsKey("type")) {
			Map<String, String> playerData = null;
			Map<String, String> objData = null;

			if(ad.get("type").equals(Player.PLAYER_USER_DATA)) {
				playerData = ad;
				objData = bd;
			} else if(bd.get("type").equals(Player.PLAYER_USER_DATA)) {
				playerData = bd;
				objData = ad;
			} // TODO: We assume player never self-collides.

			if(playerData != null) {
				if(objData.get("type").equals(Level.COOL_TYPE)) {
					playerCooling = true;
				} else if(objData.get("type").equals(Level.TELEPORT_TYPE)) {
					teleportX = Integer.parseInt(objData.get("teleportx"));
					teleportY = Integer.parseInt(objData.get("teleporty"));
					if(objData.containsKey("teleportmap")) {
						teleportMap = objData.get("teleportmap");
					}
					playerTeleport = true;
				} else if(objData.get("type").equals(Level.GOAL_TYPE)) {
					reachedGoal = true;
				}
			}
		}
	}

	@Override
	public void endContact(Contact contact) {
		Fixture fa = contact.getFixtureA();
		Fixture fb = contact.getFixtureB();

		Map<String, String> ad = (Map<String, String>)fa.getUserData();
		Map<String, String> bd = (Map<String, String>)fb.getUserData();

		if(ad != null && bd != null && ad.containsKey("type") && bd.containsKey("type")) {
			Map<String, String> playerData = null;
			Map<String, String> objData = null;

			if(ad.get("type").equals(Player.PLAYER_USER_DATA)) {
				playerData = ad;
				objData = bd;
			} else if(bd.get("type").equals(Player.PLAYER_USER_DATA)) {
				playerData = bd;
				objData = ad;
			} // TODO: We assume player never self-collides.

			if(playerData != null) {
				if(objData.get("type").equals(Level.COOL_TYPE)) {
					playerCooling = false;
				}
			}
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {

	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {

	}
}
