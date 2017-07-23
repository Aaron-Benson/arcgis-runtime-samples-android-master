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
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
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
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import au.com.bytecode.opencsv.CSVReader;


public class MainActivity extends AppCompatActivity {

    private static final String sTag = "Gesture";
    private MapView mMapView;
    private Callout mCallout;
    private FeatureLayer mFeatureLayer;
    private boolean mFeatureSelected = false;
    private ArcGISFeature mIdentifiedFeature;

    private int width = 0;
    private int height = 0;

    private double userLocX;
    private double userLocY;

    private ImageView imageView;
    private ImageView pointerView;
    private float pointerViewInitLocation;

    private final SpatialReference wgs84 = SpatialReference.create(4326);
    final Context context = this;

    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};

    private class DownloadWebPageTask extends AsyncTask<String, Void, double[]> {

        @Override
        protected double[] doInBackground(String... urls) {
            double[] response = new double[2];
            /*String htmlContent = "";
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
            while (Character.isDigit(htmlContent.charAt(endIndexY)) || htmlContent.charAt(endIndexY) == '-' || htmlContent.charAt(endIndexY) == '.') {endIndexY++;}*/
            //return new double[] { Double.parseDouble(htmlContent.substring(beginIndexX, endIndexX)), Double.parseDouble(htmlContent.substring(beginIndexY, endIndexY)) };
            return response;
        }
        @Override
        protected void onPostExecute(double[] result) {
            double distance = 0;

            QueryParameters query = new QueryParameters();
            query.setWhereClause("OBJECTID = 111");
            query.setReturnGeometry(true);
            query.setOutSpatialReference(wgs84);
            final ListenableFuture<FeatureQueryResult> queryResult = mFeatureLayer.getFeatureTable().queryFeaturesAsync(query);
            queryResult.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // call get on the future to get the result
                        FeatureQueryResult result = queryResult.get();
                        Feature feature = result.iterator().next();
                        Point p = (Point) feature.getGeometry();
                        double x = p.getX();
                        double y = p.getY();
                        UpdateDistanceBar(distanceCheckUnicorn(userLocX, userLocY, x, y));
                        //Toast.makeText(getApplicationContext(), "Distance: " + Double.toString(distance), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {}

                }
            });

            if (!mFeatureSelected) {

               // mFeatureLayer.setDefinitionExpression("OBJECTID == 107");
                android.graphics.Point screenCoordinate = new android.graphics.Point(Math.round(width / 2), Math.round(height / 2));
                double tolerance = 30;
                //Identify Layers to find features
                final ListenableFuture<IdentifyLayerResult> identifyFuture = mMapView.identifyLayerAsync(mFeatureLayer, screenCoordinate, tolerance, false, 1);
                identifyFuture.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // call get on the future to get the result
                            IdentifyLayerResult layerResult = identifyFuture.get();
                            List<GeoElement> resultGeoElements = layerResult.getElements();

                            if (resultGeoElements.size() > 0) {
                                if (resultGeoElements.get(0) instanceof ArcGISFeature) {
                                    mIdentifiedFeature = (ArcGISFeature) resultGeoElements.get(0);
                                    //Select the identified feature
                                    mFeatureLayer.selectFeature(mIdentifiedFeature);
                                    mFeatureSelected = true;

                                    // begin unicorn
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setPositiveButton("OK",null);
                                    final AlertDialog dialog = builder.create();
                                    LayoutInflater inflater = getLayoutInflater();
                                    View dialogLayout = inflater.inflate(R.layout.found_unicorn_alert, null);
                                    dialog.setView(dialogLayout);
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                    dialog.show();
                                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                                        // @Override
                                        public void onShow(DialogInterface d) {
                                            ImageView image = (ImageView) dialog.findViewById(R.id.foundUnicornImage);
                                            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.found_unicorn_image);
                                            float imageWidthInPX = (float)image.getWidth();
                                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Math.round(imageWidthInPX),
                                                    Math.round(imageWidthInPX * (float)icon.getHeight() / (float)icon.getWidth()));
                                            image.setLayoutParams(layoutParams);
                                        }
                                    });
                                    // end unicorn image popup

                                    // message on top of
                                    Toast.makeText(getApplicationContext(), "You found Fire, the Unicorn!!!", Toast.LENGTH_LONG).show();

                                }
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            Log.e(getResources().getString(R.string.app_name), "Update feature failed: " + e.getMessage());
                        }
                    }
                });
            } else {
                //Point movedPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(width), Math.round(height)));
                Point movedPoint = generateNewUnicornPoint();
                final Point normalizedPoint = (Point) GeometryEngine.normalizeCentralMeridian(movedPoint);
                mIdentifiedFeature.addDoneLoadingListener(new Runnable() {
                    @Override
                    public void run() {
                        mIdentifiedFeature.setGeometry(normalizedPoint);
                        final ListenableFuture<Void> updateFuture = mFeatureLayer.getFeatureTable().updateFeatureAsync(mIdentifiedFeature);
                        updateFuture.addDoneListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // track the update
                                    updateFuture.get();
                                    // apply edits once the update has completed
                                    if (updateFuture.isDone()) {
                                        applyEditsToServer();
                                        mFeatureLayer.clearSelection();
                                        mFeatureSelected = false;
                                    } else {
                                        Log.e(getResources().getString(R.string.app_name), "Update feature failed");
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    Log.e(getResources().getString(R.string.app_name), "Update feature failed: " + e.getMessage());
                                }
                            }
                        });
                    }
                });
                mIdentifiedFeature.loadAsync();
            }
            //if (distance < 4)
            //    Toast.makeText(getApplicationContext(), "You found the unicorn!!!", Toast.LENGTH_SHORT).show();

        }

        private double distanceCheckUnicorn(double latUser, double longUser, double latUnicorn, double longUnicorn) {
            double distance = Math.sqrt(Math.pow(latUnicorn - latUser, 2) + Math.pow(longUnicorn - longUser, 2));
            distance *= 111.325f * 1000; //Gets distance in meters
            return distance;
        }
    }

    private void applyEditsToServer() {
        final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = ((ServiceFeatureTable) mFeatureLayer.getFeatureTable()).applyEditsAsync();
        applyEditsFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // get results of edit
                    List<FeatureEditResult> featureEditResultsList = applyEditsFuture.get();
                    if (!featureEditResultsList.get(0).hasCompletedWithErrors()) {
                        Toast.makeText(getApplicationContext(), "Applied Geometry Edits to Server. ObjectID: " + featureEditResultsList.get(0).getObjectId(), Toast.LENGTH_SHORT).show();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(getResources().getString(R.string.app_name), "Update feature failed: " + e.getMessage());
                }
            }
        });
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

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(getApplicationContext(), mMapView) {
            @Override
            public boolean onRotate(MotionEvent event, double rotationAngle) {
                return false;
            }

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return false;
            }

        });
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
        mFeatureLayer = new FeatureLayer(serviceFeatureTable);
        mMap.getOperationalLayers().add(mFeatureLayer);

        final Handler h = new Handler();
        final int delay = 3000; //milliseconds

        h.postDelayed(new Runnable(){
            public void run(){
                // Get user location
                mMapView.getLocationDisplay().setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
                mMapView.getLocationDisplay().setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);

                if (!mMapView.getLocationDisplay().isStarted())
                    mMapView.getLocationDisplay().startAsync();
                userLocX = mMapView.getLocationDisplay().getLocation().getPosition().getX();
                userLocY = mMapView.getLocationDisplay().getLocation().getPosition().getY();

                readWebpage("https://services8.arcgis.com/DnMrXNZ4mQTjDjkz/ArcGIS/rest/services/Unicorn/FeatureServer/0");
                h.postDelayed(this, delay);
            }
        }, delay);

        imageView = (ImageView) findViewById(R.id.outside_imageview);
        imageView.setImageResource(R.drawable.lvl9);
        pointerView = (ImageView) findViewById(R.id.pointer_imageview);
        pointerView.setImageResource(R.drawable.pointer);
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        pointerViewInitLocation = pointerView.getX();
        params.width=120*9;
        imageView.setLayoutParams(params);
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
        width = mMapView.getWidth();
        height = mMapView.getHeight();
        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute(new String[] { webAddress });
    }

    private void UpdateDistanceBar(double distance){

        if (distance < 25) {
            pointerView.setX(pointerViewInitLocation + 120 * 8);
        } else if (distance < 50) {
            pointerView.setX(pointerViewInitLocation + 120 * 7);
        } else if (distance < 75) {
            pointerView.setX(pointerViewInitLocation + 120 * 6);
        } else if (distance < 100) {
            pointerView.setX(pointerViewInitLocation + 120 * 5);
        } else if (distance < 125) {
            pointerView.setX(pointerViewInitLocation + 120 * 4);
        } else if (distance < 150) {
            pointerView.setX(pointerViewInitLocation + 120 * 3);
        } else if (distance < 175) {
            pointerView.setX(pointerViewInitLocation + 120 * 2);
        } else if (distance < 200) {
            pointerView.setX(pointerViewInitLocation + 120);
        } else {
            pointerView.setX(pointerViewInitLocation);
        }
        Toast.makeText(getApplicationContext(), "Distance: " + Double.toString(distance), Toast.LENGTH_SHORT).show();
    }

    private Point generateNewUnicornPoint() {
        String next[] = {};
        String[] cla = null;

        //int rand = new Random(35).nextInt();
        int rand = (int)(Math.random() * 34 + 1);

        try {
            CSVReader reader = new CSVReader(new InputStreamReader(getAssets().open("unicorn_locations.csv")));
            int i = 0;
            while(true && i < rand) {
                next = reader.readNext();
                if(next != null) {
                    cla = next;
                } else {
                    break;
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Point(Double.parseDouble(cla[0]), Double.parseDouble(cla[1]), wgs84);
    }

}