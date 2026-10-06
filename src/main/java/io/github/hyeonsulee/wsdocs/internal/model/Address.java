package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * A STOMP destination such as {@code /topic/room/{roomId}}, always starting with a slash.
 */
public record Address(@NonNull String value) implements Comparable<Address> {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\s*([^/{}:\\s]+)\\s*(?::[^{}]*)?}");

    public Address {
        String v = value.strip();
        value = v.startsWith("/") ? v : "/" + v;
    }

    public List<String> variableNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Matcher matcher = VARIABLE.matcher(value);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return List.copyOf(names);
    }

    public boolean isUnder(DestinationPrefix prefix) {
        return !prefix.isEmpty() && (value.equals(prefix.value()) || value.startsWith(prefix.value() + "/"));
    }

    @Override
    public int compareTo(Address other) {
        return value.compareTo(other.value);
    }
}
