package org.multidrone.controller;

import javafx.collections.ObservableList;
import org.multidrone.controller.pid.DronePIDController;
import org.multidrone.controller.pid.PIDControllerX;
import org.multidrone.controller.pid.PIDControllerY;
import org.multidrone.coordinates.CoordinateTranslator;
import org.multidrone.coordinates.GeodeticCoordinate;
import org.multidrone.coordinates.GlobalRefrencePoint;
import org.multidrone.coordinates.NEDCoordinate;
import org.multidrone.server.ServerController;
import org.multidrone.server.User;

import java.net.DatagramSocket;
import java.util.List;

public class MultiDronePositionController implements Runnable {
    ObservableList<User> currentUsers;
    final float refreshRateHz = 4;
    long sleepTime;
    final float projectionLengthSeconds = 1.5f;
    private boolean active = false;

    private GlobalRefrencePoint globalRef;

    private float MIN_SAFETY_DIST = 10;
    private float timeSinceEmergencySend = 0;
    private float emergencySendWait = 5000;
    private float heightOffset = 0;

    private NEDCoordinate targetPos;
    private float radius = 20;
    private final float MIN_RADIUS = 10;
    private float rotationTime = 150_000;
    private float rotationTimer=0;
    private int droneNumber = 4;
    private boolean circlingEnabled = false;
    private boolean setInitialTargets = false;

    private DronePIDController droneController = new DronePIDController();

    @Override
    public void run() {
        try {
            this.initialize();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void setRotationTime(float millis) {
        rotationTimer = (rotationTimer / rotationTime) * millis;
        rotationTime = millis;
    }

    public void stepCircle(){
        rotationTimer += rotationTime / (droneNumber * 2);
    }

    public void startCircling(int _droneNumber){
        circlingEnabled = true;
        droneNumber = _droneNumber;
    }

    public void stopCircling(){
        circlingEnabled = false;
    }

    public void setControlLoopActive(boolean _active){
        if (currentUsers != null && _active){
            active = true;
        } else if (!_active){
            active = false;
        }
    }

    //Filming target is assumed to be the point of interest
    public void setGlobalRef(GlobalRefrencePoint ref){
        globalRef = ref;
        targetPos = new NEDCoordinate(0,0,0);
    }

    public void setUserList(ObservableList<User> users){
        currentUsers = users;
    }

    public void setInitialTargets(int _droneNumber){
        setInitialTargets = true;
        droneNumber = _droneNumber;
    }

    private void initialize() throws Exception {
        sleepTime = Math.round(1000/refreshRateHz);
        droneController.initialise();

        while(true){

            Thread.sleep(sleepTime);
            if (active){
                droneControlLoop(currentUsers);
            }
        }
    }

    //The main loop of the program!
    private void droneControlLoop(List<User> users){
        safetyCheck(users);
        if (targetPos != null && radius > MIN_RADIUS){
            double rotationSteps = 2 * Math.PI / droneNumber;
            for (int i = 0; i < droneNumber; i++) {
                double targetX = targetPos.x + radius * Math.cos(rotationSteps * i + (rotationTimer/rotationTime)*Math.PI*2);
                double targetY = targetPos.y + radius * Math.sin(rotationSteps * i + (rotationTimer/rotationTime)*Math.PI*2);
                double targetZ = targetPos.z - heightOffset;
                GeodeticCoordinate targGD = CoordinateTranslator.nedToGeodetic(new NEDCoordinate(targetX,targetY,targetZ),globalRef);
                if (setInitialTargets || circlingEnabled){ //This sets the targets of all users to help them get into formation
                    ServerController.getInstance().setTargetMarker(targGD,i);
                    if (currentUsers.size() > i){
                        User u = currentUsers.get(i);
                        u.target = targGD;
                        u.targetYaw = getTargetYaw(targetX,targetY,0,0);
                    }
                }
            }
            setInitialTargets = false;
            if (circlingEnabled){
                rotationTimer += sleepTime;
                for (User u : currentUsers){
                    if (u.target != null){
                        ServerController.getInstance().sendSetPosTargetGlobalCommand(u,u.target.lat,u.target.lng,u.targetYaw,u.target.height);
                    }
                }
            }


        }

    }

    //Calculate yaw required to look at a point in NED space
    private float getTargetYaw(double x, double y, double targX, double targY){

        double yaw = Math.atan2((targX - x),(targY - y)) - Math.PI/2;
        if (yaw < -Math.PI){
            yaw += Math.PI * 2;
        }
        return (float)-yaw;
    }

    //Currently projects to the point the drone will be at in the future and compares it to all other drones, could upgrade to compare similar points across the projected lines
    private void safetyCheck(List<User> users){
        if (globalRef != null){
            calculateProjectedPos(users,projectionLengthSeconds);
            for (User u: users) {
                float  minDist = Float.MAX_VALUE;
                int closestID=-1;
                int closestSYSID = -1;
                for(User u2 : users){
                    if (u.getID() != u2.getID()){
                        float dist = (float)Math.sqrt(Math.pow(u.projectedPos.x-u2.projectedPos.x,2)+ Math.pow(u.projectedPos.y-u2.projectedPos.y,2) + Math.pow(u.projectedPos.z-u2.projectedPos.z,2));
                        if (dist < minDist){
                            minDist = dist;
                            closestID = u2.getID();
                            closestSYSID = u2.getUserSystemID();
                        }
                    }
                }
                u.closestDroneDistDist = minDist;
                u.closestDroneID = closestID;
                u.closestDroneSYSID = closestSYSID;
                if (u.closestDroneDistDist < MIN_SAFETY_DIST && timeSinceEmergencySend > emergencySendWait){
                    ServerController.getInstance().stopAllDrones();
                    System.out.println("pos: " + u.currentPos + " " + u.projectedPos + " otherID: " + u.closestDroneID + " " + users.get(u.closestDroneID).currentPos + " proj: " + users.get(u.closestDroneID).projectedPos);
                    System.out.println("STOPPED ALL DRONES");
                    timeSinceEmergencySend = 0;
                }
            }
            timeSinceEmergencySend += sleepTime;
        }
    }

    public void calculateProjectedPos(List<User> users, float projectTimeSeconds){
        for (User u : users) {
            if (u.currentPos == null) {
                u.currentPos = CoordinateTranslator.GeodeticToNED(new GeodeticCoordinate(u.data.lat,u.data.lng,u.data.height), globalRef);
            }
            u.projectedPos = new NEDCoordinate(u.currentPos.x + (u.data.vx * projectTimeSeconds) / 100, u.currentPos.y + (u.data.vy * projectTimeSeconds) / 100, u.currentPos.z + (u.data.vz * projectTimeSeconds) / 100);
        }
    }

    public void setTargetYaw(float yaw) {
        droneController.setTargetYaw(yaw);
    }

    public void setRadius(float _radius) {
        radius = _radius;
    }

    public void setSwarmHeight(float alt) {
        heightOffset = alt;
    }
}
