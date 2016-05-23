package com.pyler.youtubebackgroundplayback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONObject;

public class HookDataUpdater extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {
      Toast.makeText(context, "Saving Hooks!", Toast.LENGTH_LONG).show();
      
      String hooks = intent.getExtras().getString("Hooks");
      
      try {
         JSONObject jsonObject = new JSONObject(hooks);
         //Save To Shared Preferences
      } catch (Exception e) {
      }
   }
}

