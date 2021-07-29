package org.multidrone.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_manual_control;
import com.MAVLink.common.msg_set_position_target_global_int;
import com.MAVLink.enums.MAV_CMD;
import javafx.application.Platform;

import com.MAVLink.common.msg_command_long;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.multidrone.Main;
import org.multidrone.controller.MultiDronePositionController;
import org.multidrone.coordinates.*;
import org.multidrone.sharedclasses.UserDroneData;
import org.multidrone.video.StreamReceiver;

public class ServerController {
	
	private static ServerController instance = null;
	private NotificationListener notificationListener = new NotificationListener();
	private List<DataListener> dataListeners = new ArrayList<>();
	private Main main;
	private int currentID = 0;
	private ExecutorService serverExecutor;
	private MultiDronePositionController swarmController = new MultiDronePositionController();
	private GlobalRefrencePoint refPoint;
	
	private ObservableList<User> users = FXCollections.observableArrayList(param -> new Observable[] { param.lastUpdateTimeProperty(), param.lastServerCheckTimeProperty() });
	final String idHeader =  "i";
	final String messageHeader =  "m";

	private float targetYaw;

	private StreamReceiver videoRecv = new StreamReceiver();

	public GlobalRefrencePoint getRefPoint() {
		return refPoint;
	}

	//Sets refrence server uses for distance calculations and local xy calculations
	public void setRefPoint(GlobalRefrencePoint ref){
		refPoint = ref;
		swarmController.setGlobalRef(ref);
		System.out.println("set ref point");
	}

	private ServerController() {
		this.initializeNotificationListener();
		//Uses server executor to send mav messages with threads
		serverExecutor = Executors.newSingleThreadExecutor();
		swarmController.setUserList(users);
		initializeSwarmController();

		Thread videoThread = new Thread(videoRecv);
		videoThread.start();
	}
	
	public static ServerController getInstance() {
		if (instance == null) {
			instance = new ServerController();
		}
		return instance;
	}

	private void initializeSwarmController(){
		Thread swarmControllerThread = new Thread(swarmController);
		setSwarmControllerActive(true);
		swarmControllerThread.start();

	}
	
	//This is listening for new drones to join
	private void initializeNotificationListener() {
		Thread notificationsThread = new Thread(this.notificationListener);
		notificationsThread.start();
	}

	//Creates a data listener for a given user id, this is where the drone sends all mav link messages
	private DataListener initializeDataListener(int id) {
		DataListener newDataListener = new DataListener();
		newDataListener.setMyID(id);
		try {
			DatagramSocket socket = new DatagramSocket();
			int port = socket.getLocalPort();
			newDataListener.setMyPort(port);
			socket.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		System.out.println(newDataListener.getMyPort());
		Thread newDataThread = new Thread(newDataListener);
		newDataThread.start();
		return newDataListener;
	}
	
	
	public void fillMain(Main _main){
		main = _main;
		main.setConnectedNamesList(users);
	}

	public void setSwarmControllerActive(boolean active){
		swarmController.setControlLoopActive(active);
	}

	//Sends messages for initial handshakes
	public void transmitMessage(User user, String msg) throws Exception {
		DatagramSocket socket = new DatagramSocket(); 
		byte[] buffer = msg.getBytes();	
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, user.getAddress(), user.getPort()); 																												// paketa
		socket.send(packet);	
		socket.close();
	}

	public ObservableList<User> getUsers() {
		return users;
	}

	//This is called by the notification listener when a phone sends a connection handshake
	public void addUser(User user) {
		if (!users.contains(user)){//if a new user set id and open new data listener
			user.setID(currentID);
			currentID++;
			user.data = new UserDroneData();
			DataListener newListener = initializeDataListener(user.getID());
			user.setDataPort(newListener.getMyPort());
			this.users.add(user);

			dataListeners.add(newListener);
			try{
				Thread.sleep(1000L);
			} catch (Exception e){
				System.out.println("timeout slep interrupted");
			}

			sendID(user,user.getID(),newListener.getMyPort());
		} else{ //if the user already exists
			for (User u: users) {
				if (u.equals(user)){
					sendID(u,u.getID(),u.getDataPort());//re send the user their ID and data listener
					System.out.println("found user " + u.getID());
				}
			}
		}
		System.out.println(this.getUsers());
	}

	public void sendData(User u, String data){
		sendMessage(u,messageHeader+data);
	}

	public void sendID(User u, int id, int port){
		sendMessage(u,idHeader+Integer.toString(id)+";"+port);
	}

	public User getUserByID(int id){
		for (User u: users) {
			if (u.getID() == id){
				return u;
			}
		}
		return null;
	}

