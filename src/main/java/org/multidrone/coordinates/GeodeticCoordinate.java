package org.multidrone.coordinates;

public class GeodeticCoordinate {
    public double lat;
    public double lng;
    public double height;

    public GeodeticCoordinate(){

    }

    public GeodeticCoordinate(double _lat, double _lng, double _height){
        lat = _lat;
        lng = _lng;
        height = _height;
    }

    @Override
    public String toString() {
        return "GEO{"+lat+ ", " + lng + ", " + height + "}";
    }
}
