package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaBuilder;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wraps the victools schema generator: hands out {@code $ref}s while collecting the definitions they point to.
 */
final class SchemaRegistry {

    private static final String SCHEMAS_PATH = "components/schemas";

    // Maps and enums are inlined where they are used; a separate "Map-1" or "MessageType" definition adds nothing.
    private static final CustomDefinitionProviderV2 INLINE_MAPS_AND_ENUMS = new CustomDefinitionProviderV2() {
        @Override
        public CustomDefinition provideCustomSchemaDefinition(ResolvedType type, SchemaGenerationContext context) {
            if (!type.isInstanceOf(Map.class) && !type.getErasedType().isEnum()) {
                return null;
            }
            ObjectNode standard = context.createStandardDefinition(type, this);
            return new CustomDefinition(standard, CustomDefinition.DefinitionType.INLINE,
                    CustomDefinition.AttributeInclusion.YES);
        }
    };

    private final SchemaBuilder schemas;

    SchemaRegistry(ObjectMapper mapper) {
        this.schemas = new SchemaGenerator(config(mapper)).buildMultipleSchemaDefinitions();
    }

    JsonNode reference(PayloadType type) {
        return schemas.createSchemaReference(type.unwrapOptional().javaType());
    }

    Map<String, JsonNode> definitions() {
        Map<String, JsonNode> sorted = new TreeMap<>();
        for (Map.Entry<String, JsonNode> definition : schemas.collectDefinitions(SCHEMAS_PATH).properties()) {
            sorted.put(definition.getKey(), definition.getValue());
        }
        return sorted;
    }

    private static SchemaGeneratorConfig config(ObjectMapper mapper) {
        SchemaGeneratorConfigBuilder builder = new SchemaGeneratorConfigBuilder(mapper, SchemaVersion.DRAFT_7,
                OptionPreset.PLAIN_JSON)
                .with(new JacksonModule(
                        JacksonOption.RESPECT_JSONPROPERTY_ORDER,
                        JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                        JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE))
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(Option.FLATTENED_OPTIONALS)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .without(Option.EXTRA_OPEN_API_FORMAT_VALUES);
        builder.forTypesInGeneral()
                .withDefinitionNamingStrategy((key, context) ->
                        definitionName(context.getTypeContext().getSimpleTypeDescription(key.getType())))
                .withCustomDefinitionProvider(INLINE_MAPS_AND_ENUMS);
        // The Jackson module only honours @JsonProperty/@JsonNaming. Ask the application's ObjectMapper for the
        // serialized name so a global PropertyNamingStrategy (e.g. SNAKE_CASE) shows up in the schema too.
        builder.forFields().withPropertyNameOverrideResolver(field -> serializedName(mapper, field));
        return builder.build();
    }

    private static String serializedName(final ObjectMapper mapper, final FieldScope field) {
        final var beanType = mapper.constructType(field.getDeclaringType().getErasedType());
        return mapper.getSerializationConfig().introspect(beanType).findProperties().stream()
                .filter(property -> property.getField() != null && property.getField().getAnnotated().equals(field.getRawMember()))
                .map(BeanPropertyDefinition::getName)
                .findFirst()
                .orElse(null);
    }

    private static String definitionName(String simpleTypeDescription) {
        return simpleTypeDescription.replace("[]", "Array").replace(", ", "_").replace("<", "_").replace(">", "");
    }
}
