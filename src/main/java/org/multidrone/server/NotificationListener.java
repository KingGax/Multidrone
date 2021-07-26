package org.multidrone.server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.multidrone.shared.Notification;
import org.multidrone.shared.NotificationStatus;
import org.multidrone.sharedclasses.UserDroneData;

public class NotificationListener implements Runnable {
	

	private int notificationsPort = 32323;
	private String onlineStatusUpdateStartPattern = "---START STATUS UPDATE---";
	private String onlineStatusUpdateEndPattern = "---END STATUS UPDATE---";
	
	private String onlineUsersStartPattern = "---START ONLINE U UPDATE---";
	private String onlineUsersEndPattern = "---END ONLINE U UPDATE---";
	private String delimiter = ";";
	
	private boolean IGNOREDATA = true;
	@Override
	public void run() {
		try {
			this.initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void initialize() throws Exception {
		DatagramSocket socket = new DatagramSocket(this.notificationsPort);
		
		System.out.println("Server notification listener started on port "+this.notificationsPort);
		while(true){
			
			byte[] buffer = new byte[1500]; // MTU = 1500 bytes
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			System.out.println("Waiting");
			socket.receive(packet);
			System.out.println("Received notification");
			int recvSize = packet.getLength();
			byte[] myObject = new byte[recvSize];

			for(int i = 0; i < recvSize; i++)
			{
				myObject[i] = buffer[i];
			}

			InetAddress senderAddress = packet.getAddress();
			int senderPort = packet.getPort();
			
			String msg = new String(buffer).trim();
			System.out.println(msg);
			if (msg.startsWith("N")) {
				this.handleNotification(msg,senderAddress,senderPort);
			} else if (msg.startsWith("P")){
				this.handlePort(msg,senderAddress,senderPort);
			} else if(!IGNOREDATA)  {

				try {

					ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(myObject));
					UserDroneData data = (UserDroneData) iStream.readObject();
					iStream.close();
					this.handleMessage(data);
				} catch (Exception e) {
					System.out.println("Could not send message (Empty)");
				}
			}
			
					
		}
	}

	private void handlePort(String msg, InetAddress senderAddress,int senderPort) {
		ServerController controller = ServerController.getInstance();

		String[] strParts = msg.substring(1).split(delimiter);
		int port = Integer.parseInt(strParts[0]);
		int id = Integer.parseInt(strParts[1]);
		short sysID = Short.parseShort(strParts[2]);
		User user = controller.getUserByID(id);
		if (user != null){
			controller.updateMavDetails(id,port,sysID);
			controller.sendMessage(user,"aMAVPORT");
		}
		System.out.println("New mav port " + port + " notification from id: " + id + "at "+senderAddress+ " from port " +senderPort);
	}
	
	private void handleNotification(String msg, InetAddress senderAddress,int senderPort) throws Exception{
		ServerController controller = ServerController.getInstance();
		
		Notification n = Notification.deserialize(msg);
		
		User user = new User(n.getUsername(), senderAddress, n.getPort());
		
		NotificationStatus type = n.getStatus();
		if(type.equals(NotificationStatus.CONNECTED)) {
			controller.addUser(user);
		} else if (n.getStatus().equals(NotificationStatus.DISCONNECTED)) {
			controller.removeUser(user);	
		}
		
		/*for (User u : controller.getUsers()) {
			if (!u.equals(user)) {
				controller.sendMessage(u,this.buildNotificationMessage(user.getUsername(), type));
				System.out.println("Updating "+u.getUsername()+"'s list of online users");
			} 
			if (u.equals(user) && type.equals(NotificationStatus.CONNECTED)){
				System.out.println("sending online users list to "+u.getUsername());				
				controller.sendMessage(u, this.buildOnlineUsersList(user.getUsername()));
			}		
		}*/
				
		System.out.println("New notification from "+senderAddress+ " from port " +senderPort+n.toString());	
	}

	private void handleMessage(UserDroneData data) throws Exception{
		ServerController controller = ServerController.getInstance();
		controller.handleData(data);
		/*String [] strArr = msg.split(delimiter);
		System.out.println("Server should forward data ["+strArr[3]+"] to "+strArr[2]+" sent by "+strArr[1]);
		String sender = strArr[1];
		String receiver = strArr[2];
		String data = strArr[3];
		*/
		
	}

}
