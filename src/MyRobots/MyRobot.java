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
	
	// Store the scanned enemy robots
	HashMap<String, TankBot> enemies = new HashMap<String, TankBot>();

	public void run() {
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		while (true) {
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
		}
		else{
			TankBot enemy = new TankBot();
			enemy.update(e);
			enemies.put(enemy.name, enemy);
		}
		
		// Every time obtain new information, we call updateStrategy to analyze the environment and decide choosing which strategy 
		updateStrategy();
		
	}
	
	public void updateStrategy(){
		for(String s : enemies.keySet()){
			out.println(s);
		}
	}

	void Radar() {
		setTurnRadarRight(360);
		out.println("in the radar!");
	}

	void Movement() {

	}

	void Target() {

	}
}