	private void transmitMavPacket(MAVLinkPacket pack,User user) throws Exception{
		if (user.getAddress() != null){
			DatagramSocket socket = new DatagramSocket();
			byte[] buffer = pack.encodePacket();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, user.getAddress(), user.getUserMavPort()); 																												// paketa
			socket.send(packet);
			System.out.println("sending to user "+ user.getID() + " with sys: " + pack.sysid + " at "+ user.getAddress() +": "+ user.getUserMavPort());
			socket.close();
		}
	}

	public void sendMavCommand(User user, int commandID, float param1,float param7){
		serverExecutor.submit(() -> {
			try {
				msg_command_long cmd = new msg_command_long();
				cmd.command = commandID;
				cmd.param1 = param1;
				cmd.param2 = 0;
				cmd.param3 = 0;
				cmd.param4 = 0;
				cmd.param5 = 0;
				cmd.param6 = 0;
				cmd.param7 = param7;
				cmd.target_component = 0;
				cmd.confirmation = 0;
				cmd.sysid = 0;
				cmd.target_system = user.getUserSystemID();
				transmitMavPacket(cmd.pack(),user);

			}
			catch (Exception e){
				e.printStackTrace();
			}

		});
	}

	public void sendMavCommand(User user, int commandID, float param1,float param5, float param6,float param7){
		serverExecutor.submit(() -> {
			try {
				msg_command_long cmd = new msg_command_long();
				cmd.command = commandID;
				cmd.param1 = param1;
				cmd.param2 = 0;
				cmd.param3 = 0;
				cmd.param4 = 0;
				cmd.param5 = param5;
				cmd.param6 = param6;
				cmd.param7 = param7;
				cmd.target_component = 0;
				cmd.confirmation = 0;
				cmd.sysid = 0;
				cmd.target_system = user.getUserSystemID();
				transmitMavPacket(cmd.pack(),user);

			}
			catch (Exception e){
				e.printStackTrace();
			}

		});
	}


	public void sendJoystickCommand(User user, short x, short y, short z, short r){
		serverExecutor.submit(() -> {
			try {
				msg_manual_control cmd = new msg_manual_control();
				cmd.r = r;
				cmd.x = x;
				cmd.y = y;
				cmd.z = z;
				cmd.sysid = 0;
				cmd.target = user.getUserSystemID();
				transmitMavPacket(cmd.pack(),user);

			}
			catch (Exception e){
				e.printStackTrace();
			}

		});
	}

	public void sendUserTarget(User u){
		sendSetPosTargetGlobalCommand(u,u.target.lat,u.target.lng,u.targetYaw,u.target.height);
	}

	public void sendSetPosTargetGlobalCommand(User user, double lat, double lng, double yaw, double alt){
		serverExecutor.submit(() -> {
			try {
				msg_set_position_target_global_int cmd = new msg_set_position_target_global_int();
				cmd.lat_int = (int)(lat * Math.pow(10,7));
				cmd.lon_int = (int)(lng * Math.pow(10,7));
				cmd.alt = (float) alt;
				cmd.yaw = (float) yaw;
				cmd.sysid = 0;
				cmd.target_system = user.getUserSystemID();
				transmitMavPacket(cmd.pack(),user);
				System.out.println(user.getUserSystemID());

			}
			catch (Exception e){
				e.printStackTrace();
			}

		});
	}


	public void sendMessage(User u, String msg){
		System.out.println("SEND: " + msg);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					transmitMessage(u, msg);
				}
				catch (Exception e){
					e.printStackTrace();
				}

			}
		});
	}
	public void removeUser(User user) {
		this.users.remove(user);
	}

	//It can receive user drone data for testing, currently unused outside of testing
	public void handleData(UserDroneData data){
		System.out.println(data.batteryPercent + " " +  data.height + " " + data.id);
		for (User u:users) {
			if (u.getID() == data.id){
				u.data.height = data.height;
				u.data.batteryPercent = data.batteryPercent;

				Platform.runLater(() -> u.setLastUpdateTime(System.currentTimeMillis()));

				main.updateUser(u);
			}
		}
	}

	//Sets target marker on main, used by the swarm controller
	public void setTargetMarker(GeodeticCoordinate gc, int id){
		main.moveTarget((float)gc.lat,(float)gc.lng,id);
	}

	public void updateBattery(byte batteryPercent, int userID){
		for (User u:users) {
			if (u.getID() == userID){
				u.data.batteryPercent = batteryPercent;
				Platform.runLater(() -> u.setLastUpdateTime(System.currentTimeMillis()));
				main.updateUser(u);
			}
		}
	}

	//Enable and disable circling, if circling is enabled then set the reference point as the region of interest
	public void setCirclingEnabled(boolean enabled){
		if (refPoint != null){
			if (enabled) {
				swarmController.startCircling(users.size());
				for(User u : users){
					sendMavCommand(u,MAV_CMD.MAV_CMD_DO_SET_ROI_LOCATION,0, (float) (refPoint.lat*Math.pow(10,7)),(float) (refPoint.lng*Math.pow(10,7)),(float) (refPoint.h));
				}
			} else {
				swarmController.stopCircling();
				for(User u : users){
					sendMavCommand(u,MAV_CMD.MAV_CMD_DO_SET_ROI_NONE,0, 0);
				}
			}
		} else{
			System.out.println("Please set a reference point");
		}
	}

	public void setInitialCircleTargets(){
		swarmController.setInitialTargets(users.size());
	}

	//For receiving command ports and sysids from drones
	public void updateMavDetails(int userID, int mavPort, short systemID){
		for (User u:users) {
			if (u.getID() == userID){
				u.setUserMavPort(mavPort);
				u.setUserSystemID(systemID);
			}
		}
	}

	public void stopAllDrones(){
		for (User u:users) {
			sendMavCommand(u, MAV_CMD.MAV_CMD_DO_PAUSE_CONTINUE,0,0);
		}
	}

	public void testProjectedPos(User u){
		System.out.println("xyz: \n" + u.data.xNED + " " + u.data.yNED + " " + u.data.zNED + " \nvxyz: \n" + u.data.vx + " " + u.data.vy + " " + u.data.vz +
				" \nprojected: \n" + u.projectedPos.x + " " + u.projectedPos.y + " " + u.projectedPos.z);
	}

	//Processes the global_pos_int command. It also calculates xyz relative to refrence point and a point in front of the drone to plot on the map to show yaw direction
	public void receivePosInt(msg_global_position_int msg, int userID) {
		for (User u:users) {
			if (u.getID() == userID){
				u.data.height = (float) (msg.relative_alt / Math.pow(10,3));
				u.data.lng = (float) (msg.lon / Math.pow(10,7));
				u.data.lat = (float) (msg.lat / Math.pow(10,7));

				u.data.vx = msg.vx;
				u.data.vy = msg.vy;
				u.data.vz = msg.vz;

				if (refPoint != null){
					NEDCoordinate coord = CoordinateTranslator.GeodeticToNED(new GeodeticCoordinate(u.data.lat,u.data.lng,u.data.height),refPoint);
					u.data.xNED = (float)coord.x;
					u.data.yNED = (float)coord.y;
					u.data.zNED = (float)coord.z;
					u.currentPos = coord;
					BodyCoordinate bodyCoord = CoordinateTranslator.NEDToBody(coord,u.data.yaw);
					u.data.xBOD = (float) bodyCoord.x;
					u.data.yBOD = (float) bodyCoord.y;
					u.data.zBOD = (float) bodyCoord.z;
					//calculates forward point by adding a body frame vector to an NED vector
					u.forwardPoint = CoordinateTranslator.nedToGeodetic(CoordinateTranslator.addBodyFrameVectorToBase(coord,10,0,0,u.data.yaw),refPoint);
				}

				Platform.runLater(() -> u.setLastUpdateTime(System.currentTimeMillis()));

				main.moveMarker(u);
				main.updateUser(u);

			}
		}
	}

	//processes named float mav command 'R'
	public void updateRCBattery(int battery, int userID){
		for (User u:users) {
			if (u.getID() == userID){
				u.data.rcBatteryPercentage = battery;
				main.updateUser(u);
			}
		}
	}

	public void setImage(int id,Image image){
		main.setImage(id,image);
	}

	//processes msg_attitude
	public void updateAttitude(msg_attitude msg, int userID) {
		for (User u:users) {
			if (u.getID() == userID){
				u.data.yaw = msg.yaw;
				Platform.runLater(() -> u.setLastUpdateTime(System.currentTimeMillis()));

				main.updateUser(u);
			}
		}
	}

	public void updateAlt(float alt, int userID) {
		for (User u:users) {
			if (u.getID() == userID){
				u.data.alt = alt;
				main.updateUser(u);
			}
		}
	}

	public void setTargetYaw(float yaw) {
		swarmController.setTargetYaw(yaw);
		targetYaw = yaw;
	}

	public void unPauseAllDrones() {
		for (User u:users) {
			sendMavCommand(u, MAV_CMD.MAV_CMD_DO_PAUSE_CONTINUE,1,0);
		}
	}

	public void updateGPSStrength(short fix_type, int userID) {
		for (User u:users) {
			if (u.getID() == userID){
				u.data.gpsSig = fix_type;
				main.updateUser(u);
			}
		}
	}

	public void setRotationPeriod(float period) {
		swarmController.setRotationTime(period);
	}

	public void setRadius(float radius) {
		swarmController.setRadius(radius);
	}

	public void setSwarmHeight(float alt) {
		swarmController.setSwarmHeight(alt);
	}

	public void stepCircle() {
		swarmController.stepCircle();
	}
}
