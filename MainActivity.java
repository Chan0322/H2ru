package com.example.forcapstone2;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private ImageButton informationButton;
    private Switch switch1;
    private Switch switch2;

    private FrameLayout lightThemeLayout;
    private FrameLayout darkThemeLayout;
    private ImageView buttonSetting;
    private ImageView statisticsIcon;
    private ImageView bluetoothIcon;
    private ImageView reloadIcon;
    private int currentAmount = 0;
    private TextView nowAmount;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            nowAmount.setText(BluetoothService.receivedMessage);
        }
    };
    // 추가============================================================================================================
    private BluetoothService bluetoothService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            isBound = true;
            Log.d("MainActivity", "ServiceConnection-onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    //======================================================================================================================

    private static final int REQUEST_PERMISSIONS = 1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyApp myApp = (MyApp) getApplication();

        // Request necessary permissions
        requestPermissions();

        createNotificationChannel();

        // Initialize views
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);
        lightThemeLayout = findViewById(R.id.lightThemeLayout);
        darkThemeLayout = findViewById(R.id.darkThemeLayout);

        nowAmount = findViewById(R.id.nowAmount);
        ImageView button = findViewById(R.id.button);
        ImageView button2 = findViewById(R.id.button2);
        buttonSetting = findViewById(R.id.buttonSetting);
        statisticsIcon = findViewById(R.id.statisticsIcon);
        bluetoothIcon = findViewById(R.id.bluetoothIcon);
        reloadIcon = findViewById(R.id.reloadIcon);
        informationButton = findViewById(R.id.informationButton);

        // activity_main.xml의 TextView에 목표량 연결
        TextView purposewaterAmountText = findViewById(R.id.PurposewaterAmountText);
        String goalAmountText = "목표량 " + String.valueOf((float) myApp.getGoalAmount() / 1000 + "L");
        purposewaterAmountText.setText(goalAmountText);

        setMainTextVeiw(myApp.getTodayAmount(), myApp.getGoalAmount(), myApp.getBeforeAmount());

        createNotificationChannel();
        resetAlarm(MainActivity.this);

        // 추가================================================================================================================================
        // Bind to BluetoothService
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Set the initial theme to light mode
        setInitialTheme();

        //        new Handler(Looper.getMainLooper()) {
        //            @Override
        //            public void handleMessage(@NonNull Message msg) {
        //                super.handleMessage(msg);
        //            }
        //        }.post(() -> bluetoothService.readData());


        // Set click listeners
        button.setOnClickListener(v -> {
            //bluetoothService.readData();

            myApp.getTodayAmount();
            if (myApp.getTodayAmount() > myApp.getGoalAmount()) {
                createNotification();
                myApp.drainAmount();
            }
            myApp.drainAmount();

            TextView waterAmountText = findViewById(R.id.waterAmountText);
            String todayAmountText = String.valueOf(myApp.getTodayAmount()) + "mL";
            waterAmountText.setText(todayAmountText);

            setMainTextVeiw(myApp.getTodayAmount(), myApp.getGoalAmount(), myApp.getBeforeAmount());

            Toast.makeText(getApplicationContext(), "물을 버렸어요!", Toast.LENGTH_SHORT).show();

            // Send data 'A' to Arduino
            if (isBound) {
                bluetoothService.sendData("A");
            }

            // Send data 'C' to Arduino
            if (isBound) {
                bluetoothService.sendData("C");
            }

            // Send data 'D' to Arduino
            if (isBound) {
                bluetoothService.sendData("D");
            }

            // Send data 'E' to Arduino
            if (isBound) {
                bluetoothService.sendData("E");
            }
        });

        button2.setOnClickListener(view -> {
            myApp.getTodayAmount();
            if (myApp.getTodayAmount() > myApp.getGoalAmount()) {
                createNotification();
                myApp.drinkAmount();
            }
            myApp.drinkAmount();

            TextView waterAmountText = findViewById(R.id.waterAmountText);
            String todayAmountText = String.valueOf(myApp.getTodayAmount()) + "mL";
            waterAmountText.setText(todayAmountText);

            int percentage = (int) ((float) myApp.getTodayAmount() / myApp.getGoalAmount() * 100);
            TextView amountPercent = findViewById(R.id.amountPercent);
            String percent = String.valueOf(percentage) + " %";
            amountPercent.setText(percent);

            // 완성되면 지울 것. 테스트용

            String now = "현재 텀블러 측정값 : " + String.valueOf(myApp.getBeforeAmount());


            Toast.makeText(getApplicationContext(), "물을 추가했어요!", Toast.LENGTH_SHORT).show();

        });

        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchToDarkTheme();
            } else {
                switchToLightTheme();
            }
        });

        switch2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchToDarkTheme();
            } else {
                switchToLightTheme();
            }
        });

        buttonSetting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        statisticsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Stat.class);
            startActivity(intent);
        });

        bluetoothIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BluetoothConnect.class);
            startActivity(intent);
        });

        new Runnable() {
            @Override
            public void run() {
//                handler.postDelayed(() -> {
                if (bluetoothService != null) {
                    bluetoothService.readData();
                    Message message = handler.obtainMessage();
                    handler.sendMessage(message);
                }
//                }, 3000);
                handler.postDelayed(this, 3000);
            }
        }.run();


        reloadIcon.setOnClickListener(v -> {

            Toast.makeText(getApplicationContext(), "새로고침 성공!", Toast.LENGTH_SHORT).show();

            myApp.reloadAmount();
        });

        informationButton.setOnClickListener(v -> showInformationPopup());
    }

    // 추가================================================================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void resetAlarm(MainActivity mainActivity) {
    }

    private void setMainTextVeiw(int todayAmount, int goalAmount, int beforeAmount) {
        // activity_main.xml의 TextView에 연결해서 퍼센트 계산
        int percentage = (int) ((float) todayAmount / goalAmount * 100);
        TextView amountPercent = findViewById(R.id.amountPercent);
        String percent = String.valueOf(percentage) + " %";
        amountPercent.setText(percent);

        // activity_main.xml의 TextView에 오늘 마신양 연결
        TextView waterAmountText = findViewById(R.id.waterAmountText);
        String todayAmountText = String.valueOf(todayAmount) + "mL";
        waterAmountText.setText(todayAmountText);

        // 완성되면 지울 것. 테스트용
        TextView nowAmount = findViewById(R.id.nowAmount);
        String now = "현재 물 측정값 : " + String.valueOf(beforeAmount);
        nowAmount.setText(now);
    }


    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_PERMISSIONS);
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_PERMISSIONS);
            }
        }
    }

    private void setInitialTheme() {
        lightThemeLayout.setVisibility(View.VISIBLE);
        darkThemeLayout.setVisibility(View.GONE);
        switch1.setChecked(false);
        switch2.setChecked(false);
        switch2.setChecked(false);
        updateIconsForLightTheme();
    }

    private void switchToDarkTheme() {
        lightThemeLayout.setVisibility(View.GONE);
        darkThemeLayout.setVisibility(View.VISIBLE);
        switch1.setChecked(true);
        switch2.setChecked(true);
        updateIconsForDarkTheme();
    }

    private void switchToLightTheme() {
        lightThemeLayout.setVisibility(View.VISIBLE);
        darkThemeLayout.setVisibility(View.GONE);
        switch1.setChecked(false);
        switch2.setChecked(false);
        updateIconsForLightTheme();
    }

    private void updateIconsForLightTheme() {
        buttonSetting.setImageResource(R.drawable.settings);
        statisticsIcon.setImageResource(R.drawable.chart);
        bluetoothIcon.setImageResource(R.drawable.bluetooth);
        reloadIcon.setImageResource(R.drawable.reload);
    }

    private void updateIconsForDarkTheme() {
        buttonSetting.setImageResource(R.drawable.settings_w);
        statisticsIcon.setImageResource(R.drawable.chart_w);
        bluetoothIcon.setImageResource(R.drawable.bluetooth_w);
        reloadIcon.setImageResource(R.drawable.reload_w);
    }

    private void showInformationPopup() {
        Dialog dialog = new Dialog(MainActivity.this, R.style.RoundedDialog);
        dialog.setContentView(R.layout.popup_activity);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        params.width = (int) (width * 0.85);
        params.height = (int) (height * 0.7);
        params.gravity = Gravity.CENTER;

        window.setAttributes(params);
        dialog.show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("GOALATTAINMENT_CHANNEL_ID", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "GOALATTAINMENT_CHANNEL_ID")
                .setSmallIcon(R.drawable.hirue)
                .setContentTitle("목표 달성 알림")
                .setContentText("축하합니다~!! 일일 목표 달성했습니다!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    public static void resetAlarm(Context context) {
        AlarmManager resetAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent resetIntent = new Intent(context, Initialize.class);
        PendingIntent resetSender = PendingIntent.getBroadcast(context, 0, resetIntent, PendingIntent.FLAG_IMMUTABLE);

        // 자정 시간
        Calendar resetCal = Calendar.getInstance();
        resetCal.setTimeInMillis(System.currentTimeMillis());
        resetCal.set(Calendar.HOUR_OF_DAY, 0);
        resetCal.set(Calendar.MINUTE, 0);
        resetCal.set(Calendar.SECOND, 0);

        //다음날 0시에 맞추기 위해 24시간을 뜻하는 상수인 AlarmManager.INTERVAL_DAY를 더해줌.
        resetAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, resetCal.getTimeInMillis()
                + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, resetSender);


        SimpleDateFormat format1 = new SimpleDateFormat("MM/dd kk:mm:ss");
        String setResetTime = format1.format(new Date(resetCal.getTimeInMillis() + AlarmManager.INTERVAL_DAY));

        Log.d("resetAlarm", "ResetHour : " + setResetTime);

    }

}