package com.github.mikephil.charting.stockChart;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.R;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ICandleDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.stockChart.charts.CandleCombinedChart;
import com.github.mikephil.charting.stockChart.charts.CoupleChartGestureListener;
import com.github.mikephil.charting.stockChart.charts.MyCombinedChart;
import com.github.mikephil.charting.stockChart.dataManage.KLineDataManage;
import com.github.mikephil.charting.stockChart.enums.TimeType;
import com.github.mikephil.charting.stockChart.markerView.BarBottomMarkerView;
import com.github.mikephil.charting.stockChart.markerView.KRightMarkerView;
import com.github.mikephil.charting.stockChart.markerView.LeftMarkerView;
import com.github.mikephil.charting.utils.CommonUtil;
import com.github.mikephil.charting.utils.DataTimeUtil;
import com.github.mikephil.charting.utils.NumberUtils;
import com.github.mikephil.charting.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * K线
 */
public class KLineChart extends BaseChart {

    protected int chartType1 = 1;
    protected int chartTypes1 = 5;

    private Context mContext;
    private CandleCombinedChart candleChart;
    public boolean isFirst = true;//是否是第一次加载数据
    private MyCombinedChart barChart;
    SimpleDateFormat dataFormat = new SimpleDateFormat("yy.MM.dd HH:mm:ss");
    private CardView infoCard;
    public int quantityPrecision = 2;//小数精度

