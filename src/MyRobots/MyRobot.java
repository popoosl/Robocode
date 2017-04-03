package MyRobots;

import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode._AdvancedRadiansRobot;
import robocode.*;
import robocode.util.*;
import java.awt.*;
import java.util.*;


public class MyRobot extends AdvancedRobot {
	
	// Strategy
	public final static int SEARCH = 1;
	public final static int LOWLIFE = 2;
	// Radar Strategy
	public final static int LOCK = 11;
	public final static int CIRCLESCAN = 12;
	
	// Store the scanned enemy robots
	HashMap<String, TankBot> enemies = new HashMap<String, TankBot>();
	// Radar State
	int radarStrategy = CIRCLESCAN;
	int turnCount = 0;

	public void run() {
		
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		while (true) {
			turnCount++;
			Radar();
			Movement();
			Target();
			execute();

		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		out.println("in the scannedRobot");
		if(enemies.containsKey(e.getName())){
			enemies.get(e.getName()).update(e);
			double enemyBearing = this.getHeading() + e.getBearing();
			enemies.get(e.getName()).X = getX() + e.getDistance() * Math.sin(Math.toRadians(enemyBearing));
			enemies.get(e.getName()).Y = getY() + e.getDistance() * Math.cos(Math.toRadians(enemyBearing));
			out.println("X: " + enemies.get(e.getName()).X);
			out.println("Y: " + enemies.get(e.getName()).Y);
		}
		else{
			TankBot enemy = new TankBot();
			enemy.update(e);
			enemies.put(enemy.name, enemy);
			double enemyBearing = this.getHeading() + e.getBearing();
			enemies.get(e.getName()).X = getX() + e.getDistance() * Math.sin(Math.toRadians(enemyBearing));
			enemies.get(e.getName()).Y = getY() + e.getDistance() * Math.cos(Math.toRadians(enemyBearing));
		}
		
		// Every time obtain new information, we call updateStrategy to analyze the environment and decide choosing which strategy 
		updateStrategy();
	}
	
	public void updateStrategy(){
		// Use first 20 turns to scan 360-degree, make sure there's no enemy miss
		if(enemies.size()==1 && turnCount > 20)
			radarStrategy = LOCK;
		else
			radarStrategy = CIRCLESCAN;
	}

	void Radar() {
		switch(radarStrategy){
		case LOCK:
			radarLocker();
			break;
		case CIRCLESCAN:
			setTurnRadarRight(360);
			break;
		default:
			break;
		}
	}
	
	void radarLocker(){
		TankBot enemy = new TankBot();
		// There are only one element in the Hashmap
		for(String s : enemies.keySet()){
			enemy = enemies.get(s);
		}
		
		double radarTurn = getHeadingRadians() + enemy.bearingRadians - getRadarHeadingRadians();
		setTurnRadarRightRadians(1.9*Utils.normalRelativeAngle(radarTurn));
		
		
	}
	
	void OnHitByBullet(HitByBulletEvent event)
	{
		// If is hit by other tank, apply CIRCLESCAN Strategy but not LOCK
		if(enemies.size() == 1){
			if(!enemies.containsKey(event.getName()))
				radarStrategy = CIRCLESCAN;
		}
	}

	void Movement() {

	}

	void Target() {

	}
}
