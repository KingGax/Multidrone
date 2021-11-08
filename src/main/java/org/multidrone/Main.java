package org.multidrone;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.multidrone.coordinates.CoordinateTranslator;
import org.multidrone.coordinates.GeodeticCoordinate;
import org.multidrone.coordinates.GlobalRefrencePoint;
import org.multidrone.coordinates.NEDCoordinate;
import org.multidrone.maps.DownloadPreplannedMapController;
import org.multidrone.maps.DroneColour;
import org.multidrone.server.ServerController;
import org.multidrone.server.User;

import com.MAVLink.enums.MAV_CMD;
import org.multidrone.sharedclasses.UserDroneData;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;


public class Main extends Application {


    Stage mainStage;
    Scene mainScene;
    ServerController sc;

    AnchorPane selectedPane;

    Label selectedName;
    Label selectedHeight;
    Label selectedBattery;
    Label selectedAlt;
    Label selectedYaw;
    Label selectedLat;
    Label selectedLong;

    Button launchAll;
    Button armAll;
    Button disarmAll;

    Button btnLandAll;
    Button btnSetCircleTargets;
    Button forward;
    Button backwards;
    Button home;
    Button focusMapButton;
    Button toggleMapButton;

    Button btnSetD0Targ;
    Button btnSetD1Targ;
    Button btnSetD2Targ;
    Button btnSetD3Targ;
    Button btnStopAll;
    Button btnContinue;
    Button btnSetCircleHeight;
    Button btnRTL;
    Button btnSetDroneOrder;

    Button btnStartController;
    Button btnStartCircling;
    Button btnSetRefPos;
    Button btnSetRadius;
    Button btnSetRotationPeriod;

    ImageView imgSelected;

    TextField txtSetRefAlt;
    TextField txtD0TargetHeight;
    TextField txtD1TargetHeight;
    TextField txtD2TargetHeight;
    TextField txtD3TargetHeight;
    TextField txtSetRotationPeriod;
    TextField txtSetRadius;
    TextField txtSetCircleHeight;
    TextField txtSetDroneOrder;

    Button btnDemoOut;
    Button btnDemoCircle;
    Button btnStepCircle;

    GridPane gridPaneImages;

    VBox mapVBox;

    static DownloadPreplannedMapController mapController;



    Alert goHomeConfirmation;


    ListView<User> connectedNamesList;

    Timeline systemDropoutTimeChecker = new Timeline(
            new KeyFrame(Duration.seconds(1),
                    event -> setLastSystemCheckForUsers()));


    int selectedUserID = -1;

    boolean mapLoaded = false;
    boolean circling = false;


    float clickedLat=-1000;
    float clickedLng=-1000;

    int numImageBoxes = 0;

    HashMap<Integer, ImageView> idToImages = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org.multidrone/multidronemain.fxml"));
        Parent root = loader.load();

        //addUser(new User("dave",null,0));

        /*byte[] b = {1,2,3,4,5};
        FFmpegFrameGrabber g = new FFmpegFrameGrabber(new ByteArrayInputStream(b));
        try {
            g.start();
        } catch (Exception e){
            e.printStackTrace();
        }*/

        Text listViewTitle = new Text("Connected Drones");
        //VBox mainVbox = new VBox(listViewTitle,connectedNamesList);
        primaryStage.setTitle("Multidrone Server");
        primaryStage.setScene(new Scene(root, 1200, 700));
        mainStage = primaryStage;
        mainScene = primaryStage.getScene();
        primaryStage.show();

        //reads in values of items in scene, would be better to have done with fxml
        setupSceneVariables();

        sc = ServerController.getInstance();
        sc.fillMain(this);

        //Adds on click listeners, could also be replaced by fxml
        setupButtons();

        //starts timer for updating timeout values
        systemDropoutTimeChecker.setCycleCount(Timeline.INDEFINITE);
        systemDropoutTimeChecker.play();

