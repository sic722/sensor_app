package com.example.sensor_app;

import static android.content.Context.WIFI_SERVICE;
import static android.os.SystemClock.elapsedRealtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.List;

public class WifiModule{

    WifiManager wifiManager;
    WifiReceiver wifiReceiver;

    // WiFi 스캔 관련 변수들
    boolean flag_running = false;
    int scan_counter = 0;
    long last_scan_time_ms = elapsedRealtime();
    final int scan_interval_ms = 5000;

    // 스레드 관련
    Looper wifi_scan_looper;

    // 마지막 실행
    String current_state = "";

    // 디버깅 관련
    String TAG = "WIFI_MODULE";

    WifiModule(Context context){
        // 와이파이 리시버를 필터와 함께 등록함
        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiReceiver, intentFilter);

        // 스레드 생성하고 시작
        HandlerThread handlerThread = new HandlerThread(
                "WIFI_THREAD", Process.THREAD_PRIORITY_DISPLAY);
        handlerThread.start();
        wifi_scan_looper = handlerThread.getLooper();
    }

    public String get_latest_state(){
        return current_state;
    }

    public void start(){
        flag_running = true;
        scan_counter = 0;

        invoke_wifi_scan_thread();
    }

    private void invoke_wifi_scan_thread(){
        if (!flag_running)
            return;

        wifiManager.startScan();
        scan_counter += 1;
        last_scan_time_ms = elapsedRealtime();

        Handler handler = new Handler(wifi_scan_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invoke_wifi_scan_thread();
            }
        }, scan_interval_ms);
    }

    public void stop(){
        flag_running = false;
    }

    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Scan results received");
            List<ScanResult> scanResults = wifiManager.getScanResults();

            float P0 = -30;
            float eta = 2;
            float dist;

            String str = "";
            str += "Found " + scanResults.size() + " APs\n";
            for (int k=0; k<scanResults.size(); k++) {
                str += scanResults.get(k).SSID;
                str += ", " + scanResults.get(k).BSSID;
                str += ", " + scanResults.get(k).frequency + " MHz";
                str += ", " + scanResults.get(k).level + "dBm";

                float curr_p = scanResults.get(k).level;
                dist = (float) Math.pow(10, (P0 - curr_p) / eta /10);
                str += String.format(", dist: %.2f m\n", dist);
            }
            current_state = "Scan counter: " + scan_counter + ", # APs: " + scanResults.size();
        }
    }

}
