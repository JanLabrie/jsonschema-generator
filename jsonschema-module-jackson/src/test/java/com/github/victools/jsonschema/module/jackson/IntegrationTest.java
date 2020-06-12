/*
 * Copyright 2020 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;

/**
 * Integration test of this module being used in a real SchemaGenerator instance.
 */
@RunWith(JUnitParamsRunner.class)
public class IntegrationTest {

    public Object[] parametersForGenerateAndCompare() {
        return new Object[]{
                new Object[]{TestBasicAnnotations.class, "integration-test-basic-annotations.json"},
                new Object[]{TestBackReference.class, "integration-test-back-reference.json"},
                new Object[]{TestIdentityReference.class, "integration-test-identity-reference.json"}
        };
    }

    @Test
    @Parameters
    public void generateAndCompare(Class testClass, String schemaFileName) throws Exception {
        JacksonModule module = new JacksonModule(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE, JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY);
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                .with(module)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(testClass);
        String generatedSchema = result.toString();

        String expectedSchema = loadResource(schemaFileName);
        JSONAssert.assertEquals('\n' + generatedSchema + '\n',
                expectedSchema, generatedSchema, JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = IntegrationTest.class
                .getResourceAsStream(resourcePath);
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Test class for testing description, override and enum handling
     */
    @JsonClassDescription("test description")
    static class TestBasicAnnotations {

        @JsonPropertyDescription("field description")
        public String fieldWithDescription;

        @JsonProperty("fieldWithOverriddenName")
        public boolean originalFieldName;

        public TestEnum enumValueHandledByStandardOption;

        public TestEnumWithJsonValueAnnotation enumValueWithJsonValueAnnotation;

        public TestEnumWithJsonPropertyAnnotations enumValueWithJsonPropertyAnnotations;
    }

    enum TestEnum {
        A, B, C
    }

    enum TestEnumWithJsonValueAnnotation {
        ENTRY1, ENTRY2, ENTRY3;

        @JsonValue
        public String getJsonValue() {
            return this.name().toLowerCase();
        }
    }

    enum TestEnumWithJsonPropertyAnnotations {
        @JsonProperty("x_property") X,
        @JsonProperty Y
    }

    /**
     * Test class to test the schema generation of @JsonBackReference fields
     */
    static class TestBackReference {
        public String someField;

        @JsonManagedReference
        public Set<TestBackReference> children;

        @JsonBackReference
        public TestBackReference parent;
    }

    /**
     * Test class to test the schema generation of @JsonIdentityReference fields
     */
    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class TestIdentityReference {

        public String id;

        @JsonIdentityReference(alwaysAsId = true)
        public TestIdentityReference other;
    }

}
