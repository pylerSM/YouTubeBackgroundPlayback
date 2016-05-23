package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage, IXposedHookZygoteInit {

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
	public static String METHOD_4 = "shouldShowBackgroundAudioSettingsDialog";

	public static String CLASS_4 = "com.google.android.libraries.youtube.common.util.PackageUtil";
	public static String METHOD_5 = "isDogfoodOrDevBuild";


    	int checkVersion;
    	ClassLoader loader;
    	Context nContext;
	String version;
	static XSharedPreferences mPreferences;

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

                		Iterator<?> keys = jsonObject.keys();

                		String hookFound = "No";

                		while (keys.hasNext()) {
                    			String key = (String) keys.next();
                    			System.out.println("Info: " +key);
                    			if (key.equals(version) && hookFound.equals("No")) {
                        			JSONObject hooksObject = jsonObject.getJSONObject(key);
                        			CLASS_1 = hooksObject.getString("CLASS_1");
                        			CLASS_2 = hooksObject.getString("CLASS_2");
                        			CLASS_3 = hooksObject.getString("CLASS_3");

                        			METHOD_1 = hooksObject.getString("METHOD_1");
                        			METHOD_2 = hooksObject.getString("METHOD_1");
                        			METHOD_3 = hooksObject.getString("METHOD_1");

                        			FIELD_1 = hooksObject.getString("FIELD_1");
                        			FIELD_2 = hooksObject.getString("FIELD_1");

                        			SUBFIELD_1 = hooksObject.getString("SUBFIELD_1");
                        
                        			//Need To Double Check This Method!
                        			Intent intent=new Intent();
									intent.setAction("com.pyler.youtubebackgroundplayback.HOOKS");
									intent.putExtra("Hooks", hooksObject.toString());
									nContext.sendBroadcast(intent);
                        
                        			hookFound = "Yes";
                    			} else if (!keys.hasNext() && hookFound.equals("No")) {
                        			CLASS_1 = "Nope";
                        			XposedBridge.log("Could not enable background playback for the YouTube app. Your installed version of it is not supported. Attempting to use latest hooks.");
                    			}
        	 		}
			} catch (Exception e) {
                		XposedBridge.log("Hook Fetching Error: " +e);
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
	public void handleLoadPackage(final LoadPackageParam lpparam) throws PackageManager.NameNotFoundException {
		if (!lpparam.packageName.equals(APP_PACKAGE)) return;

		loader = lpparam.classLoader;

        	// Thank you to KeepChat For the Following Code Snippet
        	// http://git.io/JJZPaw
        	Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        	nContext = (Context) callMethod(activityThread, "getSystemContext");

        	version = String.valueOf(nContext.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionCode / 1000);
        	//End Snippet

		mPreferences.makeWorldReadable();
		
        	try {
        		if (mPreferences.getString("CLASS_1", null) != null); {
        			//Assign hooks to hooks saved 
        			hookYoutube();
        		}
        	} catch (Exception e) {
        		//Update!
        		checkVersion();
        	}
	}

	void checkVersion() {
		try {
			checkVersion = getVersionIndex(loader);

			if (checkVersion == 1) {
				new getHooks().execute("https://raw.githubusercontent.com/pylerSM/YouTubeBackgroundPlayback/1e2f97422afc09eea4a67c615870480a9bc54ec7/youtube_hooks.json");
			}
		} catch (Exception e) {
		}
		}

    	void hookYoutube () {
            	// hooks
       	    	try {
                	findAndHookMethod(CLASS_1, loader, METHOD_1, new XC_MethodHook() {
                    	@Override
                    	protected void beforeHookedMethod(final MethodHookParam param) {
                        	XposedBridge.log("Hooked!");
                        	setBooleanField(getObjectField(param.thisObject, FIELD_1), SUBFIELD_1, true);
                    	}
                	});
            	} catch (Exception e) {
                	XposedBridge.log("YTBP First Hook - " +e);
            	}

            	try {
            		findAndHookMethod(CLASS_2, loader, METHOD_2, new XC_MethodHook() {
                	@Override
                	protected void beforeHookedMethod(final MethodHookParam param) {
                	    setBooleanField(param.thisObject, FIELD_2, true);
                	}
            		});
            	} catch (Exception e) {
                XposedBridge.log("YTBP Second Hook - " +e);
            	}

            	try {
            		findAndHookMethod(CLASS_3, loader, METHOD_3, returnConstant("on"));
            	} catch (Exception e) {
                	XposedBridge.log("YTBP Third Hook - " +e);
            	}

            	if (checkVersion == 0) {
                	// hook specific methods for unobfuscated releases
                	try {
                		findAndHookMethod(CLASS_3, loader, METHOD_4, returnConstant(true));
                	} catch (Exception e) {
                		XposedBridge.log("YTBP Forth Hooks - " +e);
                	}

                	try {
                	findAndHookMethod(CLASS_4, loader, METHOD_5, returnConstant(true));
                	} catch (Exception e) {
                    		XposedBridge.log("YTBP Fifth Hooks - " +e);
                	}
            	}
    	}

	@Override
	public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
		mPreferences = new XSharedPreferences("com.pyler.youtubebackgroundplayback", "Hooks");
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
