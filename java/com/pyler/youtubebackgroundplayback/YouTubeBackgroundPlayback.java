package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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

	public static final int[] APP_VERSIONS = { 0,
		108058, 108358, 108360, 108362, 108656,
		108752, 108754, 108755, 108957, 108958,
		108959, 110153, 110155, 110156, 110354,
		110456, 110759, 110851, 111056, 111057,
		111060, 111157, 111257, 111355, 111356,
		111555, 111662, 111752, 111852, 111956,
		112054, 112153, 112254, 112256, 112356,
		112555, 112559, 112753, 112953, 112954,
		112955, 113253, 113355};

	public static final String[] CLASS_1 = { "com.google.android.libraries.youtube.player.background.BackgroundTransitioner",
		"kyr", "lco", "lha", "lzb", "moc",
		"mtp", "mtp", "mtq", "myb", "myb",
		"myb", "ndr", "nds", "nds", "nxu",
		"odu", "omt", "oom", "owe", "owe",
		"owe", "ozp", "pez", "pih", "phr",
		"pvk", "qec", "qgh", "qit", "qcn",
		"qfe", "qkl", "qly", "qly", "qmo",
		"qrg", "qrg", "qts", "rgs", "rew",
		"rew", "rpw", "rrg"};
	public static final String[] METHOD_1 = { "updateBackgroundService",
		"P", "a", "a", "a", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"e", "e", "e", "e", "e",
		"e", "e", "e", "c", "c",
		"c", "c", "c"};
	public static final String[] FIELD_1 = { "playbackModality",
		"e", "d", "d", "d", "e",
		"e", "e", "e", "e", "e",
		"e", "e", "e", "e", "e",
		"e", "e", "e", "e", "e",
		"e", "e", "e", "e", "e",
		"e", "e", "e", "e", "e",
		"g", "g", "i", "i", "i",
		"i", "i", "i", "a", "a",
		"a", "a", "a"};
	public static final String[] SUBFIELD_1 = { "isInBackground",
		"e", "e", "e", "e", "e",
		"e", "e", "e", "e", "e",
		"e", "f", "f", "f", "f",
		"f", "f", "f", "f", "f",
		"f", "f", "f", "f", "f",
		"f", "f", "f", "f", "f",
		"f", "f", "f", "f", "f",
		"f", "f", "f", "f", "f",
		"f", "f", "f"};

	public static final String[] CLASS_2 = { "com.google.android.libraries.youtube.innertube.model.PlayabilityStatusModel",
		"iqp", "iur", "izd", "jmo", "kam",
		"kft", "kft", "kft", "kin", "kin",
		"kin", "klp", "klq", "klq", "lcl",
		"lhu", "lpf", "lqa", "lwt", "lwt",
		"lwt", "lzg", "mep", "mht", "mhd",
		"mtk", "nbi", "ncm", "ndv", "mvl",
		"mvs", "nbp", "ndz", "ndz", "nec",
		"nhe", "nhe", "niw", "niy", "nhc",
		"nhc"};

	public static final String[] METHOD_2 = { "isPlayable",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a", "a", "a", "a", "a",
		"a"};
	public static final String[] FIELD_2 = { "isBackgroundable",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c", "c", "c", "c", "c",
		"c"};

	public static final String[] CLASS_3 = { "com.google.android.apps.youtube.app.background.BackgroundSettings",
		"azq", "azl", "bdx", "azw", "bhj",
		"biz", "biz", "biz", "biv", "biv",
		"biv", "bji", "bji", "bji", "bze",
		"cad", "cbo", "ccl", "btf", "btf",
		"btf", "bsv", "bsu", "btr", "btq",
		"bzy", "cam", "cas", "cba", "cbr",
		"ccb", "ccw", "ccv", "ccv", "ccs",
		"ceh", "ceh", "cen", "cgf", "cej",
		"cej", "chf", "cgs"};
	public static final String[] METHOD_3 = { "getBackgroundAudioSetting",
		"c", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d", "d", "d",
		"d", "d", "d"};

	public static final String[] CLASS_4 = { "com.google.android.apps.youtube.app.background.BackgroundSettings" };
	public static final String[] METHOD_4 = { "shouldShowBackgroundAudioSettingsDialog" };

	public static final String[] CLASS_5 = { "com.google.android.libraries.youtube.common.util.PackageUtil" };
	public static final String[] METHOD_5 = { "isDogfoodOrDevBuild" };

	/*
	PlayablilityStatusModel was transformed into a helper class with static methods.
	The property isBackgroundable is obtained through the method PlayabilityStatusHelper.isBackgroundable(PlayabilityStatus).
	 */
	public static final int INDEX_SWITCH_TO_PlayabilityStatusHelper = 42;

	public static final String[] CLASS_PlayabilityStatus = {
		"vla", "voa"
	};

	public static final String[] CLASS_PlayabilityStatusHelper = {
		"shz", "sjp"
	};

	public static final String[] METHOD_PlayabilityStatusHelper_isBackgroundable = {
		"d", "d"
	};

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws PackageManager.NameNotFoundException {
		if (!lpparam.packageName.equals(APP_PACKAGE)) return;

		final ClassLoader loader = lpparam.classLoader;

		// get classes/methods/fields names index
		final int i = getVersionIndex(loader);
		if (i == -1) {
			log("Could not enable background playback for the YouTube app. Your installed version of it is not supported.");
			return;
		}

		// hooks
		findAndHookMethod(CLASS_1[i], loader, METHOD_1[i], new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(final MethodHookParam param) {
				setBooleanField(getObjectField(param.thisObject, FIELD_1[i]), SUBFIELD_1[i], true);
			}
		});

		if(i < INDEX_SWITCH_TO_PlayabilityStatusHelper) {
			findAndHookMethod(CLASS_2[i], loader, METHOD_2[i], new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(final MethodHookParam param) {
					setBooleanField(param.thisObject, FIELD_2[i], true);
				}
			});
		} else {
			int ir = i - INDEX_SWITCH_TO_PlayabilityStatusHelper;
			findAndHookMethod(CLASS_PlayabilityStatusHelper[ir], loader, METHOD_PlayabilityStatusHelper_isBackgroundable[ir],
					findClass(CLASS_PlayabilityStatus[ir], loader), returnConstant(true));
		}

		findAndHookMethod(CLASS_3[i], loader, METHOD_3[i], returnConstant("on"));

		if (i == 0) {
			// hook specific methods for unobfuscated releases
			findAndHookMethod(CLASS_4[0], loader, METHOD_4[0], returnConstant(true));

			findAndHookMethod(CLASS_5[0], loader, METHOD_5[0], returnConstant(true));
		}
	}

	// returns 0 for unobfuscated code and positive integer for obfuscated
	private int getVersionIndex(final ClassLoader loader) throws PackageManager.NameNotFoundException {
		try {
			// check if the app is unobfuscated
			loader.loadClass(CLASS_1[0]);
			return 0;
		} catch (Exception e) {
			// look through all the known app versions
			final Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
			final Context context = (Context) callMethod(activityThread, "getSystemContext");
			final int versionCode = context.getPackageManager().getPackageInfo(APP_PACKAGE, 0).versionCode / 1000;
			for (int i = 1; i < APP_VERSIONS.length; i++) {
				if (APP_VERSIONS[i] == versionCode) {
					return i;
				}
			}
			return -1;
		}
	}

}
