package com.github.mikephil.charting.stockChart.dataManage;

import android.content.Context;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.R;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.stockChart.model.KLineDataModel;
import com.github.mikephil.charting.stockChart.model.bean.BOLLEntity;
import com.github.mikephil.charting.stockChart.model.bean.KDJEntity;
import com.github.mikephil.charting.stockChart.model.bean.MACDEntity;
import com.github.mikephil.charting.stockChart.model.bean.RSIEntity;
import com.github.mikephil.charting.utils.DataTimeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * K线数据解析
 */
public class KLineDataManage {
    //MA参数
    public int N1 = 5;
    public int N2 = 10;
    public int N3 = 20;
    //EMA参数
    public int EMAN1 = 5;
    public int EMAN2 = 10;
    public int EMAN3 = 30;
    //SMA参数
    public int SMAN = 14;
    //BOLL参数
    public int BOLLN = 26;
    //MACD参数
    public int SHORT = 12;
    public int LONG = 26;
    public int M = 9;
    //KDJ参数
    public int KDJN = 9;
    public int KDJM1 = 3;
    public int KDJM2 = 3;
    //CCI参数
    public int CCIN = 14;
    //RSI参数
    public int RSIN1 = 6;
    public int RSIN2 = 12;
    public int RSIN3 = 24;
    float sum = 0;
    private Context mContext;
    private ArrayList<KLineDataModel> kDatas = new ArrayList<>();
    private float offSet = 0f;//K线图最右边偏移量
    private int precision;
    //    private boolean landscape = false;//横屏还是竖屏
    //X轴数据
    private ArrayList<String> xVal = new ArrayList<>();
    private CandleDataSet candleDataSet;//蜡烛图集合
    private BarDataSet volumeDataSet;//成交量集合
    private BarDataSet barDataMACD;//MACD集合
    private CandleDataSet bollCandleDataSet;//BOLL蜡烛图集合
    private List<ILineDataSet> lineDataMA = new ArrayList<>();
    private List<ILineDataSet> lineDataMACD = new ArrayList<>();
    private ArrayList<BarEntry> macdData = new ArrayList<>();
    private ArrayList<Entry> deaData = new ArrayList<>();
    private ArrayList<Entry> difData = new ArrayList<>();
    private List<ILineDataSet> lineDataKDJ = new ArrayList<>();
    private ArrayList<Entry> kData = new ArrayList<>();
    private ArrayList<Entry> dData = new ArrayList<>();
    private ArrayList<Entry> jData = new ArrayList<>();
    private List<ILineDataSet> lineDataBOLL = new ArrayList<>();
    private ArrayList<Entry> bollDataUP = new ArrayList<>();
    private ArrayList<Entry> bollDataMB = new ArrayList<>();
    private ArrayList<Entry> bollDataDN = new ArrayList<>();
    private List<ILineDataSet> lineDataRSI = new ArrayList<>();
    private ArrayList<Entry> rsiData6 = new ArrayList<>();
    private ArrayList<Entry> rsiData12 = new ArrayList<>();
    private ArrayList<Entry> rsiData24 = new ArrayList<>();
    private double preClosePrice;//K线图昨收价
    private boolean landscape;

    public KLineDataManage(Context context) {
        mContext = context;
    }

