package gr.aueb.distrsys.recommendations;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ReadPOIs
{
    private ArrayList<Poi> poiList=new ArrayList<Poi>();
    @SuppressWarnings("unchecked")
    public void readPois()
    {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();


        try
        {
            FileReader reader = new FileReader("src\\main\\resources\\POIs.json");
            //Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONObject JsonObj=(JSONObject) obj;
            JSONArray POIsList = new JSONArray();
            POIsList.add(JsonObj);


            //Iterate over pois array

            for(int i=0;i<1692;i++){
                parsePoiObject((JSONObject)POIsList.get(0),i);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
      
    }

    private void parsePoiObject(JSONObject poi,int i)
    {
        //Get poi object within list
        JSONObject PoiObject = (JSONObject) poi.get(String.valueOf(i));

        String POI = (String) PoiObject.get("POI");
        Object latidude = PoiObject.get("latidude");
        Object longitude = PoiObject.get("longitude");
        String photos = (String) PoiObject.get("photos");
        String POI_category_id = (String) PoiObject.get("POI_category_id");
        String POI_name = (String) PoiObject.get("POI_name");

        Poi p =new Poi(i,POI_name,(Double)latidude,(Double)longitude,POI_category_id,photos,POI);
        poiList.add(p);
    }

    public ArrayList<Poi> getPoiList() {
        return poiList;
    }
}

