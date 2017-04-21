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

	//Targeting: basic
	int[] fireNums = new int[13];
	final int fireNumsBar = 30;
	final int enemyDistanceBar = 500;
	//Trageting: for guessFactor
	ArrayList<WaveBullet> waves = new ArrayList<WaveBullet>();
	int[][] stats = new int[13][31];
	int direction = 1;
	
	//Movement
	//keep a distance against the locked enemy and apply squaring off circling movement
	int squaringOffDistance = 250;
	//a degree when our tank approach or evade the enemy while applying circling movement
	double squaringOffAngle = 30.0;
	int distanceToWall = 100;
	//timeOfAvoid used to handle the "avoid_wall" event, during this time period, it do not handle the same event
	int timeOfAvoid = 30;
	//"avoid_wall" event will be handled only when avoidWallFlag <= 0
	int avoidWallFlag = 0; 
	int moveDirection = 1;

	public void run() {

		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		addCustomEvent(new Condition("avoid_wall") {
			public boolean test() {
				return ((getX() <= distanceToWall || getX() >= getBattleFieldWidth() - distanceToWall ||
						getY() <= distanceToWall || getY() >= getBattleFieldHeight() - distanceToWall));
			}
		});
		
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
		enemyDistribution = updateEnemyDistribution();
		if(enemyDistribution == 0){
			out.println("Sparse!");
		}
		else
			out.println("enemy concentrated in: " + enemyDistribution);
	}

	public int updateEnemyDistribution(){
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

	public void onCustomEvent(CustomEvent e) {
		if (e.getCondition().getName().equals("avoid_wall"))
		{
			if(avoidWallFlag <= 0) {
				avoidWallFlag += timeOfAvoid;
				setMaxVelocity(0);
			} 
		}
	}
	
	public void onHitWall(HitWallEvent e) { out.println("Hit a wall anyway!"); }
	
	public void onHitRobot(HitRobotEvent e) { avoidWallFlag = 0; }
	
	void squaringOff() {
		TankBot enemy = new TankBot();
		enemy = enemies.entrySet().iterator().next().getValue();
		
		if(enemy.distance <= squaringOffDistance){
			setTurnRight(enemy.bearing + 90 + squaringOffAngle);
		} else {
			setTurnRight(enemy.bearing + 90 - squaringOffAngle);
		}
		
		if (avoidWallFlag > 0) avoidWallFlag--;
		
		if (getVelocity() == 0) {
			setMaxVelocity(10);
			moveDirection *= -1;
			setAhead(10000 * moveDirection);
		}
		
		
	}
	
	//If there is a flock, move away from them
	public int wallBearing() {
		switch(updateEnemyDistribution()) {
		case 1: return -180;
		case 2: return -90;
		case 3: return 0;
		case 4: return 90;
		default: return 0;
		}
	}
	
	void goForWalls() {
		int randomNum = ThreadLocalRandom.current().nextInt(-180,180);
		avoidWallFlag = 1; //Disable "avoid_wall" event
		setTurnRight(wallBearing());
		setAhead(10000 * moveDirection);
//		if (getVelocity() == 0) {
//			setTurnRight(randomNum);
//			setAhead(20);
//		}
	}
	
	void Movement() {
		switch(radarStrategy){
		case LOCK://if radar locks onto one enemy, apply squaring off and avoid corners
			squaringOff();
			break;
		case CIRCLESCAN://if there are multiple enemies, pick a corner and move to that corner
//			goForWalls();
			break;
		default:
			break;
		}
	}

	void Target() {
		double energy = getEnergy();
		int enenmyNum = getOthers();
		if (energy < 3) {
//			return;
		} else {
			if (enemies.size() >= 2 && enemyDistribution!=0) {
				turnToCluster(enemyDistribution);
			} else if (enemies.size() == 1) {
				TankBot e = enemies.entrySet().iterator().next().getValue();
				if (fireNums[(int)(e.distance / 100)] > fireNumsBar) {
					turnByGuess();
				} else {
					if (e.distance < enemyDistanceBar) {
						turnToEnemy();
					} else {
						if (e.isConstantMoving()) {
							turnByPrediction();
						} else {
							turnRandom();
						}
					}
				}
			}
		}
//		turnByGuess();
//		turnByPrediction();
//		turnToEnemy();
//		turnRandom();

//		fire();
	}

	private void turnToEnemy() {
		if(enemies.size()==1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			double gunTurn = getHeadingRadians() + e.bearingRadians - getGunHeadingRadians();
			setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurn));
			if (getGunHeat() == 0 && gunTurn < Math.atan2(9, e.distance) ) { 
				fire();
			}
		}
	}
	private void turnRandom() {
		if(enemies.size()==1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			double targetAngle = getHeadingRadians() + e.bearingRadians;
			double escapeAngle = Math.asin(8 / Rules.getBulletSpeed(11));
			double randomAimOffset = -escapeAngle + Math.random() * 2 * escapeAngle;
			double headOnTargeting = targetAngle - getGunHeadingRadians();
			double gunTurn = Utils.normalRelativeAngle(headOnTargeting + randomAimOffset);
			setTurnGunRightRadians(gunTurn);
			if (getGunHeat() == 0 && gunTurn < Math.atan2(9, e.distance) ) { 
				fire();
			}
			
		}
	}
	private void turnByPrediction() {
		if(enemies.size()==1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			double absoluteBearing = getHeadingRadians() + e.bearingRadians;
			double gunTurn = Utils.normalRelativeAngle(absoluteBearing - 
				    getGunHeadingRadians() + (e.velocity * Math.sin(e.headingRadians - 
						    absoluteBearing) / 13.0));
			setTurnGunRightRadians(gunTurn);
			if (getGunHeat() == 0 && gunTurn < Math.atan2(9, e.distance) ) { 
				fire();
			}
		}
	}
	private void turnByGuess() {
		if(enemies.size()==1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			int[] currentStats = stats[(int)(e.distance / 100)]; 
			int bestindex = 15;	
			for (int i=0; i<31; i++)
				if (currentStats[bestindex] < currentStats[i])
					bestindex = i;
			
			double absBearing = getHeadingRadians() + e.bearingRadians;
			double guessfactor = (double)(bestindex - (stats[0].length - 1) / 2) / ((stats[0].length - 1) / 2);
			double escapeAngle = Math.asin(8 / Rules.getBulletSpeed(11));
			double angleOffset = direction * guessfactor * escapeAngle;
	        double gunTurn = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset);
	        setTurnGunRightRadians(gunTurn);
			if (getGunHeat() == 0 && gunTurn < Math.atan2(9, e.distance) ) { 
				fire();
			}
		}
	}
	private void turnToCluster(int clusterPosition) {
		if(enemies.size()>1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			double gunTurn = (clusterPosition - 1) * Math.PI/2 - getGunHeadingRadians();
			setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurn));
