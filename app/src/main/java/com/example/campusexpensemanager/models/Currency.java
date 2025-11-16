package com.example.campusexpensemanager.models;

/**
 * Model class cho Tiền tệ (Currency)
 * Lưu trữ mã, tỷ giá, và biểu tượng
 */
public class Currency {
    private int id;
    private String code; // e.g., "VND", "USD"
    private double rateToVND; // Tỷ giá quy đổi về VND
    private String symbol; // e.g., "đ", "$"

    public Currency(int id, String code, double rateToVND, String symbol) {
        this.id = id;
        this.code = code;
        this.rateToVND = rateToVND;
        this.symbol = symbol;
    }

    // Getters
    public int getId() { return id; }
    public String getCode() { return code; }
    public double getRateToVND() { return rateToVND; }
    public String getSymbol() { return symbol; }

    /**
     * Dùng cho Spinner
     */
    @Override
    public String toString() {
        return code; // Hiển thị "VND" hoặc "USD" trong Spinner
    }
}