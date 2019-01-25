package com.rk.myApp.callscreenblocker;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import com.rk.myApp.R;
import com.rk.myApp.receiver.Alarmactivater;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private int OVERLAY_PERMISSION_CODE = 0;
    private Button btn, btn2;
    private SharedPreferences sp;
    private String btntext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_main);

        sp = getSharedPreferences("call_blocker", MODE_PRIVATE);
        btn = (Button) findViewById(R.id.btn);
        btn2 = (Button) findViewById(R.id.btn2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btn.getText().toString().equals("Activate")) {
                    Alarmactivater.addBlockerAlarm(MainActivity.this);
                    sp.edit().putString("status", "Deactivate").commit();
                    btn.setText("Deactivate");
                } else {
                    if (sp.getBoolean("permission_status", false)) {
                        Alarmactivater.cancelAlarm(MainActivity.this);
                        sp.edit().putString("status", "Activate").commit();
                        btn.setText("Activate");
                    } else {
                        checkANDgetpermission();
                    }
                }
//                Common.getInstance(MainActivity.this).block_ui();
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Alarmactivater.isAlarmRunning(MainActivity.this);
//                Log.e("Is service running", "" + Common.getInstance(MainActivity.this).isMyServiceRunning(IncomingService.class));

            }
        });
        btntext = sp.getString("status", "Activate");
        if (btntext.equals("Activate")) {
            if (Common.alarm) {
                if (Alarmactivater.isAlarmRunning(MainActivity.this))
                    btntext = "Deactivate";
            } else {
                if (Common.getInstance(this).isMyServiceRunning(IncomingService.class))
                    btntext = "Deactivate";
            }
        } else if (btntext.equals("Deactivate")) {
            if (Common.alarm) {
                if (!Alarmactivater.isAlarmRunning(MainActivity.this))
                    btntext = "Activate";
            } else {
                if (!Common.getInstance(this).isMyServiceRunning(IncomingService.class))
                    btntext = "Activate";
            }
        }
        btn.setText(btntext);
        sp.edit().putString("status", btntext).commit();
        if (!sp.getBoolean("permission_status", false)) {
            checkANDgetpermission();
        } else if (btntext.equals("Deactivate")) {
            Alarmactivater.addBlockerAlarm(MainActivity.this);
//            Log.e("activated","activated");
        }
        if (canDrawOverlays(this)) {
            sp.edit().putBoolean("permission_status", true).commit();
        }
//        Log.e("CanDrawOverlays",""+canDrawOverlays(this)) ;
    }


    public boolean addOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);

                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    check_overlay();
                }
            }, 1500);

        }
    }

    private void check_overlay() {
        if (canDrawOverlays(this)) {
//               Common.getInstance(this).block_ui();
            sp.edit().putBoolean("permission_status", true).commit();
            check_autostart();
        } else {
            Toast.makeText(this, "Kindly switch on the permission button", Toast.LENGTH_SHORT).show();
            addOverlay();
        }
    }

    static boolean canDrawOverlays(Context context) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && Settings.canDrawOverlays(context))
            return true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            //USING APP OPS MANAGER
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (manager != null) {
                try {
                    int result = manager.checkOp(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Binder.getCallingUid(), context.getPackageName());
                    return result == AppOpsManager.MODE_ALLOWED;
                } catch (Exception ignore) {
                }
            }
        }

        try {
            //IF This Fails, we definitely can't do it
            WindowManager mgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (mgr == null) return false; //getSystemService might return null
            View viewToAdd = new View(context);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(0, 0, android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSPARENT);
            viewToAdd.setLayoutParams(params);
            mgr.addView(viewToAdd, params);
            mgr.removeView(viewToAdd);
            return true;
        } catch (Exception ignore) {
        }
        return false;

    }

    private void check_autostart() {
        String manufacturer = "xiaomi";
        if (manufacturer.equalsIgnoreCase(android.os.Build.MANUFACTURER)) {
            //this will open auto start screen where user can enable permission for your app
            Intent intent1 = new Intent();
            intent1.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent1);
        }
    }


    private int checkANDgetpermission() {
        String[] network = {Manifest.permission.READ_PHONE_STATE};
        ArrayList<String> list = new ArrayList();
        int j = 0;
        for (int i = 0; i < network.length; i++) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, network[i]) != PackageManager.PERMISSION_GRANTED) {
                list.add(network[i]);
                j++;
            }
        }

        if (list.size() > 0) {
            String[] get = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                get[i] = list.get(i);
            }
            ActivityCompat.requestPermissions(MainActivity.this, get, 0);
        } else
            addOverlay();
        return j;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
            boolean permission_granted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    permission_granted = false;
                }
            }
            if (!permission_granted)
                checkANDgetpermission();
            else {
                addOverlay();
            }
        }


    }
}
