package com.pyler.youtubebackgroundplayback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONObject;

public class HookDataUpdater extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {
      String hooks = intent.getExtras().getString("Hooks");

      SharedPreferences preferences = getSharedPreferences("Hooks", MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();
      editor.putString("Hooks",hooks);
      editor.apply();
   }
}

