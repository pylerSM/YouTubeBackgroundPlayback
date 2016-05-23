package com.pyler.youtubebackgroundplayback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class HookDataUpdater extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {
      Toast.makeText(context, "We should update hooks now", Toast.LENGTH_LONG).show();
   }
}
