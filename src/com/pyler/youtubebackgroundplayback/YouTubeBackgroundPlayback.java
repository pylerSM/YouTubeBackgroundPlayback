package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage {
	public static final String PACKAGE = "com.google.android.youtube";
	public static final String[] CLASS = { "ctz", "cyj", "cyy" };
	public static final String METHOD = "u";

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		if (!PACKAGE.equals(lpparam.packageName)) {
			return;
		}
		Object activityThread = XposedHelpers.callStaticMethod(
				XposedHelpers.findClass("android.app.ActivityThread", null),
				"currentActivityThread");
		Context context = (Context) XposedHelpers.callMethod(activityThread,
				"getSystemContext");
		int versionCode = context.getPackageManager().getPackageInfo(
				lpparam.packageName, 0).versionCode;
		int i = getVersionIndex(versionCode);
		if (i != -1) {
			XposedHelpers.findAndHookMethod(CLASS[i], lpparam.classLoader,
					METHOD, XC_MethodReplacement.returnConstant(true));
		} else {
			XposedBridge.log("This YouTube version is not supported yet.");
		}
	}

	public int getVersionIndex(int version) {
		if ((version == 100506130) || (version == 100506170)) {
			// YouTube 10.05.6
			return 2;
		} else if ((version == 100405130) || (version == 100405170)) {
			// YouTube 10.04.5
			return 1;
		} else if ((version == 100305130) || (version == 100305170)) {
			// YouTube 10.03.5
			return 0;
		} else {
			// Unsupported version
			return -1;
		}
	}
}
