package org.multidrone.server;

import javafx.beans.property.*;
import org.multidrone.coordinates.GeodeticCoordinate;
import org.multidrone.coordinates.NEDCoordinate;
import org.multidrone.enums.SingleDroneState;
import org.multidrone.sharedclasses.UserDroneData;

import java.net.InetAddress;

public class User {
	private String username;
	private InetAddress address;
	private int port;
	private int id;
	private int dataPort;
	private int videoPort;
	private int userMavPort;
	private short userSystemID;
	public SingleDroneState state = SingleDroneState.Grounded;
	public UserDroneData data;
	public GeodeticCoordinate target;
	public float closestDroneDistDist;
	public int closestDroneID;
	public int closestDroneSYSID;
	public GeodeticCoordinate forwardPoint;
	public NEDCoordinate projectedPos;
	public NEDCoordinate currentPos;
	public float targetYaw;
	public GeodeticCoordinate preLandPoint;

	//private final FloatProperty battery = new SimpleFloatProperty(0);
	private final LongProperty lastUpdateTime = new SimpleLongProperty(System.currentTimeMillis());
	private final LongProperty lastServerCheckTime = new SimpleLongProperty(System.currentTimeMillis());

	public User(String username, InetAddress address, int port) {
		super();
		this.username = username;
		this.address = address;
		this.port = port;
	}

	/*public float getBattery() {
		return battery.get();
	}

	public void setBattery(float b){
		battery.setValue(b);
	}

	public FloatProperty batteryProperty() {
		return battery;
	}*/

	public void setVideoPort(int port){
		videoPort = port;
	}
	public int getVideoPort(){
		return videoPort;
	}

	public long getLastServerCheckTime() {
		return lastServerCheckTime.get();
	}

	public LongProperty lastServerCheckTimeProperty() {
		return lastServerCheckTime;
	}

	public void setLastServerCheckTime(long time){
		lastServerCheckTime.set(time);
	}

	public long getLastUpdateTime() {
		return lastUpdateTime.get();
	}


	public LongProperty lastUpdateTimeProperty() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long time){
		lastUpdateTime.set(time);
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public InetAddress getAddress() {
		return address;
	}
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	public void setUserMavPort(int userMavPort) {
		this.userMavPort = userMavPort;
	}

	public int getUserMavPort() {
		return userMavPort;
	}

	public short getUserSystemID() {
		return userSystemID;
	}

	public void setUserSystemID(short userSystemID) {
		this.userSystemID = userSystemID;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", address=" + address + ", port=" + port + ", id=" + id +"]";
	}
	public int getID(){
		return  id;
	}
	public void setID(int _id){
		id = _id;
	}

	public int getDataPort() {
		return dataPort;
	}

	public void setDataPort(int dataPort) {
		this.dataPort = dataPort;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}


}
