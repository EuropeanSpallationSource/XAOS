/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2026 by European Spallation Source ERIC.
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


import java.util.List;
import java.util.concurrent.TimeoutException;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import eu.ess.xaos.ui.plot.util.LineStyle;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Checks that the per-series styles of {@link LineChartFX} stay with their
 * series (and not with a position in the chart data) when series are
 * removed, re-ordered or replaced.
 * <p>
 * No mouse/keyboard interaction is involved: styles are read from the series
 * nodes.</p>
 */
@SuppressWarnings( { "UseOfSystemOutOrSystemErr", "ClassWithoutLogger" } )
public class LineChartFXStyleUITest extends ApplicationTest {

	private static final Color CUSTOM = Color.web("#123456");
	private static final String CUSTOM_STROKE = "-fx-stroke: #123456;";

	@BeforeClass
	public static void setUpClass() {
		System.out.println("---- LineChartFXStyleUITest ------------------------------------");
	}

	private LineChartFX<Number, Number> chart;

	@Override
	public void start( Stage stage ) {

		NumberAxis xAxis = new NumberAxis();
		NumberAxis yAxis = new NumberAxis();

		xAxis.setAnimated(false);
		yAxis.setAnimated(false);

		chart = new LineChartFX<>(xAxis, yAxis);
		chart.setAnimated(false);

		stage.setScene(new Scene(new BorderPane(chart), 600, 400));
		stage.show();

	}

	@After
	public void tearDown() throws TimeoutException {
		FxToolkit.cleanupStages();
	}

	@Test
	public void testStyleFollowsSeriesWhenPrecedingSeriesRemoved() {

		System.out.println("  Testing ''style follows series when a preceding series is removed''...");

		Series<Number, Number> a = series("a");
		Series<Number, Number> b = series("b");
		Series<Number, Number> c = series("c");

		run(() -> {
			chart.getData().addAll(a, b, c);
			chart.setSeriesColor(c, CUSTOM);
			chart.setShowLine(a, false);
			chart.setShowMarker(c, true);
		});

		assertThat(lineStyle(c)).contains(CUSTOM_STROKE);
		assertThat(lineStyle(a)).contains("-fx-stroke: transparent;");

		//	"b" and "c" move to positions 0 and 1.
		run(() -> chart.getData().remove(a));

		assertThat(lineStyle(c)).contains(CUSTOM_STROKE);
		assertThat(lineStyle(b)).doesNotContain(CUSTOM_STROKE).doesNotContain("transparent");
		assertThat(symbols(c)).allSatisfy(node -> assertThat(node.getStyle()).contains("visibility: visible;"));
		assertThat(symbols(b)).isEmpty();

	}

	@Test
	public void testStyleFollowsSeriesWhenReordered() {

		System.out.println("  Testing ''style follows series when re-ordered''...");

		Series<Number, Number> a = series("a");
		Series<Number, Number> b = series("b");

		run(() -> {
			chart.getData().addAll(a, b);
			chart.setSeriesColor(a, CUSTOM);
		});
		run(() -> chart.getData().setAll(b, a));

		assertThat(lineStyle(a)).contains(CUSTOM_STROKE);
		assertThat(lineStyle(b)).doesNotContain(CUSTOM_STROKE);

	}

	@Test
	public void testStyleSetBeforeSeriesAdded() {

		System.out.println("  Testing ''style set before the series is added''...");

		Series<Number, Number> a = series("a");

		run(() -> {
			chart.setSeriesColor(a, CUSTOM);
			chart.getData().add(a);
		});

		assertThat(lineStyle(a)).contains(CUSTOM_STROKE);

	}

	@Test
	public void testStyleKeptWhenSeriesReplacedBySameName() {

		System.out.println("  Testing ''style kept when a series is replaced by one with the same name''...");

		Series<Number, Number> a = series("a");
		Series<Number, Number> newA = series("a");

		run(() -> {
			chart.getData().add(a);
			chart.setSeriesColor(a, CUSTOM);
		});
		run(() -> chart.getData().setAll(newA));

		assertThat(lineStyle(newA)).contains(CUSTOM_STROKE);

	}

	@Test
	public void testUnnamedSeriesStyledIndependently() {

		System.out.println("  Testing ''unnamed series are styled independently''...");

		Series<Number, Number> a = series(null);
		Series<Number, Number> b = series(null);

		run(() -> {
			chart.getData().addAll(a, b);
			chart.setSeriesColor(b, CUSTOM);
		});

		assertThat(lineStyle(b)).contains(CUSTOM_STROKE);
		assertThat(lineStyle(a)).doesNotContain(CUSTOM_STROKE);

	}

	@Test
	public void testClearResetsStyles() {

		System.out.println("  Testing ''clear() resets all styles''...");

		Series<Number, Number> a = series("a");
		Series<Number, Number> newA = series("a");

		run(() -> {
			chart.getData().add(a);
			chart.setSeriesColor(a, CUSTOM);
			chart.setLineStyle(a, LineStyle.DASHED);
		});

		assertThat(lineStyle(a)).contains(CUSTOM_STROKE).contains("-fx-stroke-dash-array:");

		run(() -> {
			chart.clear();
			chart.getData().add(newA);
		});

		assertThat(lineStyle(newA)).doesNotContain(CUSTOM_STROKE).doesNotContain("-fx-stroke-dash-array:");

	}

	private String lineStyle( Series<Number, Number> series ) {
		assertThat(series.getNode()).as("node of series ''%s''", series.getName()).isNotNull();
		return series.getNode().getStyle();
	}

	/**
	 * Runs the given action on the FX thread, then waits for the following
	 * layout pass (where fresh symbol nodes are styled).
	 */
	private void run( Runnable action ) {
		interact(action);
		interact(chart::layout);
		WaitForAsyncUtils.waitForFxEvents();
	}

	private static Series<Number, Number> series( String name ) {

		Series<Number, Number> series = new Series<>();

		series.setName(name);

		for ( int i = 0; i < 10; i++ ) {
			series.getData().add(new Data<>(i, i * i));
		}

		return series;

	}

	private static List<Node> symbols( Series<Number, Number> series ) {
		return series.getData().stream().map(Data::getNode).filter(n -> n != null).toList();
	}

}
