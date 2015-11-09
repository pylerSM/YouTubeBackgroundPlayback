package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage {

	public static final String YOUTUBE_PACKAGE = "com.google.android.youtube";
	public static final int[] YOUTUBE_VERSION = { 108058, 108358, 108360, 108362 };
	public static final String[] CLASS_1 = { "kyr", "lco", "lco", "lzb" };
	public static final String[] METHOD_1_1 = { "P", "a", "a", "a" };
	public static final String[] FIELD_1_1 = { "e", "d", "d", "d" };
	public static final String[] FIELD_1_2 = { "e", "e", "e", "e" };
	public static final String[] CLASS_2 = { "iqp", "iur", "iur", "jmo" };
	public static final String[] METHOD_2_1 = { "a", "a", "a", "a" };
	public static final String[] FIELD_2_1 = { "c", "c", "c", "c" };
	public static final String[] CLASS_3 = { "azq", "azl", "azl", "azw" };
	public static final String[] METHOD_3_1 = { "c", "d", "d", "d" };
	public int id;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!YOUTUBE_PACKAGE.equals(lpparam.packageName)) {
			return;
		}

		Object activityThread = XposedHelpers.callStaticMethod(
			XposedHelpers.findClass("android.app.ActivityThread", null),
			"currentActivityThread");
		Context context = (Context) XposedHelpers.callMethod(activityThread,
			"getSystemContext");
		int versionCode = context.getPackageManager().getPackageInfo(
			lpparam.packageName, 0).versionCode / 1000;
		id = getVersionIndex(versionCode);
		
		if (id != -1 ) {

			XposedHelpers.findAndHookMethod(CLASS_1[id], lpparam.classLoader, METHOD_1_1[id], new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						super.beforeHookedMethod(param);
//						XposedHelpers.callMethod(param.thisObject, METHOD_1_2[id]);
						Object o = XposedHelpers.getObjectField(param.thisObject, FIELD_1_1[id]);
						XposedHelpers.setBooleanField(o, FIELD_1_2[id], true);
					}
			});

			XposedHelpers.findAndHookMethod(CLASS_2[id], lpparam.classLoader, METHOD_2_1[id], new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						super.beforeHookedMethod(param);
						XposedHelpers.setBooleanField(param.thisObject, FIELD_2_1[id], true);
					}
			});	
			
			XposedHelpers.findAndHookMethod(CLASS_3[id],lpparam.classLoader,METHOD_3_1[id],XC_MethodReplacement.returnConstant("on"));
			
		} else {
			XposedBridge.log("This YouTube version is not supported yet.");
		}
	}

	public int getVersionIndex(int versionCode) {
		for (int i = 0; i < YOUTUBE_VERSION.length; i++) {
			if ( versionCode == YOUTUBE_VERSION[i] ) return i;
		}
		return -1;
	}
}
