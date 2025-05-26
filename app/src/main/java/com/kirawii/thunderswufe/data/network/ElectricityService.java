package com.kirawii.thunderswufe.data.network;

import com.kirawii.thunderswufe.data.model.ElectricityData;
import io.reactivex.rxjava3.core.Single;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ElectricityService {
    @GET("easytong_app/GetPayAccInfoNew")
    Single<ElectricityData> getCurrentElectricityData(
        @Header("Authorization") String authToken,
        @Query("roomNo") String roomNo,
        @Query("buildingNo") String buildingNo
    );
} 