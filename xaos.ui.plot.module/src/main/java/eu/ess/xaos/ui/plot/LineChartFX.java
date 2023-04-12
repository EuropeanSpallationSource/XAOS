/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2018-2019 by European Spallation Source ERIC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.ess.xaos.ui.plot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import org.apache.commons.lang3.Validate;
import eu.ess.xaos.core.util.LogUtils;
import eu.ess.xaos.ui.plot.Legend.LegendItem;
import eu.ess.xaos.ui.plot.plugins.Pluggable;
import eu.ess.xaos.ui.plot.util.LineStyle;
import eu.ess.xaos.ui.plot.util.MarkerSymbol;
import eu.ess.xaos.ui.plot.util.SeriesColorUtils;
import eu.ess.xaos.ui.util.ColorUtils;
import java.util.HashMap;
import java.util.Map;

import static java.util.logging.Level.WARNING;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.paint.Color;

/**
 * A thin extension of the FX {@link LineChart} supporting custom
 * {@link Plugin plugin} implementations.
 *
 * @param <X> Type of X values.
 * @param <Y> Type of Y values.
 * @author Grzegorz Kruk (original author).
 * @author claudio.rosati@esss.se
 */
public class LineChartFX<X, Y> extends LineChart<X, Y> implements Pluggable {

    private static final Logger LOGGER = Logger.getLogger(LineChartFX.class.getName());

    private final Map<Integer, Color> colorMap = new HashMap<>();
    private final Map<Integer, Boolean> lineFlag = new HashMap<>();
    private final Map<Integer, Boolean> markerFlag = new HashMap<>();
    private final Map<Integer, LineStyle> lineStyleMap = new HashMap<>();
    private final Map<Integer, MarkerSymbol> markerSymbolMap = new HashMap<>();

    // General flag to enable/disable plotting markers
    private boolean showMarkersFlag = false;

    // Width for all lines in the plot
    private float linesWidth = 1.3f;

    /**
     * Quick way of creating a line chart showing the given {@code data}. X axis
     * will contain the index in the data point in the given list.
     *
     * @param data The data list to be charted.
     * @param seriesName The name of the {@link Series} created from the given
     * {@code data}.
     * @return A {@link LineChartFX} chart.
     */
    public static LineChartFX<Number, Number> of(ObservableList<Double> data, String seriesName) {
        Series<Number, Number> dataSet = new Series<>();

        dataSet.setName(seriesName);

        ObservableList<Data<Number, Number>> list = dataSet.getData();

        for (int i = 0; i < data.size(); i++) {
            list.add(new Data<>(i, data.get(i)));
        }

        return new LineChartFX<>(
                new NumberAxis(),
                new NumberAxis(),
                FXCollections.singletonObservableList(dataSet)
        );
    }

    private List<String> notShownInLegend;
    private final Group pluginsNodesGroup = new Group();
    private final PluginManager pluginManager = new PluginManager(this, pluginsNodesGroup);
    private final Map<String, Boolean> seriesDrawnInPlot = new HashMap<>();

    /**
     * Construct a new line chart with the given axis and data.
     *
     * @param xAxis The x axis to use.
     * @param yAxis The y axis to use.
     * @see javafx.scene.chart.LineChart#LineChart(Axis, Axis)
     */
    public LineChartFX(Axis<X> xAxis, Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    /**
     * Construct a new line chart with the given axis and data.
     *
     * @param xAxis The x axis to use.
     * @param yAxis The y axis to use.
     * @param data The data to use, this is the actual list used so any changes
     * to it will be reflected in the chart.
     * @see javafx.scene.chart.LineChart#LineChart(Axis, Axis, ObservableList)
     */
    public LineChartFX(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);

        for (Series<X, Y> series : data) {
            seriesDrawnInPlot.put(series.getName(), true);
        }

        getPlotChildren().add(pluginsNodesGroup);
    }

    @Override
    public String getUserAgentStylesheet() {
        return LineChartFX.class.getResource("/styles/chart.css").toExternalForm();
    }