    /**
     * 解析K线数据
     */
    public void parseKlineData(JSONObject object, int precision, boolean landscape) {
        this.precision = precision;
        this.landscape = landscape;
        if (object != null) {
            kDatas.clear();
            lineDataMA.clear();
            JSONArray dataT = object.optJSONArray("t");
            JSONArray dataC = object.optJSONArray("c");
            JSONArray dataO = object.optJSONArray("p");
            JSONArray dataH = object.optJSONArray("h");
            JSONArray dataL = object.optJSONArray("l");
            JSONArray dataV = object.optJSONArray("v");

            xVal.clear();
            ArrayList<CandleEntry> candleEntries = new ArrayList<>();
            ArrayList<BarEntry> barEntries = new ArrayList<>();
            ArrayList<Entry> line5Entries = new ArrayList<>();
            ArrayList<Entry> line10Entries = new ArrayList<>();
            ArrayList<Entry> line20Entries = new ArrayList<>();


            double[] ma7s = calcMA(dataC, 7);
            double[] ma30s = calcMA(dataC, 30);
            double[] ma90s = calcMA(dataC, 90);

            for (int i = 0; i < dataT.length(); i++) {
                KLineDataModel klineDatamodel = new KLineDataModel();
                try {
                    klineDatamodel.setDateMills(dataT.getLong(i) * 1000);
                    klineDatamodel.setOpen(dataO.getDouble(i));
                    klineDatamodel.setHigh(dataH.getDouble(i));
                    klineDatamodel.setLow(dataL.getDouble(i));
                    klineDatamodel.setClose(dataC.getDouble(i));
                    klineDatamodel.setVolume(Double.isNaN(dataV.getDouble(i)) ? 0 : dataV.getDouble(i));
//                    klineDatamodel.setTotal(NumberUtils.stringNoE10ForVol(Double.isNaN(dataV.getLong(i)) ? 0 : dataV.getLong(i)));

                    klineDatamodel.setMa7(ma7s[i]);
                    klineDatamodel.setMa30(ma30s[i]);
                    klineDatamodel.setMa90(ma90s[i]);

                    if (i > 1) { //TODO maybe nothing useful
                        klineDatamodel.setPreClose(dataC.getDouble(i - 1));
                    } else {
                        klineDatamodel.setPreClose(dataO.getDouble(i));
                    }

                    preClosePrice = klineDatamodel.getPreClose();
                    kDatas.add(klineDatamodel);

                    xVal.add(DataTimeUtil.secToDate(getKLineDatas().get(i).getDateMills()));
                    candleEntries.add(new CandleEntry(i + offSet, (float) getKLineDatas().get(i).getHigh(), (float) getKLineDatas().get(i).getLow(), (float) getKLineDatas().get(i).getOpen(), (float) getKLineDatas().get(i).getClose()));

                    float color = getKLineDatas().get(i).getOpen() > getKLineDatas().get(i).getClose() ? 0f : 1f;
                    barEntries.add(new BarEntry(i + offSet, (float) getKLineDatas().get(i).getVolume(), color));

                    if ((float) getKLineDatas().get(i).getMa7() >= 0)
                        line5Entries.add(new Entry(i, (float) getKLineDatas().get(i).getMa7()));
                    if ((float) getKLineDatas().get(i).getMa30() >= 0)
                        line10Entries.add(new Entry(i, (float) getKLineDatas().get(i).getMa30()));
                    if ((float) getKLineDatas().get(i).getMa90() >= 0)
                        line20Entries.add(new Entry(i, (float) getKLineDatas().get(i).getMa90()));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            candleDataSet = setACandle(candleEntries);
            bollCandleDataSet = setBOLLCandle(candleEntries);
            volumeDataSet = setABar(barEntries, "成交量");
            lineDataMA.add(setALine(ColorType.blue, line5Entries, false));
            lineDataMA.add(setALine(ColorType.yellow, line10Entries, false));
            lineDataMA.add(setALine(ColorType.purple, line20Entries, false));
        }
    }

    /**
     * 初始化自己计算MACD
     */
    public void initMACD() {
        MACDEntity macdEntity = new MACDEntity(getKLineDatas(), SHORT, LONG, M);

        macdData = new ArrayList<>();
        deaData = new ArrayList<>();
        difData = new ArrayList<>();
        for (int i = 0; i < macdEntity.getMACD().size(); i++) {
            macdData.add(new BarEntry(i + offSet, macdEntity.getMACD().get(i), macdEntity.getMACD().get(i)));
            deaData.add(new Entry(i + offSet, macdEntity.getDEA().get(i)));
            difData.add(new Entry(i + offSet, macdEntity.getDIF().get(i)));
        }
        barDataMACD = setABar(macdData);
        lineDataMACD.add(setALine(ColorType.blue, deaData));
        lineDataMACD.add(setALine(ColorType.yellow, difData));
    }

    /**
     * 初始化自己计算KDJ
     */
    public void initKDJ() {
        KDJEntity kdjEntity = new KDJEntity(getKLineDatas(), KDJN, KDJM1, KDJM2);

        kData = new ArrayList<>();
        dData = new ArrayList<>();
        jData = new ArrayList<>();
        for (int i = 0; i < kdjEntity.getD().size(); i++) {
            kData.add(new Entry(i + offSet, kdjEntity.getK().get(i)));
            dData.add(new Entry(i + offSet, kdjEntity.getD().get(i)));
            jData.add(new Entry(i + offSet, kdjEntity.getJ().get(i)));
        }
        lineDataKDJ.add(setALine(ColorType.blue, kData, "KDJ" + N1, false));
        lineDataKDJ.add(setALine(ColorType.yellow, dData, "KDJ" + N2, false));
        lineDataKDJ.add(setALine(ColorType.purple, jData, "KDJ" + N3, true));
    }

    /**
     * 初始化自己计算BOLL
     */
    public void initBOLL() {
        BOLLEntity bollEntity = new BOLLEntity(getKLineDatas(), BOLLN);
        bollDataUP = new ArrayList<>();
        bollDataMB = new ArrayList<>();
        bollDataDN = new ArrayList<>();
        for (int i = 0; i < bollEntity.getUPs().size(); i++) {
            bollDataUP.add(new Entry(i + offSet, bollEntity.getUPs().get(i)));
            bollDataMB.add(new Entry(i + offSet, bollEntity.getMBs().get(i)));
            bollDataDN.add(new Entry(i + offSet, bollEntity.getDNs().get(i)));
        }
        lineDataBOLL.add(setALine(ColorType.blue, bollDataUP, false));
        lineDataBOLL.add(setALine(ColorType.yellow, bollDataMB, false));
        lineDataBOLL.add(setALine(ColorType.purple, bollDataDN, false));
    }

    /**
     * 初始化自己计算RSI
     */
    public void initRSI() {
        RSIEntity rsiEntity6 = new RSIEntity(getKLineDatas(), RSIN1);
        RSIEntity rsiEntity12 = new RSIEntity(getKLineDatas(), RSIN2);
        RSIEntity rsiEntity24 = new RSIEntity(getKLineDatas(), RSIN3);

        rsiData6 = new ArrayList<>();
        rsiData12 = new ArrayList<>();
        rsiData24 = new ArrayList<>();
        for (int i = 0; i < rsiEntity6.getRSIs().size(); i++) {
            rsiData6.add(new Entry(i + offSet, rsiEntity6.getRSIs().get(i)));
            rsiData12.add(new Entry(i + offSet, rsiEntity12.getRSIs().get(i)));
            rsiData24.add(new Entry(i + offSet, rsiEntity24.getRSIs().get(i)));
        }
        lineDataRSI.add(setALine(ColorType.blue, rsiData6, "RSI" + RSIN1, true));
        lineDataRSI.add(setALine(ColorType.yellow, rsiData12, "RSI" + RSIN2, false));
        lineDataRSI.add(setALine(ColorType.purple, rsiData24, "RSI" + RSIN3, false));
    }

    private CandleDataSet setACandle(ArrayList<CandleEntry> candleEntries) {
        CandleDataSet candleDataSet = new CandleDataSet(candleEntries, "蜡烛线");
        candleDataSet.setDrawHorizontalHighlightIndicator(true);
        candleDataSet.setHighlightEnabled(true);
        candleDataSet.setHighLightColor(ContextCompat.getColor(mContext, R.color.highLight_Color));
        candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        candleDataSet.setDecreasingColor(ContextCompat.getColor(mContext, R.color.down_color));
        candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setIncreasingColor(ContextCompat.getColor(mContext, R.color.up_color));
        candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setNeutralColor(ContextCompat.getColor(mContext, R.color.equal_color));
        candleDataSet.setShadowColorSameAsCandle(true);
        candleDataSet.setValueTextSize(10);
        candleDataSet.setDrawValues(true);

        return candleDataSet;
    }

    private CandleDataSet setBOLLCandle(ArrayList<CandleEntry> candleEntries) {
        CandleDataSet candleDataSet = new CandleDataSet(candleEntries, "BOLL叠加蜡烛线");
        candleDataSet.setDrawHorizontalHighlightIndicator(false);
        candleDataSet.setHighlightEnabled(true);
        candleDataSet.setHighLightColor(ContextCompat.getColor(mContext, R.color.highLight_Color));
        candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        candleDataSet.setDecreasingColor(ContextCompat.getColor(mContext, R.color.down_color));
        candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setIncreasingColor(ContextCompat.getColor(mContext, R.color.up_color));
        candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setNeutralColor(ContextCompat.getColor(mContext, R.color.equal_color));
        candleDataSet.setDrawValues(false);
        candleDataSet.setDrawIcons(false);
        candleDataSet.setShowCandleBar(false);
        return candleDataSet;
    }

    private LineDataSet setALine(ColorType ma, ArrayList<Entry> lineEntries) {
        String label = "ma" + ma;
        return setALine(ma, lineEntries, label);
    }

    private LineDataSet setALine(ColorType ma, ArrayList<Entry> lineEntries, boolean highlightEnable) {
        String label = "ma" + ma;
        return setALine(ma, lineEntries, label, highlightEnable);
    }

    private LineDataSet setALine(ColorType ma, ArrayList<Entry> lineEntries, String label) {
        boolean highlightEnable = false;
        return setALine(ma, lineEntries, label, highlightEnable);
    }

    private LineDataSet setALine(ColorType colorType, ArrayList<Entry> lineEntries, String label, boolean highlightEnable) {
        LineDataSet lineDataSetMa = new LineDataSet(lineEntries, label);
        lineDataSetMa.setDrawHorizontalHighlightIndicator(false);
        lineDataSetMa.setHighlightEnabled(true);
        lineDataSetMa.setHighLightColor(ContextCompat.getColor(mContext, R.color.highLight_Color));
        lineDataSetMa.setDrawValues(false);
        if (colorType == ColorType.blue) {
            lineDataSetMa.setColor(ContextCompat.getColor(mContext, R.color.ma5));
        } else if (colorType == ColorType.yellow) {
            lineDataSetMa.setColor(ContextCompat.getColor(mContext, R.color.ma10));
        } else if (colorType == ColorType.purple) {
            lineDataSetMa.setColor(ContextCompat.getColor(mContext, R.color.ma20));
        }
        lineDataSetMa.setLineWidth(0.6f);
        lineDataSetMa.setDrawCircles(false);
        lineDataSetMa.setAxisDependency(YAxis.AxisDependency.LEFT);
        return lineDataSetMa;
    }

    private BarDataSet setABar(ArrayList<BarEntry> barEntries) {
        String label = "BarDataSet";
        return setABar(barEntries, label);
    }

    private BarDataSet setABar(ArrayList<BarEntry> barEntries, String label) {
        BarDataSet barDataSet = new BarDataSet(barEntries, label);
        barDataSet.setHighlightEnabled(true);
        barDataSet.setHighLightColor(ContextCompat.getColor(mContext, R.color.highLight_Color));
        barDataSet.setValueTextSize(10);
        barDataSet.setDrawValues(false);
        barDataSet.setNeutralColor(ContextCompat.getColor(mContext, R.color.equal_color));
        barDataSet.setIncreasingColor(ContextCompat.getColor(mContext, R.color.up_color));
        barDataSet.setDecreasingColor(ContextCompat.getColor(mContext, R.color.down_color));
        barDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        barDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        return barDataSet;
    }

    private float getSum(Integer a, Integer b) {
        sum = 0;
        if (a < 0) {
            return 0;
        }
        for (int i = a; i <= b; i++) {
            sum += getKLineDatas().get(i).getClose();
        }
        return sum;
    }

    public void addAKLineData(KLineDataModel kLineData) {
        kDatas.add(kLineData);
    }

    public void addKLineDatas(List<KLineDataModel> kLineData) {
        kDatas.addAll(kLineData);
    }

    public synchronized ArrayList<KLineDataModel> getKLineDatas() {
        return kDatas;
    }

    public void resetKLineData() {
        kDatas.clear();
    }

    public void setKLineData(ArrayList<KLineDataModel> datas) {
        kDatas.clear();
        kDatas.addAll(datas);
    }

    public ArrayList<String> getxVals() {
        return xVal;
    }

    public List<ILineDataSet> getLineDataMA() {
        return lineDataMA;
    }

    public List<ILineDataSet> getLineDataBOLL() {
        return lineDataBOLL;
    }

    public List<ILineDataSet> getLineDataKDJ() {
        return lineDataKDJ;
    }

    public List<ILineDataSet> getLineDataRSI() {
        return lineDataRSI;
    }

    public List<ILineDataSet> getLineDataMACD() {
        return lineDataMACD;
    }

    public BarDataSet getBarDataMACD() {
        return barDataMACD;
    }

    public BarDataSet getVolumeDataSet() {
        return volumeDataSet;
    }

    public CandleDataSet getCandleDataSet() {
        return candleDataSet;
    }

    public CandleDataSet getBollCandleDataSet() {
        return bollCandleDataSet;
    }

    public float getOffSet() {
        return offSet;
    }

    public ArrayList<BarEntry> getMacdData() {
        return macdData;
    }

    public ArrayList<Entry> getDeaData() {
        return deaData;
    }

    public ArrayList<Entry> getDifData() {
        return difData;
    }

    public ArrayList<Entry> getkData() {
        return kData;
    }

    public ArrayList<Entry> getdData() {
        return dData;
    }

    public ArrayList<Entry> getjData() {
        return jData;
    }

    public ArrayList<Entry> getBollDataUP() {
        return bollDataUP;
    }

    public ArrayList<Entry> getBollDataMB() {
        return bollDataMB;
    }

    public ArrayList<Entry> getBollDataDN() {
        return bollDataDN;
    }

    public ArrayList<Entry> getRsiData6() {
        return rsiData6;
    }

    public ArrayList<Entry> getRsiData12() {
        return rsiData12;
    }

    public ArrayList<Entry> getRsiData24() {
        return rsiData24;
    }

    public void setOneMaValue(LineData lineData, int i) {
        for (int k = 0; k < lineData.getDataSets().size(); k++) {
            ILineDataSet lineDataSet = lineData.getDataSetByIndex(k);
            lineDataSet.removeEntryByXValue(i);
            if (k == 0) {
                if (i >= N1) {
                    sum = 0;
                    float all5 = getSum(i - (N1 - 1), i) / N1;
                    lineDataSet.addEntry(new Entry(i + offSet, all5));
                }
            } else if (k == 1) {
                if (i >= N2) {
                    sum = 0;
                    float all10 = getSum(i - (N2 - 1), i) / N2;
                    lineDataSet.addEntry(new Entry(i + offSet, all10));
                }
            } else if (k == 2) {
                if (i >= N3) {
                    sum = 0;
                    float all20 = getSum(i - (N3 - 1), i) / N3;
                    lineDataSet.addEntry(new Entry(i + offSet, all20));
                }
            }
        }
    }

    public int getPrecision() {
        return precision;
    }

    public double getPreClosePrice() {
        return preClosePrice;
    }

    enum ColorType {
        blue,
        yellow,
        purple
    }

    private double[] calcMA(JSONArray datac, int days) {
        double sum = 0;
        double[] ma = new double[datac.length()];
        try {
            for (int index = 0; index < ma.length; index++) {
                sum += datac.getDouble(index);
                if (index >= days - 1) {
                    if (index >= days) {
                        sum -= datac.getDouble(index - days);
                    }
                    ma[index] = sum / days;
                } else {
                    ma[index] = -1;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ma;
    }
}
