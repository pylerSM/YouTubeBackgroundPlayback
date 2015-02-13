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
	public static final String[] CLASS_ENABLE_BACKGROUND_PLAYBACK = { "cti",
			"ctz", "cyj", "cyy" };
	public static final String METHOD_ENABLE_BACKGROUND_PLAYBACK = "u";
	public static final String FIELD_PLAYBACK_CONTROL = "i";
	public static final String METHOD_START_PLAYBACK = "k";

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		if (!YOUTUBE_PACKAGE.equals(lpparam.packageName)) {
			return;
		}
		XC_MethodHook backgroundPlayback = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				Object playbackControl = (Object) XposedHelpers.getObjectField(
						param.thisObject, FIELD_PLAYBACK_CONTROL);
				XposedHelpers
						.callMethod(playbackControl, METHOD_START_PLAYBACK);

			}
		};
		Object activityThread = XposedHelpers.callStaticMethod(
				XposedHelpers.findClass("android.app.ActivityThread", null),
				"currentActivityThread");
		Context context = (Context) XposedHelpers.callMethod(activityThread,
				"getSystemContext");
		int versionCode = context.getPackageManager().getPackageInfo(
				lpparam.packageName, 0).versionCode;
		int id = getVersionIndex(versionCode);
		if (id != -1) {
			XposedHelpers.findAndHookMethod(
					CLASS_ENABLE_BACKGROUND_PLAYBACK[id], lpparam.classLoader,
					METHOD_ENABLE_BACKGROUND_PLAYBACK,
					XC_MethodReplacement.returnConstant(true));
			XposedBridge
					.hookAllMethods(
							XposedHelpers
									.findClass(
											"com.google.android.apps.youtube.core.player.BackgroundPlayerService",
											lpparam.classLoader),
							"handlePlaybackServiceException",
							backgroundPlayback);
		} else {
			XposedBridge.log("This YouTube version is not supported yet.");
		}

	}

	public int getVersionIndex(int version) {
		if ((version == 100506130) || (version == 100506170)) {
			// YouTube 10.05.6
			return 3;
		} else if ((version == 100405130) || (version == 100405170)) {
			// YouTube 10.04.5
			return 2;
		} else if ((version == 100305130) || (version == 100305170)) {
			// YouTube 10.03.5
			return 1;
		} else if ((version == 100203130) || (version == 100203170)) {
			// YouTube 10.02.3
			return 0;
		} else {
			// Unsupported version
			return -1;
		}
	}
}