    /**
     * More robust method for adding plugins to chart.
     * <p>
     * <b>Note:</b> Only necessary if more than one plugin is being added at
     * once.</p>
     *
     * @param plugins List of {@link Plugin}s to be added.
     */
    public void addChartPlugins(ObservableList<Plugin> plugins) {
        plugins.forEach(plugin -> {
            try {
                pluginManager.getPlugins().add(plugin);
            } catch (Exception ex) {
                LogUtils.log(
                        LOGGER,
                        WARNING,
                        "Error occured whilst adding {0} [{1}].",
                        plugin.getClass().getName(),
                        ex.getMessage()
                );
            }
        });
    }

    @Override
    public Chart getChart() {
        return this;
    }

    @Override
    public ObservableList<LegendItem> getLegendItems() {
        Node legend = getLegend();

        if (legend instanceof Legend) {
            return ((Legend) legend).getItems();
        } else {
            return FXCollections.emptyObservableList();
        }
    }

    @Override
    public final ObservableList<Node> getPlotChildren() {
        return super.getPlotChildren();
    }

    @Override
    public final ObservableList<Plugin> getPlugins() {
        return pluginManager.getPlugins();
    }

    @Override
    public boolean isNotShownInLegend(String name) {
        return notShownInLegend().contains(name);
    }

    public boolean isSeriesDrawn(String name) {
        return seriesDrawnInPlot.getOrDefault(name, false);
    }

    public void setSeriesDrawn(String name, boolean flag) {
        seriesDrawnInPlot.put(name, flag);

        // Make sure the legend items show the correct state
        if (!isNotShownInLegend(name)) {
            for (LegendItem item : getLegendItems()) {
                if (name.equals(item.getText())) {
                    item.setSelected(flag);
                }
            }
        }
    }

    /**
     * Sets which series has to be considered "horizontal", "vertical" and
     * "longitudinal". Special colors will be used to represent horizontal
     * (red), vertical (blue) and longitudinal (green) series.
     *
     * @param horizontal Index of the horizontal series. Use -1 if no horizontal
     * series exists.
     * @param vertical Index of the vertical series. Use -1 if no vertical
     * series exists.
     * @param longitudinal Index of the longitudinal series. Use -1 if no
     * longitudinal series exists.
     */
    public final void setHVLSeries(int horizontal, int vertical, int longitudinal) {
        int size = getData().size();

        Validate.isTrue(horizontal < size, "Out of range 'horizontal' parameter.");
        Validate.isTrue(vertical < size, "Out of range 'vertical' parameter.");
        Validate.isTrue(longitudinal < size, "Out of range 'longitudinal' parameter.");
        if (horizontal != -1) {
            colorMap.put(horizontal, SeriesColorUtils.HORIZONTAL);
            setSeriesStyle(horizontal);
        }
        if (vertical != -1) {
            colorMap.put(vertical, SeriesColorUtils.VERTICAL);
            setSeriesStyle(vertical);
        }
        if (longitudinal != -1) {
            colorMap.put(longitudinal, SeriesColorUtils.LONGITUDINAL);
            setSeriesStyle(longitudinal);
        }
    }

    @Override
    public final void setNotShownInLegend(String name) {
        notShownInLegend().add(name);
        updateLegend();
    }

    @Override
    protected void layoutPlotChildren() {
        //	Layout plot children. This call will create fresh new symbols
        //	that are by default visible.
        super.layoutPlotChildren();

        // Make sure all lines and markers have the right style
        for (int i = 0; i < getData().size(); i++) {
            setSeriesStyle(i);
        }

        //	Move plugins nodes to front.
        ObservableList<Node> plotChildren = getPlotChildren();

        plotChildren.remove(pluginsNodesGroup);
        plotChildren.add(pluginsNodesGroup);

        // Required to update plugins properly.
        layout();
    }

    /**
     * Make sure the series is assigned the right style when added. Also make
     * sure to use setSeriesDrawn if you want to make sure the plot is not
     * plotted.
     *
     * @param c
     */
    @Override
    protected void seriesChanged(Change<? extends Series> c) {
        // Parent method not called because we do styling differently here.

        for (Series removedSeries : c.getRemoved()) {
            seriesDrawnInPlot.remove(removedSeries.getName());
        }

        for (Series<Number, Number> series : c.getAddedSubList()) {
            if (!seriesDrawnInPlot.containsKey(series.getName())) {
                setSeriesDrawn(series.getName(), true);
            }

            updateSeriesStyle(series);
        }
    }

