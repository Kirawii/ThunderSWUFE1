package com.kirawii.thunderswufe.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.kirawii.thunderswufe.ThunderApplication;
import com.kirawii.thunderswufe.data.UserPreferences;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SettingsViewModel extends AndroidViewModel {
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<UserPreferences> userPreferences = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        loadPreferences();
    }

    public LiveData<UserPreferences> getUserPreferences() {
        return userPreferences;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    private void loadPreferences() {
        ThunderApplication app = (ThunderApplication) getApplication();
        disposables.add(app.getUserPreferencesManager().getRoomInfoSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        preferences -> userPreferences.setValue(preferences),
                        throwable -> error.setValue(throwable.getMessage())
                ));
    }

    public void savePreferences(String roomNo, String buildingNo, String areaNo) {
        ThunderApplication app = (ThunderApplication) getApplication();
        UserPreferences preferences = new UserPreferences(roomNo, buildingNo, areaNo, "");
        
        isSaving.setValue(true);
        error.setValue(null);
        saved.setValue(false);

        disposables.add(app.getUserPreferencesManager().saveRoomInfo(preferences)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            saved.setValue(true);
                        },
                        throwable -> {
                            isSaving.setValue(false);
                            error.setValue(throwable.getMessage());
                        }
                ));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
} 