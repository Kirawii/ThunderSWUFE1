package com.kirawii.thunderswufe.network;

import com.google.gson.annotations.SerializedName;

/**
 * 电量信息响应模型
 */
public class ElectricityResponse {
    @SerializedName("code")
    private String code;
    
    @SerializedName("msg")
    private String message;
    
    @SerializedName("data")
    private ElectricityData data;
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ElectricityData getData() {
        return data;
    }
    
    public boolean isSuccess() {
        return "0".equals(code);
    }
    
    /**
     * 电量数据内部类
     */
    public static class ElectricityData {
        @SerializedName("balance")
        private double balance;
        
        @SerializedName("roomNo")
        private String roomNo;
        
        @SerializedName("meterStatus")
        private String meterStatus;
        
        @SerializedName("lastUpdateTime")
        private String lastUpdateTime;
        
        public double getBalance() {
            return balance;
        }
        
        public String getRoomNo() {
            return roomNo;
        }
        
        public String getMeterStatus() {
            return meterStatus;
        }
        
        public String getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
} 