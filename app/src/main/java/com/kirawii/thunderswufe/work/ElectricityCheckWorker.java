package com.kirawii.thunderswufe.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;
import com.kirawii.thunderswufe.ThunderApplication;
import com.kirawii.thunderswufe.data.database.ElectricityDatabase;
import com.kirawii.thunderswufe.data.model.ElectricityData;
import com.kirawii.thunderswufe.data.network.ElectricityService;
import io.reactivex.rxjava3.core.Single;

public class ElectricityCheckWorker extends RxWorker {
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
    public Single<Result> createWork() {
        Log.i(TAG, "开始执行电量检查任务");
        
        return electricityService.getCurrentElectricityData()
                .flatMap(data -> {
                    if (data == null) {
                        Log.w(TAG, "获取电量数据失败，跳过检查");
                        return Single.just(Result.failure());
                    }
                    return database.electricityDao().insertRecord(data)
                            .andThen(Single.just(Result.success()));
                })
                .onErrorReturn(e -> {
                    Log.e(TAG, "执行电量检查任务时发生错误", e);
                    return Result.retry();
                });
    }
} 