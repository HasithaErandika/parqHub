package com.prmplatform.parqhub.model;

public class VehicleDTO {
    private Long id;
    private String vehicleNo;
    private String vehicleType;

    public VehicleDTO(Long id, String vehicleNo, String vehicleType) {
        this.id = id;
        this.vehicleNo = vehicleNo;
        this.vehicleType = vehicleType;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }
}