package generator;

/*
    TODO
    - Extractor 만들기
        - 특정 어노테이션이 붙은 Method 추출
        - 특정 어노테이션이 붙은 Class 추출
    - Channel 만들어 주는 클래스 작성
        - AppChannel, TopicChannel
    - Operation 만들어 주는 클래스 작성
    - info 만들어 주는 클래스 작성
    - Component 만들어 주는 클래스 작성
        - Schema, Messages
    - Channel, Operation, info, component 합쳐주는 클래스 만들기
    - JsonNode 직관적으로 사용할 수 있게 도와주는 클래스 만들기
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import generator.annotaions.JsonSchemaEnumType;
import generator.annotaions.MessageResponse;
import generator.annotaions.Operation;
import generator.config.DocsProperties;
import generator.util.TypeGenerator;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;

public class AsyncApiGenerator {

    private static final String ASYNCAPI_VERSION = "3.0.0";

    private final ObjectMapper mapper = new ObjectMapper();
    private final DocsProperties properties;
    private final Reflections reflections;

    public AsyncApiGenerator(DocsProperties properties) {
        this.properties = properties;
        String basePackage = properties.getBasePackage();
        if (basePackage == null || basePackage.trim().isEmpty()) {
            basePackage = "coffeeshout";
        }
        this.reflections = new Reflections(basePackage, Scanners.MethodsAnnotated);
    }

    public String generateAsyncapiYml() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode channel = mapper.createObjectNode();
        generateAppChannel(channel);
        generateTopicChannel(channel);

        ObjectNode schema = mapper.createObjectNode();
        generateSchema(schema);

        ObjectNode message = mapper.createObjectNode();
        generateMessage(message);

        ObjectNode operation = mapper.createObjectNode();
        generateSendOperation(operation);
        generateTopicOperation(operation);

        root.put("asyncapi", ASYNCAPI_VERSION);
        root.put("info", generateMeta());

        root.put("channels", channel);
        root.put("operations", operation);

        ObjectNode components = mapper.createObjectNode();
        components.put("messages", message);
        components.put("schemas", schema);

        root.put("components", components);

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        return yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    public JsonNode generateTopicOperation(ObjectNode operationNode) {
        /*
            1. MessageMapping을 찾는다.
            2. MessageMapping에 따라서 json정의
            3. Operation이 있으면 summery, description 정의
            4. MessageResponse있으면 reply정의
         */
        final Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageResponse.class);
        for (var method : methods) {
            if (!isTopic(method)) {
                continue;
            }
            ObjectNode body = mapper.createObjectNode();
            Operation operation = method.getAnnotation(Operation.class);
            MessageResponse messageResponse = method.getAnnotation(MessageResponse.class);
            body.put("action", "receive");
            body.put("channel", operationChannelRef(messageResponse.path(), properties.getTopicPath()));
            if (operation != null) {
                body.put("summary", operation.summary());
                body.put("description", operation.description());
            }
            ArrayNode messagesArray = mapper.createArrayNode();
            Type returnType = TypeGenerator.generateType(messageResponse.returnType(), messageResponse.genericType());
            String messageTypeName = getSimpleTypeName(returnType);
            messagesArray.add(messageParameterNode(messageTypeName));
            body.put("messages", messagesArray);
            operationNode.put(messageResponse.path(), body);
        }
        return operationNode;
    }

    private boolean isTopic(Method method) {

        return method.getAnnotation(MessageMapping.class) == null;
    }


    public JsonNode generateSendOperation(ObjectNode operationNode) {
        /*
            1. MessageMapping을 찾는다.
            2. MessageMapping에 따라서 json정의
            3. Operation이 있으면 summery, description 정의
            4. MessageResponse있으면 reply정의
         */
        final Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageMapping.class);
        for (var method : methods) {
            ObjectNode body = mapper.createObjectNode();
            MessageMapping messageMapping = method.getAnnotation(MessageMapping.class);
            Operation operation = method.getAnnotation(Operation.class);
            MessageResponse messageResponse = method.getAnnotation(MessageResponse.class);
            body.put("action", "send");
            body.put("channel", operationChannelRef(messageMapping.value()[0], properties.getAppPath()));
            if (operation != null) {
                body.put("summary", operation.summary());
                body.put("description", operation.description());
            }
            ArrayNode messagesArray = mapper.createArrayNode();
            for (var param : method.getParameters()) {
                if (!isDestinationVariable(param)) {
                    messagesArray.add(messageParameterNode(param.getType().getSimpleName()));
                }
            }
            body.put("messages", messagesArray);
            if (messageResponse != null) {
                ObjectNode reply = mapper.createObjectNode();
                reply.put("channel", operationChannelRef(messageResponse.path(), properties.getTopicPath()));
                ArrayNode responseNodes = mapper.createArrayNode();
                Type returnType = TypeGenerator.generateType(messageResponse.returnType(), messageResponse.genericType());
                String messageTypeName = getSimpleTypeName(returnType);
                responseNodes.add(messageParameterNode(messageTypeName));
                reply.put("messages", responseNodes);
                body.put("reply", reply);
                operationNode.put(messageMapping.value()[0], body);
            }
        }
        return operationNode;
    }

    public JsonNode generateMeta() {
        final ObjectNode metadata = mapper.createObjectNode();
        metadata.put("title", properties.getInfo().getTitle());
        metadata.put("version", properties.getInfo().getVersion());
        metadata.put("description", properties.getInfo().getDescription());
        return metadata;
    }

    public JsonNode generateMessage(ObjectNode messageNode) {
        Set<Type> requests = getRequests();
        Set<Type> responses = getResponses();
        requests.addAll(responses);

        // 기본 클래스들의 메시지 생성
        for (var type : requests) {
            String messageTypeName = getSimpleTypeName(type);
            ObjectNode payloadNode = mapper.createObjectNode();
            
            if (type.toString().startsWith("List_")) {
                // List 타입의 경우 직접 array 스키마 생성
                ObjectNode arraySchema = mapper.createObjectNode();
                String genericName = type.toString().substring(5); // List_ 이후 부분
                arraySchema.put("type", "array");
                arraySchema.put("items", schemaNode(genericName));
                payloadNode.put("payload", arraySchema);
            } else {
                payloadNode.put("payload", schemaNode(getSimpleTypeName(type)));
            }
            messageNode.put(messageTypeName, payloadNode);
        }
        return messageNode;
    }

    public JsonNode generateSchema(ObjectNode schemaNode) {

        // ⚡ victools 설정
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                        .without(Option.DEFINITIONS_FOR_ALL_OBJECTS)   // definitions/ref 없애고 inline
                        .without(Option.EXTRA_OPEN_API_FORMAT_VALUES); // 필요없으면 뺄 수 있음

        // Enum 처리 커스터마이징
        configBuilder.forFields().withEnumResolver(field -> {
            JsonSchemaEnumType annotation = field.getAnnotation(JsonSchemaEnumType.class);
            if (annotation != null) {
                Class<? extends Enum<?>> enumClass = annotation.enumType();
                return Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
            }
            return null;
        });

        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // 커스텀 어노테이션 붙은 클래스만 스캔
        Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageResponse.class);
        for (var method : methods) {
            MessageResponse annotation = method.getAnnotation(MessageResponse.class);
            Class<?> returnType = annotation.returnType();
            Class<?> genericType = annotation.genericType();

            // List가 아닌 returnType만 스키마에 추가
            if (returnType != List.class) {
                JsonNode schema = generator.generateSchema(returnType);
                ((ObjectNode) schemaNode).set(returnType.getSimpleName(), schema);
            }
            
            // 제네릭 타입이 있고 List가 아닌 경우 genericType 스키마 추가
            if (genericType != null && genericType != Void.class && genericType != List.class) {
                JsonNode genericSchema = generator.generateSchema(genericType);
                ((ObjectNode) schemaNode).set(genericType.getSimpleName(), genericSchema);
            }
        }

        Set<Method> messageMappingMethods = reflections.getMethodsAnnotatedWith(MessageMapping.class);
        for (var method : messageMappingMethods) {
            Parameter[] parameters = method.getParameters();
            for (var parameter : parameters) {
                if (isDestinationVariable(parameter)) {
                    continue;
                }
                
                Type paramType = parameter.getParameterizedType();
                
                // List<T> 형태인 경우 T만 스키마에 추가
                if (paramType instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) paramType;
                    if (pType.getRawType().equals(List.class)) {
                        Type actualTypeArg = pType.getActualTypeArguments()[0];
                        if (actualTypeArg instanceof Class) {
                            Class<?> genericClass = (Class<?>) actualTypeArg;
                            JsonNode schema = generator.generateSchema(genericClass);
                            ((ObjectNode) schemaNode).set(genericClass.getSimpleName(), schema);
                        }
                    } else {
                        // 다른 제네릭 타입의 경우 raw type 사용
                        Class<?> rawType = (Class<?>) pType.getRawType();
                        JsonNode schema = generator.generateSchema(rawType);
                        ((ObjectNode) schemaNode).set(rawType.getSimpleName(), schema);
                    }
                } else {
                    // 일반 클래스인 경우
                    Class<?> paramClass = (Class<?>) paramType;
                    JsonNode schema = generator.generateSchema(paramClass);
                    ((ObjectNode) schemaNode).set(paramClass.getSimpleName(), schema);
                }
            }
        }

        return schemaNode;
    }

