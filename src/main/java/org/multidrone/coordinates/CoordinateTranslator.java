package org.multidrone.coordinates;

import org.ejml.simple.SimpleMatrix;

public class CoordinateTranslator {

    final static float e_eccentricity = 0.08181919f;
    final static float e_eccentricity_squared = 0.00669437985f;
    final static float r_ea_semi_major_axis = 6_378_137;
    final static float r_ea_semi_major_axis_squared = (float) Math.pow(r_ea_semi_major_axis,2);
    public static NEDCoordinate GeodeticToNED(GeodeticCoordinate coord, GlobalRefrencePoint ref){
        double lat_r = Math.toRadians(coord.lat);
        double lng_r = Math.toRadians(coord.lng);
        float Ne = getVerticalRadius(lat_r);

        double xe =  ((Ne + coord.height) * Math.cos(lat_r) * Math.cos(lng_r));
        double ye =  ((Ne + coord.height) * Math.cos(lat_r) * Math.sin(lng_r));
        double ze =  ((Ne*(1-e_eccentricity_squared) + coord.height) * Math.sin(lat_r));

        double ref_lat_r = Math.toRadians(ref.lat);
        double ref_lng_r = Math.toRadians(ref.lng);

        final double[][] rotArray = {
                {-Math.sin(ref_lat_r) * Math.cos(ref_lng_r),-Math.sin(ref_lat_r) * Math.sin(ref_lng_r),Math.cos(ref_lat_r)},
                {-Math.sin(ref_lng_r),Math.cos(ref_lng_r),0},
                {-Math.cos(ref_lat_r) * Math.cos(ref_lng_r),-Math.cos(ref_lat_r) * Math.sin(ref_lng_r),-Math.sin(ref_lat_r)}
        };

        double[][] peArray = {
                {xe - ref.xe},
                {ye - ref.ye},
                {ze - ref.ze}
        };

        SimpleMatrix pe = new SimpleMatrix(peArray);
        SimpleMatrix rot = new SimpleMatrix(rotArray);
        SimpleMatrix nedMatrix = rot.mult(pe);




        return new NEDCoordinate(nedMatrix.get(0),nedMatrix.get(1),nedMatrix.get(2));
    }

    public static GeodeticCoordinate nedToGeodetic(NEDCoordinate coord, GlobalRefrencePoint ref){
        double ref_lat_r = Math.toRadians(ref.lat);
        double ref_lng_r = Math.toRadians(ref.lng);

        final double[][] rotArray = {
                {-Math.sin(ref_lat_r) * Math.cos(ref_lng_r),-Math.sin(ref_lat_r) * Math.sin(ref_lng_r),Math.cos(ref_lat_r)},
                {-Math.sin(ref_lng_r),Math.cos(ref_lng_r),0},
                {-Math.cos(ref_lat_r) * Math.cos(ref_lng_r),-Math.cos(ref_lat_r) * Math.sin(ref_lng_r),-Math.sin(ref_lat_r)}
        };

        SimpleMatrix rotInv = (new SimpleMatrix(rotArray)).invert();
        double[][] pnArray = {
                {coord.x},
                {coord.y},
                {coord.z}
        };

        SimpleMatrix pn = new SimpleMatrix(pnArray);
        SimpleMatrix ecefMatrix = rotInv.mult(pn);

        return ecefToGeodetic(ecefMatrix.get(0)+ref.xe, ecefMatrix.get(1)+ref.ye, ecefMatrix.get(2)+ref.ze);

    }

    public static GeodeticCoordinate ecefToGeodetic(double xe, double ye, double ze){
        double x = xe;
        double y = ye;
        double z = ze;

        double b = Math.sqrt( r_ea_semi_major_axis_squared * (1-e_eccentricity_squared) );
        double bsq = Math.pow(b,2);
        double ep = Math.sqrt( (r_ea_semi_major_axis_squared - bsq)/bsq);
        double p = Math.sqrt( Math.pow(x,2) + Math.pow(y,2) );
        double th = Math.atan2(r_ea_semi_major_axis*z, b*p);

        double lon = Math.atan2(y,x);
        double lat = Math.atan2( (z + Math.pow(ep,2)*b*Math.pow(Math.sin(th),3) ), (p - e_eccentricity_squared*r_ea_semi_major_axis*Math.pow(Math.cos(th),3)) );
        double N = r_ea_semi_major_axis/( Math.sqrt(1-e_eccentricity_squared*Math.pow(Math.sin(lat),2)) );
        double alt = p / Math.cos(lat) - N;

        // mod lat to 0-2pi
        lon = lon % (2*Math.PI);

        // correction for altitude near poles left out.

        return new GeodeticCoordinate((float)Math.toDegrees(lat),(float)Math.toDegrees(lon),(float)alt);
    }


    public static BodyCoordinate GeodeticToBody(GeodeticCoordinate coord, GlobalRefrencePoint ref, double yaw){
        NEDCoordinate temp = GeodeticToNED(coord, ref);
        return NEDToBody(temp,yaw);
    }

    //translates a body frame vector into NED frame and adds it
    public static NEDCoordinate addBodyFrameVectorToBase(NEDCoordinate base, float vx, float vy, float vz, float yaw){
        double[][] rotArray = {
                {Math.cos(yaw),-Math.sin(yaw),0},
                {Math.sin(yaw),Math.cos(yaw),0},
                {0,0,1}
        };

        double[][] peArray = {
                {vx},
                {vy},
                {vz}
        };

        SimpleMatrix pe = new SimpleMatrix(peArray);
        SimpleMatrix rot = new SimpleMatrix(rotArray);
        SimpleMatrix nedMatrix = rot.mult(pe);
        return new NEDCoordinate(nedMatrix.get(0) + base.x,nedMatrix.get(1) + base.y,nedMatrix.get(2) + base.z);
    }

    public static BodyCoordinate NEDToBody(NEDCoordinate coord, double yaw){

        double negyaw = -yaw;
        double[][] rotArray = {
                {Math.cos(negyaw),-Math.sin(negyaw),0},
                {Math.sin(negyaw),Math.cos(negyaw),0},
                {0,0,1}
        };

        double[][] peArray = {
                {coord.x},
                {coord.y},
                {coord.z}
        };

        SimpleMatrix pe = new SimpleMatrix(peArray);
        SimpleMatrix rot = new SimpleMatrix(rotArray);
        SimpleMatrix nedMatrix = rot.mult(pe);




        return new BodyCoordinate(nedMatrix.get(0),nedMatrix.get(1),nedMatrix.get(2));
    }

    static float getVerticalRadius(double lat){
        return (float) (r_ea_semi_major_axis / (Math.sqrt(1-(e_eccentricity_squared*Math.pow(Math.sin(lat),2)))));
    }


}
