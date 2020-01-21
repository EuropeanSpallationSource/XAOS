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

/**
 * @author claudio.rosati@esss.se
 */
module xaos.ui {

	requires java.logging;
	requires java.xml;
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.fxml;
	requires transitive javafx.graphics;
	requires javafx.media;
	requires javafx.swing;
	requires javafx.web;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	requires org.kordamp.iconli.core;
	requires org.kordamp.ikonli.fontawesome;
	requires org.kordamp.ikonli.javafx;
	requires org.kordamp.ikonli.materialdesign;
	requires transitive xaos.core;
	requires transitive xaos.tools;

	uses eu.ess.xaos.ui.spi.ClassIconProvider;
	uses eu.ess.xaos.ui.spi.FileExtensionIconProvider;
	uses eu.ess.xaos.ui.spi.IconProvider;
	uses eu.ess.xaos.ui.spi.MIMETypeIconProvider;

	provides eu.ess.xaos.ui.spi.ClassIconProvider
		with eu.ess.xaos.ui.spi.impl.DefaultJavaFXClassIconProvider;
	provides eu.ess.xaos.ui.spi.FileExtensionIconProvider
		with eu.ess.xaos.ui.spi.impl.DefaultFileExtensionIconProvider;
	provides eu.ess.xaos.ui.spi.IconProvider
		with eu.ess.xaos.ui.spi.impl.DefaultCommonIconProvider;
	provides eu.ess.xaos.ui.spi.MIMETypeIconProvider
		with eu.ess.xaos.ui.spi.impl.DefaultMIMETypeIconProvider;

	exports eu.ess.xaos.ui.control;
	exports eu.ess.xaos.ui.control.svg;
	exports eu.ess.xaos.ui.control.tree;
	exports eu.ess.xaos.ui.control.tree.directory;
	exports eu.ess.xaos.ui.spi;
	exports eu.ess.xaos.ui.util;

	opens eu.ess.xaos.ui.control to javafx.fxml;

}
