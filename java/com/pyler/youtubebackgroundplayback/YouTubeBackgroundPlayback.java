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

    public static final int[] APP_VERSIONS =   { 108058, 108358, 108360, 108362 };

    public static final String[] CLASS_1 =     { "kyr", "lco", "lco", "lzb" };
    public static final String[] METHOD_1 =    { "P", "a", "a", "a" };
    public static final String[] FIELD_1 =     { "e", "d", "d", "d" };
    public static final String[] SUBFIELD_1 =  { "e", "e", "e", "e" };

    public static final String[] CLASS_2 =     { "iqp", "iur", "iur", "jmo" };
    public static final String[] METHOD_2 =    { "a", "a", "a", "a" };
    public static final String[] FIELD_2 =     { "c", "c", "c", "c" };

    public static final String[] CLASS_3 =     { "azq", "azl", "azl", "azw" };
    public static final String[] METHOD_3 =    { "c", "d", "d", "d" };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.youtube")) return;

        final Object activityThread = callStaticMethod(
                findClass("android.app.ActivityThread", null), "currentActivityThread");
        final Context context = (Context) callMethod(activityThread, "getSystemContext");
        final int i = getVersionIndex(context.getPackageManager()
                .getPackageInfo("com.google.android.youtube", 0).versionCode / 1000);

        if (i == -1) {
            log("Your version of the YouTube app is not yet supported.");
            return;
        }

        final ClassLoader loader = lpparam.classLoader;

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
    }

    private int getVersionIndex(final int versionCode) {
        for (int i = 0; i < APP_VERSIONS.length; i++)
            if (APP_VERSIONS[i] == versionCode)
                return i;
        return -1;
    }

}