    private XAxis xAxisBar, xAxisK;
    private YAxis axisLeftK;
    private YAxis axisLeftBar;
    private YAxis axisRightBar;
    private YAxis axisRightK;

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    };

    private KLineDataManage kLineData;
    private TextView timeValue, openValue, highValue, lowValue, closeValue, upDownValue, upDownPercent, volValue;
    private Matrix srcZoom = null;
    private int zbColor[];

    public void notifyDataSetChanged() {
        candleChart.notifyDataSetChanged();
        candleChart.postInvalidate();
        barChart.notifyDataSetChanged();
        barChart.postInvalidate();
    }

    public int getQuantityPrecision() {
        return quantityPrecision;
    }

    public void setQuantityPrecision(int quantityPrecision) {
        this.quantityPrecision = quantityPrecision;
    }

    public void reset() {
        showCard(false);

        candleChart.clear();
        barChart.clear();

        candleChart.resetZoom();
        barChart.resetZoom();

        candleChart.resetViewPortOffsets();
        barChart.resetViewPortOffsets();

        notifyDataSetChanged();
    }

    public KLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.view_kline, this);
        candleChart = findViewById(R.id.candleChart);
        barChart = findViewById(R.id.barchart);

        infoCard = findViewById(R.id.infoCardView);

        timeValue = findViewById(R.id.timeValue);
        openValue = findViewById(R.id.openValue);
        highValue = findViewById(R.id.highValue);
        lowValue = findViewById(R.id.lowValue);
        closeValue = findViewById(R.id.closeValue);
        upDownValue = findViewById(R.id.upDownValue);
        upDownPercent = findViewById(R.id.upDownPercent);
        volValue = findViewById(R.id.volValue);

        zbColor = new int[]{ContextCompat.getColor(context, R.color.ma5), ContextCompat.getColor(context, R.color.ma10), ContextCompat.getColor(context, R.color.ma30)};
        candleChart.setDescriptionCustom(zbColor, new String[]{"MA7: ", "MA30: ", "MA90: "});
        barChart.setDescription(null);
    }

    public KLineChart(Context context) {
        this(context, null);
    }

    public void resetMA() {
        candleChart.setDescriptionCustom(zbColor, new String[]{"MA7: ", "MA30: ", "MA90: "});
        barChart.setDescription(null);
    }

    public void showCard(boolean show) {
        if (show)
            infoCard.setVisibility(View.VISIBLE);
        else
            infoCard.setVisibility(View.GONE);
    }

    private void refreshInfo(String time, String open, String high, String low, String close, String upDown, String upDownPer, String volume) {
        timeValue.setText(time);
        openValue.setText(open);
        highValue.setText(high);
        lowValue.setText(low);
        closeValue.setText(close);
        upDownValue.setText(upDown);
        upDownPercent.setText(upDownPer);
        volValue.setText(volume);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 初始化图表数据
     */
    public void initChart(boolean landscape) {
        this.landscape = landscape;

        //蜡烛图
        candleChart.setDrawBorders(true);
        candleChart.setBorderWidth(0.7f);
        candleChart.setBorderColor(ContextCompat.getColor(mContext, R.color.border_color));
        candleChart.setDragEnabled(true);

        candleChart.setScaleXEnabled(true);
        candleChart.setScaleYEnabled(false);

        candleChart.setHardwareAccelerationEnabled(true);
        Legend mChartKlineLegend = candleChart.getLegend();
        mChartKlineLegend.setEnabled(false);
        candleChart.setDragDecelerationEnabled(true);
        candleChart.setDragDecelerationFrictionCoef(0.6f);//0.92持续滚动时的速度快慢，[0,1) 0代表立即停止。
        candleChart.setDoubleTapToZoomEnabled(false);
        candleChart.setNoDataText(getResources().getString(R.string.loading));
        candleChart.setLogoEnable(true);
        candleChart.setAutoScaleMinMaxEnabled(true);
        candleChart.setPinchZoom(false);

//        candleChart.setHighlightPerTapEnabled(true);
//        candleChart.setHighlightPerDragEnabled(true);

        //副图
        barChart.setDrawBorders(true);
        barChart.setBorderWidth(0.7f);
        barChart.setBorderColor(ContextCompat.getColor(mContext, R.color.border_color));
        barChart.setDragEnabled(true);
        barChart.setScaleXEnabled(true);
        barChart.setScaleYEnabled(false);
        barChart.setHardwareAccelerationEnabled(true);
        Legend mChartChartsLegend = barChart.getLegend();
        mChartChartsLegend.setEnabled(false);
        barChart.setDragDecelerationEnabled(true);
        barChart.setDragDecelerationFrictionCoef(0.6f);//设置太快，切换滑动源滑动不同步
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setNoDataText(getResources().getString(R.string.loading));
        barChart.setAutoScaleMinMaxEnabled(true);
        barChart.setPinchZoom(false);

        //蜡烛图X轴
        xAxisK = candleChart.getXAxis();
        xAxisK.setDrawLabels(false);
        xAxisK.setLabelCount(landscape ? 5 : 4, true);
        xAxisK.setDrawGridLines(true);
        xAxisK.setDrawAxisLine(false);
        xAxisK.setGridLineWidth(0.7f);
        xAxisK.setGridColor(ContextCompat.getColor(mContext, R.color.grid_color));
        xAxisK.setTextColor(ContextCompat.getColor(mContext, R.color.label_text));
        xAxisK.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisK.setAvoidFirstLastClipping(true);
        xAxisK.setDrawLimitLinesBehindData(true);

        //蜡烛图右Y轴
        axisRightK = candleChart.getAxisRight();
        axisRightK.setDrawGridLines(true);
        axisRightK.setDrawAxisLine(false);
        axisRightK.setDrawLabels(true);
        axisRightK.setLabelCount(5, true);
        axisRightK.enableGridDashedLine(CommonUtil.dip2px(mContext, 4), CommonUtil.dip2px(mContext, 3), 0);
        axisRightK.setTextColor(ContextCompat.getColor(mContext, R.color.axis_text));
        axisRightK.setGridColor(ContextCompat.getColor(mContext, R.color.grid_color));
        axisRightK.setGridLineWidth(0.7f);
        axisRightK.setValueLineInside(true);
        axisRightK.setDrawTopBottomGridLine(false);
        axisRightK.setPosition(landscape ? YAxis.YAxisLabelPosition.OUTSIDE_CHART : YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisRightK.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return NumberUtils.keepPrecisionR(value, precision);
            }
        });

        //蜡烛图左Y轴
        axisLeftK = candleChart.getAxisLeft();
        axisLeftK.setDrawLabels(false);
        axisLeftK.setDrawGridLines(false);
        axisLeftK.setDrawAxisLine(false);
        axisLeftK.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);

        //副图X轴
        xAxisBar = barChart.getXAxis();
        xAxisBar.setDrawGridLines(true);
        xAxisBar.setDrawAxisLine(false);
        xAxisBar.setDrawLabels(true);
        xAxisBar.setLabelCount(landscape ? 5 : 4, true);
        xAxisBar.setTextColor(ContextCompat.getColor(mContext, R.color.label_text));
        xAxisBar.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBar.setGridColor(ContextCompat.getColor(mContext, R.color.grid_color));
        xAxisBar.setGridLineWidth(0.7f);
        xAxisBar.setAvoidFirstLastClipping(true);
        xAxisBar.setDrawLimitLinesBehindData(true);

        //副图右Y轴
        axisRightBar = barChart.getAxisRight();
        axisRightBar.setAxisMinimum(0);
        axisRightBar.setDrawGridLines(false);
        axisRightBar.setDrawAxisLine(false);
        axisRightBar.setTextColor(ContextCompat.getColor(mContext, R.color.axis_text));
        axisRightBar.setDrawLabels(true);
        axisRightBar.setLabelCount(2, true);
        axisRightBar.setValueLineInside(true);
        axisRightBar.setPosition(landscape ? YAxis.YAxisLabelPosition.OUTSIDE_CHART : YAxis.YAxisLabelPosition.INSIDE_CHART);

        //副图左Y轴
        axisLeftBar = barChart.getAxisLeft();
        axisLeftBar.setDrawLabels(false);
        axisLeftBar.setDrawGridLines(true);
        axisLeftBar.setDrawAxisLine(false);
        axisLeftBar.setLabelCount(3, true);
        axisLeftBar.setDrawTopBottomGridLine(false);
        axisLeftBar.setGridColor(ContextCompat.getColor(mContext, R.color.grid_color));
        axisLeftBar.setGridLineWidth(0.7f);
        axisLeftBar.enableGridDashedLine(CommonUtil.dip2px(mContext, 4), CommonUtil.dip2px(mContext, 3), 0);

        //手势联动监听
        gestureListenerCandle = new CoupleChartGestureListener(candleChart, new Chart[]{barChart});
        gestureListenerBar = new CoupleChartGestureListener(barChart, new Chart[]{candleChart});
        candleChart.setOnChartGestureListener(gestureListenerCandle);
        barChart.setOnChartGestureListener(gestureListenerBar);

        //移动十字标数据监听
        candleChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                candleChart.highlightValue(h);
                if (barChart.getData().getBarData().getDataSets().size() != 0) {
                    Highlight highlight = new Highlight(h.getX(), h.getDataSetIndex(), h.getStackIndex());
                    highlight.setDataIndex(h.getDataIndex());
                    barChart.highlightValues(new Highlight[]{highlight});
                } else {
                    Highlight highlight = new Highlight(h.getX(), 2, h.getStackIndex());
                    highlight.setDataIndex(0);
                    barChart.highlightValues(new Highlight[]{highlight});
                }
                updateText((int) e.getX(), true);

                float screenW = candleChart.getContext().getResources().getDisplayMetrics().widthPixels;
                if (h.getXPx() > screenW / 2) {
                    infoCard.setX(dip2px(candleChart.getContext(), 10));
                } else {
                    infoCard.setX(screenW - infoCard.getWidth() - dip2px(candleChart.getContext(), 10));
                }
                showCard(true);
            }

            @Override
            public void onNothingSelected() {
//                barChart.highlightValues(null);
//                updateText(kLineData.getKLineDatas().size() - 1, false);
                showCard(false);
            }
        });

        //移动十字标数据监听
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {

            @Override
            public void onValueSelected(Entry e, Highlight h) {
                barChart.highlightValue(h);
                Highlight highlight = new Highlight(h.getX(), 0, h.getStackIndex());
                highlight.setDataIndex(1);
                candleChart.highlightValues(new Highlight[]{highlight});

                updateText((int) e.getX(), true);

                float screenW = candleChart.getContext().getResources().getDisplayMetrics().widthPixels;
                if (h.getXPx() > screenW / 2) {
                    infoCard.setX(dip2px(candleChart.getContext(), 10));
                } else {
                    infoCard.setX(screenW - infoCard.getWidth() - dip2px(candleChart.getContext(), 10));
                }
                showCard(true);
            }

            @Override
            public void onNothingSelected() {
//                candleChart.highlightValues(null);
//                updateText(kLineData.getKLineDatas().size() - 1, false);
                showCard(false);
            }
        });


    }

    /**
     * 设置K线数据
     */
    public void setDataToChart(KLineDataManage data) {
        kLineData = data;
        if (kLineData.getKLineDatas().size() == 0) {
            candleChart.setNoDataText(getResources().getString(R.string.no_data));
            barChart.setNoDataText(getResources().getString(R.string.no_data));
            candleChart.invalidate();
            barChart.invalidate();
            return;
        }

        setPrecision(data.getPrecision());

        CombinedData candleChartData;
        CombinedData barChartData;
        CandleDataSet candleDataSet;
        /*************************************蜡烛图数据*****************************************************/
        candleDataSet = kLineData.getCandleDataSet();
        candleDataSet.setPrecision(precision);
        candleChartData = new CombinedData();
        candleChartData.setData(new CandleData(candleDataSet));
        candleChartData.setData(new LineData(kLineData.getLineDataMA()));
//        candleChartData.setHighlightEnabled(true);
        candleChart.setData(candleChartData);
        /*************************************成交量数据*****************************************************/
        barChartData = new CombinedData();
        barChartData.setData(new BarData(kLineData.getVolumeDataSet()));
        barChartData.setData(new LineData());
        barChartData.setData(new CandleData());
        //重新请求数据时保持副图指标还是显示原来的指标
        if (chartType1 == 1) {
            barChart.setData(barChartData);
        }

        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) (value - kLineData.getOffSet());
                if (index < 0 || index >= kLineData.getxVals().size()) {
                    return "";
                } else {
                    return kLineData.getxVals().get(index);
                }
            }
        });

        //请注意，修改视口的所有方法需要在为Chart设置数据之后调用。
        //设置当前视图四周的偏移量。 设置这个，将阻止图表自动计算它的偏移量。使用 resetViewPortOffsets()撤消此设置。
        float left_right;
        if (landscape) {
            float volwidth = Utils.calcTextWidth(mPaint, "###0.00");
            float pricewidth = Utils.calcTextWidth(mPaint, NumberUtils.keepPrecision(data.getPreClosePrice() + "", precision) + "#");
            left_right = CommonUtil.dip2px(mContext, Math.max(pricewidth, volwidth));
            candleChart.setViewPortOffsets(left_right, CommonUtil.dip2px(mContext, 15), CommonUtil.dip2px(mContext, 5), 0);
            barChart.setViewPortOffsets(left_right, CommonUtil.dip2px(mContext, 15), CommonUtil.dip2px(mContext, 5), CommonUtil.dip2px(mContext, 16));
        } else {
            left_right = CommonUtil.dip2px(mContext, 5);
            candleChart.setViewPortOffsets(left_right, CommonUtil.dip2px(mContext, 15), CommonUtil.dip2px(mContext, 5), 0);
            barChart.setViewPortOffsets(left_right, CommonUtil.dip2px(mContext, 15), CommonUtil.dip2px(mContext, 5), CommonUtil.dip2px(mContext, 16));
        }

        setMarkerView(kLineData);
        setBottomMarkerView(kLineData);

        candleChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMinimum(0);
        candleChart.getXAxis().setAxisMaximum(candleChartData.getXMax() + 0.5f);
        barChart.getXAxis().setAxisMaximum(barChartData.getXMax() + 0.5f);


        xAxisBar.setLabelCount((int) (Math.min(candleChartData.getXMax(), 199) / 50.0f) + 1, true);
        xAxisK.setLabelCount((int) (Math.min(candleChartData.getXMax(), 199) / 50.0f) + 1, true);

        if (isFirst) {
//            updateText(kLineData.getKLineDatas().size() - 1, false);

//            candleChart.resetCustomZoom();
//            barChart.resetCustomZoom();

//            candleChart.resetZoom();
//            barChart.resetZoom();

//            float xScale = calMaxScale(kLineData.getKLineDatas().size());
////            float xScale = calMaxScale(kLineData.getxVals().size());
//            ViewPortHandler viewPortHandlerCombin = candleChart.getViewPortHandler();
//            viewPortHandlerCombin.setMaximumScaleX(50);
//            candleChart.zoom(xScale, 0, 0, 0);
////
//            ViewPortHandler viewPortHandlerBar = barChart.getViewPortHandler();
//            viewPortHandlerBar.setMaximumScaleX(50);
//            barChart.zoom(xScale, 0, 0, 0);

            candleChart.setVisibleXRange(25, 50);
            barChart.setVisibleXRange(25, 50);

//            candleChart.setVisibleXRange(25, 500);
//            barChart.setVisibleXRange(25, 500);

            if (kLineData.getKLineDatas().size() > 50) {
                //moveViewTo(...) 方法会自动调用 invalidate()
                candleChart.moveViewToX(kLineData.getKLineDatas().size() - 1);
                barChart.moveViewToX(kLineData.getKLineDatas().size() - 1);
            }

            isFirst = false;
        }

        handler.sendEmptyMessageDelayed(0, 100);
    }


    public void doBarChartSwitch(int chartType) {
        chartType1 = chartType;
        if (chartType1 > chartTypes1) {
            chartType1 = 1;
        }
        switch (chartType1) {
            case 1:
                setVolumeToChart();
                break;
            case 2:
                setMACDToChart();
                break;
            case 3:
                setKDJToChart();
                break;
            case 4:
                setBOLLToChart();
                break;
            case 5:
                setRSIToChart();
                break;
            default:
                break;
        }
        chartSwitch(kLineData.getKLineDatas().size() - 1);
    }

    /**
     * 副图指标成交量
     */
    public void setVolumeToChart() {
        if (barChart != null) {
            if (barChart.getBarData() != null) {
                barChart.getBarData().clearValues();
            }
            if (barChart.getLineData() != null) {
                barChart.getLineData().clearValues();
            }
            if (barChart.getCandleData() != null) {
                barChart.getCandleData().clearValues();
            }
            axisRightBar.resetAxisMaximum();
            axisRightBar.resetAxisMinimum();
            axisRightBar.setAxisMinimum(0);
//            axisRightBar.setValueFormatter(new VolFormatter(mContext, kLineData.getAssetId()));

            CombinedData combinedData = barChart.getData();
            combinedData.setData(new BarData(kLineData.getVolumeDataSet()));
            combinedData.setData(new LineData());
            barChart.notifyDataSetChanged();
//            barChart.animateY(1000);
        }
    }

    /**
     * 副图指标MACD
     */
    public void setMACDToChart() {
        if (barChart != null) {
            if (barChart.getBarData() != null) {
                barChart.getBarData().clearValues();
            }
            if (barChart.getLineData() != null) {
                barChart.getLineData().clearValues();
            }
            if (barChart.getCandleData() != null) {
                barChart.getCandleData().clearValues();
            }

            axisRightBar.resetAxisMaximum();
            axisRightBar.resetAxisMinimum();
            axisRightBar.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return NumberUtils.keepPrecision(value, precision);
                }
            });

            CombinedData combinedData = barChart.getData();
            combinedData.setData(new LineData(kLineData.getLineDataMACD()));
            combinedData.setData(new BarData(kLineData.getBarDataMACD()));
            barChart.notifyDataSetChanged();
            barChart.invalidate();
        }
    }

    /**
     * 副图指标KDJ
     */
    public void setKDJToChart() {
        if (barChart != null) {
            if (barChart.getBarData() != null) {
                barChart.getBarData().clearValues();
            }
            if (barChart.getLineData() != null) {
                barChart.getLineData().clearValues();
            }
            if (barChart.getCandleData() != null) {
                barChart.getCandleData().clearValues();
            }

            axisRightBar.resetAxisMaximum();
            axisRightBar.resetAxisMinimum();
            axisRightBar.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return NumberUtils.keepPrecision(value, precision);
                }
            });

            CombinedData combinedData = barChart.getData();
            combinedData.setData(new LineData(kLineData.getLineDataKDJ()));
            barChart.notifyDataSetChanged();
            barChart.invalidate();
        }
    }

    /**
     * 副图指标BOLL
     */
    public void setBOLLToChart() {
        if (barChart != null) {
            if (barChart.getBarData() != null) {
                barChart.getBarData().clearValues();
            }
            if (barChart.getLineData() != null) {
                barChart.getLineData().clearValues();
            }
            if (barChart.getCandleData() != null) {
                barChart.getCandleData().clearValues();
            }

            axisRightBar.resetAxisMaximum();
            axisRightBar.resetAxisMinimum();
            axisRightBar.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return NumberUtils.keepPrecision(value, precision);
                }
            });

            CombinedData combinedData = barChart.getData();
            combinedData.setData(new CandleData(kLineData.getBollCandleDataSet()));
            combinedData.setData(new LineData(kLineData.getLineDataBOLL()));
            barChart.notifyDataSetChanged();
            barChart.invalidate();
        }
    }

    /**
     * 副图指标RSI
     */
    public void setRSIToChart() {
        if (barChart != null) {
            if (barChart.getBarData() != null) {
                barChart.getBarData().clearValues();
            }
            if (barChart.getLineData() != null) {
                barChart.getLineData().clearValues();
            }
            if (barChart.getCandleData() != null) {
                barChart.getCandleData().clearValues();
            }

            axisRightBar.resetAxisMaximum();
            axisRightBar.resetAxisMinimum();
            axisRightBar.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return NumberUtils.keepPrecision(value, precision);
                }
            });

            CombinedData combinedData = barChart.getData();
            combinedData.setData(new LineData(kLineData.getLineDataRSI()));
            barChart.notifyDataSetChanged();
            barChart.invalidate();
        }
    }

    /**
     * 动态增加一个点数据
     *
     * @param kLineData 最新数据集
     */
    public void dynamicsAddOne(KLineDataManage kLineData) {
        int size = kLineData.getKLineDatas().size();
        CombinedData candleChartData = candleChart.getData();
        CandleData candleData = candleChartData.getCandleData();
        ICandleDataSet candleDataSet = candleData.getDataSetByIndex(0);
        int i = size - 1;
        candleDataSet.addEntry(new CandleEntry(i + kLineData.getOffSet(), (float) kLineData.getKLineDatas().get(i).getHigh(), (float) kLineData.getKLineDatas().get(i).getLow(), (float) kLineData.getKLineDatas().get(i).getOpen(), (float) kLineData.getKLineDatas().get(i).getClose()));
        kLineData.getxVals().add(DataTimeUtil.secToDate(kLineData.getKLineDatas().get(i).getDateMills()));
        candleChart.getXAxis().setAxisMaximum(kLineData.getKLineDatas().size() < 70 ? 70 : candleChartData.getXMax() + kLineData.getOffSet());

        if (chartType1 == 1) {//副图是成交量
            CombinedData barChartData = barChart.getData();
            IBarDataSet barDataSet = barChartData.getBarData().getDataSetByIndex(0);
            if (barDataSet == null) {//当没有数据时
                return;
            }
            float color = kLineData.getKLineDatas().get(i).getOpen() > kLineData.getKLineDatas().get(i).getClose() ? 0f : 1f;
            BarEntry barEntry = new BarEntry(i + kLineData.getOffSet(), (float) kLineData.getKLineDatas().get(i).getVolume(), color);

            barDataSet.addEntry(barEntry);
            barChart.getXAxis().setAxisMaximum(kLineData.getKLineDatas().size() < 70 ? 70 : barChartData.getXMax() + kLineData.getOffSet());
        } else {//副图是其他技术指标
            doBarChartSwitch(chartType1);
        }

        candleChart.notifyDataSetChanged();
        barChart.notifyDataSetChanged();
        if (kLineData.getKLineDatas().size() > 50) {
            //moveViewTo(...) 方法会自动调用 invalidate()
            candleChart.moveViewToX(kLineData.getKLineDatas().size() - 1);
            barChart.moveViewToX(kLineData.getKLineDatas().size() - 1);
        } else {
            candleChart.invalidate();
            barChart.invalidate();
        }
    }

    /**
     * 动态更新最后一点数据 最新数据集
     *
     * @param kLineData
     */
    public void dynamicsUpdateOne(KLineDataManage kLineData) {
        int size = kLineData.getKLineDatas().size();
        int i = size - 1;
        CombinedData candleChartData = candleChart.getData();
        CandleData candleData = candleChartData.getCandleData();
        ICandleDataSet candleDataSet = candleData.getDataSetByIndex(0);
        candleDataSet.removeEntry(i);

        candleDataSet.addEntry(new CandleEntry(i + kLineData.getOffSet(), (float) kLineData.getKLineDatas().get(i).getHigh(), (float) kLineData.getKLineDatas().get(i).getLow(), (float) kLineData.getKLineDatas().get(i).getOpen(), (float) kLineData.getKLineDatas().get(i).getClose()));
        if (chartType1 == 1) {//副图是成交量
            CombinedData barChartData = barChart.getData();
            IBarDataSet barDataSet = barChartData.getBarData().getDataSetByIndex(0);
            barDataSet.removeEntry(i);
            float color = kLineData.getKLineDatas().get(i).getOpen() > kLineData.getKLineDatas().get(i).getClose() ? 0f : 1f;
            BarEntry barEntry = new BarEntry(i + kLineData.getOffSet(), (float) kLineData.getKLineDatas().get(i).getVolume(), color);
            barDataSet.addEntry(barEntry);
        } else {//副图是其他技术指标
            doBarChartSwitch(chartType1);
        }

        candleChart.notifyDataSetChanged();
        barChart.notifyDataSetChanged();
        candleChart.invalidate();
        barChart.invalidate();
    }

    public void setMarkerView(KLineDataManage kLineData) {
        LeftMarkerView leftMarkerView = new LeftMarkerView(mContext, R.layout.my_markerview, precision);
        KRightMarkerView rightMarkerView = new KRightMarkerView(mContext, R.layout.my_markerview, precision);
        candleChart.setMarker(leftMarkerView, rightMarkerView, kLineData);
    }

    public void setBottomMarkerView(KLineDataManage kLineData) {
        BarBottomMarkerView bottomMarkerView = new BarBottomMarkerView(mContext, R.layout.my_markerview);
        barChart.setMarker(bottomMarkerView, kLineData, TimeType.TIME_DATE);
    }

    public float calMaxScale(float count) {
        float xScale = 1;
        if (count >= 200) {
            xScale = 4f;
        } else if (count >= 100) {
            xScale = 2f;
        } else {
            xScale = 0.5f;
        }
        return xScale;
    }

    //移动十字标更新数据
    public void updateText(int index, boolean isSelect) {
        if (mHighlightValueSelectedListener != null) {
            mHighlightValueSelectedListener.onKHighlightValueListener(kLineData, index, isSelect);
        }
        //更新MA均线数据
        if (kLineData.getKLineDatas().size() <= index) {
            return;
        }

        double ma7value = kLineData.getKLineDatas().get(index).getMa7();
        double ma30value = kLineData.getKLineDatas().get(index).getMa30();
        double ma90value = kLineData.getKLineDatas().get(index).getMa90();

        String ma7 = ma7value < 0 ? " " : NumberUtils.keepPrecision(ma7value, precision);
        String ma30 = ma30value < 0 ? " " : NumberUtils.keepPrecision(ma30value, precision);
        String ma90 = ma90value < 0 ? " " : NumberUtils.keepPrecision(ma90value, precision);

        candleChart.setDescriptionCustom(zbColor, new String[]{"MA7:" + ma7, "MA30:" + ma30, "MA90:" + ma90});

        chartSwitch(index);

        double changeValue = (kLineData.getKLineDatas().get(index).getClose() - kLineData.getKLineDatas().get(index).getOpen());
        double changeRate = changeValue / kLineData.getKLineDatas().get(index).getOpen() * 100;

        refreshInfo(
                dataFormat.format(new Date(kLineData.getKLineDatas().get(index).getDateMills())),
                NumberUtils.keepPrecision(kLineData.getKLineDatas().get(index).getOpen(), precision),
                NumberUtils.keepPrecision(kLineData.getKLineDatas().get(index).getHigh(), precision),
                NumberUtils.keepPrecision(kLineData.getKLineDatas().get(index).getLow(), precision),
                NumberUtils.keepPrecision(kLineData.getKLineDatas().get(index).getClose(), precision),
                NumberUtils.keepPrecision(changeValue, precision),
                String.format("%.2f", changeRate) + "%",
                NumberUtils.keepPrecision(kLineData.getKLineDatas().get(index).getVolume(), quantityPrecision)
        );
    }

    //副图切换
    private void chartSwitch(int index) {
        switch (chartType1) {
//            case 1:
//                barChart.setDescriptionCustom(ContextCompat.getColor(mContext, R.color.label_text), getResources().getString(R.string.vol_name) + NumberUtils.formatVol(mContext, kLineData.getAssetId(), kLineData.getKLineDatas().get(index).getVolume()));
//                break;
//            case 2:
//                barChart.setDescriptionCustom(zbColor, new String[]{"DIFF:" + (kLineData.getDifData().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getDifData().get(index).getY(), 3)), "DEA:" + (kLineData.getDeaData().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getDeaData().get(index).getY(), 3)), "MACD:" + (kLineData.getMacdData().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getMacdData().get(index).getY(), 3))});
//                break;
//            case 3:
//                barChart.setDescriptionCustom(zbColor, new String[]{"K:" + (kLineData.getkData().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getkData().get(index).getY(), 3)), "D:" + (kLineData.getdData().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getdData().get(index).getY(), 3)), "J:" + (kLineData.getjData().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getjData().get(index).getY(), 3))});
//                break;
//            case 4:
//                barChart.setDescriptionCustom(zbColor, new String[]{"UPPER:" + (kLineData.getBollDataUP().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getBollDataUP().get(index).getY(), 3)), "MID:" + (kLineData.getBollDataMB().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getBollDataMB().get(index).getY(), 3)), "LOWER:" + (kLineData.getBollDataDN().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getBollDataDN().get(index).getY(), 3))});
//                break;
//            case 5:
//                barChart.setDescriptionCustom(zbColor, new String[]{"RSI6:" + (kLineData.getRsiData6().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getRsiData6().get(index).getY(), 3)), "RSI12:" + (kLineData.getRsiData12().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getRsiData12().get(index).getY(), 3)), "RSI24:" + (kLineData.getRsiData24().size() <= index ? "--" : NumberUtils.keepPrecision(kLineData.getRsiData24().get(index).getY(), 3))});
//                break;
            default:
                barChart.setDescription(null);
//                barChart.setDescriptionCustom(ContextCompat.getColor(mContext, R.color.label_text), "");
                break;
        }
    }

}

