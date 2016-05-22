package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage {

	public static final String APP_PACKAGE = "com.google.android.youtube";

	public static String CLASS_1 = "com.google.android.libraries.youtube.player.background.BackgroundTransitioner";
	public static String METHOD_1 = "updateBackgroundService";
	public static String FIELD_1 = "playbackModality";
	public static String SUBFIELD_1 = "isInBackground";

	public static String CLASS_2 = "com.google.android.libraries.youtube.innertube.model.PlayabilityStatusModel";
	public static String METHOD_2 = "isPlayable";
	public static String FIELD_2 = "isBackgroundable" ;

	public static String CLASS_3 = "com.google.android.apps.youtube.app.background.BackgroundSettings";
	public static String METHOD_3 = "getBackgroundAudioSetting";

	public static String CLASS_4;
	public static String METHOD_4;

	public static String CLASS_5;
	public static String METHOD_5;


    	int checkVersion;

    	ClassLoader loader;
    	Context nContext;
	String version;

	class getHooks extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... uri) {
			String responseString = "Nope";

			try {
				URL u = new URL(uri[0]);
				URLConnection c = u.openConnection();
				c.connect();

				InputStream inputStream = c.getInputStream();

				responseString = convertStreamToString(inputStream);

                		JSONObject jsonObject = new JSONObject(responseString);
                		//Need To Get Versions From JSON To Compare To Version Installed

			} catch (Exception e) {
                		XposedBridge.log("Hook Fetching Error: " +e);
			}

			return responseString;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
            		hooksYoutube();
		}
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws PackageManager.NameNotFoundException {
		if (!lpparam.packageName.equals(APP_PACKAGE)) return;

		loader = lpparam.classLoader;

        	// Thank you to KeepChat For the Following Code Snippet
        	// http://git.io/JJZPaw
        	Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        	nContext = (Context) callMethod(activityThread, "getSystemContext");

        	version = String.valueOf(nContext.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionCode);
        	//End Snippet

		//Add Check To See If We Have Latest Hooks (XSharedPreferences)

        	checkVersion = getVersionIndex(loader);

        	if (checkVersion == 1) {
            	new getHooks().execute("https://raw.githubusercontent.com/pylerSM/YouTubeBackgroundPlayback/master/youtube_hooks.json");
        	}	
	}

    	void hooksYoutube () {
        	try {
            	// hooks
            	findAndHookMethod(CLASS_1, loader, METHOD_1, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) {
                    setBooleanField(getObjectField(param.thisObject, FIELD_1), SUBFIELD_1, true);
                }
	        });

            	findAndHookMethod(CLASS_2, loader, METHOD_2, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) {
                    setBooleanField(param.thisObject, FIELD_2, true);
                }
            	});

            	findAndHookMethod(CLASS_3, loader, METHOD_3, returnConstant("on"));

            	if (checkVersion == 1) {
                	// hook specific methods for unobfuscated releases
                	findAndHookMethod(CLASS_4, loader, METHOD_4, returnConstant(true));
                	findAndHookMethod(CLASS_5, loader, METHOD_5, returnConstant(true));
            	}
        	} catch (Exception e) {
        	}
    	}

	// returns 0 for unobfuscated code and positive integer for obfuscated
	private int getVersionIndex(final ClassLoader loader) throws PackageManager.NameNotFoundException {
		try {
			// check if the app is unobfuscated
			loader.loadClass(CLASS_1);
			return 0;
		} catch (Exception e) {
			return 1;
		}
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

}
