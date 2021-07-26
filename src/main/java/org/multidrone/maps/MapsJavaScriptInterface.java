package org.multidrone.maps;

import org.multidrone.Main;

public class MapsJavaScriptInterface {

    Main callback;
    public MapsJavaScriptInterface(Main parent) {
        callback = parent;
    }

    public void setClickedPos(String latlng){
        String[] split = latlng.split(",");
        float lat = Float.parseFloat(split[0].substring(1));
        float lng = Float.parseFloat(split[1].substring(0,split[1].length()-1));
        callback.setSelectedCoordinate(lat,lng);
    }
}
