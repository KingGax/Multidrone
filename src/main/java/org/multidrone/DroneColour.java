package org.multidrone;

public enum DroneColour {

    GREEN(DroneIDs.GREEN_ID),
    RED(DroneIDs.RED_ID),
    YELLOW(DroneIDs.YELLOW_ID),
    BLUE(DroneIDs.BLUE_ID),
    PINK(DroneIDs.PINK_ID);


    public final int id;
    public final String filePath;


    private DroneColour(int id) {
        this.id = id;
        String root = "download_preplanned_map/mapres/drones/";
        switch (id){
            case DroneIDs.GREEN_ID:
                filePath = root + "greenptr.png";
                break;
            case DroneIDs.RED_ID:
                filePath = root + "redptr.png";
                break;
            default:
                filePath = root + "defaultptr.png";
        }
    }

    public static DroneColour valueOfID(int id) {
        for (DroneColour e : values()) {
            if (e.id==id) {
                return e;
            }
        }
        return null;
    }
}
