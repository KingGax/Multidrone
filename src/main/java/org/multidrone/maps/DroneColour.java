package org.multidrone.maps;

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
    public final String colourStr;
    public final String colourClass;

    private DroneColour(int id) {
        this.id = id;
        String root = "download_preplanned_map/mapres/drones/";
        switch (id){
            case DroneIDs.GREEN_ID:
                filePath = root + "greenptr.png";
                colourStr = "GREEN";
                colourClass = "panel_green";
                break;
            case DroneIDs.RED_ID:
                filePath = root + "redptr.png";
                colourStr = "RED";
                colourClass = "panel_red";
                break;
            case DroneIDs.PINK_ID:
                filePath = root + "pinkptr.png";
                colourStr = "PINK";
                colourClass = "panel_pink";
                break;
            case DroneIDs.YELLOW_ID:
                filePath = root + "yellowptr.png";
                colourStr = "YELLOW";
                colourClass = "panel_yellow";
                break;
            case DroneIDs.BLUE_ID:
                filePath = root + "blueptr.png";
                colourStr = "BLUE";
                colourClass = "panel_blue";
                break;
            case DroneIDs.BLACK_ID:
                filePath = root + "blackptr.png";
                colourStr = "BLACK";
                colourClass = "panel_black";
                break;
            case DroneIDs.WHITE_ID:
                filePath = root + "whiteptr.png";
                colourStr = "WHITE";
                colourClass = "panel_white";
                break;
            case DroneIDs.MAROON_ID:
                filePath = root + "maroonptr.png";
                colourStr = "MAROON";
                colourClass = "panel_maroon";
                break;
            case DroneIDs.PURPLE_ID:
                filePath = root + "purpleptr.png";
                colourStr = "PURPLE";
                colourClass = "panel_purple";
                break;
            case DroneIDs.CYAN_ID:
                filePath = root + "cyanptr.png";
                colourStr = "CYAN";
                colourClass = "panel_cyan";
                break;
            default:
                filePath = root + "defaultptr.png";
                colourStr = "???";
                colourClass = "panel_grey";
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
