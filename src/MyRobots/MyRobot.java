package MyRobots;

import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode._AdvancedRadiansRobot;
import robocode.*;
import robocode.util.*;
import java.awt.*;
import java.awt.geom.Point2D;
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
	// Enemy Distributio
	// 0 means there is only 1 enemy or the enemies distribute around character equally.
	// There are 4 area. 1:front, (-45,45); 2:right, (45,135); 3:behind, (-135,-135); 4:left, (-45,-135)  
	int enemyDistribution = 0;
	
	double enemyDistributionThreshold = 500;
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
		// Update radar strategy: Use first 20 turns to scan 360-degree, make sure there's no enemy miss
		if(enemies.size()==1 && turnCount > 20)
			radarStrategy = LOCK;
		else
			radarStrategy = CIRCLESCAN;
		
		// Update enemy state
		enemyDistribution = updateEnemyState();
		if(enemyDistribution == 0){
			out.println("Sparse!");
		}
		else
			out.println("enemy concentrated in: " + enemyDistribution);
	}
	
	public int updateEnemyState(){
		// If there's only 1 enemy, return 0
		if(enemies.size() <= 1){
			return 0;
		}
		
		int AREA[] = new int[5];
		Arrays.fill(AREA, 0);
		for(TankBot t : enemies.values()){
			double bearing = t.bearing;
//			out.println("Name: " + t.name + "  bear: " + bearing);
			if(-45 <= bearing && bearing <= 45)
				AREA[1]++;
			if(45 <= bearing && bearing <= 135)
				AREA[2]++;
			if((135 <= bearing && bearing <= 180) || (-180 <= bearing && bearing <= -135))
				AREA[3]++;
			if(-135 <= bearing && bearing <= -45)
				AREA[4]++;
		}
		
//		out.println(AREA[1] + " - " + AREA[2] + " - " + AREA[3] + " - " + AREA[4]);
		
		int max = 0;
		int index = 0;
		for(int i = 1; i < AREA.length; i++){
			if(AREA[i] > max){
				max = AREA[i];
				index = i;
			}
				
		}
		
		if(max > enemies.size()/4 + 1)
			return index;
		else
			return 0;
		
		// If the vector is larger than threshold, the enemy is concentrate and return the mean center
		// else return null
//		if(Math.sqrt(X*X + Y*Y) > enemyDistributionThreshold){
//			enemyState = ENEMYCONCENTRATE;
//			Point2D.Double p = new Point2D.Double();
//			int c = enemies.size();
//			p.x = X/c + this.getX();
//			p.y = Y/c + this.getY();
//			return p;
//		}
//		else{
//			enemyState = ENEMYSPARSE;
//			return null;
//		}

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
