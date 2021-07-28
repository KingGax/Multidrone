package org.multidrone;


import com.MAVLink.enums.GPS_FIX_TYPE;
import com.MAVLink.enums.MAV_CMD;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.multidrone.Main;
import org.multidrone.enums.GridCellColour;
import org.multidrone.server.ServerController;
import org.multidrone.server.User;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;

/**
 * Created by Johannes on 23.05.16.
 *
 */

public class DroneListCell extends ListCell<User> {

    @FXML
    private Label lblName;
    @FXML
    private Label lblID;

    @FXML
    private Label lblRCBat;

    @FXML
    private Label lblBattery;

    @FXML
    private Label lblHeight;

    @FXML
    private Label lblYaw;

    @FXML
    private Label lblVelocity;

    @FXML
    private Label lblUpdateTimer;

    @FXML
    private Label lblX;
    @FXML
    private Label lblY;
    @FXML
    private Label lblZ;

    @FXML
    private Label lblVX;
    @FXML
    private Label lblVY;
    @FXML
    private Label lblVZ;

    @FXML
    private Label lblGPS;

    @FXML
    private Label lblClosestDist;
    @FXML
    private Label lblClosestID;

    @FXML
    private Button btnPause;
    @FXML
    private Button btnPlay;
    @FXML
    private Button btnHome;

    @FXML
    private Button btnLand;

    @FXML
    private Button btnLaunch;


    DecimalFormat decimalFormatter;

    @FXML
    private GridPane mainGridPane;

    private FXMLLoader mLLoader;

    ServerController sc;

    Alert goHomeConfirmation;
    Alert landConfirmation;

    @Override
    protected void updateItem(User user, boolean empty) { //Called whenever the LongProperties in User change
        super.updateItem(user, empty);

        if(empty || user == null) {

            setText(null);
            setGraphic(null);

        } else {
            if (mLLoader == null) {//The first setup of the item

                URL resource = getClass().getResource("/org.multidrone/DroneListCell.fxml");
                if (resource == null) {
                    throw new IllegalArgumentException("file not found!");
                }

                mLLoader = new FXMLLoader(getClass().getResource("/org.multidrone/DroneListCell.fxml"));
                mLLoader.setController(this);
                decimalFormatter = new DecimalFormat();
                decimalFormatter.setMaximumFractionDigits(2);



                try {
                    mLLoader.load();
                    if (ServerController.getInstance() != null){ //Setup all the individual drone buttons
                        sc = ServerController.getInstance();
                        btnPlay.setOnAction(e -> {
                            sc.sendMavCommand(user, MAV_CMD.MAV_CMD_DO_PAUSE_CONTINUE, 1, 0);
                        });
                    btnPause.setOnAction(e -> {
                            sc.sendMavCommand(user, MAV_CMD.MAV_CMD_DO_PAUSE_CONTINUE, 0, 0);
                    });
                    btnLaunch.setOnAction(e ->  sc.sendMavCommand(user, MAV_CMD.MAV_CMD_NAV_TAKEOFF,0,10));

                    goHomeConfirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want drone sys:" + user.getUserSystemID() + " to return home?");
                    btnHome.setOnAction(e -> goHomeConfirmation.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                                sc.sendMavCommand(user,MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH,0,0);
                        }
                    }));

                        landConfirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want drone sys:" + user.getUserSystemID() + " to land?");
                        btnLand.setOnAction(e -> landConfirmation.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                sc.sendMavCommand(user,MAV_CMD.MAV_CMD_NAV_LAND,0,0);
                            }
                        }));
                    } else{
                        System.out.println("SERVER CONTROLLER NULL");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }


            //Interpret all the values into strings
            lblBattery.setText(String.valueOf(user.data.batteryPercent) + "%");
            lblRCBat.setText("RC: " + user.data.rcBatteryPercentage + "%");
            lblName.setText(String.valueOf("SYS:" + user.getUserSystemID()));
            lblID.setText(String.valueOf("ID:" + user.getID()));
            lblYaw.setText("Yaw: " + decimalFormatter.format(Math.toDegrees(user.data.yaw)));
            lblHeight.setText("Height: " + user.data.height + "m");

            lblX.setText("xe:" + (Math.abs(user.data.xNED) < 0.0001f ? 0 : user.data.xNED));
            lblY.setText("ye:" + (Math.abs(user.data.yNED) < 0.0001f ? 0 : user.data.yNED));
            lblZ.setText("ze:" + (Math.abs(user.data.zNED) < 0.0001f ? 0 : user.data.zNED));

            String gpsStr = "GPS: ??";
            switch (user.data.gpsSig){
                case GPS_FIX_TYPE.GPS_FIX_TYPE_NO_GPS:
                    gpsStr = "GPS: NO";
                    break;
                case GPS_FIX_TYPE.GPS_FIX_TYPE_2D_FIX:
                    gpsStr = "GPS: WEAK";
                    break;
                case GPS_FIX_TYPE.GPS_FIX_TYPE_3D_FIX:
                    gpsStr = "GPS: GOOD";
                    break;
            }

            lblGPS.setText(gpsStr);

            lblVX.setText("vx: " + user.data.vx);
            lblVY.setText("vy: " + user.data.vy);
            lblVZ.setText("vz: " + user.data.vz);

            lblClosestDist.setText("CDist: " + user.closestDroneDistDist);
            lblClosestID.setText("ClosestID: " + user.closestDroneID);




            //prevent negative last check time
            if (user.getLastServerCheckTime() < user.getLastUpdateTime()){
                user.lastServerCheckTimeProperty().set(user.getLastUpdateTime());
            }
            long timeSinceUpdate = ((user.getLastServerCheckTime() - user.getLastUpdateTime())/1000);
            lblUpdateTimer.setText("Last Update: " + timeSinceUpdate + "s");
            GridCellColour col = getCellColour(user,timeSinceUpdate);
            switch (col){
                case Amber:
                    mainGridPane.getStyleClass().set(0,"gridcell_amber");
                    break;
                case Green:
                    mainGridPane.getStyleClass().set(0,"gridcell_green");
                    break;
                case Red:
                    mainGridPane.getStyleClass().set(0,"gridcell_red");
                    break;
                case Cyan:
                    mainGridPane.getStyleClass().set(0,"gridcell_cyan");
                    break;
            }
            setText(null);
            Platform.runLater(() -> setGraphic(mainGridPane));
        }

    }

    //This is the function that defines the conditions for each grid cell colour
    private GridCellColour getCellColour(User u, long timeSinceUpdate){
        if (u.getUserMavPort() == 0){
            return GridCellColour.Cyan;
        }

        if (u.data.gpsSig == GPS_FIX_TYPE.GPS_FIX_TYPE_NO_FIX){
            return GridCellColour.Red;
        }
        if (u.data.gpsSig == GPS_FIX_TYPE.GPS_FIX_TYPE_2D_FIX){
            return GridCellColour.Amber;
        }

        if (u.data.batteryPercent < 30){
            return GridCellColour.Red;
        }
        if (timeSinceUpdate > 10){
            return GridCellColour.Red;
        }

        if (u.data.batteryPercent < 50){
            return GridCellColour.Amber;
        }
        if (timeSinceUpdate > 5){
            return GridCellColour.Amber;
        }

        return  GridCellColour.Green;
    }


}