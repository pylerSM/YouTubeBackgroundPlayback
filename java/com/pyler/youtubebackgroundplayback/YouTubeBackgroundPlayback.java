package com.pyler.youtubebackgroundplayback;

import android.content.Context;

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
	public static final String APP_PACKAGE =   "com.google.android.youtube";

	public static final int[] APP_VERSIONS =   { 0, 108058, 108358, 108360, 108362, 108656, 108752, 108754, 108755, 108957, 108958, 108959, 110153, 110155, 110156, 110354, 110456, 110759, 110851, 111056, 111057, 111060 };

	public static final String[] CLASS_1 =     { "com.google.android.libraries.youtube.player.background.BackgroundTransitioner", "kyr", "lco", "lha", "lzb", "moc", "mtp", "mtp", "mtq", "myb", "myb", "myb", "ndr", "nds", "nds", "nxu", "odu", "omt", "oom", "owe", "owe", "owe" };
	public static final String[] METHOD_1 =    { "updateBackgroundService", "P", "a", "a", "a", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d" };
	public static final String[] FIELD_1 =     { "playbackModality", "e", "d", "d", "d", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e","e", "e", "e", "e" };
	public static final String[] SUBFIELD_1 =  { "isInBackground", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "f", "f", "f", "f", "f", "f", "f", "f", "f", "f" };

	public static final String[] CLASS_2 =     { "com.google.android.libraries.youtube.innertube.model.PlayabilityStatusModel", "iqp", "iur", "izd", "jmo", "kam", "kft", "kft", "kft", "kin", "kin", "kin", "klp", "klq", "klq", "lcl", "lhu", "lpf", "lqa", "lwt", "lwt", "lwt" };
	public static final String[] METHOD_2 =    { "isPlayable", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a" };
	public static final String[] FIELD_2 =     { "isBackgroundable", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c" };

	public static final String[] CLASS_3 =     { "com.google.android.apps.youtube.app.background.BackgroundSettings", "azq", "azl", "bdx", "azw", "bhj", "biz", "biz", "biz", "biv", "biv", "biv", "bji", "bji", "bji", "bze", "cad", "cbo", "ccl", "btf", "btf", "btf" };
	public static final String[] METHOD_3 =    { "getBackgroundAudioSetting", "c", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d", "d" };
	public static final String[] METHOD_4 =    { "shouldShowBackgroundAudioSettingsDialog" };

	public static final String[] CLASS_4 =    { "com.google.android.libraries.youtube.common.util.PackageUtil" };	
	public static final String[] METHOD_5 =    { "isDogfoodOrDevBuild" };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
       	if (!lpparam.packageName.equals(APP_PACKAGE)) return;
		
	final ClassLoader loader = lpparam.classLoader;
	
	// check if deobfuscated class name is present
	boolean isObfuscatedCode = false;
	try {
		loader.loadClass(CLASS_1[0]);
	} catch (Exception e) {
		isObfuscatedCode = true;
	}
	
	final Object activityThread = callStaticMethod(
		findClass("android.app.ActivityThread", null), "currentActivityThread");
	final Context context = (Context) callMethod(activityThread, "getSystemContext");
	
	// get classes/methods/fields names index
	final int i = getVersionIndex(context.getPackageManager()
		.getPackageInfo(APP_PACKAGE, 0).versionCode / 1000, isObfuscatedCode);
	if (i == -1) {
		log("Your version of the YouTube app is not yet supported.");
		return;
	}

	// hooks
        findAndHookMethod(CLASS_1[i], loader, METHOD_1[i], new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                setBooleanField(getObjectField(param.thisObject, FIELD_1[i]), SUBFIELD_1[i], true);
            }
        });

        findAndHookMethod(CLASS_2[i], loader, METHOD_2[i], new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                setBooleanField(param.thisObject, FIELD_2[i], true);
            }
        });

        findAndHookMethod(CLASS_3[i], loader, METHOD_3[i], returnConstant("on"));
        
        // Support only for deobfuscated releases
        if (!isObfuscatedCode) {
	    findAndHookMethod(CLASS_3[0], loader, METHOD_4[0] /*normal method name*/, returnConstant(true));
	    findAndHookMethod(CLASS_4[0], loader, METHOD_5[0] /*normal method name*/, returnConstant(true));
	}
	
    }

	// returns 0 for deobfuscated code and positive integer for obfuscated
	private int getVersionIndex(final int versionCode, final boolean isDeobfuscated ) {
		if (isDeobfuscated == true) {
			return 0;
		} else {
			for (int i = 1; i < APP_VERSIONS.length; i++) {
				if (APP_VERSIONS[i] == versionCode) {
					return i;
				}
			}
			return -1;
		}
	}

}
