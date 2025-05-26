package com.kirawii.thunderswufe.ui.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.kirawii.thunderswufe.ThunderApplication;
import com.kirawii.thunderswufe.data.ElectricityData;
import com.kirawii.thunderswufe.data.database.ElectricityDao;
import com.kirawii.thunderswufe.data.database.ElectricityRecord;
import com.kirawii.thunderswufe.work.ElectricityCheckWorker;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {
    private final ElectricityDao electricityDao;
    private final WorkManager workManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    private final BehaviorSubject<List<ElectricityRecord>> recordsSubject = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> isRefreshingSubject = BehaviorSubject.createDefault(false);
    
    // 添加新的 LiveData 对象
    private final MutableLiveData<List<ElectricityData>> usageHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ElectricityData> currentBalance = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HomeViewModel(Application application) {
        super(application);
        ThunderApplication app = (ThunderApplication) application;
        this.electricityDao = app.getDatabase().electricityDao();
        this.workManager = WorkManager.getInstance(application);

        // 订阅数据库变化
        disposables.add(
            electricityDao.getAllRecords()
                .subscribe(
                    records -> recordsSubject.onNext(records),
                    throwable -> {/* 处理错误 */}
                )
        );
    }

    public Flowable<List<ElectricityRecord>> getRecords() {
        return recordsSubject.toFlowable(BackpressureStrategy.LATEST);
    }

    public Flowable<Boolean> getIsRefreshing() {
        return isRefreshingSubject.toFlowable(BackpressureStrategy.LATEST);
    }
    
    // 添加新的 getter 方法
    public LiveData<List<ElectricityData>> getUsageHistory() {
        return usageHistory;
    }
    
    public LiveData<ElectricityData> getCurrentBalance() {
        return currentBalance;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }

    public void refreshData() {
        if (isRefreshingSubject.getValue() != null && isRefreshingSubject.getValue()) {
            return;
        }

        isRefreshingSubject.onNext(true);
        isLoading.setValue(true);
        error.setValue(null);

        // 创建一次性工作请求
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ElectricityCheckWorker.class)
                .build();
        workManager.enqueue(workRequest);

        // 模拟刷新完成的延迟
        disposables.add(
            io.reactivex.rxjava3.core.Observable.timer(1500, TimeUnit.MILLISECONDS)
                .subscribe(
                    aLong -> {
                        isRefreshingSubject.onNext(false);
                        isLoading.setValue(false);
                    },
                    throwable -> {
                        isRefreshingSubject.onNext(false);
                        isLoading.setValue(false);
                        error.setValue(throwable.getMessage());
                    }
                )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
} 