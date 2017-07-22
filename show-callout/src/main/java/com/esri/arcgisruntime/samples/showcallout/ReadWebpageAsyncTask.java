package com.esri.arcgisruntime.samples.showcallout;

import android.os.AsyncTask;
import android.text.Html;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by aaro9237 on 7/22/2017.
 */

public class ReadWebpageAsyncTask {

    String result = "";

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String response = "";
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
                        response += s;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            result = result;
        }
    }

    public ReadWebpageAsyncTask() {};

    public void readWebpage(String webAddress) {
        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute(new String[] { webAddress });
    }
}