    /*
        This method update the axis range taking into account only the visible lines.
     */
    @Override
    protected void updateAxisRange() {
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<X> xData = null;
        List<Y> yData = null;
        if (xa.isAutoRanging()) {
            xData = new ArrayList<>();
        }
        if (ya.isAutoRanging()) {
            yData = new ArrayList<>();
        }
        if (xData != null || yData != null) {
            for (Series<X, Y> series : getData()) {
                if (isSeriesDrawn(series.getName())) {
                    for (Data<X, Y> data : series.getData()) {
                        if (xData != null) {
                            xData.add(data.getXValue());
                        }
                        if (yData != null) {
                            yData.add(data.getYValue());
                        }
                    }
                }
            }
            if (xData != null) {
                xa.invalidateRange(xData);
            }
            if (yData != null) {
                ya.invalidateRange(yData);
            }
        }
    }

    @Override
    protected void updateLegend() {
        final Legend legend = new Legend();

        for (int i = 0; i < getData().size(); i++) {
            final int seriesIndex = i;
            Series series = getData().get(seriesIndex);
            String seriesName = series.getName();

            if (!notShownInLegend().contains(seriesName) && seriesName != null) {
                Legend.LegendItem legenditem = new Legend.LegendItem(seriesName, selected -> {
                    seriesDrawnInPlot.put(seriesName, selected);
                    updateSeriesStyle(series);

                    getPlugins().forEach(p -> p.seriesVisibilityUpdated(this, series, seriesIndex, selected));

                    updateAxisRange();
                }, isSeriesDrawn(seriesName));

                legenditem.getSymbol().getStyleClass().addAll(
                        "chart-line-symbol",
                        "area-legend-symbol"
                );

                legend.getItems().add(legenditem);
            }
        }

        setLegend(legend);

        // Set the legend symbol style after setting the Legend, since getLegend() is used to get the items.
        for (int i = 0; i < getData().size(); i++) {
            Series<X, Y> series = getData().get(i);
            String seriesName = series.getName();
            if (!notShownInLegend().contains(seriesName)) {
                setLegendItemStyle(seriesName);
            }
        }
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private List<String> notShownInLegend() {
        if (notShownInLegend == null) {
            notShownInLegend = new ArrayList<>(4);
        }

        return notShownInLegend;
    }

    private int getSeriesIndex(String seriesName) {
        for (Series<X, Y> series : getData()) {
            if (series.getName().equals(seriesName)) {
                return getData().indexOf(series);
            }
        }
        return -1;
    }

    private int getSeriesIndex(int seriesHash) {
        for (Series<X, Y> series : getData()) {
            if (series.hashCode() == seriesHash) {
                return getData().indexOf(series);
            }
        }
        return -1;
    }

    /**
     * Set the color used by a series
     *
     * @param series the series
     * @param color
     */
    public void setSeriesColor(Series series, Color color) {
        int index = getSeriesIndex(series.getName());
        if (index != -1) {
            colorMap.put(index, color);
            updateSeriesStyle(series);
        }
    }

    /**
     * Toggle display line option for the specified series and update the plot
     *
     * By default, lines are shown in LineChartFX
     *
     * @param series the series
     * @param flag True to enable line, False to disable
     */
    public void setShowLine(Series series, boolean flag) {
        int index = getSeriesIndex(series.getName());
        if (index != -1) {
            lineFlag.put(index, flag);
            updateSeriesStyle(series);
        }
    }

    public void setLinesWidth(float width) {
        linesWidth = width;
    }

    public void setLineStyle(Series series, LineStyle style) {
        int index = getSeriesIndex(series.getName());
        if (index != -1) {
            lineStyleMap.put(index, style);
            updateSeriesStyle(series);
        }
    }

    /**
     * Toggle display markers for the specified series and update the plot
     *
     * By default, markers are not shown in LineChartFX
     *
     * @param series the series
     * @param flag True to enable markers, False to disable
     */
    public void setShowMarker(Series series, boolean flag) {
        int index = getSeriesIndex(series.getName());
        if (index != -1) {
            markerFlag.put(index, flag);
            updateSeriesStyle(series);
        }
    }

    /**
     * Set the flag to enable/disable showing symbols on the plot. Use this
     * method instead of setCreateSymbols because the create Symbols flags must
     * always be enabled in this chart. Enabling/disabling symbols is done by
     * CSS styling.
     *
     * @param flag
     */
    public void setShowMarkers(boolean flag) {
        showMarkersFlag = flag;
    }

    /**
     * Get the flag to enable/disable showing symbols on the plot.
     *
     * @return
     */
    public boolean getShowMarkers() {
        return showMarkersFlag;
    }

    public void setMarkerSymbol(Series series, MarkerSymbol symbol) {
        int index = getSeriesIndex(series.getName());
        if (index != -1) {
            markerSymbolMap.put(index, symbol);
            updateSeriesStyle(series);
        }
    }

    /**
     * Replace default JavaFX colors by XAOS default colors
     */
    public void setDefaultLineColors() {
        lookup(".chart").setStyle(SeriesColorUtils.styles());
        colorMap.clear();
    }

    private String getColorFor(int i) {
        if (colorMap.containsKey(i)) {
            return ColorUtils.toWeb(colorMap.get(i));
        } else {
            return ColorUtils.toWeb(SeriesColorUtils.COLORS[i % 8]);
        }
    }

    private void setLegendItemStyle(String name) {
        // Set color of the legend symbol
        for (LegendItem legendItem : getLegendItems()) {
            if (name.equals(legendItem.getText())) {
                int index = getSeriesIndex(name);
                legendItem.getSymbol().setStyle(getMarkerStyle(index, true));
                legendItem.getLine().setStyle(getLineStyle(index, true));
            }
        }
    }

    private void setSeriesStyle(int i) {
        Series s = getData().get(i);
        updateSeriesStyle(s);
    }

    private void updateSeriesStyle(Series<Number, Number> series) {
        boolean shown = isSeriesDrawn(series.getName());
        int index = getSeriesIndex(series.hashCode());

        String lineStyle = getLineStyle(index, shown);
        String markerStyle = getMarkerStyle(index, shown);

        // Update all nodes to use this style
        Node seriesNode = series.getNode();
        if (seriesNode != null) {
            seriesNode.setStyle(lineStyle);
        }
        for (int j = 0; j < series.getData().size(); j++) {
            final Node symbol = series.getData().get(j).getNode();
            if (symbol != null) {
                symbol.setStyle(markerStyle);
            }
        }

        setLegendItemStyle(series.getName());
    }

    private String getLineStyle(int i, boolean shown) {
        StringBuilder style = new StringBuilder();

        String color = getColorFor(i);
        // Set line color
        if (lineFlag.getOrDefault(i, Boolean.TRUE) && shown) {
            style.append("-fx-stroke: ").append(color).append("; ");
        } else {
            return style.append("-fx-stroke: transparent; ").toString();
        }

        style.append("-fx-background-color: ").append(color).append("; ");
        if (lineStyleMap.getOrDefault(i, LineStyle.SOLID) != LineStyle.SOLID) {
            style.append("-fx-stroke-dash-array: ").append(lineStyleMap.get(i).getStyle()).append("; ");
        }

        style.append("-fx-stroke-width: ").append(Float.toString(linesWidth)).append("px; -fx-effect: null;");

        return style.toString();
    }

    private String getMarkerStyle(int i, boolean shown) {
        StringBuilder style = new StringBuilder();

        String color = getColorFor(i);

        // Set marker color
        style.append("-fx-background-color: ").append(color);

        MarkerSymbol marker = markerSymbolMap.getOrDefault(i, MarkerSymbol.HOLLOW_CIRCLE);
        switch (marker) {
            case SOLID_CIRCLE:
            case SOLID_SQUARE:
            case SOLID_DIAMOND:
            case SOLID_TRIANGLE:
            case CROSS:
                style.append("; ");
                break;
            default:
                style.append(", white; ");
        }

        if (markerFlag.getOrDefault(i, getShowMarkers()) && shown) {
            style.append("visibility: visible; ");
        } else {
            style.append("visibility: hidden; ");
        }

        return style.append(marker.getStyle()).toString();
    }

    /**
     * Clear all the data in the plot and the styling
     */
    public void clear() {
        getData().clear();
        lineFlag.clear();
        markerFlag.clear();
        colorMap.clear();
    }
}
