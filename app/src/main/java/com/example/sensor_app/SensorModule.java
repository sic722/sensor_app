package com.example.sensor_app;

import static android.os.SystemClock.elapsedRealtime;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

public class SensorModule implements SensorEventListener {
    private boolean flag_is_sensor_running = false;

    private Activity mActivity;
    private FileModule file;

    // Sensors
    SensorManager sm;
    Sensor s1, s2, s3, s4, s5, s6, s7, s8;

    // counter
    private int[] count = new int[8];   // 센서 측정 개수를 저장

    private int sensor_sampling_interval_ms = 10;   // 측정 간격
    private long sensor_measurement_start_time;

    // 센서 데이터 저장 공간
    float[] accL = new float[4];        // acc
    float[] gyroL = new float[4];       // gyro
    float[] magL = new float[4];        // mag
    float prox;                         // proximity
    float pres;                         // air pressure
    float[] quat = new float[4];        // quaternion
    float[] game_quat = new float[4];

    float[] rot_mat = new float[16];
    float[] rot_mat_opengl = new float[16];
    float[] game_rot_mat = new float[16];
    float[] orientation_angle = new float[4];
    float[] accW = new float[4];
    float[] gyroW = new float[4];

    // 현재까지 측정 결과
    String current_state = "";

    SensorModule(Activity activity){
        mActivity = activity;

        sm = (SensorManager) activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        s1 = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        s2 = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        s3 = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        s4 = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        s5 = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        s6 = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        s7 = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    public void start(long start_time, FileModule file_in){
        flag_is_sensor_running = true;
        sensor_measurement_start_time = start_time;
        file = file_in;

        sm.registerListener(this, s1, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, s2, SensorManager.SENSOR_DELAY_UI);
//        sm.registerListener(this, s3, SensorManager.SENSOR_DELAY_FASTEST);
//        sm.registerListener(this, s4, SensorManager.SENSOR_DELAY_FASTEST);
//        sm.registerListener(this, s5, SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, s6, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, s7, SensorManager.SENSOR_DELAY_UI);

        for (int i=0; i<8; i++)
            count[i] = 0;
    }

    public void stop(){
        flag_is_sensor_running = false;

        sm.unregisterListener((SensorEventListener) this, s1);
        sm.unregisterListener((SensorEventListener) this, s2);
        sm.unregisterListener((SensorEventListener) this, s3);
        sm.unregisterListener((SensorEventListener) this, s4);
        sm.unregisterListener((SensorEventListener) this, s5);
        sm.unregisterListener((SensorEventListener) this, s6);
        sm.unregisterListener((SensorEventListener) this, s7);

    }

    public float get_heading(){
        return orientation_angle[0] * 180f / 3.141592f;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d("SENSOR", "CALLED");
        float elapsed_time_s = (float)(elapsedRealtime()/1e3 - sensor_measurement_start_time/1e3);
        float elapsed_fw_time_s = (float) (sensorEvent.timestamp / 1e9 - sensor_measurement_start_time / 1e3);

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, accL, 0, 3);
            file.save_str_to_file(String.format("ACC, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, accL[0], accL[1], accL[2]));
            count[0] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            System.arraycopy(sensorEvent.values, 0, gyroL, 0, 3);
            file.save_str_to_file(String.format("GYRO, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, gyroL[0], gyroL[1], gyroL[2]));
            count[1] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            System.arraycopy(sensorEvent.values, 0, magL, 0, 3);
            file.save_str_to_file(String.format("MAG, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, magL[0], magL[1], magL[2]));
            count[2] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY){
            prox = sensorEvent.values[0];
            file.save_str_to_file(String.format("PROX, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, prox));
            count[3] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE){
            pres = sensorEvent.values[0];
            file.save_str_to_file(String.format("PRES, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, pres));
            count[4] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            System.arraycopy(sensorEvent.values, 0, quat, 0, 4);
            SensorManager.getRotationMatrixFromVector(rot_mat, quat);
            SensorManager.getOrientation(rot_mat, orientation_angle);
            Matrix.transposeM(rot_mat_opengl, 0, rot_mat, 0);
            Matrix.multiplyMV(accW, 0, rot_mat_opengl, 0, accL, 0);
            file.save_str_to_file(String.format("ROT_VEC, %f, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, quat[0], quat[1], quat[2], quat[3]));
            count[5] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR){
            System.arraycopy(sensorEvent.values, 0, game_quat, 0, 4);
            file.save_str_to_file(String.format("GAME_ROT_VEC, %f, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, game_quat[0], game_quat[1], game_quat[2], game_quat[3]));
            count[6] += 1;
        }

        String str = "";
        str += String.format("Acc x: %2.3f, y: %2.3f, z: %2.3f\n", accL[0], accL[1], accL[2]);
        str += String.format("Gyro x: %2.3f, y: %2.3f, z: %2.3f\n", gyroL[0], gyroL[1], gyroL[2]);
        str += String.format("Mag x: %2.3f, y: %2.3f, z: %2.3f\n", magL[0], magL[1], magL[2]);
        str += String.format("Prox: %2.3f cm\n", prox);
        str += String.format("Pressure: %2.3f hPa\n", pres);
        str += String.format("Heading: %f, pitch: %f, roll: %f\n",
                orientation_angle[0]*180/3.1415, orientation_angle[1]*180/3.1415, orientation_angle[2]*180/3.1415);
        str += String.format("AccW x: %2.3f, y: %2.3f, z: %2.3f",
                accW[0], accW[1], accW[2]);
        current_state = str;
    }

    public String get_latest_state(){
        return current_state;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
