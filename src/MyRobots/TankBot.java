package MyRobots;

import robocode.*;

public class TankBot {
	
	String name;
	double bearing;
	double distance;
	double energy;
	double heading;
	double velocity;
	
	public TankBot() {
		name = "";
		bearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading = 0.0;
		velocity = 0.0;
	}
	
	public void update(ScannedRobotEvent e) {
		name = e.getName();
		bearing = e.getBearing();
		distance = e.getDistance();
		energy = e.getEnergy();
		heading = e.getHeading();
		velocity = e.getVelocity();
	}
	
	public void reset() {
		name = "";
		bearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading = 0.0;
		velocity = 0.0;
	}
	
	public boolean isEmpty() { 
		return name.equals(""); 
		
	}

}
