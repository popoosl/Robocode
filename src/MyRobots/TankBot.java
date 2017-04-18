package MyRobots;

import java.util.*;

import robocode.*;

public class TankBot {
	
	String name;
	double bearing;
	double distance;
	double energy;
	double heading;
	double velocity;
	double bearingRadians;
	double headingRadians;
	double X;
	double Y;
	//for targeting
	Queue<Double> velocityHistory = new LinkedList<Double>();
	Queue<Double> headingHistory = new LinkedList<Double>();
	final int historyNum = 3;
	
	public TankBot() {
		name = "";
		bearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading = 0.0;
		velocity = 0.0;
		bearingRadians = 0.0;
		X = 0.0;
		Y = 0.0;
		headingRadians = 0.0;
		resetMovingHistory();
	}
	
	public void update(ScannedRobotEvent e) {
		name = e.getName();
		bearing = e.getBearing();
		distance = e.getDistance();
		energy = e.getEnergy();
		heading = e.getHeading();
		velocity = e.getVelocity();
		bearingRadians = e.getBearingRadians();
		headingRadians = e.getHeadingRadians();
		updateMovingHistory();
	}
	private void updateMovingHistory(){
		velocityHistory.add(velocity);
		headingHistory.add(heading);
		if(velocityHistory.size() > historyNum) {
			velocityHistory.poll();
			headingHistory.poll();
		}
	}
	public boolean isConstantMoving(){
		Double v = velocityHistory.peek();
		for (Double vv : velocityHistory) {
			if (v != vv);
				return false;
		}
		Double h = velocityHistory.peek();
		for (Double hh : headingHistory) {
			if (h != hh);
				return false;
		}
		return true;
	}
	private void resetMovingHistory(){
		velocityHistory.clear();
		headingHistory.clear();
	}
	public void reset() {
		name = "";
		bearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading = 0.0;
		velocity = 0.0;
		bearingRadians = 0.0;
		X = 0.0;
		Y = 0.0;
		headingRadians = 0.0;
		resetMovingHistory();
	}
	
	public boolean isEmpty() { 
		return name.equals(""); 
		
	}

}
