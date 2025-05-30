package com.kirawii.thunderswufe.data.preferences;

import java.util.Objects;

public class RoomInfo {
    private String roomNo;
    private String buildingNo;
    private String areaNo;

    public RoomInfo() {
        this("", "", "");
    }

    public RoomInfo(String roomNo, String buildingNo, String areaNo) {
        this.roomNo = roomNo;
        this.buildingNo = buildingNo;
        this.areaNo = areaNo;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public void setBuildingNo(String buildingNo) {
        this.buildingNo = buildingNo;
    }

    public String getAreaNo() {
        return areaNo;
    }

    public void setAreaNo(String areaNo) {
        this.areaNo = areaNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomInfo roomInfo = (RoomInfo) o;
        return Objects.equals(roomNo, roomInfo.roomNo) && Objects.equals(buildingNo, roomInfo.buildingNo) && Objects.equals(areaNo, roomInfo.areaNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNo, buildingNo, areaNo);
    }

    @Override
    public String toString() {
        return "RoomInfo{" +
                "roomNo='" + roomNo + '\'' +
                ", buildingNo='" + buildingNo + '\'' +
                ", areaNo='" + areaNo + '\'' +
                '}';
    }
}
