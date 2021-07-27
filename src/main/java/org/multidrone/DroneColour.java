package org.multidrone;

public enum DroneColour {

    GREEN(DroneIDs.GREEN_ID),
    RED(DroneIDs.RED_ID),
    YELLOW(DroneIDs.YELLOW_ID),
    BLUE(DroneIDs.BLUE_ID),
    PINK(DroneIDs.PINK_ID),
    PURPLE(DroneIDs.PURPLE_ID),
    WHITE(DroneIDs.WHITE_ID),
    BLACK(DroneIDs.BLACK_ID),
    MAROON(DroneIDs.MAROON_ID),
    CYAN(DroneIDs.CYAN_ID);


    public final int id;
    public final String filePath;
    public final String targetFilePath;


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
            case DroneIDs.PINK_ID:
                filePath = root + "pinkptr.png";
                break;
            case DroneIDs.YELLOW_ID:
                filePath = root + "yellowptr.png";
                break;
            case DroneIDs.BLUE_ID:
                filePath = root + "blueptr.png";
                break;
            case DroneIDs.BLACK_ID:
                filePath = root + "blackptr.png";
                break;
            case DroneIDs.WHITE_ID:
                filePath = root + "whiteptr.png";
                break;
            case DroneIDs.MAROON_ID:
                filePath = root + "maroonptr.png";
                break;
            case DroneIDs.PURPLE_ID:
                filePath = root + "purpleptr.png";
                break;
            case DroneIDs.CYAN_ID:
                filePath = root + "cyanptr.png";
                break;
            default:
                filePath = root + "defaultptr.png";
        }

        String targetRoot = "download_preplanned_map/mapres/targets/";
        switch (id){
            case DroneIDs.GREEN_ID:
                targetFilePath = targetRoot + "greentarget.png";
                break;
            case DroneIDs.RED_ID:
                targetFilePath = targetRoot + "redtarget.png";
                break;
            case DroneIDs.PINK_ID:
                targetFilePath = targetRoot + "pinktarget.png";
                break;
            case DroneIDs.YELLOW_ID:
                targetFilePath = targetRoot + "yellowtarget.png";
                break;
            case DroneIDs.BLUE_ID:
                targetFilePath = targetRoot + "bluetarget.png";
                break;
            case DroneIDs.BLACK_ID:
                targetFilePath = targetRoot + "blacktarget.png";
                break;
            case DroneIDs.WHITE_ID:
                targetFilePath = targetRoot + "whitetarget.png";
                break;
            case DroneIDs.MAROON_ID:
                targetFilePath = targetRoot + "maroontarget.png";
                break;
            case DroneIDs.PURPLE_ID:
                targetFilePath = targetRoot + "purpletarget.png";
                break;
            case DroneIDs.CYAN_ID:
                targetFilePath = targetRoot + "cyantarget.png";
                break;
            default:
                targetFilePath = targetRoot + "defaulttarget.png";
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
