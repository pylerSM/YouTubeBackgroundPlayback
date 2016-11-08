package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage {

	public static final String LOG_TAG = "YTBackgroundPlayback";

	public static final String APP_PACKAGE = "com.google.android.youtube";

	public static final String HOOKS_DOWNLOAD_URL = "https://raw.githubusercontent.com/pylerSM/YouTubeBackgroundPlayback/master/assets/hooks-1.json";

	private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor();

	private ClassLoader loader = null;

	private int secondsUntilReload = 5;

	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals(APP_PACKAGE)) return;
		loader = lpparam.classLoader;
		new HooksDownloadTask(this).execute();
	}

	public void hook(JSONObject hooksFile) throws PackageManager.NameNotFoundException {
		if (hooksFile == null) {
			return;
		}

		final int versionMultiplier = hooksFile.optInt("version_multiplier", 1);
		final Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
		final Context context = (Context) callMethod(activityThread, "getSystemContext");
		final int versionCode = context.getPackageManager().getPackageInfo(APP_PACKAGE, 0).versionCode;
		final String version = Integer.toString(versionCode / (versionMultiplier < 1 ? 1 : versionMultiplier), 10);

		JSONArray hooks = hooksFile.optJSONArray(version);
		if (hooks == null) {
			Log.i(LOG_TAG, "No hook details were found for the version of YouTube installed on your device. [vc:" + versionCode + "]");
			hooks = hooksFile.optJSONArray("fallback");
			if (hooks == null) {
				Log.w(LOG_TAG, "No fall-back hook details for YouTube were found. Stopping.");
				return;
			}
			Log.i(LOG_TAG, "Using the fall-back hook details for YouTube as a last resort.");
		}

		for (int iHook = 0; iHook < hooks.length(); iHook++) {
			final JSONObject hook = hooks.optJSONObject(iHook);
			if (hook == null) {
				continue;
			}

			final String className = hook.optString("class_name", "");
			final String methodName = hook.optString("method_name", "");
			final JSONArray parameterTypes = hook.optJSONArray("parameter_types");
			final JSONArray actions = hook.optJSONArray("actions");

			if (className.length() == 0 || methodName.length() == 0 || actions == null) {
				Log.w(LOG_TAG, "Ignoring an incomplete hook detail for YouTube in the list. [vc:" + versionCode + "]");
				continue;
			}

			for (int iAction = 0; iAction < actions.length(); iAction++) {
				final JSONObject action = actions.optJSONObject(iAction);
				if (action == null) {
					continue;
				}

				final String actionName = action.optString("name", "");
				final ArrayList<Object> parameterTypesAndCallback = new ArrayList<>();

				if (parameterTypes != null) {
					for (int iParameterType = 0; iParameterType < parameterTypes.length(); iParameterType++) {
						final String parameterType = parameterTypes.optString(iParameterType, "");
						if (parameterType.length() == 0) {
							continue;
						}
						final Class<?> resolvedParameterType = findClassIfExists(parameterType, loader);
						if (resolvedParameterType == null) {
							continue;
						}
						parameterTypesAndCallback.add(resolvedParameterType);
					}
				}

				XC_MethodHook callback = null;
				if (actionName.equals("return_boolean")) {
					callback = returnConstant(action.optBoolean("value"));
				} else if (actionName.equals("return_string")) {
					callback = returnConstant(action.optString("value"));
				} else if (actionName.equals("set_field_boolean_before_method")) {
					final String fieldName = action.optString("field_name");
					final boolean value = action.optBoolean("value");
					if (fieldName.length() > 0) {
						callback = new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(final MethodHookParam param) {
								final String[] fieldParts = fieldName.split("\\.");
								Object thisObject = param.thisObject;
								for (int iPart = 0; iPart < fieldParts.length - 1; iPart++) {
									thisObject = getObjectField(thisObject, fieldParts[iPart]);
								}
								setBooleanField(thisObject, fieldParts[fieldParts.length - 1], value);
							}
						};
					}
				} else if (actionName.equals("set_field_string_before_method")) {
					final String fieldName = action.optString("field_name");
					final boolean value = action.optBoolean("value");
					if (fieldName.length() > 0) {
						callback = new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(final MethodHookParam param) {
								final String[] fieldParts = fieldName.split("\\.");
								Object thisObject = param.thisObject;
								for (int iPart = 0; iPart < fieldParts.length - 1; iPart++) {
									thisObject = getObjectField(thisObject, fieldParts[iPart]);
								}
								setObjectField(thisObject, fieldParts[fieldParts.length - 1], value);
							}
						};
					}
				} else if (actionName.equals("set_field_boolean_after_method")) {
					final String fieldName = action.optString("field_name");
					final boolean value = action.optBoolean("value");
					if (fieldName.length() > 0) {
						callback = new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(final MethodHookParam param) {
								final String[] fieldParts = fieldName.split("\\.");
								Object thisObject = param.thisObject;
								for (int iPart = 0; iPart < fieldParts.length - 1; iPart++) {
									thisObject = getObjectField(thisObject, fieldParts[iPart]);
								}
								setBooleanField(thisObject, fieldParts[fieldParts.length - 1], value);
							}
						};
					}
				} else if (actionName.equals("set_field_string_after_method")) {
					final String fieldName = action.optString("field_name");
					final boolean value = action.optBoolean("value");
					if (fieldName.length() > 0) {
						callback = new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(final MethodHookParam param) {
								final String[] fieldParts = fieldName.split("\\.");
								Object thisObject = param.thisObject;
								for (int iPart = 0; iPart < fieldParts.length - 1; iPart++) {
									thisObject = getObjectField(thisObject, fieldParts[iPart]);
								}
								setObjectField(thisObject, fieldParts[fieldParts.length - 1], value);
							}
						};
					}
				} else {
					Log.w(LOG_TAG, "Ignoring an unrecognized hook action for YouTube in the list. [a:" + actionName + ";vc:" + versionCode + "]");
				}
				if (callback == null) {
					continue;
				}
				parameterTypesAndCallback.add(callback);

				try {
					findAndHookMethod(className, loader, methodName, parameterTypesAndCallback.toArray(new Object[parameterTypesAndCallback.size()]));
				} catch (NoSuchMethodError | XposedHelpers.ClassNotFoundError e) {
					String em = e.getMessage();
					Log.w(LOG_TAG, "One of the hooks could not be applied: " + (em == null ? "Unknown error" : em));
				}
			}
		}

		Log.i(LOG_TAG, "Done applying the hooks. [vc:" + versionCode + "]");
	}

	public class HooksDownloadTask extends AsyncTask<Void, Void, JSONObject> {

		private final YouTubeBackgroundPlayback callback;

		public HooksDownloadTask(YouTubeBackgroundPlayback callback) {
			super();
			this.callback = callback;
		}

		@Override
		protected JSONObject doInBackground(Void... params) {
			InputStream in = null;
			try {
				HttpsURLConnection conn = (HttpsURLConnection) new URL(HOOKS_DOWNLOAD_URL).openConnection();
				conn.setChunkedStreamingMode(0);
				conn.setConnectTimeout(40 * 1000 /* ms */);
				conn.setDoInput(true);
				conn.setDoOutput(false);
				conn.setInstanceFollowRedirects(true);
				conn.setReadTimeout(20 * 1000 /* ms */);
				conn.setRequestMethod("GET");
				conn.setUseCaches(true);

				conn.connect();
				in = conn.getInputStream();

				BufferedReader inReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				StringBuilder lineBuilder = new StringBuilder();

				String line;
				while ((line = inReader.readLine()) != null) {
					lineBuilder.append(line);
				}

				return new JSONObject(lineBuilder.toString());
			} catch (IOException | JSONException e) {
				String em = e.getMessage();
				Log.i(LOG_TAG, "The hook details could not be downloaded: " + (em == null ? "Unknown error" : em));
				return null;
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignored) {
						// no-op
					}
				}
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			if (result == null) {
				secondsUntilReload *= 2;
				if (secondsUntilReload >= 90) {
					secondsUntilReload = 90;
				}

				WORKER.schedule(new Runnable() {
					@Override
					public void run() {
						new HooksDownloadTask(callback).execute();
					}
				}, ((int) Math.floor(Math.random() * secondsUntilReload)) + 1, TimeUnit.SECONDS);
			} else {
				secondsUntilReload = 5;
				if (callback != null) {
					try {
						callback.hook(result);
					} catch (PackageManager.NameNotFoundException e) {
						// if this happens, something has gone very very (very!) wrong.
						throw new RuntimeException("YouTube package reportedly not found even though it was loaded", e);
					}
				}
			}
		}

	}

}
