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
package eu.ess.xaos.ui.plot.demo;


import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import eu.ess.xaos.ui.plot.AreaChartFX;
import eu.ess.xaos.ui.plot.LogAxis;
import eu.ess.xaos.ui.plot.NumberAxis;
import eu.ess.xaos.ui.plot.TimeAxis;
import eu.ess.xaos.ui.plot.data.DataReducingSeries;
import eu.ess.xaos.ui.plot.plugins.Plugins;

import static eu.ess.xaos.ui.plot.demo.ChartGenerator.generateData;


/**
 * @author claudio.rosati@esss.se
 */
@SuppressWarnings( "ClassWithoutLogger" )
public class TimeAreaChartGenerator implements ChartGenerator<Number, Number> {

	private DataReducingSeries<Number, Number> series0;
	private DataReducingSeries<Number, Number> series1;
	private DataReducingSeries<Number, Number> series2;

	@Override
	public XYChart<Number, Number> getNewChart( int numberOfPoints, boolean logXAxis, boolean logYAxis ) {

//		ValueAxis<Number> xAxis = new TimeAxis(SECONDS, MILLISECONDS);
		ValueAxis<Number> xAxis = new TimeAxis();
		ValueAxis<Number> yAxis = logYAxis ? new LogAxis() : new NumberAxis();

		xAxis.setAnimated(false);
		yAxis.setAnimated(false);

		AreaChartFX<Number, Number> chart = new AreaChartFX<Number, Number>(xAxis, yAxis);

		chart.setTitle("Time Area Test Data");
		chart.setAnimated(false);
		chart.setOnMouseClicked(event -> chart.requestFocus());
		chart.getPlugins().addAll(Plugins.all());
		
		if ( series0 == null ) {

			series0 = new DataReducingSeries<>();
			series0.setName("Generated test data-horizontal");
			series0.setData(generateData(numberOfPoints));

			series1 = new DataReducingSeries<>();
			series1.setName("Generated test data-vertical");
			series1.setData(generateData(numberOfPoints));

			series2 = new DataReducingSeries<>();
			series2.setName("Generated test data-longitudinal");
			series2.setData(generateData(numberOfPoints));

		}

		chart.getData().add(series0.getSeries());
		chart.getData().add(series1.getSeries());
		chart.getData().add(series2.getSeries());

		chart.setHVLSeries(0, 1, 2);

		return chart;

	}

}
