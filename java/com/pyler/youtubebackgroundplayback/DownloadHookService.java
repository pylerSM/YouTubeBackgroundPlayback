package com.pyler.youtubebackgroundplayback;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.lang.Override;
import java.lang.String;

public class DownloadHookService extends IntentService {

    @Override
    public void setIntentRedelivery(boolean enabled) {
        super.setIntentRedelivery(enabled);
    }

    public DownloadHookService() {
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Toast.makeText(getApplicationContext(), "Saving Hooks!", Toast.LENGTH_LONG).show();
        new getHooks().execute("https://raw.githubusercontent.com/pylerSM/YouTubeBackgroundPlayback/master/youtube_hooks.json");


    }

    static String convertStreamToString(InputStream is) throws UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public DownloadHookService(String name) {
        super(name);
    }

    class getHooks extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {
            String responseString = "Nope";
            version = getYTVersion();

            try {
                URL u = new URL(uri[0]);
                URLConnection c = u.openConnection();
                c.connect();

                InputStream inputStream = c.getInputStream();

                responseString = convertStreamToString(inputStream);

                JSONObject jsonObject = new JSONObject(responseString);

                Iterator<?> keys = jsonObject.keys();

                String hookFound = "No";

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.equals(version) && hookFound.equals("No")) {
                        JSONObject hooksObject = jsonObject.getJSONObject(key);
                        String CLASS_1 = hooksObject.getString("CLASS_1");
                        String CLASS_2 = hooksObject.getString("CLASS_2");
                        String CLASS_3 = hooksObject.getString("CLASS_3");

                        String METHOD_1 = hooksObject.getString("METHOD_1");
                        String METHOD_2 = hooksObject.getString("METHOD_2");
                        String METHOD_3 = hooksObject.getString("METHOD_3");

                        String FIELD_1 = hooksObject.getString("FIELD_1");
                        String FIELD_2 = hooksObject.getString("FIELD_2");

                        String SUBFIELD_1 = hooksObject.getString("SUBFIELD_1");

                        String hooks = version + ";" + CLASS_1 + ";" + CLASS_2 + ";" + CLASS_3 + ";" + METHOD_1 + ";" + METHOD_2 + ";" + METHOD_3 + ";" + FIELD_1 + ";" + FIELD_2 + ";" + SUBFIELD_1 + ";";

                        SharedPreferences preferences = getSharedPreferences("youtube", MODE_WORLD_READABLE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("Hooks", hooks);
                        editor.apply();

                        hookFound = "Yes";
                    } else if (!keys.hasNext() && hookFound.equals("No")) {
                        CLASS_1 = "Nope";
                        XposedBridge.log("Could not enable background playback for the YouTube app. Your installed version of it is not supported. Attempting to use latest hooks.");
                    }
                }
            } catch (Exception e) {
                XposedBridge.log("Hook Fetching Error: " + e);
                CLASS_1 = "Nope";
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (!CLASS_1.equals("Nope")) {
                hookYoutube();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}


