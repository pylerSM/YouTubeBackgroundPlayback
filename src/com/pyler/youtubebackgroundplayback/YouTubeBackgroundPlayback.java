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
	public static final int[] YOUTUBE_VERSION = { 1002, 1003, 1004, 1005, 1006, 1008, 1009, 1010,
		1011, 1012, 1013, 1014, 1015, 1016, 1018, 1019, 1020, 1021, 1024, 1025, 1028, 1029 };
	public static final String BACKGROUND_PLAYER_SERVICE = "com.google.android.apps.youtube.core.player.BackgroundPlayerService";
public static final String[] CLASS_ENABLE_BACKGROUND_PLAYBACK = { "cti", "ctz", "cyj", "cyy", "cyk", "cyl", "cza", "cyj", "cym",
		"cyc", "cyb", "cxa", "cxx", "cxw", "cxy", "dao", "dag", "dap", "dbq", "dcq", "dcg", "dbs" };
	public static final String[] METHOD_ENABLE_BACKGROUND_PLAYBACK = { "u", "u", "u", "u", "u", "u", "u", "u", "v", "v", "x", "x",
		"x", "x", "x", "y", "z", "A", "A", "A", "z", "z" };
	public static final String[] FIELD_PLAYBACK_CONTROL = { "i", "i", "i", "i", "i", "i", "i", "i", "i", "i", "i", "i",
		"i", "i", "i", "i", "i", "i", "i", "i", "i", "f" };
	public static final String[] METHOD_RESTART_PLAYBACK = { "k", "k", "k", "k", "k", "j", "j", "j", "j", "j", "j", "j", "j", "j",
		"j", "j", "j", "j", "j", "j", "j", "j" };
	public static final String FIELD_ENABLE_NOTIFICATION = "e";
	public static final String METHOD_NEXT_TRACK = "d";
	public static final String FIELD_TIME_MILLS = "a";
	public static final String FIELD_LENGTH_MILLS = "b";
	public int id;
	public long prevTime = -1;
	public boolean advanceSent = false;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!YOUTUBE_PACKAGE.equals(lpparam.packageName)) {
			return;
		}
		XC_MethodHook restartPlayback = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				Object playbackControl = (Object) XposedHelpers.getObjectField(
						param.thisObject, FIELD_PLAYBACK_CONTROL[id]);
				XposedHelpers.callMethod(playbackControl,
						METHOD_RESTART_PLAYBACK[id]);

			}
		};
		XC_MethodHook enableNotification = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				XposedHelpers.setBooleanField(param.thisObject,
						FIELD_ENABLE_NOTIFICATION, true);
			}
		};
		XC_MethodHook advanceNextTrack = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				long time = XposedHelpers.getLongField(param.args[0],
						FIELD_TIME_MILLS);
				long length = XposedHelpers.getLongField(param.args[0],
						FIELD_LENGTH_MILLS);
				if (time == prevTime && time > 0 && length > 0
						&& time + 2000 > length) {
					if (!advanceSent) {
						advanceSent = true;
						Object playbackControl = (Object) XposedHelpers
								.getObjectField(param.thisObject,
										FIELD_PLAYBACK_CONTROL[id]);
						XposedHelpers.callMethod(playbackControl,
								METHOD_NEXT_TRACK);
					}
				} else {
					prevTime = time;
					advanceSent = false;
				}
			}
		};
		Object activityThread = XposedHelpers.callStaticMethod(
				XposedHelpers.findClass("android.app.ActivityThread", null),
				"currentActivityThread");
		Context context = (Context) XposedHelpers.callMethod(activityThread,
				"getSystemContext");
		int versionCode = context.getPackageManager().getPackageInfo(
				lpparam.packageName, 0).versionCode;
		id = getVersionIndex(versionCode);
		if (id != -1) {
			XposedHelpers.findAndHookMethod(
					CLASS_ENABLE_BACKGROUND_PLAYBACK[id], lpparam.classLoader,
					METHOD_ENABLE_BACKGROUND_PLAYBACK[id],
					XC_MethodReplacement.returnConstant(true));
			XposedBridge.hookAllMethods(XposedHelpers.findClass(
					BACKGROUND_PLAYER_SERVICE, lpparam.classLoader),
					"handlePlaybackServiceException", restartPlayback);
			XposedBridge.hookAllConstructors(XposedHelpers.findClass(
					BACKGROUND_PLAYER_SERVICE, lpparam.classLoader),
					enableNotification);
			XposedBridge.hookAllMethods(XposedHelpers.findClass(
					BACKGROUND_PLAYER_SERVICE, lpparam.classLoader),
					"handleVideoTimeEvent", advanceNextTrack);
		} else {
			XposedBridge.log("This YouTube version is not supported yet.");
		}
	}
	public int getVersionIndex(int build) {
		int version = build / 100000;
		for (int i = 0; i < YOUTUBE_VERSION.length; i++) {
			if ( version == YOUTUBE_VERSION[i] ) return i;
		}
		return -1;
	}
}
