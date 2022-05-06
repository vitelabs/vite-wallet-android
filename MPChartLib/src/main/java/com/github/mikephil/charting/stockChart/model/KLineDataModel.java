package com.github.mikephil.charting.stockChart.model;

import java.io.Serializable;

/**
 * Created by Administrator on 2016/9/8.
 */
public class KLineDataModel implements Serializable {
    public String m_szInstrumentID;// 合约ID

    //时间戳
    private Long dateMills = 0L;
    private double high;// 最高价
    private double low;// 最低价
    private double open;// 开盘价
    private double close;// 收盘价
    private double volume;// 成交量
    private double total;// 成交额
    private double preClose;// 昨收价
    private double ma7;
    private double ma90;

    public double getMa7() {
        return ma7;
    }

    public void setMa7(double ma7) {
        this.ma7 = ma7;
    }

    public double getMa90() {
        return ma90;
    }

    private double ma30;

    public void setMa90(double ma90) {
        this.ma90 = ma90;
    }

    public Long getDateMills() {
        return dateMills;
    }

    public void setDateMills(Long dateMills) {
        this.dateMills = dateMills;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPreClose() {
        return preClose;
    }

    public void setPreClose(double preClose) {
        this.preClose = preClose;
    }

    public double getMa30() {
        return ma30;
    }

    public void setMa30(double ma30) {
        this.ma30 = ma30;
    }
}
