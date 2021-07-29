/* Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.multidrone.maps;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.ArcGISVectorTiledLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.tasks.offlinemap.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.multidrone.Main;
import org.multidrone.maps.PreplannedMapAreaListCell;
import org.multidrone.coordinates.GeodeticCoordinate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import java.util.prefs.Preferences;

public class DownloadPreplannedMapController {

  @FXML private ListView<PreplannedMapArea> preplannedAreasListView;
  @FXML private ListView<DownloadPreplannedOfflineMapJob> downloadJobsListView;
  @FXML private MapView mapView;
  @FXML private Button downloadButton;
  @FXML private Button btnLoad;
  @FXML private Button btnDownloadScreen;
  @FXML private ProgressBar progressBar;
  @FXML private Button btnLoadRecent;
  @FXML private Button btnToggleSettings;
  @FXML private VBox settingsVBox;
  @FXML private TextField txtLatLng;
  private GraphicsOverlay markers = new GraphicsOverlay();

  private boolean settingsHidden = false;

  private ArcGISMap onlineMap;
  private GraphicsOverlay areasOfInterestGraphicsOverlay;
  private OfflineMapTask offlineMapTask;
  private List<PreplannedMapArea> preplannedMapAreas; // keep loadable in scope to avoid garbage collection

  private List<Graphic> droneMarkers = new ArrayList<>();
  private List<Graphic> targetMarkers = new ArrayList<>();
  private List<Graphic> sightMarkers = new ArrayList<>();

  private ArcGISMap loadedMap;

  private Stage mainStage;
  private Main parent;
  Graphic downloadArea;
  private Preferences prefs;
  private String lastLoadedPath;

  final String USERNAME_PREF = "username";
  final String PASSWORD_PREF = "password";
  final String LAST_LOADED_PREF = "lastloaded";

  Graphic clickedPoint;
  Graphic refPoint;

  @FXML
  private void initialize() {
    try {

      downloadArea = new Graphic();

      prefs = Preferences.userRoot().node(this.getClass().getName());

      // create a portal to ArcGIS Online
      Portal portal = new Portal("https://www.arcgis.com/");

      // set the authentication manager to handle OAuth challenges when accessing the portal
      AuthenticationManager.setAuthenticationChallengeHandler(new DefaultAuthenticationChallengeHandler());

      // create a portal item using the portal and the item id of a map service
      //PortalItem portalItem = new PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674");
      PortalItem portalItem = new PortalItem(portal, "2999a1f15ef141838d662572af458c43");
      // create a map with the portal item
      onlineMap = new ArcGISMap(portalItem);


      // show the map
      mapView.setMap(onlineMap);


      //add onclick
      mapView.setOnMouseClicked(e -> {
        Point p = mapView.screenToLocation(new Point2D(e.getX(),e.getY()));
        String latLonDecimalDegrees = CoordinateFormatter.toLatitudeLongitude(p, CoordinateFormatter
                .LatitudeLongitudeFormat.DECIMAL_DEGREES, 7);
        GeodeticCoordinate g = parseLatLngString(latLonDecimalDegrees);
        parent.setSelectedCoordinate((float)g.lat,(float)g.lng);
        moveClickedPoint(g.lat,g.lng);
      });

      btnLoadRecent.setOnAction(e ->{
                loadLastMap(e);
              });

      // create a graphics overlay to show the preplanned map areas extents (areas of interest)
      areasOfInterestGraphicsOverlay = new GraphicsOverlay();
      mapView.getGraphicsOverlays().add(areasOfInterestGraphicsOverlay);

      initialiseDroneMarkers();
      initialiseTargetMarkers();
      initialiseSightMarkers();
      loadClickMarker();
      loadRefMarker();
      mapView.getGraphicsOverlays().add(markers);

      btnDownloadScreen.setOnAction(e -> {
        if (mapView.getMap().getLoadStatus() == LoadStatus.LOADED) {
          // upper left corner of the area to take offline
          Point2D minScreenPoint = new Point2D(50, 50);
          // lower right corner of the downloaded area
          Point2D maxScreenPoint = new Point2D(mapView.getWidth() - 50, mapView.getHeight() - 50);
          // convert screen points to map points
          Point minPoint = mapView.screenToLocation(minScreenPoint);
          Point maxPoint = mapView.screenToLocation(maxScreenPoint);
          // use the points to define and return an envelope
          if (minPoint != null && maxPoint != null) {
            Envelope envelope = new Envelope(minPoint, maxPoint);
            downloadArea.setGeometry(envelope);
          }

          try {
            // show the progress bar
            progressBar.setVisible(true);

            // specify the extent, min scale, and max scale as parameters
            double minScale = mapView.getMapScale()+2;
            double maxScale = mapView.getMap().getMaxScale();
            // minScale must always be larger than maxScale
            if (minScale <= maxScale) {
              minScale = maxScale + 1;
            }
            GenerateOfflineMapParameters params = new GenerateOfflineMapParameters(downloadArea.getGeometry(), minScale, maxScale);

            // create an offline map task with the map
            OfflineMapTask task = new OfflineMapTask(mapView.getMap());

            // create an offline map job with the download directory path and parameters and start the job
            Path tempDirectory = Files.createTempDirectory("offline_map");

            System.out.println(tempDirectory);
            GenerateOfflineMapJob job = task.generateOfflineMap(params, tempDirectory.toAbsolutePath().toString());
            job.start();
            job.addJobDoneListener(() -> {
              if (job.getStatus() == Job.Status.SUCCEEDED) {
                // replace the current map with the result offline map when the job finishes
                GenerateOfflineMapResult result = job.getResult();
                new Alert(Alert.AlertType.INFORMATION, tempDirectory.toAbsolutePath().toString()).show();
                mapView.setMap(result.getOfflineMap());
                btnDownloadScreen.setDisable(true);
              } else {
                new Alert(Alert.AlertType.ERROR, job.getError().getAdditionalMessage()).show();
              }
              Platform.runLater(() -> progressBar.setVisible(false));
            });
            // show the job's progress with the progress bar
            job.addProgressChangedListener(() -> progressBar.setProgress(job.getProgress() / 100.0));
          } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to create temporary directory").show();
          }
        }

      });


      // create a red outline to mark the areas of interest of the preplanned map areas
      SimpleLineSymbol areaOfInterestLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0x80FF0000, 5.0f);
      SimpleRenderer areaOfInterestRenderer = new SimpleRenderer();
      areaOfInterestRenderer.setSymbol(areaOfInterestLineSymbol);
      areasOfInterestGraphicsOverlay.setRenderer(areaOfInterestRenderer);



      // create an offline map task for the portal item
      offlineMapTask = new OfflineMapTask(portalItem);

      // use a cell factory which shows the preplanned area's title
      preplannedAreasListView.setCellFactory(c -> new PreplannedMapAreaListCell());

      // get the preplanned map areas from the offline map task and show them in the list view
      ListenableFuture<List<PreplannedMapArea>> preplannedMapAreasFuture = offlineMapTask.getPreplannedMapAreasAsync();
      preplannedMapAreasFuture.addDoneListener(() -> {
        try {
          // get the preplanned areas and add them to the list view
          preplannedMapAreas = preplannedMapAreasFuture.get();
          preplannedAreasListView.getItems().addAll(preplannedMapAreas);

          // load each area and show a red border around their area of interest
          preplannedMapAreas.forEach(preplannedMapArea -> {
            preplannedMapArea.loadAsync();
            preplannedMapArea.addDoneLoadingListener(() -> {
              if (preplannedMapArea.getLoadStatus() == LoadStatus.LOADED) {
                areasOfInterestGraphicsOverlay.getGraphics().add(new Graphic(preplannedMapArea.getAreaOfInterest()));
              } else {
                new Alert(Alert.AlertType.ERROR, "Failed to load preplanned map area").show();
              }
            });
          });

        } catch (InterruptedException | ExecutionException e) {
          new Alert(Alert.AlertType.ERROR, "Failed to get the Preplanned Map Areas from the Offline Map Task.").show();
        }
      });


      preplannedAreasListView.getSelectionModel().selectedItemProperty().addListener((o, p, n) -> {
        PreplannedMapArea selectedPreplannedMapArea = preplannedAreasListView.getSelectionModel().getSelectedItem();
        if (selectedPreplannedMapArea != null) {

          // clear the download jobs list view selection
          downloadJobsListView.getSelectionModel().clearSelection();

          // show the online map with the areas of interest
          mapView.setMap(onlineMap);
          areasOfInterestGraphicsOverlay.setVisible(true);

          // set the viewpoint to the preplanned map area's area of interest
          Envelope areaOfInterest = GeometryEngine.buffer(selectedPreplannedMapArea.getAreaOfInterest(), 100).getExtent();
          mapView.setViewpointAsync(new Viewpoint(areaOfInterest), 0.5f);
        }
      });

      // disable the download button when no area is selected
      downloadButton.disableProperty().bind(preplannedAreasListView.getSelectionModel().selectedItemProperty().isNull());

      // use a cell factory which shows the download preplanned offline map job's progress and title
      downloadJobsListView.setCellFactory(c -> new DownloadPreplannedOfflineMapJobListCell());

      ChangeListener<DownloadPreplannedOfflineMapJob> selectedDownloadChangeListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends DownloadPreplannedOfflineMapJob> observable, DownloadPreplannedOfflineMapJob oldValue, DownloadPreplannedOfflineMapJob newValue) {
          DownloadPreplannedOfflineMapJob selectedJob = downloadJobsListView.getSelectionModel().getSelectedItem();
          if (selectedJob != null) {

            // hide the preplanned map areas and clear the preplanned area list view's selection
            areasOfInterestGraphicsOverlay.setVisible(false);
            preplannedAreasListView.getSelectionModel().clearSelection();

            if (selectedJob.getStatus() == Job.Status.SUCCEEDED) {
              DownloadPreplannedOfflineMapResult result = selectedJob.getResult();

              // check if the result has errors
              if (result.hasErrors()) {

                // collect the layer and table errors into a single alert message
                StringBuilder stringBuilder = new StringBuilder("Errors: ");

                Map<Layer, ArcGISRuntimeException> layerErrors = result.getLayerErrors();
                layerErrors.forEach((layer, exception) ->
                        stringBuilder.append("Layer: ").append(layer.getName()).append(". Exception: ").append(exception.getMessage()).append(". ")
                );

                Map<FeatureTable, ArcGISRuntimeException> tableError = result.getTableErrors();
                tableError.forEach((table, exception) ->
                        stringBuilder.append("Table: ").append(table.getTableName()).append(". Exception: ").append(exception.getMessage()).append(". ")
                );

                new Alert(Alert.AlertType.ERROR, "One or more errors occurred with the Offline Map Result: " + stringBuilder.toString()).show();
              } else {
                mapView.addNavigationChangedListener(listener -> {
                  // if the mapview is navigating to a new pleplanned map area, wait for it to finish
                  if (!listener.isNavigating()) {
                    // show the offline map in the map view
                    ArcGISMap downloadOfflineMap = result.getOfflineMap();
                    mapView.setMap(downloadOfflineMap);
                  }
                });
              }

            } else {
              // alert the user the job is still in progress if selected before the job is done
              new Alert(Alert.AlertType.WARNING, "Job status: " + selectedJob.getStatus()).show();

              // when the job is done, re-trigger the listener to show the job's result if it is still selected
              selectedJob.addJobDoneListener(() ->
                this.changed(observable, oldValue, downloadJobsListView.getSelectionModel().getSelectedItem())
              );
            }
          }
        }
      };

      downloadJobsListView.getSelectionModel().selectedItemProperty().addListener(selectedDownloadChangeListener);

    } catch (Exception e) {
      // on any exception, print the stacktrace
      e.printStackTrace();
    }
  }

  public void focusDrones(){
    PointCollection dronePoints = new PointCollection(SpatialReferences.getWgs84());
    for (Graphic drone: droneMarkers) {
      if (drone.getGeometry() != null){
        dronePoints.add(drone.getGeometry().getExtent().getCenter());
      }
    }
    Geometry g = new Polyline(dronePoints);
    mapView.setViewpointGeometryAsync(g);
  }

  public void dispose(){
    mapView.dispose();
  }

  private void initialiseSightMarkers() {
    for (DroneColour dc : DroneColour.values()) {
      sightMarkers.add(new Graphic());
    }
    for (DroneColour dc : DroneColour.values()) {
      Image newImage = new Image("download_preplanned_map/mapres/drones/viewmarker.png");
      PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(newImage);
      // set size of the image
      markerSymbol.setHeight(10);
      markerSymbol.setWidth(10);
      // load symbol asynchronously
      markerSymbol.loadAsync();

      // add to the graphic overlay once done loading
      PictureMarkerSymbol finalMarkerSymbol = markerSymbol;
      markerSymbol.addDoneLoadingListener(() -> {
        if (finalMarkerSymbol.getLoadStatus() == LoadStatus.LOADED) {
          sightMarkers.get(dc.id).setSymbol(finalMarkerSymbol);
          markers.getGraphics().add(sightMarkers.get(dc.id));
        } else {
          Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
          alert.show();
        }
      });
    }
  }

  private void loadClickMarker() {
    Image newImage = new Image("download_preplanned_map/mapres/targets/selectedpoint.png");
    PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(newImage);
    // set size of the image
    markerSymbol.setHeight(20);
    markerSymbol.setWidth(30);
    // load symbol asynchronously
    markerSymbol.loadAsync();
    // add to the graphic overlay once done loading
    PictureMarkerSymbol finalMarkerSymbol = markerSymbol;
    markerSymbol.addDoneLoadingListener(() -> {
      if (finalMarkerSymbol.getLoadStatus() == LoadStatus.LOADED) {
        clickedPoint = new Graphic();
        clickedPoint.setSymbol(finalMarkerSymbol);
        markers.getGraphics().add(clickedPoint);
      } else {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
        alert.show();
      }
    });
  }
  private void loadRefMarker() {
    Image newImage = new Image("download_preplanned_map/mapres/targets/refrencepos.png");
    PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(newImage);
    // set size of the image
    markerSymbol.setHeight(20);
    markerSymbol.setWidth(30);
    // load symbol asynchronously
    markerSymbol.loadAsync();
    // add to the graphic overlay once done loading
    PictureMarkerSymbol finalMarkerSymbol = markerSymbol;
    markerSymbol.addDoneLoadingListener(() -> {
      if (finalMarkerSymbol.getLoadStatus() == LoadStatus.LOADED) {
        refPoint = new Graphic();
        refPoint.setSymbol(finalMarkerSymbol);
        markers.getGraphics().add(refPoint);
      } else {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
        alert.show();
      }
    });
  }

  public boolean isLoaded(){
    if (mapView.getMap() !=  null){
      if (mapView.getMap().getLoadStatus() == LoadStatus.LOADED) {
        return true;
      }
    }
    return false;
  }

  public void setParent(Main _parent){
    parent = _parent;
  }

  private GeodeticCoordinate parseLatLngString(String latlng){
    String[] split = latlng.split(" ");
    double lat = 0;
    double lng = 0;
    try {
      lat = Double.parseDouble(split[0].substring(0,split[0].length()-1));
      if (split[0].charAt(split[0].length()-1) == 'S'){
        lat *= -1;
      }
      lng = Double.parseDouble(split[1].substring(0,split[1].length()-1));
      if (split[1].charAt(split[1].length()-1) == 'W'){
        lng *= -1;
      }
    } catch (Exception e){
       System.out.println("latLngStr parse failed");
       return null;
    }

    return new GeodeticCoordinate(lat,lng,0);
  }

  private Point latLngToPoint(double lat, double lng){
    return new Point(lng,lat,SpatialReferences.getWgs84());
  }

  public void moveMarker(int id, double lat, double lng) {
    Point p = latLngToPoint(lat,lng);
    droneMarkers.get(id).setGeometry(p);
  }

  public void moveSightMarker(int id, double lat, double lng) {
    Point p = latLngToPoint(lat,lng);
    sightMarkers.get(id).setGeometry(p);
  }

  public void moveTarget(int id, double lat, double lng) {
    Point p = latLngToPoint(lat,lng);
    targetMarkers.get(id).setGeometry(p);
  }



  public void moveClickedPoint(double lat, double lng) {
    Point p = latLngToPoint(lat,lng);
    clickedPoint.setVisible(true);
    clickedPoint.setGeometry(p);
  }

  public void hideClickedPoint() {
    clickedPoint.setVisible(false);
  }

  public void moveRefPoint(double lat, double lng) {
    Point p = latLngToPoint(lat,lng);
    Point2D markerPos = mapView.locationToScreen(p);
    refPoint.setGeometry(p);
  }

  private void moveMarker(int id, Point graphicPoint) {
    droneMarkers.get(id).setGeometry(graphicPoint);
  }

  void initialiseDroneMarkers(){

    for (DroneColour dc : DroneColour.values()) {
      droneMarkers.add(new Graphic());
    }
    for (DroneColour dc : DroneColour.values()){
      Image newImage = new Image(dc.filePath);
      PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(newImage);

      // set size of the image
      markerSymbol.setHeight(20);
      markerSymbol.setWidth(30);

      // load symbol asynchronously
      markerSymbol.loadAsync();


      // add to the graphic overlay once done loading
      PictureMarkerSymbol finalMarkerSymbol = markerSymbol;
      markerSymbol.addDoneLoadingListener(() -> {
        if (finalMarkerSymbol.getLoadStatus() == LoadStatus.LOADED) {
          droneMarkers.get(dc.id).setSymbol(finalMarkerSymbol);
          markers.getGraphics().add(droneMarkers.get(dc.id));
        } else {
          Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
          alert.show();
        }
      });
    }
  }

  void initialiseTargetMarkers(){

    for (DroneColour dc : DroneColour.values()) {
      targetMarkers.add(new Graphic());
    }
    for (DroneColour dc : DroneColour.values()){
      Image newImage = new Image(dc.targetFilePath);
      PictureMarkerSymbol markerSymbol = new PictureMarkerSymbol(newImage);

      // set size of the image
      markerSymbol.setHeight(20);
      markerSymbol.setWidth(30);

      // load symbol asynchronously
      markerSymbol.loadAsync();


      // add to the graphic overlay once done loading
      PictureMarkerSymbol finalMarkerSymbol = markerSymbol;
      markerSymbol.addDoneLoadingListener(() -> {
        if (finalMarkerSymbol.getLoadStatus() == LoadStatus.LOADED) {
          targetMarkers.get(dc.id).setSymbol(finalMarkerSymbol);
          markers.getGraphics().add(targetMarkers.get(dc.id));
        } else {
          Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
          alert.show();
        }
      });
    }
  }

  /**
   * Download the selected preplanned map area from the list view to a temporary directory. The download job is tracked in another list view.
   */
  @FXML
  private void handleDownloadPreplannedAreaButtonClicked() {
    PreplannedMapArea selectedMapArea = preplannedAreasListView.getSelectionModel().getSelectedItem();
    if (selectedMapArea != null) {
      // hide the preplanned areas and clear the selection
      preplannedAreasListView.getSelectionModel().clearSelection();
      // create default download parameters from the offline map task
      ListenableFuture<DownloadPreplannedOfflineMapParameters> downloadPreplannedOfflineMapParametersFuture = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParametersAsync(selectedMapArea);
      downloadPreplannedOfflineMapParametersFuture.addDoneListener(() -> {
        try {
          DownloadPreplannedOfflineMapParameters downloadPreplannedOfflineMapParameters = downloadPreplannedOfflineMapParametersFuture.get();

          // set the update mode to not receive updates
          downloadPreplannedOfflineMapParameters.setUpdateMode(PreplannedUpdateMode.NO_UPDATES);

          // create a job to download the preplanned offline map to a temporary directory
          //Path path = Files.createTempDirectory(selectedMapArea.getPortalItem().getTitle());
          DownloadPreplannedOfflineMapJob downloadPreplannedOfflineMapJob = offlineMapTask.downloadPreplannedOfflineMap(downloadPreplannedOfflineMapParameters, "offline");
          //System.out.println(path.toFile().getAbsolutePath());
          // start the job
          downloadPreplannedOfflineMapJob.start();
          // track the job in the second list view
          downloadJobsListView.getItems().add(downloadPreplannedOfflineMapJob);

        } catch (InterruptedException | ExecutionException e) {
          new Alert(Alert.AlertType.ERROR, "Failed to generate default parameters for the download job.").show();
        }
      });
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  void terminate() {

    if (mapView != null) {
      mapView.dispose();
    }
  }

  public void setMainStage(Stage s){
    mainStage = s;
  }

  public void loadLastMap(ActionEvent actionEvent) {
    String path = prefs.get(LAST_LOADED_PREF,"");
    if (path != ""){
      try {
        loadMap(path);
      } catch (Exception e){

      }
    } else{
      new Alert(Alert.AlertType.ERROR, "No file recently loaded").show();
    }
  }

  private void  loadMap(String s){
    String type = FilenameUtils.getExtension(s);
    System.out.println(type);
    final Basemap b;
    if (type.equals("vtpk")){
      ArcGISVectorTiledLayer localVectorTiledLayer = new ArcGISVectorTiledLayer(s);
      b = new Basemap(localVectorTiledLayer);
    } else if (type.equals("tpk")){
      TileCache offlineTileCache = new TileCache(s);
      ArcGISTiledLayer reopenedImagedTileLayer = new ArcGISTiledLayer(offlineTileCache);
      b = new Basemap(reopenedImagedTileLayer);
    } else{
      b = null;
    }
    if (b != null){
      loadedMap = new ArcGISMap();
      loadedMap.setBasemap(b);
      mapView.setMap(loadedMap);
      btnDownloadScreen.setDisable(true);
    } else{
      new Alert(Alert.AlertType.ERROR, "File type invalid, use tpk or vtpk").show();
    }
  }

  private void loadOnlineMap(){

  }

  public void loadMapButtonEvent(ActionEvent actionEvent) {
    try {
      FileChooser fileChooser = new FileChooser();
      File workingDirectory = new File(System.getProperty("user.dir"));
      fileChooser.setInitialDirectory(workingDirectory);
      File selectedFile = fileChooser.showOpenDialog(mainStage);
      prefs.put(LAST_LOADED_PREF,selectedFile.getPath());
      loadMap(selectedFile.getPath());

    } catch (Exception e){

    }
  }

  public void toggleSettings(ActionEvent actionEvent) {
      settingsVBox.setVisible(settingsHidden);
      settingsHidden = !settingsHidden;
  }

  public void goMapPointButtonEvent(ActionEvent actionEvent) {
    String text = txtLatLng.getText();
    if(text != ""){
      try {
        String[] latLngStr = text.replaceAll("\\s","").split(",");
        double lat = Double.parseDouble(latLngStr[0]);
        double lng = Double.parseDouble(latLngStr[1]);
        Point p = latLngToPoint(lat,lng);
        if (mapView.getMap() != null){
          mapView.setViewpointCenterAsync(p,10000);
        }
      } catch (Exception e){
        new Alert(Alert.AlertType.ERROR, "lat,lng string invalid").show();
      }
    } else {
      new Alert(Alert.AlertType.ERROR, "Please enter lat,long").show();
    }
  }
}
