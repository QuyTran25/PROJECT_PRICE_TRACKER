package com.pricetracker.models;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * PriceHistory - Lưu thông tin biến động giá của sản phẩm theo thời gian
 */
public class PriceHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    private int priceId;
    private int productId;
    private double price;
    private Timestamp capturedAt;

    public PriceHistory() {}

    public PriceHistory(int productId, double price, Timestamp capturedAt) {
        this.productId = productId;
        this.price = price;
        this.capturedAt = capturedAt;
    }

    public int getPriceId() {
        return priceId;
    }

    public void setPriceId(int priceId) {
        this.priceId = priceId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Timestamp getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Timestamp capturedAt) {
        this.capturedAt = capturedAt;
    }

    @Override
    public String toString() {
        return "PriceHistory{" +
                "priceId=" + priceId +
                ", productId=" + productId +
                ", price=" + price +
                ", capturedAt=" + capturedAt +
                '}';
    }
}
