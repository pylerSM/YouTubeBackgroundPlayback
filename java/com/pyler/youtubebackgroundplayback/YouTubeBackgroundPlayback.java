package com.pyler.youtubebackgroundplayback;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class YouTubeBackgroundPlayback implements IXposedHookLoadPackage {

	private static final String LOG_TAG = "YTBackgroundPlayback";

	private static final HashMap<String, String> PACKAGE_HOOKS_MAP = new HashMap<String, String>() {{
		put("com.google.android.youtube", "https://raw.githubusercontent.com/pylerSM/YouTubeBackgroundPlayback/master/assets/hooks-3.json");
		put("com.google.android.apps.youtube.music", "https://raw.githubusercontent.com/pylerSM/YouTubeBackgroundPlayback/yt-music/assets/hooks-music.json");
	}};

	private String APP_PACKAGE;

	private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor();

	private ClassLoader loader = null;

	private int secondsUntilReload = 5;

	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
		if (PACKAGE_HOOKS_MAP.containsKey(lpparam.packageName)) {
			APP_PACKAGE = lpparam.packageName;
			loader = lpparam.classLoader;
			new HooksDownloadTask(this, PACKAGE_HOOKS_MAP.get(lpparam.packageName)).execute();
		}
	}

	private void hook(JSONObject hooksFile) throws PackageManager.NameNotFoundException {
		if (hooksFile == null) {
			return;
		}

		final Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
		final Context context = (Context) callMethod(activityThread, "getSystemContext");
		final int versionCode = context.getPackageManager().getPackageInfo(APP_PACKAGE, 0).versionCode;

		final ArrayList<Integer> versionMultipliers = new ArrayList<>();
		final Iterator<String> keys = hooksFile.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.substring(0, 1).equals("x")) {
				try {
					versionMultipliers.add(Integer.parseInt(key.substring(1), 10));
				} catch (NumberFormatException e) {
					Log.i(LOG_TAG, "Bad hook multiplier key. [k:" + versionCode + "]");
				}
			}
		}

		Collections.sort(versionMultipliers, new Comparator<Integer>() {
			@Override
			public int compare(Integer first, Integer second) {
				return (first == null || second == null) ? 0 : first < second ? 1 : first > second ? -1 : 0;
			}
		});

		JSONArray hooks = null;
		for (int versionMultiplier : versionMultipliers) {
			final String version = Integer.toString(versionCode / (versionMultiplier < 1 ? 1 : versionMultiplier), 10);
			try {
				hooks = hooksFile.getJSONObject("x" + versionMultiplier).optJSONArray(version);
			} catch (JSONException ignored) {
				// no-op
			}
			if (hooks != null) {
				break;
			}
		}

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
					boolean gotAllTypes = true;
					for (int iParameterType = 0; iParameterType < parameterTypes.length(); iParameterType++) {
						try {
							final String parameterType = parameterTypes.optString(iParameterType, "");
							if (parameterType.length() == 0) {
								continue;
							}
							final Class<?> resolvedParameterType = findClass(parameterType, loader);
							if (resolvedParameterType == null) {
								continue;
							}
							parameterTypesAndCallback.add(resolvedParameterType);
						} catch (XposedHelpers.ClassNotFoundError e) {
							String em = e.getMessage();
							Log.w(LOG_TAG, "One of the hooks could not be applied: " + (em == null ? "Unknown error" : em));
							gotAllTypes = false;
							break;
						}
					}
					if (!gotAllTypes) {
						continue;
					}
				}

				XC_MethodHook callback = null;
				switch (actionName) {
					case "return_boolean": {
						callback = returnConstant(action.optBoolean("value"));
						break;
					}

					case "return_string": {
						callback = returnConstant(action.optString("value"));
						break;
					}

					case "set_field_boolean_before_method": {
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
						break;
					}

					case "set_field_string_before_method": {
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
						break;
					}

					case "set_field_boolean_after_method": {
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
						break;
					}

					case "set_field_string_after_method": {
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
						break;
					}
					
					case "set_args_value_before_method": {
						// only spupport base type
						final String args_value = action.optString("args_value");
						if (args_value.length() > 0) {
						    callback = new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(final MethodHookParam param) {
							    final String[] args = args_value.split("_|_");
							    int i=0;
							    for (String m : args) {
								XposedBridge.log("set_args_value_before_method = " + m);
								String temp;
								if (m.startsWith("STRING:")){
								    temp=m.replace("STRING:","");
								    param.args[i] = temp;
								}else  if (m.startsWith("INT:")){
								    temp=m.replace("INT:","");
								    param.args[i] = Integer.valueOf(temp);
								}else  if (m.startsWith("BOOLEAN:")){
								    temp=m.replace("BOOLEAN:","");
								    param.args[i] = Boolean.valueOf(temp);
								}else  if (m.startsWith("LONG:")){
								    temp=m.replace("LONG:","");
								    param.args[i] = Long.valueOf(temp);
								}

								i++;
							    }
							}
						    };
						}
						break;
					}

					default: {
						Log.w(LOG_TAG, "Ignoring an unrecognized hook action for YouTube in the list. [a:" + actionName + ";vc:" + versionCode + "]");
						break;
					}
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

	public static class HooksDownloadTask extends AsyncTask<Void, Void, JSONObject> {

		private final YouTubeBackgroundPlayback callback;

		private final String hooksDownloadUrl;

		private HooksDownloadTask(YouTubeBackgroundPlayback callback, String hooksDownloadUrl) {
			super();
			this.callback = callback;
			this.hooksDownloadUrl = hooksDownloadUrl;
		}

		@Override
		protected JSONObject doInBackground(Void... params) {
			InputStream in = null;
			try {
				URLConnection conn = new URL(hooksDownloadUrl).openConnection();
				conn.setConnectTimeout(40 * 1000 /* ms */);
				conn.setDoInput(true);
				conn.setDoOutput(false);
				conn.setReadTimeout(20 * 1000 /* ms */);
				conn.setUseCaches(true);

				if (conn instanceof HttpURLConnection) {
					HttpURLConnection hConn = (HttpURLConnection) conn;
					hConn.setChunkedStreamingMode(0);
					hConn.setInstanceFollowRedirects(true);
					hConn.setRequestMethod("GET");
				} else {
					Log.w(LOG_TAG, "Our connection is not java.net.HttpURLConnection but instead " + conn.getClass().getName());
				}

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
				callback.secondsUntilReload *= 2;
				if (callback.secondsUntilReload >= 90) {
					callback.secondsUntilReload = 90;
				}

				WORKER.schedule(new Runnable() {
					@Override
					public void run() {
						new HooksDownloadTask(callback, hooksDownloadUrl).execute();
					}
				}, ((int) Math.floor(Math.random() * callback.secondsUntilReload)) + 1, TimeUnit.SECONDS);
			} else {
				callback.secondsUntilReload = 5;
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