//    public JsonNode generateSchemaV2(ObjectNode schemaNode) {
//
//        // ⚡ victools 설정
//        SchemaGeneratorConfigBuilder configBuilder =
//                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
//                        .without(Option.DEFINITIONS_FOR_ALL_OBJECTS)   // definitions/ref 없애고 inline
//                        .without(Option.EXTRA_OPEN_API_FORMAT_VALUES); // 필요없으면 뺄 수 있음
//
//        // Enum 처리 커스터마이징
//        configBuilder.forFields().withEnumResolver(field -> {
//            JsonSchemaEnumType annotation = field.getAnnotation(JsonSchemaEnumType.class);
//            if (annotation != null) {
//                Class<? extends Enum<?>> enumClass = annotation.enumType();
//                return Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
//            }
//            return null;
//        });
//
//        SchemaGeneratorConfig config = configBuilder.build();
//        SchemaGenerator generator = new SchemaGenerator(config);
//
//        // 커스텀 어노테이션 붙은 클래스만 스캔
//        Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageResponse.class);
//        for (var method : methods) {
//            MessageResponse annotation = method.getAnnotation(MessageResponse.class);
//            Class<?> clazz = annotation.returnType();
//            JsonNode schema = generator.generateSchema(clazz);
//            ((ObjectNode) schemaNode).set(clazz.getSimpleName(), schema);
//        }
//
//        Set<Method> messageMappingMethods = reflections.getMethodsAnnotatedWith(MessageMapping.class);
//        for (var method : messageMappingMethods) {
//            Parameter[] parameters = method.getParameters();
//            for (var parameter : parameters) {
//                if (isDestinationVariable(parameter)) {
//                    continue;
//                }
//                JsonNode schema = generator.generateSchema(parameter.getType());
//                ((ObjectNode) schemaNode).set(parameter.getType().getSimpleName(), schema);
//            }
//        }
//
//        return schemaNode;
//    }

    private Set<Type> getResponses() {
        final Set<Type> ret = new HashSet<>();
        Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageResponse.class);
        for (var method : methods) {
            MessageResponse annotation = method.getAnnotation(MessageResponse.class);
            ret.add(TypeGenerator.generateType(annotation.returnType(), annotation.genericType()));
        }
        return ret;
    }

    private Set<Type> getRequests() {
        final Set<Type> ret = new HashSet<>();
        final Set<Method> messageMappingMethods = reflections.getMethodsAnnotatedWith(MessageMapping.class);
        for (var method : messageMappingMethods) {
            Parameter[] parameters = method.getParameters();
            for (var parameter : parameters) {
                if (isDestinationVariable(parameter)) {
                    continue;
                }
                // 파라미터의 실제 제네릭 타입 정보 사용
                ret.add(parameter.getParameterizedType());
            }
        }
        return ret;
    }

    public JsonNode generateAppChannel(ObjectNode channel) {

        final Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageMapping.class);
        for (Method method : methods) {
            final MessageMapping annotation = method.getAnnotation(MessageMapping.class);
            final String path = properties.getAppPath() + annotation.value()[0];
            final ObjectNode body = mapper.createObjectNode();
            final ObjectNode messageNode = mapper.createObjectNode();
            final ObjectNode paramNode = mapper.createObjectNode();
            for (var param : method.getParameters()) {
                String paramName = param.getName();
                if (isDestinationVariable(param)) {
                    paramNode.put(paramName, refParameterNode(paramName));
                } else {
                    // 파라미터의 실제 제네릭 타입 정보 추출
                    Type paramType = param.getParameterizedType();
                    String paramTypeName = getSimpleTypeName(paramType);
                    messageNode.put(paramTypeName, messageParameterNode(paramTypeName));
                }
            }
            if (!messageNode.isEmpty()) {
                body.put("messages", messageNode);
            }
            channel.put(path, body);
        }
        return channel;
    }

    public JsonNode generateTopicChannel(ObjectNode channel) {
        final Set<Method> methods = reflections.getMethodsAnnotatedWith(MessageResponse.class);
        for (Method method : methods) {
            final MessageResponse annotation = method.getAnnotation(MessageResponse.class);
            final String path = properties.getTopicPath() + annotation.path();
            final ObjectNode body = mapper.createObjectNode();
            final ObjectNode messageNode = mapper.createObjectNode();
            final ObjectNode paramNode = mapper.createObjectNode();
            for (var paramName : getParams(path)) {
                paramNode.put(paramName, refParameterNode(paramName));
            }
            Type returnType = TypeGenerator.generateType(annotation.returnType(), annotation.genericType());
            String messageTypeName = getSimpleTypeName(returnType);
            messageNode.put(messageTypeName, messageParameterNode(messageTypeName));
            if (!messageNode.isEmpty()) {
                body.put("messages", messageNode);
            }
            channel.put(path, body);
        }
        return channel;
    }

    private ObjectNode refParameterNode(String paramName) {
        ObjectNode refNode = mapper.createObjectNode();
        refNode.put("$ref", "#/components/parameters/" + paramName);
        return refNode;
    }

    private ObjectNode messageParameterNode(String paramName) {
        ObjectNode refNode = mapper.createObjectNode();
        refNode.put("$ref", "#/components/messages/" + paramName);
        return refNode;
    }

    private ObjectNode schemaNode(String paramName) {
        ObjectNode refNode = mapper.createObjectNode();
        refNode.put("$ref", "#/components/schemas/" + paramName);
        return refNode;
    }

    private ObjectNode operationChannelRef(String path, String prefix) {
        ObjectNode refNode = mapper.createObjectNode();
        String concat = prefix + path;
        String parse = concat.replaceAll("/", "~1");
        refNode.put("$ref", "#/channels/" + parse);
        return refNode;
    }

    private boolean isDestinationVariable(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
                .anyMatch(ann -> ann.annotationType() == DestinationVariable.class);
    }

    private List<String> getParams(String path) {
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(path);

        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(matcher.group(1));
        }
        return results;
    }

    /**
     * Type에서 간단한 타입명을 추출합니다.
     * 패키지명 없이 클래스명만 반환하며, JSON 호환성을 위해 <> 를 _로 변경합니다.
     */
    private String getSimpleTypeName(Type type) {
        String typeString = type.toString();
        
        // List<coffeeshout.room.ui.request.ReadyChangeMessage> -> List_ReadyChangeMessage
        if (typeString.contains("<") && typeString.contains(">")) {
            int startIndex = typeString.indexOf('<') + 1;
            int endIndex = typeString.lastIndexOf('>');
            String innerType = typeString.substring(startIndex, endIndex);
            
            // 패키지명 제거
            String simpleInnerType = innerType.contains(".") 
                ? innerType.substring(innerType.lastIndexOf('.') + 1)
                : innerType;
                
            String outerType = typeString.substring(0, typeString.indexOf('<'));
            return outerType + "_" + simpleInnerType;  // <> 대신 _ 사용
        } else {
            // 단순 타입: coffeeshout.room.ui.request.ReadyChangeMessage -> ReadyChangeMessage
            return typeString.contains(".") 
                ? typeString.substring(typeString.lastIndexOf('.') + 1)
                : typeString;
        }
    }
    
    /**
     * MessageResponse 애노테이션에서 메시지 타입명을 생성합니다. 제네릭 타입이 있으면 "ReturnType<GenericType>" 형태로, 없으면 "ReturnType"으로 반환합니다.
     */
    private String getMessageTypeName(MessageResponse annotation) {
        Class<?> returnType = annotation.returnType();
        Class<?> genericType = annotation.genericType();

        if (genericType != null && genericType != Void.class) {
            // 제네릭 타입이 있는 경우: List<User> -> ListUser
            return returnType.getSimpleName() + genericType.getSimpleName();
        } else {
            // 제네릭 타입이 없는 경우
            return returnType.getSimpleName();
        }
    }
}
