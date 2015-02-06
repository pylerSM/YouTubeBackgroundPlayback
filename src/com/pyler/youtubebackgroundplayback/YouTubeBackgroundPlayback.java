package com.pyler.youtubebackgroundplayback;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage {
	public static final String PACKAGE = "com.google.android.youtube";
	public static final String CLASS = "ctz";
	public static final String METHOD = "u";

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		if (!PACKAGE.equals(lpparam.packageName)) {
			return;
		}
		XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, METHOD,
				XC_MethodReplacement.returnConstant(true));
	}
}