        //loads in map component and adds it to the scene
        FXMLLoader mapLoader = new FXMLLoader(getClass().getResource("/download_preplanned_map/main.fxml"));
        Parent mapRoot = mapLoader.load();
        mapController = mapLoader.getController();
        mapController.setParent(this);
        mapVBox.getChildren().add(mapRoot);
        addTestData();

        //makes sure the application closes properly when you press X
        primaryStage.setOnCloseRequest(t -> {
            mapController.dispose();
            Platform.exit();
            System.exit(0);
        });
    }

    private void setupSceneVariables() {
        ((Label) mainScene.lookup("#usersLabel")).setText("Connected Drones");
        setupConnectedNamesList((ListView<User>) mainScene.lookup("#usersList"));
        selectedHeight = ((Label) mainScene.lookup("#lblHeight"));
        selectedBattery = ((Label) mainScene.lookup("#lblBattery"));
        selectedName = ((Label) mainScene.lookup("#lblSelectedName"));
        selectedAlt = ((Label) mainScene.lookup("#lblAlt"));
        selectedYaw = ((Label) mainScene.lookup("#lblYaw"));
        selectedLat = ((Label) mainScene.lookup("#lblLat"));
        selectedLong = ((Label) mainScene.lookup("#lblLong"));
        imgSelected = ((ImageView) mainScene.lookup("#imgSelected"));
        selectedPane = ((AnchorPane) mainScene.lookup("#selectedPane"));

        launchAll = ((Button) mainScene.lookup("#btnLaunchAll"));
        armAll = ((Button) mainScene.lookup("#btnArmAll"));
        disarmAll = ((Button) mainScene.lookup("#btnDisarmAll"));
        btnDemoOut = ((Button) mainScene.lookup("#btnDemoOut"));
        btnDemoCircle = ((Button) mainScene.lookup("#btnDemoCircle"));

        focusMapButton = ((Button) mainScene.lookup("#btnFocusMap"));
        toggleMapButton = ((Button) mainScene.lookup("#btnToggleMap"));

        btnLandAll = ((Button) mainScene.lookup("#btnLandAll"));
        btnSetCircleTargets = ((Button) mainScene.lookup("#btnSetCircleTargets"));
        forward = ((Button) mainScene.lookup("#btnForward"));
        backwards = ((Button) mainScene.lookup("#btnBack"));
        home = ((Button) mainScene.lookup("#btnReturnHome"));
        btnSetRefPos = ((Button) mainScene.lookup("#btnSetRefPos"));

        mapVBox = ((VBox) mainScene.lookup("#mapVBox"));

        txtSetRefAlt = ((TextField) mainScene.lookup("#txtRefHeight"));
        txtD0TargetHeight = ((TextField) mainScene.lookup("#txtD0TargetHeight"));
        txtD1TargetHeight = ((TextField) mainScene.lookup("#txtD1TargetHeight"));
        txtD2TargetHeight = ((TextField) mainScene.lookup("#txtD2TargetHeight"));
        txtD3TargetHeight = ((TextField) mainScene.lookup("#txtD3TargetHeight"));
        txtSetRotationPeriod = ((TextField) mainScene.lookup("#txtSetRotationPeriod"));
        txtSetRadius = ((TextField) mainScene.lookup("#txtSetRadius"));
        txtSetCircleHeight = ((TextField) mainScene.lookup("#txtSetCircleHeight"));
        txtSetDroneOrder = ((TextField) mainScene.lookup("#txtSetDroneOrder"));

        btnSetD0Targ = ((Button) mainScene.lookup("#btnSetD0Targ"));
        btnSetD1Targ = ((Button) mainScene.lookup("#btnSetD1Targ"));
        btnSetD2Targ = ((Button) mainScene.lookup("#btnSetD2Targ"));
        btnSetD3Targ = ((Button) mainScene.lookup("#btnSetD3Targ"));
        btnSetRotationPeriod = ((Button) mainScene.lookup("#btnSetRotationPeriod"));
        btnSetCircleHeight = ((Button) mainScene.lookup("#btnSetCircleHeight"));
        btnSetDroneOrder = ((Button) mainScene.lookup("#btnSetDroneOrder"));
        btnStepCircle = ((Button) mainScene.lookup("#btnStepCircle"));

        btnStartController = ((Button) mainScene.lookup("#btnStartController"));
        btnContinue = ((Button) mainScene.lookup("#btnUnpause"));
        btnStopAll = ((Button) mainScene.lookup("#btnStopAll"));
        btnStartCircling = ((Button) mainScene.lookup("#btnStartCircling"));
        btnSetRadius = ((Button) mainScene.lookup("#btnSetRadius"));
        btnRTL = ((Button) mainScene.lookup("#btnRTL"));

        gridPaneImages = ((GridPane) mainScene.lookup("#gridPaneImages"));
    }

    private void setupButtons() {
        setupArmButton(armAll,true);
        setupArmButton(disarmAll,false);
        setupLaunchAllButton();
        setupFocusMapButton();
        setupGOTOButton();
        setupSetTargetPosButton(btnSetD0Targ,0);
        setupSetTargetPosButton(btnSetD1Targ,1);
        setupSetTargetPosButton(btnSetD2Targ,2);
        setupSetTargetPosButton(btnSetD3Targ,3);
        setupPauseContinueButton(btnStopAll, true);
        setupPauseContinueButton(btnContinue, false);
        setupStartCircling();
        setupSetRotationPeriodButton();
        setupSetRadiusButton();
        setupSetCircleHeightButton();
        setupSmartRTL();
        setupSetDroneOrder();
        setupDemoOut();
        setupDemoCircle();
        setupStepCircle();
        setupLandAll();
        setupInitialTargetsButton();
        goHomeConfirmation = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure you want all the drones to return home?");
        setupHomeButton();

        setupSetRefPosButton();
    }

    public void addNewImageBox(User u){
        Platform.runLater(() -> {
            if (!idToImages.containsKey(u.getID())){
                VBox vb = new VBox(10);
                Label droneLabel = new Label("SYS: " + u.getUserSystemID() + " " + DroneColour.valueOfID(u.getID()).colourStr);
                ImageView iv = new ImageView();
                //makes image fit its container
                iv.fitWidthProperty().bind(vb.widthProperty());
                iv.fitHeightProperty().bind(vb.heightProperty().multiply(0.8));
                vb.getChildren().addAll(droneLabel,iv);
                int rows = gridPaneImages.getRowCount();
                int cols = gridPaneImages.getColumnCount();
                if (numImageBoxes >=rows * cols){
                    gridPaneImages.addRow(rows);
                }
                gridPaneImages.add(vb,numImageBoxes%cols, numImageBoxes/cols);
                idToImages.put(u.getID(),iv);
                numImageBoxes++;
            }
        });
    }

    //Rotates target circle positions
    private void setupStepCircle() {
        btnStepCircle.setOnAction(e -> {
            sc.stepCircle();
            sc.setInitialCircleTargets();
        });
    }


    //Hard coded demo values
    int[] droneOrder = {0,1,2,3};
    float[] goOutLats = {
            51.423225f,
            51.423195f,
            51.423172f,
            51.423192f
    };
    float[] goOutLongs = {
            -2.671130f,
            -2.671279f,
            -2.6714041f,
            -2.671548f
    };
    float goOutHeight = 20;
    GeodeticCoordinate refCirclePos = new GeodeticCoordinate(51.423164f, -2.6713874f, 2);
    float demoVerticalCircleOffset = 18f;
    float demoStartRadius = 15f;

    private void setupDemoCircle(){
        btnDemoCircle.setOnAction(e ->{
            sc.setRefPoint(new GlobalRefrencePoint(refCirclePos.lat, refCirclePos.lng, refCirclePos.height));
            //mapEngine.executeScript("document.setRefPoint(" + refCirclePos.lat + "," + refCirclePos.lng + ")");
            if (mapController.isLoaded()){
                mapController.moveRefPoint(refCirclePos.lat, refCirclePos.lng);
            }
            txtSetRefAlt.setPromptText(Double.toString(refCirclePos.height));
            txtSetRefAlt.setText("");
            txtSetCircleHeight.setPromptText(Float.toString(demoVerticalCircleOffset));
            txtSetCircleHeight.setText("");
            sc.setSwarmHeight(demoVerticalCircleOffset);
            sc.setRadius(demoStartRadius);
            txtSetRadius.setPromptText(Float.toString(demoStartRadius));
            txtSetRadius.setText("");
        });
    }

    private void setupDemoOut(){
        btnDemoOut.setOnAction(e -> {
            for (int i = 0; i < 4; i++) {
                int userID = droneOrder[i];
                if (connectedNamesList.getItems().size() > userID){
                    User u = connectedNamesList.getItems().get(userID);
                    u.target = new GeodeticCoordinate(goOutLats[i],goOutLongs[i],goOutHeight);
                    u.targetYaw = u.data.yaw;
                    moveTarget(goOutLats[i],goOutLongs[i],userID);
                }
            }
        });
    }

    public void setImage(int id, Image image){
        Platform.runLater(()->{
            if (selectedUserID == id){
                imgSelected.setImage(image);
            }
            if (idToImages.get(id) != null) {
                idToImages.get(id).setImage(image);
            }
        });

    }

    //rearranges the drones to allow for them to move directly forwards, currently hardcoded to work with up to 4 drones
    private void setupSetDroneOrder() {
        btnSetDroneOrder.setOnAction(e -> {
            if (txtSetDroneOrder.getText() != "") {
                try {
                    if (txtSetDroneOrder.getText().length() == 4) {
                        String input = txtSetDroneOrder.getText();
                        int d0ID = Integer.parseInt(input.substring(0,1));
                        int d1ID = Integer.parseInt(input.substring(1,2));
                        int d2ID = Integer.parseInt(input.substring(2,3));
                        int d3ID = Integer.parseInt(input.substring(3,4));
                        droneOrder[0] = d0ID;
                        droneOrder[1] = d1ID;
                        droneOrder[2] = d2ID;
                        droneOrder[3] = d3ID;
                        txtSetDroneOrder.setPromptText(txtSetDroneOrder.getText());
                        txtSetDroneOrder.setText("");
                    } else {
                        System.out.println("input 4 numbers");
                    }
                } catch (Exception e1) {
                    System.out.println("conversion error");
                }
            }
        });
    }

    private void setupSmartRTL(){
        btnRTL.setOnAction(e -> {
            for (User u : connectedNamesList.getItems()){
                u.target = u.preLandPoint;
                moveTarget((float)u.preLandPoint.lat,(float)u.preLandPoint.lng,u.getID());
            }
        });
    }

    private void setupLandAll() {
        btnLandAll.setOnAction(e -> {
            for (User u : connectedNamesList.getItems()){
                sc.sendMavCommand(u,MAV_CMD.MAV_CMD_NAV_LAND,0,0);
            }
        });
    }

    private void setupSetRadiusButton() {
        btnSetRadius.setOnAction(e -> {
            if (txtSetRadius.getText() != "") {
                try {
                    float radius = Float.parseFloat(txtSetRadius.getText());
                    if (radius > 8){
                        sc.setRadius(radius);
                        txtSetRadius.setPromptText(txtSetRadius.getText());
                        txtSetRadius.setText("");
                    } else{
                        System.out.println("radius too short");
                    }
                } catch (Exception e1) {
                    System.out.println("Radius not set, invalid text");
                }
            } else{
                System.out.println("Radius not set, no text");
            }
        });
    }


    private void setupInitialTargetsButton() {
        btnSetCircleTargets.setOnAction(e -> {
            sc.setInitialCircleTargets();
        });
    }

    void setLastSystemCheckForUsers(){
        for (User u: connectedNamesList.getItems()) {
            u.setLastServerCheckTime(System.currentTimeMillis());
        }
    }

    void setupStartCircling(){
        btnStartCircling.setOnAction(e ->
        {
            circling = !circling;
            sc.setCirclingEnabled(circling);
            if (circling){
                btnStartCircling.setText("Stop circling");
            } else{
                btnStartCircling.setText("Start circling");
            }
        });
    }

    void setupPauseContinueButton(Button b, boolean pause){
        b.setOnAction(e -> {
            if (pause) {
                sc.stopAllDrones();
            } else {
                sc.unPauseAllDrones();
            }
        });
    }


    void setupSetRefPosButton(){
        btnSetRefPos.setOnAction(e -> {
            if (txtSetRefAlt.getText() != "") {
                try {
                    float alt = Float.parseFloat(txtSetRefAlt.getText());
                    if (clickedLat != -1000 && clickedLng != -1000) {
                        sc.setRefPoint(new GlobalRefrencePoint(clickedLat, clickedLng, alt));
                        //mapEngine.executeScript("document.setRefPoint(" + clickedLat + "," + clickedLng + ")");
                        //mapEngine.executeScript("document.hideSelectedPointMarker()");

                        if (mapController.isLoaded()){
                            mapController.moveRefPoint(clickedLat,clickedLng);
                            mapController.hideClickedPoint();
                        }
                        txtSetRefAlt.setPromptText(txtSetRefAlt.getText());
                        txtSetRefAlt.setText("");
                        clickedLat = -1000;
                        clickedLng = -1000;
                    } else{
                        System.out.println("Target not set, no point selected");
                    }
                } catch (Exception e1) {
                    System.out.println("Target not set, invalid text");
                }
            } else if (selectedUserID != -1){
                for (User u : connectedNamesList.getItems()) {
                    if (u.getID() == selectedUserID) {
                        sc.setRefPoint(new GlobalRefrencePoint(u.data.lat, u.data.lng, u.data.height));
                        sc.setTargetYaw(u.data.yaw + 0.5f);
                    }
                }
            } else{
                System.out.println("Target not set, no height or selected drone");
            }
        });
    }

    void setupSetRotationPeriodButton(){
        btnSetRotationPeriod.setOnAction(e -> {
            if (txtSetRotationPeriod.getText() != "") {
                try {
                    float period = Float.parseFloat(txtSetRotationPeriod.getText());
                    if (period > 5){
                        sc.setRotationPeriod(period*1000);
                        txtSetRotationPeriod.setPromptText(txtSetRotationPeriod.getText());
                        txtSetRotationPeriod.setText("");
                    } else{
                        System.out.println("Period too short");
                    }
                } catch (Exception e1) {
                    System.out.println("Period not set, invalid text");
                }
            } else{
                System.out.println("Period not set, no text");
            }
        });
    }


    void setupSetTargetPosButton(Button button, int _id){
        TextField txt;
        int id = _id;
        switch (id){
            case 0:
                txt = txtD0TargetHeight;
                break;
            case 1:
                txt = txtD1TargetHeight;
                break;
            case 2:
                txt = txtD2TargetHeight;
                break;
            case 3:
                txt = txtD3TargetHeight;
                break;
            default:
                System.out.println("setting only supported for 0-3");
                return;
        }
        button.setOnAction(e -> {
            boolean foundUser = false;
            for (User u : connectedNamesList.getItems()){
                if (u.getID() == id){
                    foundUser = true;
                    if (txt.getText() != "") {
                        try {
                            float alt = Float.parseFloat(txt.getText());
                            if (clickedLat != -1000 && clickedLng != -1000) {
                                moveTarget(clickedLat,clickedLng,id);
                                u.target = new GeodeticCoordinate(clickedLat,clickedLng,alt);
                                u.targetYaw = u.data.yaw;
                                if (mapController.isLoaded()){
                                    mapController.hideClickedPoint();
                                }
                                txt.setPromptText(txt.getText());
                                txt.setText("");
                                clickedLat = -1000;
                                clickedLng = -1000;
                            } else{
                                System.out.println("Target not set, no point selected");
                            }
                        } catch (Exception e1) {
                            System.out.println("Target not set, invalid text");
                        }
                    } else{
                        System.out.println("Target not set, no height");
                    }
                }
            }
            if (!foundUser){
                System.out.println("Target not set, user " + id + " does not exist");
            }
        });
    }

    void setupSetCircleHeightButton(){
        btnSetCircleHeight.setOnAction(e -> {
                    if (txtSetCircleHeight.getText() != "") {
                        try {
                            float alt = Float.parseFloat(txtSetCircleHeight.getText());
                            if (alt > 0){
                                txtSetCircleHeight.setPromptText(txtSetCircleHeight.getText());
                                txtSetCircleHeight.setText("");
                                sc.setSwarmHeight(alt);
                            } else{
                                System.out.println("Setting altitude below ref point currently not allowed");
                            }

                        } catch (Exception e1) {
                            System.out.println("Target not set, invalid text");
                        }
                    } else{
                        System.out.println("Target not set, no height");
                    }
        });
    }

    /*void setupMapToggleButton(){
        toggleMapButton.setOnAction(e -> {
            boolean isVisible = mapView.isVisible();
            mapView.setVisible(!isVisible);

        });
    }*/

    void setupHomeButton(){
        home.setOnAction(e -> goHomeConfirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Platform.runLater(() -> {
                    for (User u:connectedNamesList.getItems()) {
                        if (u.getUserMavPort() != 0){
                            sc.sendMavCommand(u,MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH,0,0);
                        }
                    }
                    System.out.println("Sending all home");
                });
            }
        }));
    }

    void setupFocusMapButton(){
        focusMapButton.setOnAction(e -> focusMapOnDrones());
    }

    //add in main if you want to test without drones, useful function
    void addTestData(){
        User u1 = new User("u1", null, 50);
        u1.data = new UserDroneData();
        u1.data.lat = 50.0f;
        u1.setID(0);
        u1.setUserSystemID((short)5);

        User u2 = new User("u2", null, 50);
        u2.data = new UserDroneData();
        u2.data.lat = 50.1f;
        u2.setID(1);
        u2.setUserSystemID((short)10);

        User u3 = new User("u3", null, 50);
        u3.data = new UserDroneData();
        u3.data.lat = 50.1f;
        u3.setID(2);
        u3.setUserSystemID((short)11);

        User u4 = new User("u4", null, 50);
        u4.data = new UserDroneData();
        u4.data.lat = 50.1f;
        u4.setID(3);
        u4.setUserSystemID((short)12);

        connectedNamesList.getItems().add(u1);
        connectedNamesList.getItems().add(u2);
        connectedNamesList.getItems().add(u3);
        connectedNamesList.getItems().add(u4);
    }

    public void setSelectedCoordinate(float lat, float lng){
        clickedLat = lat;
        clickedLng = lng;
        System.out.println(clickedLat + " " + clickedLng);
    }

    void setupConnectedNamesList(ListView<User> usersList){

        usersList.setCellFactory(new Callback<ListView<User>, ListCell<User>>() {
            @Override
            public ListCell<User> call(ListView<User> studentListView) {
                return new DroneListCell();
            }
        });


        //handle double clicking drones
        usersList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2){
                    onUserClick(usersList.getSelectionModel().getSelectedItem());
                }
            }
        });
        connectedNamesList = usersList;
    }

    private void highlightUser(User user){
        displaySelectedUser(user);
        ImageView iv = idToImages.get(user.getID());
        if (iv != null){
            imgSelected.setImage(iv.getImage());
        }
        selectedUserID = user.getID();
    }

    //Used to show double clicked user
    private void displaySelectedUser(User user){
        selectedName.setText("Selected Name: " + user.getUsername());
        selectedHeight.setText("Height: " + user.data.height);
        selectedBattery.setText("Battery: " + user.data.batteryPercent);
        selectedLat.setText("Lat: " + user.data.lat);
        selectedYaw.setText("Yaw: " + user.data.yaw);
        selectedLong.setText("Long: " + user.data.lng);
    }

    private void onUserClick(User user){
        highlightUser(user);
    }

    //function called from server controller to refresh displayed user
    public void updateUser(User u){
        Platform.runLater(() -> {
            if (selectedUserID == u.getID()){
                displaySelectedUser(u);
            }
        });

    }

    public void moveMarker(float lat, float lng, int id){
        Platform.runLater(() -> {
            if (mapController.isLoaded()){
                mapController.moveMarker(id,lat,lng);
            }
        });
    }

    public void moveMarker(User u){
        Platform.runLater(() -> {
            if (mapController.isLoaded()){
                mapController.moveMarker(u.getID(),u.data.lat,u.data.lng);
                if (u.forwardPoint != null){
                    mapController.moveSightMarker(u.getID(),u.forwardPoint.lat,u.forwardPoint.lng);
                }
            }
        });
    }

    public void moveTarget(float lat, float lng, int id){
        Platform.runLater(() -> {
            if (mapController.isLoaded()){
                mapController.moveTarget(id,lat,lng);
            }
        });
    }

    private void addJoystickCommandToButton(Button b, short x, short y, short z, short r){
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                sendJoystickToAll(x,y,z,r);
            }
        });

    }

    private void focusMapOnDrones(){
        System.out.println("fitting all markers");
        Platform.runLater(() -> {
            if (mapController.isLoaded()){
                mapController.focusDrones();
            }
        });
    }


    private void setupGOTOButton(){
        btnStartController.setOnAction(e -> {
                for (User u : connectedNamesList.getItems()) {
                    if (u.target != null){
                        sc.sendUserTarget(u);
                    }

                }
        });
    }

    private void sendJoystickToAll(short x, short y, short z, short r){
        Platform.runLater(() -> {
            for (User u:connectedNamesList.getItems()) {
                if (u.getUserMavPort() != 0){
                    sc.sendJoystickCommand(u,x,y,z,r);
                }
            }
            System.out.println("Arm all");
        });
    }

    //function for setting up arm and disarm buttons
    private void setupArmButton(Button b, boolean arm){
        float armParam = arm ? 1 : 0;
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                Platform.runLater(() -> {
                    for (User u:connectedNamesList.getItems()) {
                        if (u.getUserMavPort() != 0){
                            sc.sendMavCommand(u,MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,armParam,0);
                        }
                    }
                    System.out.println("Arm all");
                });

            }
        });

    }

    private void setupLaunchAllButton(){
        launchAll.setOnAction(e -> {
            //Launch button sends takeoff command and stores location of all drones for returning to land
            Platform.runLater(() -> {
                for (User u:connectedNamesList.getItems()) {
                    if (u.getUserMavPort() != 0){
                        sc.sendMavCommand(u,MAV_CMD.MAV_CMD_NAV_TAKEOFF,0,6);
                        u.preLandPoint = new GeodeticCoordinate(u.data.lat,u.data.lng,8);
                    }
                }
                System.out.println("Arm all");
            });

        });

    }

    public void setConnectedNamesList(ObservableList<User> users){
        connectedNamesList.setItems(users);
    }


    public static void main(String[] args) {
        launch(args);
    }
}


