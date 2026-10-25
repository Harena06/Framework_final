package framework.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class Session {

    private final Map<String, Object> attributes;

    public Session() {
        this.attributes = new LinkedHashMap<>();
    }

    public Session(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : attributes;
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    public Object remove(String key) {
        return attributes.remove(key);
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }

    public void putAll(Map<String, Object> values) {
        if (values != null) {
            attributes.putAll(values);
        }
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
