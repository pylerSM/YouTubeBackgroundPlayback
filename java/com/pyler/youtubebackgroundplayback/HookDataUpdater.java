package com.pyler.youtubebackgroundplayback;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class HookDataUpdater extends IntentService {

    public HookDataUpdater() {
        super("HookDataUpdater");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Toast.makeText(getApplicationContext(), "Saving Hooks!", Toast.LENGTH_LONG).show();

        String hooks = intent.getExtras().getString("Hooks");

        SharedPreferences preferences = getSharedPreferences("Hooks", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Hooks",hooks);
        editor.apply();
    }
}


