package gr.aueb.distrsys.recommendations;

import java.io.Serializable;

public class Poi implements Serializable {
    private int id;
    private String name,category,photos,poi_code;
    private double latitude,longitude;

    public Poi(){}

    public Poi(int id,String name,double latitude,double longitude,String category,String photos,String poi_code){
        this.id=id;
        this.name=name;
        this.latitude=latitude;
        this.longitude=longitude;
        this.category=category;
        this.photos=photos;
        this.poi_code=poi_code;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public String getCategory(){
        return category;
    }

    public String getPhotos() {
        return photos;
    }

    public String getPoi_code() {
        return poi_code;
    }
}