//			if (getGunHeat() == 0 && gunTurn < Math.atan2(9, e.distance) ) { 
				fire();
//			}
		}
	}
	private void fire(){
		if(enemies.size()==1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			double dist = e.distance;
			double can1 = Math.max(0.1, 3*((enemyDistanceBar-dist)/enemyDistanceBar));
			double can2 = Math.min(3, 15*getEnergy()/100);
			double bulletPower = Math.min(can1, can2);
			setFire(bulletPower);
			updateWaves(bulletPower);
		} else {
			setFire(1.7);
		}
	}
	private void updateWaves(double power) {
		if(enemies.size()==1) {
			TankBot e = enemies.entrySet().iterator().next().getValue();
			double absBearing = getHeadingRadians() + e.bearingRadians;
			 
			// find our enemy's location:
			double ex = getX() + Math.sin(absBearing) * e.distance;
			double ey = getY() + Math.cos(absBearing) * e.distance;
	 
			// Let's process the waves now:
			for (int i=0; i < waves.size(); i++)
			{
				WaveBullet currentWave = (WaveBullet)waves.get(i);
				if (currentWave.checkHit(ex, ey, getTime()))
				{
					waves.remove(currentWave);
					i--;
				}
			}
	 
			// don't try to figure out the direction they're moving 
			// they're not moving, just use the direction we had before
			if (e.velocity != 0)
			{
				if (Math.sin(e.headingRadians-absBearing)*e.velocity < 0)
					direction = -1;
				else
					direction = 1;
			}
			int[] currentStats = stats[(int)(e.distance / 100)];
			fireNums[(int)(e.distance / 100)]++;
			WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power,
	                        direction, getTime(), currentStats);
			waves.add(newWave);
		}
	}
	
}
