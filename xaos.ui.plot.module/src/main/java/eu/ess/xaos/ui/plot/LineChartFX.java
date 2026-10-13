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
import java.util.Objects;

import static java.util.logging.Level.WARNING;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.AccessibleRole;
import javafx.scene.layout.StackPane;
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

    // Per-series styles, keyed by styleKey(series) rather than by position, so
    // that a style never moves to another series when the series change.
    private final Map<Object, Color> colorMap = new HashMap<>();
    private final Map<Object, Boolean> lineFlag = new HashMap<>();
    private final Map<Object, Boolean> markerFlag = new HashMap<>();
    private final Map<Object, LineStyle> lineStyleMap = new HashMap<>();
    private final Map<Object, MarkerSymbol> markerSymbolMap = new HashMap<>();

    // General flag to enable/disable plotting markers
    private boolean showMarkersFlag = false;

    // Width for all lines in the plot
    private float linesWidth = 1.3f;

    // Set when series or their data change, meaning JavaFX may have created
    // fresh (unstyled) symbol nodes that must be re-styled on the next layout
    // pass. Guards the styling loop in layoutPlotChildren() so it does not
    // re-parse CSS for every node on every layout (e.g. during pan/zoom/resize).
    private boolean stylesDirty = true;

    // One data-list listener per series, so in-place data updates (setAll on a
    // series' data) mark the styles dirty. Kept to allow removal when a series
    // is removed.
    private final Map<Series<X, Y>, ListChangeListener<Data<X, Y>>> dataListeners = new HashMap<>();

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

        // Symbol nodes are created by this class only for the series showing
        // markers (see createMissingSymbols). The LineChart createSymbols flag
        // is chart-wide, and toggling it adds/removes symbols on every series,
        // so it is kept disabled. If it is turned off externally, LineChart
        // removes all symbols, including the ones needed by marker series.
        setCreateSymbols(false);
        createSymbolsProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                createAllMissingSymbols();
            }
        });

        for (Series<X, Y> series : data) {
            seriesDrawnInPlot.put(series.getName(), true);
            attachDataListener(series);
        }

        createAllMissingSymbols();

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
                if (name != null && name.equals(item.getText())) {
                    item.setSelected(flag);
                }
            }
        }

        // The line's "shown" style depends on this flag; re-apply it now instead
        // of relying on a per-layout restyle.
        for (Series<X, Y> series : getData()) {
            if (Objects.equals(name, series.getName())) {
                updateSeriesStyle(series);
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
            setSeriesColor(getData().get(horizontal), SeriesColorUtils.HORIZONTAL);
        }
        if (vertical != -1) {
            setSeriesColor(getData().get(vertical), SeriesColorUtils.VERTICAL);
        }
        if (longitudinal != -1) {
            setSeriesColor(getData().get(longitudinal), SeriesColorUtils.LONGITUDINAL);
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

        // Re-style only when series/data changed (i.e. new symbols may have been
        // created). Re-applying the style to every node on every layout would
        // re-parse CSS per node on each pan/zoom/resize; per-series style
        // changes are applied immediately by the setters instead.
        if (stylesDirty) {
            restyleAllSeries();
            stylesDirty = false;
        }

        //	Move plugins nodes to front.
        ObservableList<Node> plotChildren = getPlotChildren();

        plotChildren.remove(pluginsNodesGroup);
        plotChildren.add(pluginsNodesGroup);

        // Required to update plugins properly.
        layout();
    }

    /**
     * Applies the current style to every series. Used both by the deferred
     * (dirty-triggered) styling in {@link #layoutPlotChildren()} and by the
     * mutators that change styling shared by all series (line width, marker
     * visibility, default colors).
     */
    private void restyleAllSeries() {
        for (Series<X, Y> series : getData()) {
            updateSeriesStyle(series);
        }
    }

    /**
     * Applies the current style to the series with the given style key (see
     * {@link #styleKey(Series)}), if any is in the chart.
     */
    private void restyleSeries(Object key) {
        for (Series<X, Y> series : getData()) {
            if (key.equals(styleKey(series))) {
                updateSeriesStyle(series);
            }
        }
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
            detachDataListener(removedSeries);
        }

        for (Series series : c.getAddedSubList()) {
            if (!seriesDrawnInPlot.containsKey(series.getName())) {
                setSeriesDrawn(series.getName(), true);
            }

            attachDataListener(series);
            updateSeriesStyle(series);
        }

        // New series (and their symbols) need styling on the next layout pass.
        stylesDirty = true;
    }

    @Override
    protected void seriesAdded(Series<X, Y> series, int seriesIndex) {
        super.seriesAdded(series, seriesIndex);
        createMissingSymbols(series);
    }

    @Override
    protected void dataItemAdded(Series<X, Y> series, int itemIndex, Data<X, Y> item) {
        super.dataItemAdded(series, itemIndex, item);

        // Called per added item, so only handle this one.
        if (markerFlag != null && !getCreateSymbols() && item.getNode() == null && needsSymbols(series)) {
            getPlotChildren().add(createSymbol(getData().indexOf(series), item, itemIndex));
            stylesDirty = true;
        }
    }

    /**
     * Whether the series shows markers, and therefore needs a symbol node per
     * data point.
     */
    private boolean needsSymbols(Series<X, Y> series) {
        return markerFlag.getOrDefault(styleKey(series), showMarkersFlag);
    }

    private void createAllMissingSymbols() {
        for (Series<X, Y> series : getData()) {
            createMissingSymbols(series);
        }
    }

    /**
     * Creates the symbol nodes missing in the given series if it shows
     * markers. Line-only series get no symbols, avoiding the per-point
     * layout/styling overhead. Symbols are styled on the next layout pass or by
     * the caller.
     */
    private void createMissingSymbols(Series<X, Y> series) {
        // Called by LineChart during super construction, before fields are set.
        // With createSymbols enabled LineChart already creates all symbols.
        if (markerFlag == null || getCreateSymbols()) {
            return;
        }

        int seriesIndex = getData().indexOf(series);
        if (seriesIndex == -1 || !needsSymbols(series)) {
            return;
        }

        List<Node> symbols = new ArrayList<>();
        ObservableList<Data<X, Y>> data = series.getData();
        for (int j = 0; j < data.size(); j++) {
            Data<X, Y> item = data.get(j);
            if (item.getNode() == null) {
                symbols.add(createSymbol(seriesIndex, item, j));
            }
        }

        if (!symbols.isEmpty()) {
            getPlotChildren().addAll(symbols);
            stylesDirty = true;
        }
    }

    /**
     * Creates the symbol node for a data item, as LineChart does when
     * createSymbols is enabled. The caller adds it to the plot children.
     */
    private Node createSymbol(int seriesIndex, Data<X, Y> item, int itemIndex) {
        StackPane symbol = new StackPane();
        symbol.setAccessibleRole(AccessibleRole.TEXT);
        symbol.setAccessibleRoleDescription("Point");
        symbol.getStyleClass().addAll("chart-line-symbol", "series" + seriesIndex, "data" + itemIndex);
        item.setNode(symbol);
        return symbol;
    }

    @SuppressWarnings("unchecked")
    private void attachDataListener(Series series) {
        ListChangeListener<Data<X, Y>> listener = change -> stylesDirty = true;
        ((Series<X, Y>) series).getData().addListener(listener);
        dataListeners.put((Series<X, Y>) series, listener);
    }

    @SuppressWarnings("unchecked")
    private void detachDataListener(Series series) {
        ListChangeListener<Data<X, Y>> listener = dataListeners.remove((Series<X, Y>) series);
        if (listener != null) {
            ((Series<X, Y>) series).getData().removeListener(listener);
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
                setLegendItemStyle(series);
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

    /**
     * Key of a series in the per-series style maps: its name, as for
     * {@link #isSeriesDrawn(String)} and the legend. This keeps the style when
     * the chart data is refreshed with new series of the same name, without
     * passing it to another series when the series order changes. Unnamed
     * series are keyed by the series itself.
     */
    private static Object styleKey(Series<?, ?> series) {
        return series.getName() != null ? series.getName() : series;
    }

    /**
     * Set the color used by a series
     *
     * @param series the series
     * @param color
     */
    public void setSeriesColor(Series series, Color color) {
        Object key = styleKey(series);
        colorMap.put(key, color);
        restyleSeries(key);
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
        Object key = styleKey(series);
        lineFlag.put(key, flag);
        restyleSeries(key);
    }

    public void setLinesWidth(float width) {
        linesWidth = width;
        restyleAllSeries();
    }

    public void setLineStyle(Series series, LineStyle style) {
        Object key = styleKey(series);
        lineStyleMap.put(key, style);
        restyleSeries(key);
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
        Object key = styleKey(series);
        markerFlag.put(key, flag);
        for (Series<X, Y> s : getData()) {
            if (key.equals(styleKey(s))) {
                createMissingSymbols(s);
            }
        }
        restyleSeries(key);
    }

    /**
     * Set the flag to enable/disable showing symbols on the plot. Use this
     * method (or {@link #setShowMarker(Series, boolean)}) instead of
     * setCreateSymbols: this chart creates symbol nodes only for the series
     * showing markers, while setCreateSymbols applies to all series.
     * Hiding markers is done by CSS styling.
     *
     * @param flag
     */
    public void setShowMarkers(boolean flag) {
        showMarkersFlag = flag;
        createAllMissingSymbols();
        restyleAllSeries();
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
        Object key = styleKey(series);
        markerSymbolMap.put(key, symbol);
        restyleSeries(key);
    }

    /**
     * Replace default JavaFX colors by XAOS default colors
     */
    public void setDefaultLineColors() {
        lookup(".chart").setStyle(SeriesColorUtils.styles());
        colorMap.clear();
        restyleAllSeries();
    }

    private String getColorFor(Object key, int index) {
        if (colorMap.containsKey(key)) {
            return ColorUtils.toWeb(colorMap.get(key));
        } else {
            return ColorUtils.toWeb(SeriesColorUtils.COLORS[index % 8]);
        }
    }

    private void setLegendItemStyle(Series<X, Y> series) {
        // Set color of the legend symbol
        String name = series.getName();
        int index = getData().indexOf(series);
        Object key = styleKey(series);
        for (LegendItem legendItem : getLegendItems()) {
            if (name != null && name.equals(legendItem.getText())) {
                legendItem.getSymbol().setStyle(getMarkerStyle(key, index, true));
                legendItem.getLine().setStyle(getLineStyle(key, index, true));
            }
        }
    }

    private void updateSeriesStyle(Series<X, Y> series) {
        int index = getData().indexOf(series);
        if (index == -1) {
            return;
        }

        boolean shown = isSeriesDrawn(series.getName());
        Object key = styleKey(series);

        String lineStyle = getLineStyle(key, index, shown);
        String markerStyle = getMarkerStyle(key, index, shown);

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

        setLegendItemStyle(series);
    }

    private String getLineStyle(Object key, int index, boolean shown) {
        StringBuilder style = new StringBuilder();

        String color = getColorFor(key, index);
        // Set line color
        if (lineFlag.getOrDefault(key, Boolean.TRUE) && shown) {
            style.append("-fx-stroke: ").append(color).append("; ");
        } else {
            return style.append("-fx-stroke: transparent; ").toString();
        }

        style.append("-fx-background-color: ").append(color).append("; ");
        if (lineStyleMap.getOrDefault(key, LineStyle.SOLID) != LineStyle.SOLID) {
            style.append("-fx-stroke-dash-array: ").append(lineStyleMap.get(key).getStyle()).append("; ");
        }

        style.append("-fx-stroke-width: ").append(Float.toString(linesWidth)).append("px; -fx-effect: null;");

        return style.toString();
    }

    private String getMarkerStyle(Object key, int index, boolean shown) {
        StringBuilder style = new StringBuilder();

        String color = getColorFor(key, index);

        // Set marker color
        style.append("-fx-background-color: ").append(color);

        MarkerSymbol marker = markerSymbolMap.getOrDefault(key, MarkerSymbol.HOLLOW_CIRCLE);
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

        if (markerFlag.getOrDefault(key, getShowMarkers()) && shown) {
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
        lineStyleMap.clear();
        markerSymbolMap.clear();
    }
}
