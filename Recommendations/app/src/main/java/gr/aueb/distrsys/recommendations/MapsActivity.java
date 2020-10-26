package gr.aueb.distrsys.recommendations;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<LatLng> latLngs=new ArrayList<>();
    private ArrayList<Marker> markers=new ArrayList<>();
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alert;
    private ArrayList<Poi> pois;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        pois=(ArrayList<Poi>) getIntent().getSerializableExtra("pois");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        double lat, Long;
        lat=(double) getIntent().getSerializableExtra("lat");
        Long=(double) getIntent().getSerializableExtra("long");
        int radius=(int) getIntent().getSerializableExtra("radius");

        latLngs.add(new LatLng(lat,Long));
        MarkerOptions options=new MarkerOptions();
        options.position(new LatLng(lat,Long));
        options.title("Current Location");
        markers.add(mMap.addMarker(options));

        Location userLoc=new Location("");
        userLoc.setLatitude(latLngs.get(0).latitude);
        userLoc.setLongitude(latLngs.get(0).longitude);

        LatLng userLocation = latLngs.get(0);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(9), 2000, null);

        int i=1;
        for (Poi poi: pois) {

            Location poiLoc=new Location("");
            poiLoc.setLatitude(poi.getLatitude());
            poiLoc.setLongitude(poi.getLongitude());

            if (userLoc.distanceTo(poiLoc)<=radius*1000) {

                LatLng point = new LatLng(poi.getLatitude(), poi.getLongitude());
                latLngs.add(point);
                options = new MarkerOptions();
                options.position(point);
                options.title(poi.getName());

                InfoWindowData info = new InfoWindowData();

                info.setRank("Rank : " + i);
                info.setCategory("Category : " + poi.getCategory());

                info.setDistance("Distance: " + String.format("%.02f", userLoc.distanceTo(poiLoc) / 1000) + " km");

                CustomInfoWindowGoogleMap customInfoWindow = new CustomInfoWindowGoogleMap(this);
                mMap.setInfoWindowAdapter(customInfoWindow);

                Marker m = mMap.addMarker(options);
                m.setTag(info);
                markers.add(m);
                i++;
            }
        }

        markers.get(0).showInfoWindow();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBuilder= new AlertDialog.Builder(MapsActivity.this,R.style.AlertDialogTheme);
                dialogBuilder.setMessage("Create a new query?");
                dialogBuilder.setCancelable(true);

                dialogBuilder.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent newQuery=new Intent(MapsActivity.this, MainActivity.class);
                                startActivity(newQuery);
                                finish();
                            }
                        });

                dialogBuilder.setNegativeButton(
                        "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                alert = dialogBuilder.create();
                alert.show();


            }
        });

        //mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private class CustomInfoWindowGoogleMap implements GoogleMap.InfoWindowAdapter {

        private Context context;

        public CustomInfoWindowGoogleMap(Context ctx){
            context = ctx;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {

            View view = ((Activity)context).getLayoutInflater()
                    .inflate(R.layout.info_window, null);

            TextView name= view.findViewById(R.id.name);
            TextView rank = view.findViewById(R.id.rank);

            TextView category = view.findViewById(R.id.category);
            TextView distance = view.findViewById(R.id.distance);

            name.setText(marker.getTitle());

            if (!marker.getTitle().equals("Current Location")) {
                InfoWindowData infoWindowData = (InfoWindowData) marker.getTag();

                rank.setText(infoWindowData.getRank());
                category.setText(infoWindowData.getCategory());
                distance.setText(infoWindowData.getDistance());

                return view;
            }
            return null;
        }
    }

    private class InfoWindowData {
        private String rank;
        private String category;
        private String distance;

        String getRank() {
            return rank;
        }

        void setRank(String rank) {
            this.rank = rank;
        }

        String getCategory() {
            return category;
        }

        void setCategory(String category) {
            this.category = category;
        }

        String getDistance() {
            return distance;
        }

        void setDistance(String distance) {
            this.distance = distance;
        }
    }
}

