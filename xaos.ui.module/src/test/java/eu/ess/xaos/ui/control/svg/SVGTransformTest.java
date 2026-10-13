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
package eu.ess.xaos.ui.control.svg;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;
import javax.xml.stream.XMLStreamException;
import org.assertj.core.data.Offset;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests the parsing of the SVG {@code transform} attribute: each case maps a
 * point through the transform built for a {@code <rect>} element.
 */
@SuppressWarnings( { "UseOfSystemOutOrSystemErr", "ClassWithoutLogger" } )
public class SVGTransformTest {

	private static final Offset<Double> EPSILON = Offset.offset(1E-9);

	@BeforeClass
	public static void setUpClass() {
		System.out.println("---- SVGTransformTest ------------------------------------------");
	}

	@Test
	public void testTranslateTwoValues() throws IOException, XMLStreamException {
		System.out.println("  Testing 'translate(tx,ty)'...");
		assertMaps("translate(10,20)", new Point2D(1, 1), new Point2D(11, 21));
		assertMaps("translate(10 20)", new Point2D(1, 1), new Point2D(11, 21));
	}

	@Test
	public void testTranslateOneValue() throws IOException, XMLStreamException {
		System.out.println("  Testing 'translate(tx)'...");
		assertMaps("translate(10)", new Point2D(1, 1), new Point2D(11, 1));
	}

	@Test
	public void testScaleTwoValues() throws IOException, XMLStreamException {
		System.out.println("  Testing 'scale(sx,sy)'...");
		assertMaps("scale(2,3)", new Point2D(1, 1), new Point2D(2, 3));
	}

	@Test
	public void testScaleOneValue() throws IOException, XMLStreamException {
		System.out.println("  Testing 'scale(s)'...");
		assertMaps("scale(2)", new Point2D(1, 1), new Point2D(2, 2));
	}

	@Test
	public void testTransformList() throws IOException, XMLStreamException {

		System.out.println("  Testing transform lists...");

		//	Transforms apply right to left: scale first, then translate.
		assertMaps("translate(10,20) scale(2,3)", new Point2D(1, 1), new Point2D(12, 23));
		assertMaps("translate(10, 20), scale(2)", new Point2D(1, 1), new Point2D(12, 22));
		assertMaps("scale(2)translate(10,20)", new Point2D(1, 1), new Point2D(22, 42));
		assertMaps("\n  translate(10,20)\n  rotate(90)\n", new Point2D(1, 0), new Point2D(10, 21));

	}

	@Test
	public void testDukeTransform() throws IOException, XMLStreamException {

		System.out.println("  Testing the transform used in 'duke.svg'...");

		//	Maps the glyph coordinates (y up, 1/0.045181 units per pixel) into
		//	the 75x136 view box (y down).
		String transform = "translate(0,136) scale(0.045181,-0.045181)";

		assertMaps(transform, new Point2D(0, 0), new Point2D(0, 136));
		assertMaps(transform, new Point2D(1000, 1000), new Point2D(45.181, 136 - 45.181));

	}

	private static void assertMaps( String transform, Point2D point, Point2D expected ) throws IOException, XMLStreamException {

		Rectangle rect = loadRect(transform);
		Point2D actual = rect.getLocalToParentTransform().transform(point);

		assertThat(actual.getX()).as("x of %s through ''%s''", point, transform).isEqualTo(expected.getX(), EPSILON);
		assertThat(actual.getY()).as("y of %s through ''%s''", point, transform).isEqualTo(expected.getY(), EPSILON);

	}

	private static Rectangle loadRect( String transform ) throws IOException, XMLStreamException {

		String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">"
				   + "<rect width=\"1\" height=\"1\" transform=\"" + transform + "\"/>"
				   + "</svg>";
		Rectangle rect = findRect(SVG.load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))));

		assertThat(rect).as("loaded <rect>").isNotNull();

		return rect;

	}

	private static Rectangle findRect( Node node ) {

		if ( node instanceof Rectangle rect ) {
			return rect;
		} else if ( node instanceof Parent parent ) {
			for ( Node child : parent.getChildrenUnmodifiable() ) {

				Rectangle rect = findRect(child);

				if ( rect != null ) {
					return rect;
				}

			}
		}

		return null;

	}

}
