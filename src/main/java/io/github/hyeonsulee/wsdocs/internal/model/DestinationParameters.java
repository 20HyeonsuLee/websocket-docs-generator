package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Parameters of one address, deduplicated by name with the first declaration winning.
 */
public record DestinationParameters(@NonNull List<DestinationParameter> values) {

    public static final DestinationParameters EMPTY = new DestinationParameters(List.of());

    public DestinationParameters {
        Map<String, DestinationParameter> byName = new LinkedHashMap<>();
        for (DestinationParameter variable : values) {
            byName.putIfAbsent(variable.name(), variable);
        }
        values = List.copyOf(byName.values());
    }

    public static DestinationParameters of(Address address) {
        return new DestinationParameters(address.variableNames().stream().map(DestinationParameter::untyped).toList());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<String> names() {
        return values.stream().map(DestinationParameter::name).toList();
    }

    public DestinationParameters union(@NonNull DestinationParameters other) {
        if (other.isEmpty()) {
            return this;
        }
        List<DestinationParameter> merged = new ArrayList<>(values);
        merged.addAll(other.values);
        return new DestinationParameters(merged);
    }

    public DestinationParameters forAddress(Address address) {
        List<String> names = address.variableNames();
        List<DestinationParameter> known = values.stream().filter(v -> names.contains(v.name())).toList();
        return new DestinationParameters(known).union(of(address));
    }
}
