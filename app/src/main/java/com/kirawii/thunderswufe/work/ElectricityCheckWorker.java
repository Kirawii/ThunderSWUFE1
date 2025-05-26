package com.kirawii.thunderswufe.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.kirawii.thunderswufe.ThunderApplication;
import com.kirawii.thunderswufe.data.database.ElectricityDatabase;
import com.kirawii.thunderswufe.data.ElectricityData;
import com.kirawii.thunderswufe.data.database.ElectricityRecord;
import com.kirawii.thunderswufe.network.ElectricityService;
import com.kirawii.thunderswufe.network.ElectricityResponse;
import java.time.LocalDateTime;

public class ElectricityCheckWorker extends Worker {
    private static final String TAG = "ElectricityCheckWorker";
    private final ElectricityService electricityService;
    private final ElectricityDatabase database;

    public ElectricityCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        ThunderApplication app = (ThunderApplication) context.getApplicationContext();
        this.electricityService = app.getElectricityService();
        this.database = app.getDatabase();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "开始执行电量检查任务");
        
        try {
            ElectricityResponse response = electricityService.getCurrentElectricityData().blockingGet();
            
            if (response == null || !response.isSuccess()) {
                Log.w(TAG, "获取电量数据失败，跳过检查");
                return Result.failure();
            }
            
            ElectricityResponse.ElectricityData responseData = response.getData();
            if (responseData == null) {
                Log.w(TAG, "电量数据为空，跳过检查");
                return Result.failure();
            }
            
            // 创建 ElectricityData 对象
            ElectricityData data = new ElectricityData(
                LocalDateTime.now(),
                responseData.getBalance(),
                0.0, // 变化量需要计算或者从其他地方获取
                responseData.getRoomNo(),
                responseData.getMeterStatus(),
                responseData.getLastUpdateTime()
            );
            
            // 将 ElectricityData 转换为 ElectricityRecord
            ElectricityRecord record = new ElectricityRecord(
                data.getTimestamp(),
                data.getBalance(),
                data.getChange(),
                data.getRoomNo()
            );
            
            database.electricityDao().insertRecord(record);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "执行电量检查任务时发生错误", e);
            return Result.retry();
        }
    }
} 