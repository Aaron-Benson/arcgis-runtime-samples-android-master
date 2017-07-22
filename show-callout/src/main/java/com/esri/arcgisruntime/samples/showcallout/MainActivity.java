/* Copyright 2016 Esri
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

package com.esri.arcgisruntime.samples.showcallout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final String sTag = "Gesture";
    private MapView mMapView;
    private Callout mCallout;

    private double userLocX;
    private double userLocY;

    private final SpatialReference wgs84 = SpatialReference.create(4326);


    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};

    private class DownloadWebPageTask extends AsyncTask<String, Void, double[]> {

        @Override
        protected double[] doInBackground(String... urls) {
            double[] response = new double[2];
            String htmlContent = "";
            for (String url : urls) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                try {
                    HttpResponse execute = client.execute(httpGet);
                    InputStream content = execute.getEntity().getContent();

                    BufferedReader buffer = new BufferedReader(
                            new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        htmlContent += s;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int beginIndexX = htmlContent.indexOf("XMin: ") + 6;
            int endIndexX = beginIndexX + 1;
            while (Character.isDigit(htmlContent.charAt(endIndexX)) || htmlContent.charAt(endIndexX) == '-' || htmlContent.charAt(endIndexX) == '.') {endIndexX++;}
            int beginIndexY = htmlContent.indexOf("YMin: ") + 6;
            int endIndexY = beginIndexY + 1;
            while (Character.isDigit(htmlContent.charAt(endIndexY)) || htmlContent.charAt(endIndexY) == '-' || htmlContent.charAt(endIndexY) == '.') {endIndexY++;}
            return new double[] { Double.parseDouble(htmlContent.substring(beginIndexX, endIndexX)), Double.parseDouble(htmlContent.substring(beginIndexY, endIndexY)) };
        }
        @Override
        protected void onPostExecute(double[] result) {
            double distance = distanceCheckUnicorn(userLocX, userLocY, result[0], result[1]);
            int hi = 0;
        }

        private double distanceCheckUnicorn(double latUser, double longUser, double latUnicorn, double longUnicorn) {
            double distance = Math.sqrt(Math.pow(latUnicorn - latUser, 2) + Math.pow(longUnicorn - longUser, 2));
            distance *= 111.325f * 1000; //Gets distance in meters
            return distance;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate MapView from layout
        mMapView = (MapView) findViewById(R.id.mapView);
        // create a map with the Basemap Type topographic
        final ArcGISMap mMap = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 34.0565, -117.195800, 18);
        //mMap = new ArcGISMap(Basemap.createImagery());
        // set the map to be displayed in this view
        mMapView.setMap(mMap);

        // If an error is found, handle the failure to start.
        // Check permissions to see if failure may be due to lack of permissions.
        boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) ==
                PackageManager.PERMISSION_GRANTED;
        boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) ==
                PackageManager.PERMISSION_GRANTED;

        if (!(permissionCheck1 && permissionCheck2)) {
            // If permissions are not already granted, request permission from the user.
            ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
        }

        final ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable("https://services8.arcgis.com/DnMrXNZ4mQTjDjkz/ArcGIS/rest/services/Unicorn/FeatureServer/0");
        final FeatureLayer mFeatureLayer = new FeatureLayer(serviceFeatureTable);
        mMap.getOperationalLayers().add(mFeatureLayer);

        final Handler h = new Handler();
        final int delay = 3000; //milliseconds

        h.postDelayed(new Runnable(){
            public void run(){
                // Get user location
                mMapView.getLocationDisplay().setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                if (!mMapView.getLocationDisplay().isStarted())
                    mMapView.getLocationDisplay().startAsync();
                userLocX = mMapView.getLocationDisplay().getLocation().getPosition().getX();
                userLocY = mMapView.getLocationDisplay().getLocation().getPosition().getY();

                readWebpage("https://services8.arcgis.com/DnMrXNZ4mQTjDjkz/ArcGIS/rest/services/Unicorn/FeatureServer/0");
                h.postDelayed(this, delay);
            }
        }, delay);
    }



    @Override
    protected void onPause(){
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }



    private double distanceCheckUnicorn(double latUser, double longUser, double latUnicorn, double longUnicorn) {
        double distance = Math.sqrt(Math.pow(latUnicorn - latUser, 2) + Math.pow(longUnicorn - longUser, 2));
        distance *= 111.325f * 1000; //Gets distance in meters
        return distance;
    }

    public void readWebpage(String webAddress) {
        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute(new String[] { webAddress });
    }

}

/*
    android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
            Math.round(motionEvent.getY()));
    // create a map point from screen point
    Point mapPoint = mMapView.screenToLocation(screenPoint);
    // convert to WGS84 for lat/lon format
    Point wgs84Point = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
    // create a textview for the callout
    TextView calloutContent = new TextView(getApplicationContext());
                calloutContent.setTextColor(Color.BLACK);
                        calloutContent.setSingleLine();
                        // format coordinates to 4 decimal places
                        calloutContent.setText("Lat: " +  String.format("%.4f", wgs84Point.getY()) +
                        ", Lon: " + String.format("%.4f", wgs84Point.getX()));

                        // get callout, set content and show
                        mCallout = mMapView.getCallout();
                        mCallout.setLocation(mapPoint);
                        mCallout.setContent(calloutContent);
                        mCallout.show();*/
