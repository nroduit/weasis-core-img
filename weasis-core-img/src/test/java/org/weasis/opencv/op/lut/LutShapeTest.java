/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.op.lut.LutShape.Function;

@DisplayName("LutShape Tests")
class LutShapeTest {

  private LookupTableCV validLookupTable;

  @BeforeEach
  void setUp() {
    validLookupTable = new LookupTableCV(new byte[] {1, -1, 1, -1, 0, 127, -128, -9, 9});
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create LutShape with lookup table and explanation")
    void shouldCreateLutShapeWithLookupTable() {
      String explanation = "Custom LUT Explanation";
      LutShape lutShape = new LutShape(validLookupTable, explanation);

      assertNull(
          lutShape.getFunctionType(), "LutShape with lookup table should have null function type");
      assertEquals(explanation, lutShape.toString(), "toString should return explanation");
      assertEquals(
          explanation,
          lutShape.getExplanation(),
          "getExplanation should return provided explanation");
      assertSame(
          validLookupTable, lutShape.getLookup(), "Should return same lookup table instance");
      assertFalse(lutShape.isFunction(), "Should not be a function-based LutShape");
    }

    @Test
    @DisplayName("Should create LutShape with function and default explanation")
    void shouldCreateLutShapeWithFunctionDefault() {
      LutShape lutShape = new LutShape(Function.LINEAR);

      assertEquals(
          Function.LINEAR, lutShape.getFunctionType(), "Should have correct function type");
      assertEquals(
          Function.LINEAR.getDescription(), lutShape.toString(), "Should use function description");
      assertEquals(
          Function.LINEAR.getDescription(),
          lutShape.getExplanation(),
          "Should use function description as explanation");
      assertNull(lutShape.getLookup(), "Function-based LutShape should have null lookup table");
      assertTrue(lutShape.isFunction(), "Should be a function-based LutShape");
    }

    @Test
    @DisplayName("Should create LutShape with function and custom explanation")
    void shouldCreateLutShapeWithFunctionCustom() {
      String customExplanation = "Custom Linear Explanation";
      LutShape lutShape = new LutShape(Function.LOG_INV, customExplanation);

      assertEquals(
          Function.LOG_INV, lutShape.getFunctionType(), "Should have correct function type");
      assertEquals(customExplanation, lutShape.toString(), "Should use custom explanation");
      assertEquals(customExplanation, lutShape.getExplanation(), "Should use custom explanation");
      assertNull(lutShape.getLookup(), "Function-based LutShape should have null lookup table");
      assertTrue(lutShape.isFunction(), "Should be a function-based LutShape");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null lookup table")
    void shouldThrowExceptionForNullLookupTable() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new LutShape((LookupTableCV) null, "explanation"));
      assertEquals("Lookup table cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null function")
    void shouldThrowExceptionForNullFunction() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new LutShape((Function) null, "explanation"));
      assertEquals("Function cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept empty string as explanation")
    void shouldAcceptEmptyStringAsExplanation() {
      assertDoesNotThrow(() -> new LutShape(Function.SIGMOID, null));

      LutShape lutShape = new LutShape(Function.SIGMOID, null);
      assertEquals("", lutShape.getExplanation());
      assertEquals("", lutShape.toString());
    }
  }

  @Nested
  @DisplayName("Function Enum Tests")
  class FunctionEnumTests {

    @ParameterizedTest
    @EnumSource(Function.class)
    @DisplayName("Should have valid description for all Function values")
    void shouldHaveValidDescriptionForAllFunctions(Function function) {
      assertNotNull(
          function.getDescription(), function.name() + " should have non-null description");
      assertFalse(
          function.getDescription().trim().isEmpty(),
          function.name() + " should have non-empty description");
      assertEquals(
          function.getDescription(),
          function.toString(),
          function.name() + " toString should match getDescription");
    }

    @Test
    @DisplayName("Should have expected Function enum values")
    void shouldHaveExpectedFunctionValues() {
      Function[] expectedFunctions = {
        Function.LINEAR, Function.SIGMOID, Function.SIGMOID_NORM, Function.LOG, Function.LOG_INV
      };
      Function[] actualFunctions = Function.values();

      assertEquals(
          expectedFunctions.length,
          actualFunctions.length,
          "Should have expected number of Function enum values");

      for (Function expected : expectedFunctions) {
        boolean found = false;
        for (Function actual : actualFunctions) {
          if (expected == actual) {
            found = true;
            break;
          }
        }
        assertTrue(found, "Expected Function value should exist: " + expected);
      }
    }

    @Test
    @DisplayName("Should have correct descriptions for predefined functions")
    void shouldHaveCorrectDescriptions() {
      assertAll(
          "Function descriptions",
          () -> assertEquals("Linear", Function.LINEAR.getDescription()),
          () -> assertEquals("Sigmoid", Function.SIGMOID.getDescription()),
          () -> assertEquals("Sigmoid Normalize", Function.SIGMOID_NORM.getDescription()),
          () -> assertEquals("Logarithmic", Function.LOG.getDescription()),
          () -> assertEquals("Logarithmic Inverse", Function.LOG_INV.getDescription()));
    }
  }

  @Nested
  @DisplayName("Predefined Constants Tests")
  class PredefinedConstantsTests {

    @Test
    @DisplayName("Should have correct predefined LutShape constants")
    void shouldHaveCorrectPredefinedConstants() {
      assertAll(
          "Predefined constants",
          () -> assertNotNull(LutShape.LINEAR, "LINEAR constant should exist"),
          () -> assertNotNull(LutShape.SIGMOID, "SIGMOID constant should exist"),
          () -> assertNotNull(LutShape.SIGMOID_NORM, "SIGMOID_NORM constant should exist"),
          () -> assertNotNull(LutShape.LOG, "LOG constant should exist"),
          () -> assertNotNull(LutShape.LOG_INV, "LOG_INV constant should exist"));
    }

    @Test
    @DisplayName("Should have predefined constants with correct function types")
    void shouldHavePredefinedConstantsWithCorrectTypes() {
      assertAll(
          "Predefined constant function types",
          () -> assertEquals(Function.LINEAR, LutShape.LINEAR.getFunctionType()),
          () -> assertEquals(Function.SIGMOID, LutShape.SIGMOID.getFunctionType()),
          () -> assertEquals(Function.SIGMOID_NORM, LutShape.SIGMOID_NORM.getFunctionType()),
          () -> assertEquals(Function.LOG, LutShape.LOG.getFunctionType()),
          () -> assertEquals(Function.LOG_INV, LutShape.LOG_INV.getFunctionType()));
    }

    @Test
    @DisplayName("Should have predefined constants as function-based")
    void shouldHavePredefinedConstantsAsFunctionBased() {
      assertAll(
          "Predefined constants should be function-based",
          () -> assertTrue(LutShape.LINEAR.isFunction()),
          () -> assertTrue(LutShape.SIGMOID.isFunction()),
          () -> assertTrue(LutShape.SIGMOID_NORM.isFunction()),
          () -> assertTrue(LutShape.LOG.isFunction()),
          () -> assertTrue(LutShape.LOG_INV.isFunction()));
    }

    @Test
    @DisplayName("Should have predefined constants with null lookup tables")
    void shouldHavePredefinedConstantsWithNullLookup() {
      assertAll(
          "Predefined constants should have null lookup",
          () -> assertNull(LutShape.LINEAR.getLookup()),
          () -> assertNull(LutShape.SIGMOID.getLookup()),
          () -> assertNull(LutShape.SIGMOID_NORM.getLookup()),
          () -> assertNull(LutShape.LOG.getLookup()),
          () -> assertNull(LutShape.LOG_INV.getLookup()));
    }

    @Test
    @DisplayName("Should return all predefined constants from getAllPredefined")
    void shouldReturnAllPredefinedConstants() {
      Set<LutShape> predefined = LutShape.getAllPredefined();

      assertEquals(5, predefined.size(), "Should return 5 predefined constants");
      assertAll(
          "All predefined constants should be included",
          () -> assertTrue(predefined.contains(LutShape.LINEAR)),
          () -> assertTrue(predefined.contains(LutShape.SIGMOID)),
          () -> assertTrue(predefined.contains(LutShape.SIGMOID_NORM)),
          () -> assertTrue(predefined.contains(LutShape.LOG)),
          () -> assertTrue(predefined.contains(LutShape.LOG_INV)));
    }
  }

  @Nested
  @DisplayName("String Lookup Tests")
  class StringLookupTests {

    @Test
    @DisplayName("Should return correct LutShape for valid function names")
    void shouldReturnCorrectLutShapeForValidNames() {
      assertAll(
          "Valid function name lookups",
          () -> assertSame(LutShape.LINEAR, LutShape.getLutShape("LINEAR")),
          () -> assertSame(LutShape.SIGMOID, LutShape.getLutShape("SIGMOID")),
          () -> assertSame(LutShape.SIGMOID_NORM, LutShape.getLutShape("SIGMOID_NORM")),
          () -> assertSame(LutShape.LOG, LutShape.getLutShape("LOG")),
          () -> assertSame(LutShape.LOG_INV, LutShape.getLutShape("LOG_INV")));
    }

    @Test
    @DisplayName("Should handle case-insensitive function names")
    void shouldHandleCaseInsensitiveFunctionNames() {
      assertAll(
          "Case-insensitive lookups",
          () -> assertSame(LutShape.LINEAR, LutShape.getLutShape("linear")),
          () -> assertSame(LutShape.SIGMOID, LutShape.getLutShape("sigmoid")),
          () -> assertSame(LutShape.SIGMOID_NORM, LutShape.getLutShape("sigmoid_norm")),
          () -> assertSame(LutShape.LOG, LutShape.getLutShape("log")),
          () -> assertSame(LutShape.LOG_INV, LutShape.getLutShape("log_inv")),
          () -> assertSame(LutShape.LINEAR, LutShape.getLutShape("Linear")),
          () -> assertSame(LutShape.SIGMOID, LutShape.getLutShape("Sigmoid")));
    }

    @Test
    @DisplayName("Should handle whitespace in function names")
    void shouldHandleWhitespaceInFunctionNames() {
      assertAll(
          "Whitespace handling",
          () -> assertSame(LutShape.LINEAR, LutShape.getLutShape("  LINEAR  ")),
          () -> assertSame(LutShape.SIGMOID, LutShape.getLutShape("\tSIGMOID\n")),
          () -> assertSame(LutShape.LOG, LutShape.getLutShape(" log ")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Should return null for invalid function names")
    void shouldReturnNullForInvalidFunctionNames(String invalidName) {
      assertNull(
          LutShape.getLutShape(invalidName),
          "Should return null for invalid function name: '" + invalidName + "'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "Shape", "INVALID", "LINEAR_INV", "QUADRATIC"})
    @DisplayName("Should return null for unknown function names")
    void shouldReturnNullForUnknownFunctionNames(String unknownName) {
      assertNull(
          LutShape.getLutShape(unknownName),
          "Should return null for unknown function name: " + unknownName);
    }
  }

  @Nested
  @DisplayName("Equality and HashCode Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should be equal when same function type and explanation")
    void shouldBeEqualWhenSameFunctionAndExplanation() {
      LutShape lutShape1 = new LutShape(Function.LINEAR);
      LutShape lutShape2 = new LutShape(Function.LINEAR, Function.LINEAR.getDescription());

      assertEquals(lutShape1, lutShape2, "LutShapes with same function should be equal");
      assertEquals(
          lutShape1.hashCode(), lutShape2.hashCode(), "Equal LutShapes should have same hash code");
    }

    @Test
    @DisplayName("Should be equal when same lookup table and explanation")
    void shouldBeEqualWhenSameLookupAndExplanation() {
      String explanation = "Test explanation";
      LutShape lutShape1 = new LutShape(validLookupTable, explanation);
      LutShape lutShape2 = new LutShape(validLookupTable, explanation);

      assertEquals(lutShape1, lutShape2, "LutShapes with same lookup table should be equal");
      assertEquals(
          lutShape1.hashCode(), lutShape2.hashCode(), "Equal LutShapes should have same hash code");
    }

    @Test
    @DisplayName("Should not be equal when different function types")
    void shouldNotBeEqualWhenDifferentFunctionTypes() {
      LutShape linearShape = new LutShape(Function.LINEAR);
      LutShape sigmoidShape = new LutShape(Function.SIGMOID);

      assertNotEquals(
          linearShape, sigmoidShape, "LutShapes with different functions should not be equal");
      assertNotEquals(
          linearShape.hashCode(),
          sigmoidShape.hashCode(),
          "Different LutShapes should have different hash codes");
    }

    @Test
    @DisplayName("Should not be equal when different explanations")
    void shouldNotBeEqualWhenDifferentExplanations() {
      LutShape lutShape1 = new LutShape(Function.LINEAR, "Explanation 1");
      LutShape lutShape2 = new LutShape(Function.LINEAR, "Explanation 2");

      assertNotEquals(
          lutShape1, lutShape2, "LutShapes with different explanations should not be equal");
    }

    @Test
    @DisplayName("Should not be equal when comparing function vs lookup table")
    void shouldNotBeEqualWhenFunctionVsLookup() {
      LutShape functionShape = new LutShape(Function.LINEAR);
      LutShape lookupShape = new LutShape(validLookupTable, "Linear");

      assertNotEquals(
          functionShape,
          lookupShape,
          "Function-based and lookup-based LutShapes should not be equal");
    }

    @Test
    @DisplayName("Should not be equal to null or different class")
    void shouldNotBeEqualToNullOrDifferentClass() {
      LutShape lutShape = new LutShape(Function.LINEAR);

      assertAll(
          "Null and different class equality",
          () -> assertNotEquals(lutShape, null),
          () -> assertNotEquals(lutShape, "Not a LutShape"),
          () -> assertNotEquals(lutShape, Integer.valueOf(42)));
    }

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
      LutShape lutShape = new LutShape(Function.SIGMOID);
      assertEquals(lutShape, lutShape, "LutShape should be equal to itself");
    }
  }

  @Nested
  @DisplayName("String Representation Tests")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should return explanation as string representation")
    void shouldReturnExplanationAsString() {
      String customExplanation = "Custom transformation explanation";
      LutShape functionShape = new LutShape(Function.LOG, customExplanation);
      LutShape lookupShape = new LutShape(validLookupTable, customExplanation);

      assertEquals(customExplanation, functionShape.toString());
      assertEquals(customExplanation, lookupShape.toString());
    }

    @Test
    @DisplayName("Should use function description as default string representation")
    void shouldUseFunctionDescriptionAsDefaultString() {
      LutShape lutShape = new LutShape(Function.SIGMOID_NORM);
      assertEquals(Function.SIGMOID_NORM.getDescription(), lutShape.toString());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle lookup table with single byte")
    void shouldHandleSingleByteLookupTable() {
      LookupTableCV singleByteLut = new LookupTableCV(new byte[] {42});
      assertDoesNotThrow(() -> new LutShape(singleByteLut, "Single byte LUT"));
    }

    @Test
    @DisplayName("Should handle lookup table with maximum bytes")
    void shouldHandleMaximumBytesLookupTable() {
      byte[] maxBytes = new byte[1000]; // Large lookup table
      for (int i = 0; i < maxBytes.length; i++) {
        maxBytes[i] = (byte) (i % 256);
      }
      LookupTableCV largeLut = new LookupTableCV(maxBytes);

      assertDoesNotThrow(() -> new LutShape(largeLut, "Large LUT"));
    }
  }
}
