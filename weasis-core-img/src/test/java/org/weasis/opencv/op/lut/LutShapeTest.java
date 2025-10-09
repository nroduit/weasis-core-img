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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.op.lut.LutShape.Function;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LutShapeTest {

  // Test data constants
  private static final byte[] STANDARD_LUT_DATA = {1, -1, 1, -1, 0, 127, -128, -9, 9};
  private static final byte[] SINGLE_BYTE_LUT_DATA = {42};
  private static final String CUSTOM_EXPLANATION = "Custom LUT Explanation";
  private static final String CUSTOM_LINEAR_EXPLANATION = "Custom Linear Explanation";

  private LookupTableCV validLookupTable;
  private LookupTableCV singleByteLookupTable;

  @BeforeEach
  void setUp() {
    validLookupTable = new LookupTableCV(STANDARD_LUT_DATA);
    singleByteLookupTable = new LookupTableCV(SINGLE_BYTE_LUT_DATA);
  }

  @Nested
  class Constructor_Tests {

    @Test
    void create_lut_shape_with_lookup_table_and_explanation() {
      var lutShape = new LutShape(validLookupTable, CUSTOM_EXPLANATION);

      assertAll(
          "LutShape with lookup table",
          () -> assertNull(lutShape.getFunctionType()),
          () -> assertEquals(CUSTOM_EXPLANATION, lutShape.toString()),
          () -> assertEquals(CUSTOM_EXPLANATION, lutShape.getExplanation()),
          () -> assertSame(validLookupTable, lutShape.getLookup()),
          () -> assertFalse(lutShape.isFunction()));
    }

    @Test
    void create_lut_shape_with_function_and_default_explanation() {
      var lutShape = new LutShape(Function.LINEAR);

      assertAll(
          "LutShape with function default explanation",
          () -> assertEquals(Function.LINEAR, lutShape.getFunctionType()),
          () -> assertEquals(Function.LINEAR.getDescription(), lutShape.toString()),
          () -> assertEquals(Function.LINEAR.getDescription(), lutShape.getExplanation()),
          () -> assertNull(lutShape.getLookup()),
          () -> assertTrue(lutShape.isFunction()));
    }

    @Test
    void create_lut_shape_with_function_and_custom_explanation() {
      var lutShape = new LutShape(Function.LOG_INV, CUSTOM_LINEAR_EXPLANATION);

      assertAll(
          "LutShape with function custom explanation",
          () -> assertEquals(Function.LOG_INV, lutShape.getFunctionType()),
          () -> assertEquals(CUSTOM_LINEAR_EXPLANATION, lutShape.toString()),
          () -> assertEquals(CUSTOM_LINEAR_EXPLANATION, lutShape.getExplanation()),
          () -> assertNull(lutShape.getLookup()),
          () -> assertTrue(lutShape.isFunction()));
    }

    @Test
    void throw_exception_for_null_lookup_table() {
      var exception =
          assertThrows(
              NullPointerException.class, () -> new LutShape((LookupTableCV) null, "explanation"));

      assertEquals("Lookup table cannot be null", exception.getMessage());
    }

    @Test
    void throw_exception_for_null_function() {
      var exception =
          assertThrows(
              NullPointerException.class, () -> new LutShape((Function) null, "explanation"));

      assertEquals("Function cannot be null", exception.getMessage());
    }

    @Test
    void accept_null_explanation() {
      assertAll(
          "Null explanation handling",
          () -> assertDoesNotThrow(() -> new LutShape(Function.SIGMOID, null)),
          () -> {
            var lutShape = new LutShape(Function.SIGMOID, null);
            assertEquals("", lutShape.getExplanation());
            assertEquals("", lutShape.toString());
          });
    }
  }

  @Nested
  class Function_Enum_Tests {

    @ParameterizedTest
    @EnumSource(Function.class)
    void have_valid_description_for_all_functions(Function function) {
      assertAll(
          "Function validation",
          () -> assertNotNull(function.getDescription()),
          () -> assertFalse(function.getDescription().isBlank()),
          () -> assertEquals(function.getDescription(), function.toString()));
    }

    @Test
    void have_expected_function_enum_values() {
      var expectedFunctions =
          Set.of(
              Function.LINEAR,
              Function.SIGMOID,
              Function.SIGMOID_NORM,
              Function.LOG,
              Function.LOG_INV);
      var actualFunctions = Set.of(Function.values());

      assertEquals(expectedFunctions, actualFunctions);
    }

    @Test
    void have_correct_descriptions_for_predefined_functions() {
      var expectedDescriptions =
          Map.of(
              Function.LINEAR, "Linear",
              Function.SIGMOID, "Sigmoid",
              Function.SIGMOID_NORM, "Sigmoid Normalize",
              Function.LOG, "Logarithmic",
              Function.LOG_INV, "Logarithmic Inverse");

      expectedDescriptions.forEach(
          (function, expectedDesc) ->
              assertEquals(
                  expectedDesc,
                  function.getDescription(),
                  "Function %s should have correct description".formatted(function)));
    }
  }

  @Nested
  class Predefined_Constants_Tests {

    @Test
    void have_all_predefined_constants() {
      var predefinedConstants =
          List.of(
              LutShape.LINEAR,
              LutShape.SIGMOID,
              LutShape.SIGMOID_NORM,
              LutShape.LOG,
              LutShape.LOG_INV);

      assertAll(
          "Predefined constants existence",
          predefinedConstants.stream()
              .map(constant -> (Executable) () -> assertNotNull(constant))
              .toArray(Executable[]::new));
    }

    @ParameterizedTest
    @MethodSource("predefinedConstantData")
    void have_predefined_constants_with_correct_properties(
        LutShape lutShape, Function expectedFunction) {

      assertAll(
          "Predefined constant properties",
          () -> assertEquals(expectedFunction, lutShape.getFunctionType()),
          () -> assertTrue(lutShape.isFunction()),
          () -> assertNull(lutShape.getLookup()),
          () -> assertEquals(expectedFunction.getDescription(), lutShape.getExplanation()));
    }

    static Stream<Arguments> predefinedConstantData() {
      return Stream.of(
          Arguments.of(LutShape.LINEAR, Function.LINEAR),
          Arguments.of(LutShape.SIGMOID, Function.SIGMOID),
          Arguments.of(LutShape.SIGMOID_NORM, Function.SIGMOID_NORM),
          Arguments.of(LutShape.LOG, Function.LOG),
          Arguments.of(LutShape.LOG_INV, Function.LOG_INV));
    }

    @Test
    void return_all_predefined_constants_from_get_all_predefined() {
      var predefined = LutShape.getAllPredefined();
      var expected =
          Set.of(
              LutShape.LINEAR,
              LutShape.SIGMOID,
              LutShape.SIGMOID_NORM,
              LutShape.LOG,
              LutShape.LOG_INV);

      assertEquals(expected, predefined);
    }
  }

  @Nested
  class String_Lookup_Tests {

    @ParameterizedTest
    @CsvSource({
      "LINEAR, LINEAR",
      "SIGMOID, SIGMOID",
      "SIGMOID_NORM, SIGMOID_NORM",
      "LOG, LOG",
      "LOG_INV, LOG_INV"
    })
    void return_correct_lut_shape_for_valid_function_names(String input, String expectedConstant) {
      var expected =
          switch (expectedConstant) {
            case "LINEAR" -> LutShape.LINEAR;
            case "SIGMOID" -> LutShape.SIGMOID;
            case "SIGMOID_NORM" -> LutShape.SIGMOID_NORM;
            case "LOG" -> LutShape.LOG;
            case "LOG_INV" -> LutShape.LOG_INV;
            default -> throw new IllegalArgumentException("Unknown constant: " + expectedConstant);
          };

      assertSame(expected, LutShape.getLutShape(input));
    }

    @ParameterizedTest
    @CsvSource({
      "linear, LINEAR",
      "sigmoid, SIGMOID",
      "sigmoid_norm, SIGMOID_NORM",
      "log, LOG",
      "log_inv, LOG_INV",
      "Linear, LINEAR",
      "Sigmoid, SIGMOID"
    })
    void handle_case_insensitive_function_names(String input, String expectedConstant) {
      var expected =
          switch (expectedConstant) {
            case "LINEAR" -> LutShape.LINEAR;
            case "SIGMOID" -> LutShape.SIGMOID;
            case "SIGMOID_NORM" -> LutShape.SIGMOID_NORM;
            case "LOG" -> LutShape.LOG;
            case "LOG_INV" -> LutShape.LOG_INV;
            default -> throw new IllegalArgumentException("Unknown constant: " + expectedConstant);
          };

      assertSame(expected, LutShape.getLutShape(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"  LINEAR  ", "\tSIGMOID\n", " log "})
    void handle_whitespace_in_function_names(String input) {
      assertNotNull(LutShape.getLutShape(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", ""})
    void return_null_for_invalid_function_names(String invalidName) {
      assertNull(LutShape.getLutShape(invalidName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "Shape", "INVALID", "LINEAR_INV", "QUADRATIC"})
    void return_null_for_unknown_function_names(String unknownName) {
      assertNull(LutShape.getLutShape(unknownName));
    }
  }

  @Nested
  class Equality_And_Hash_Code_Tests {

    @Test
    void be_equal_when_same_function_type_and_explanation() {
      var lutShape1 = new LutShape(Function.LINEAR);
      var lutShape2 = new LutShape(Function.LINEAR, Function.LINEAR.getDescription());

      assertAll(
          "Same function equality",
          () -> assertEquals(lutShape1, lutShape2),
          () -> assertEquals(lutShape1.hashCode(), lutShape2.hashCode()));
    }

    @Test
    void be_equal_when_same_lookup_table_and_explanation() {
      var explanation = "Test explanation";
      var lutShape1 = new LutShape(validLookupTable, explanation);
      var lutShape2 = new LutShape(validLookupTable, explanation);

      assertAll(
          "Same lookup table equality",
          () -> assertEquals(lutShape1, lutShape2),
          () -> assertEquals(lutShape1.hashCode(), lutShape2.hashCode()));
    }

    @Test
    void not_be_equal_when_different_function_types() {
      var linearShape = new LutShape(Function.LINEAR);
      var sigmoidShape = new LutShape(Function.SIGMOID);

      assertAll(
          "Different function types",
          () -> assertNotEquals(linearShape, sigmoidShape),
          () -> assertNotEquals(linearShape.hashCode(), sigmoidShape.hashCode()));
    }

    @Test
    void not_be_equal_when_different_explanations() {
      var lutShape1 = new LutShape(Function.LINEAR, "Explanation 1");
      var lutShape2 = new LutShape(Function.LINEAR, "Explanation 2");

      assertNotEquals(lutShape1, lutShape2);
    }

    @Test
    void not_be_equal_when_comparing_function_vs_lookup_table() {
      var functionShape = new LutShape(Function.LINEAR);
      var lookupShape = new LutShape(validLookupTable, "Linear");

      assertNotEquals(functionShape, lookupShape);
    }

    @ParameterizedTest
    @ValueSource(classes = {String.class, Integer.class, Object.class})
    void not_be_equal_to_different_class_types(Class<?> otherClass) throws Exception {
      var lutShape = new LutShape(Function.LINEAR);
      var otherObject =
          switch (otherClass.getSimpleName()) {
            case "String" -> "Not a LutShape";
            case "Integer" -> 42;
            default -> new Object();
          };

      assertNotEquals(lutShape, otherObject);
    }

    @Test
    void be_equal_to_itself() {
      var lutShape = new LutShape(Function.SIGMOID);
      assertEquals(lutShape, lutShape);
    }

    @Test
    void not_be_equal_to_null() {
      var lutShape = new LutShape(Function.LINEAR);
      assertNotEquals(lutShape, null);
    }
  }

  @Nested
  class String_Representation_Tests {

    @Test
    void return_explanation_as_string_representation() {
      var customExplanation = "Custom transformation explanation";
      var functionShape = new LutShape(Function.LOG, customExplanation);
      var lookupShape = new LutShape(validLookupTable, customExplanation);

      assertAll(
          "String representation",
          () -> assertEquals(customExplanation, functionShape.toString()),
          () -> assertEquals(customExplanation, lookupShape.toString()));
    }

    @Test
    void use_function_description_as_default_string_representation() {
      var lutShape = new LutShape(Function.SIGMOID_NORM);
      assertEquals(Function.SIGMOID_NORM.getDescription(), lutShape.toString());
    }
  }

  @Nested
  class Edge_Cases_Tests {

    @Test
    void handle_lookup_table_with_single_byte() {
      assertDoesNotThrow(() -> new LutShape(singleByteLookupTable, "Single byte LUT"));
    }

    @Test
    void handle_lookup_table_with_large_data() {
      var largeData =
          IntStream.range(0, 1000)
              .mapToObj(i -> (byte) (i % 256))
              .collect(Collectors.toList())
              .toArray(new Byte[0]);

      var largeDataArray = new byte[largeData.length];
      for (int i = 0; i < largeData.length; i++) {
        largeDataArray[i] = largeData[i];
      }

      var largeLut = new LookupTableCV(largeDataArray);
      assertDoesNotThrow(() -> new LutShape(largeLut, "Large LUT"));
    }

    @Test
    void handle_empty_explanation_consistently() {
      var shapes =
          List.of(
              new LutShape(Function.LINEAR, ""),
              new LutShape(Function.LINEAR, null),
              new LutShape(validLookupTable, ""),
              new LutShape(validLookupTable, null));

      shapes.forEach(
          shape ->
              assertEquals(
                  "",
                  shape.getExplanation(),
                  "Empty explanation should be normalized to empty string"));
    }
  }
}
