package com.kirawii.thunderswufe.network;

import com.google.gson.annotations.SerializedName;

public class ElectricityResponse {
    @SerializedName("balance")
    private String balance;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ElectricityResponse{" +
                "balance='" + balance + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
} 