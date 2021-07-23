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

package com.esri.samples.download_preplanned_map;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.internal.util.FilePathUtil;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DownloadPreplannedMapController {

  @FXML private ListView<PreplannedMapArea> preplannedAreasListView;
  @FXML private ListView<DownloadPreplannedOfflineMapJob> downloadJobsListView;
  @FXML private MapView mapView;
  @FXML private Button downloadButton;
  @FXML private Button btnLoad;

  private GraphicsOverlay markers;

  private ArcGISMap onlineMap;
  private GraphicsOverlay areasOfInterestGraphicsOverlay;
  private OfflineMapTask offlineMapTask;
  private List<PreplannedMapArea> preplannedMapAreas; // keep loadable in scope to avoid garbage collection

  private List<Graphic> droneMarkers = new ArrayList<>();

  private PictureMarkerSymbol greenDrone;

  private ArcGISMap loadedMap;

  private Stage mainStage;


  @FXML
  private void initialize() {
    try {

      // create a portal to ArcGIS Online
      Portal portal = new Portal("https://www.arcgis.com/");

      // set the authentication manager to handle OAuth challenges when accessing the portal
      AuthenticationManager.setAuthenticationChallengeHandler(new DefaultAuthenticationChallengeHandler());

      System.out.println("iintialising");
      // create a portal item using the portal and the item id of a map service
      PortalItem portalItem = new PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674");
      System.out.println("loadeded");
      // create a map with the portal item
      onlineMap = new ArcGISMap(portalItem);


      // show the map
      mapView.setMap(onlineMap);


      //add onclick
      mapView.setOnMouseClicked(e -> {
        System.out.println(e.getX() + " " + e.getX() + " " + e.getY());
        Point p = mapView.screenToLocation(new Point2D(e.getX(),e.getY()));

        String latLonDecimalDegrees = CoordinateFormatter.toLatitudeLongitude(p, CoordinateFormatter
                .LatitudeLongitudeFormat.DECIMAL_DEGREES, 7);

        System.out.println( latLonDecimalDegrees);
        moveMarker(0, p);
      });

      // create a graphics overlay to show the preplanned map areas extents (areas of interest)
      areasOfInterestGraphicsOverlay = new GraphicsOverlay();
      mapView.getGraphicsOverlays().add(areasOfInterestGraphicsOverlay);

      markers = new GraphicsOverlay();
      initialiseDroneMarkers();
      mapView.getGraphicsOverlays().add(markers);


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

  private Graphic sym = null;

  private void moveMarker(int id, Point graphicPoint) {
    droneMarkers.get(id).setGeometry(graphicPoint);

    /*if (sym == null){
      Image newImage = new Image("download_preplanned_map/mapres/drones/greenptr.png");
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
          //Graphic symbolGraphic =
          sym = new Graphic(graphicPoint, finalMarkerSymbol);
          markers.getGraphics().add(sym);
        } else {
          Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
          alert.show();
        }
      });
    } else{
      sym.setGeometry(graphicPoint);
    }*/


  }

  void initialiseDroneMarkers(){

    for (DroneColour dc : DroneColour.values()) {
      droneMarkers.add(new Graphic());
    }
    for (DroneColour dc : DroneColour.values()){
      System.out.println(dc.id);
      System.out.println(dc.filePath);
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

  public void loadMap(ActionEvent actionEvent) {
    FileChooser fileChooser = new FileChooser();
    File selectedFile = fileChooser.showOpenDialog(mainStage);
    //Portal portal = new Portal("");
    //portal.setLoginRequired(false);
    //PortalItem portalItem = new PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674");
    //Geodatabase geo = new Geodatabase(selectedFile.getPath());
    //geo.loadAsync();
    TileCache offlineTileCache = new TileCache(selectedFile.getPath());
    ArcGISTiledLayer reopenedImagedTileLayer = new ArcGISTiledLayer(offlineTileCache);
    loadedMap = new ArcGISMap();
    final Basemap b = new Basemap(reopenedImagedTileLayer);
    loadedMap.setBasemap(b);
    mapView.setMap(loadedMap);
    // create feature layer from geodatabase and add to the map#

    /*geo.addDoneLoadingListener(() -> {
      if (geo.getLoadStatus() == LoadStatus.LOADED) {
        // access the geodatabase's feature table Trailheads
        GeodatabaseFeatureTable geodatabaseFeatureTable = geo.getGeodatabaseFeatureTable("Trailheads");
        geodatabaseFeatureTable.loadAsync();
        // create a layer from the geodatabase feature table and add to map
        final FeatureLayer featureLayer = new FeatureLayer(geodatabaseFeatureTable);
        featureLayer.addDoneLoadingListener(() -> {
          if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
            // set viewpoint to the feature layer's extent
            mapView.setViewpointAsync(new Viewpoint(featureLayer.getFullExtent()));
          } else {
            System.out.println("Feature failed to load");
          }
        });
        // add feature layer to the map
        mapView.getMap().getOperationalLayers().add(featureLayer);
      } else {
        System.out.println("Geobase failed to load!");
      }
    });*/

  }
}
