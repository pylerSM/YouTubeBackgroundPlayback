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
	public static final int[] YOUTUBE_VERSION = { 1076, 1077, 1078, 1079, 1080 };
	public static final String[] CLASS_1 = { "knb", "koj", "kqp", "kul", "kys" };
	public static final String[] METHOD_1_1 = { "J", "K", "O", "P", "P" };
	public static final String[] FIELD_1_1 = { "f", "e", "e", "e", "e" };
	public static final String[] FIELD_1_2 = { "d", "e", "e", "e", "e" };
	public static final String[] CLASS_2 = { "iha", "ihy", "ijx", "ink", "iqq" };
	public static final String[] METHOD_2_1 = { "a", "a", "a", "a", "a" };
	public static final String[] FIELD_2_1 = { "c", "c", "c", "c", "c" };
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
			lpparam.packageName, 0).versionCode / 100000;
		id = getVersionIndex(versionCode);
		
		if (id != -1 ) {

			XposedHelpers.findAndHookMethod(CLASS_1[id], lpparam.classLoader, METHOD_1_1[id], new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						super.beforeHookedMethod(param);
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
