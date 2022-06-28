/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2018-2022 by European Spallation Source ERIC.
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
package eu.ess.xaos.ui.plot.util;

/**
 * Predefined marker symbols.
 *
 * @author Juan F. Esteban Müller <JuanF.EstebanMuller@ess.eu>
 */
public enum MarkerSymbol {
    SOLID_CIRCLE("-fx-background-radius: 5px; -fx-padding: 5px; "),
    SOLID_SQUARE("-fx-background-radius: 0; "),
    SOLID_DIAMOND("-fx-background-radius: 0; -fx-padding: 7px 5px 7px 5px; -fx-shape: \"M5,0 L10,9 L5,18 L0,9 Z\"; "),
    SOLID_TRIANGLE("-fx-background-radius: 0; -fx-background-insets: 0; -fx-shape: \"M5,0 L10,8 L0,8 Z\"; "),
    CROSS("-fx-background-radius: 0; -fx-background-insets: 0;\n" + " -fx-shape: \"M2,0 L5,4 L8,0 L10,0 L10,2 L6,5 L10,8 L10,10 L8,10 L5,6 L2,10 L0,10 L0,8 L4,5 L0,2 L0,0 Z\"; "),
    HOLLOW_CIRCLE("-fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px; "),
    HOLLOW_SQUARE("-fx-background-insets: 0, 2; -fx-background-radius: 0; "),
    HOLLOW_DIAMOND("-fx-background-radius: 0; -fx-background-insets: 0, 2.5; -fx-padding: 7px 5px 7px 5px; -fx-shape: \"M5,0 L10,9 L5,18 L0,9 Z\"; "),
    HOLLOW_TRIANGLE("-fx-background-radius: 0; -fx-background-insets: 0, 2.5; -fx-shape: \"M5,0 L10,8 L0,8 Z\"; ");
    private String style;

    private MarkerSymbol(String style) {
        this.style = style;
    }

    public String getStyle() {
        return style;
    }

}
