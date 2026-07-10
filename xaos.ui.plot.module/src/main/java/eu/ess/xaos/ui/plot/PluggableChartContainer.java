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

import java.net.URL;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Accordion;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Control;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import org.controlsfx.control.PopOver;
import eu.ess.xaos.core.util.LogUtils;
import eu.ess.xaos.tools.annotation.BundleItem;
import eu.ess.xaos.tools.annotation.BundleItems;
import eu.ess.xaos.tools.annotation.Bundles;
import eu.ess.xaos.tools.annotation.ServiceLoaderUtilities;
import eu.ess.xaos.ui.control.CommonIcons;
import static eu.ess.xaos.ui.control.CommonIcons.CHEVRON_DOWN;
import static eu.ess.xaos.ui.control.CommonIcons.CHEVRON_UP;
import eu.ess.xaos.ui.control.Icons;
import eu.ess.xaos.ui.plot.plugins.Pluggable;
import eu.ess.xaos.ui.plot.spi.ToolbarContributor;
import eu.ess.xaos.ui.util.FXUtils;

import static java.util.logging.Level.WARNING;
import static org.controlsfx.control.PopOver.ArrowLocation.TOP_RIGHT;
import static eu.ess.xaos.ui.control.CommonIcons.INFO;
import static eu.ess.xaos.ui.util.FXUtils.makeSquare;
import javafx.scene.chart.Chart;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A container for {@link XYChart}s where tool-bar is displayed when the mouse cursor is close to the top border. Once
 * the tool-bar is displayed it can be pinned.
 * <p>
 * <b>Note:</b> The chart must be installed with {@link #setPluggable(Pluggable)},
 * not by adding it directly to this container's children.</p>
 *
 * @author claudio.rosati@esss.se
 * @css.class {@code chart-container-toolbar}
 */
public class PluggableChartContainer extends AnchorPane {

    private static final Logger LOGGER = Logger.getLogger(PluggableChartContainer.class.getName());
    private final Button showButton = new Button(null, Icons.iconFor(CHEVRON_DOWN, 14));
    private final ToolBar toolbar = new ToolBar();

    private static final Tooltip showToolbarToltip = new Tooltip("Show toolbar");
    private static final Tooltip hideToolbarToltip = new Tooltip("Hide toolbar");

    /* *********************************************************************** *
	 * START OF JAVAFX PROPERTIES                                              *
	 * *********************************************************************** */

 /*
	 * ---- pluggable ----------------------------------------------------------
     */
    private final ObjectProperty<Pluggable> pluggable = new SimpleObjectProperty<>(PluggableChartContainer.this, "pluggable") {
        @Override
        protected void invalidated() {
            Chart chart = get().getChart();
            getChildren().add(0, chart);

            AnchorPane.setTopAnchor(chart, 0.0);
            AnchorPane.setLeftAnchor(chart, 0.0);
            AnchorPane.setRightAnchor(chart, 0.0);
            AnchorPane.setBottomAnchor(chart, 0.0);
        }
    };

    public final ObjectProperty<Pluggable> pluggableProperty() {
        return pluggable;
    }

    public final Pluggable getPluggable() {
        return pluggableProperty().get();
    }

    public final void setPluggable(Pluggable value) {
        pluggableProperty().set(value);
    }

    /* *********************************************************************** *
	 * END OF JAVAFX PROPERTIES                                                *
	 * *********************************************************************** */
    /**
     * Creates a new instance of this container.
     */
    @BundleItems({
        @BundleItem(key = "infoButton.tooltip", message = "Open/close the plugins info dialog."), //		@BundleItem( key = "pinButton.tooltip", message = "Pin/unpin toolbar." )
    })
    public PluggableChartContainer() {
        VBox.setVgrow(this, Priority.ALWAYS);

        //	Horizontal filler...
        Pane filler = new Pane();

        HBox.setHgrow(filler, Priority.ALWAYS);

        //	Buttons created all together in order to be passed to handling
        //	callbacks.
        ToggleButton infoButton = new ToggleButton(null, Icons.iconFor(INFO, 14));

        //	Info/Help button...
        infoButton.setOnAction(e -> handleInfoButton(infoButton));
        infoButton.setTooltip(new Tooltip(getString("infoButton.tooltip")));
        // Using listeners instead of bindings to avoid warnings.
        pluggable.addListener((obs, oldVal, newVal) -> {
            if (obs != null && !newVal.getPlugins().isEmpty()) {
                infoButton.disableProperty().setValue(Boolean.FALSE);
            } else {
                infoButton.disableProperty().setValue(Boolean.TRUE);
            }
        });
        infoButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            infoButton.disableProperty().setValue(newVal);
        });

        //	Setup the toolbar
        toolbar.setOpacity(0.66);
        // Toolbar hidden by default
        toolbar.setVisible(false);

        ObservableList<Node> tItems = toolbar.getItems();

        ServiceLoaderUtilities.of(ServiceLoader.load(ToolbarContributor.class)).forEach(tc -> {

            if (tc.isPrecededBySeparator()) {
                tItems.add(new Separator());
            }

            Control element = tc.provide(this);

            if (element != null) {
                if (element instanceof ButtonBase) {
                    tItems.add(makeSquare(element, 22));
                } else {
                    tItems.add(element);
                }
            } else {
                LogUtils.log(LOGGER, WARNING, "Null component provided by ''{0}''.", tc.getClass());
            }

        });

        tItems.add(filler);
        tItems.add(makeSquare(infoButton, 22));

        Pane rightFiller = new Pane();
        rightFiller.setPrefWidth(22);
        tItems.add(rightFiller);

        getChildren().add(toolbar);

        AnchorPane.setTopAnchor(toolbar, 0.0);
        AnchorPane.setLeftAnchor(toolbar, 0.0);
        AnchorPane.setRightAnchor(toolbar, 0.0);

        Region showButtonSquare = makeSquare(showButton, 22);
        getChildren().add(showButtonSquare);

        AnchorPane.setTopAnchor(showButtonSquare, 6.0);
        AnchorPane.setRightAnchor(showButtonSquare, 6.0);

        showButton.setOnAction((e) -> {
            toolbar.setVisible(!toolbar.isVisible());
            if (toolbar.isVisible()) {
                showButton.setGraphic(Icons.iconFor(CHEVRON_UP, 14));
                showButton.setTooltip(hideToolbarToltip);
            } else {
                showButton.setGraphic(Icons.iconFor(CHEVRON_DOWN, 14));
                showButton.setTooltip(showToolbarToltip);
            }
        });
        
        showButton.setTooltip(showToolbarToltip);
        showButton.setFocusTraversable(false);
    }

    /**
     * Creates a new instance of this container with the given {@code chart} in its center and a toolbar on the top
     * side, sliding down when the mouse cursor is close to it.
     *
     * @param pluggable The {@link Pluggable} chart to be contained.
     */
    public PluggableChartContainer(Pluggable pluggable) {
        this();
        setPluggable(pluggable);
    }

    private String getString(String key) {
        return Bundles.get(PluggableChartContainer.class, key);
    }

    @BundleItems({
        @BundleItem(
                key = "html.language.variation",
                comment = "The extension will be added to class name, before the '.html' extension.\n"
                + "It could be something like '_it', or '_fr'.",
                message = ""
        ),
        @BundleItem(key = "infoPopOver.title", message = "Plugins Info")
    })
    private void handleInfoButton(ToggleButton infoButton) {
        Accordion accordion = new Accordion();

        getPluggable().getPlugins().stream()
                .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                .forEach(p -> {

                    //	Looking at the "htmls" package for an HTML file named
                    //	"class-name.html", where "class-name" is the actual plugin
                    //	simple class name. That HTML page should contain a
                    //	descritpion for the user about what the plugin does.
                    //	The HTML file name can have locale extensions. See the
                    //	above BundleItem entry.
                    String htmlResourceName = "/htmls/"
                            + p.getClass().getSimpleName()
                            + getString("html.language.variation")
                            + ".html";
                    URL htmlResourceURL = getClass().getResource(htmlResourceName);

                    if (htmlResourceURL != null) {

                        WebView view = new WebView();

                        view.getEngine().load(htmlResourceURL.toExternalForm());
                        view.setPrefSize(500, 250);

                        TitledPane titledPane;

                        if (p.isBindFailed()) {

                            Node icon = Icons.iconFor(CommonIcons.WARNING, 16);
                            Tooltip tooltip = new Tooltip(p.getFailureMessage());

                            Tooltip.install(icon, tooltip);

                            titledPane = FXUtils.createTitlePane(p.getName(), view, icon);

                        } else {
                            titledPane = new TitledPane(p.getName(), view);
                        }

                        accordion.getPanes().add(titledPane);

                    }

                });

        accordion.setExpandedPane(accordion.getPanes().get(0));

        PopOver popOver = new PopOver(accordion);

        popOver.setAnimated(false);
        popOver.setCloseButtonEnabled(true);
        popOver.setDetachable(true);
        popOver.setHeaderAlwaysVisible(true);
        popOver.setHideOnEscape(true);
        popOver.setArrowLocation(TOP_RIGHT);
        popOver.setOnHidden(e -> infoButton.setSelected(false));
        popOver.setTitle(getString("infoPopOver.title"));

        popOver.show(infoButton);

    }

}
