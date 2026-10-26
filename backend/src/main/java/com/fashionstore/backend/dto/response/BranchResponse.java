package com.fashionstore.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BranchResponse(
        String branchId,
        String branchName,
        String address,
        String phoneNumber,
        Boolean active) {

    @JsonProperty("name")
    public String name() {
        return branchName;
    }

    @JsonProperty("phone")
    public String phone() {
        return phoneNumber;
    }

    @JsonProperty("maChiNhanh")
    public String maChiNhanh() {
        return branchId;
    }

    @JsonProperty("tenChiNhanh")
    public String tenChiNhanh() {
        return branchName;
    }

    @JsonProperty("diaChi")
    public String diaChi() {
        return address;
    }

    @JsonProperty("soDienThoai")
    public String soDienThoai() {
        return phoneNumber;
    }

    @JsonProperty("trangThai")
    public Boolean trangThai() {
        return active;
    }
}
