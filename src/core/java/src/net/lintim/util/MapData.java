package net.lintim.util;

import net.lintim.exception.MapDataTypeMismatchException;

/**
 * Base implementation for data stored in a map, used in the {@link Statistic} and the {@link Config} implementation.
 */
public interface MapData {
    /**
     * Get the string value for the given key. For specialised getters, see {@link #getBooleanValue(String)},
     * {@link #getIntegerValue(String)}, {@link #getDoubleValue(String)} and {@link #getLongValue(String)}. There may
     * be additional getters in the chosen implementation.
     * @param key the key to query
     * @return the value associated with the key. Will throw, if the key is not available. The exception may depend on
     * the implementation
     */
    String getStringValue(String key);

    /**
     * Get the integer value associated with the given key
     * @param key the key to query
     * @return the value associated with the key. Will throw, if the key is not available or cannot be parsed. The
     * exception may depend on the implementation
     */
    default int getIntegerValue(String key) {
        String value = getStringValue(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new MapDataTypeMismatchException(key, "int", value);
        }
    }
    /**
     * Get the double value associated with the given key
     * @param key the key to query
     * @return the value associated with the key. Will throw, if the key is not available or cannot be parsed. The
     * exception may depend on the implementation
     */
    default double getDoubleValue(String key) {
        String value = getStringValue(key);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new MapDataTypeMismatchException(key, "double", value);
        }
    }
    /**
     * Get the boolean value associated with the given key
     * @param key the key to query
     * @return the value associated with the key. Will throw, if the key is not available or cannot be parsed. The
     * exception may depend on the implementation
     */
    default boolean getBooleanValue(String key) {
        String value = getStringValue(key);
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            throw new MapDataTypeMismatchException(key, "boolean", value);
        }
    }
    /**
     * Get the long value associated with the given key
     * @param key the key to query
     * @return the value associated with the key. Will throw, if the key is not available or cannot be parsed. The
     * exception may depend on the implementation
     */
    default long getLongValue(String key) {
        String value = getStringValue(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new MapDataTypeMismatchException(key, "long", value);
        }
    }

    /**
     * Put the given value in the data structure, associated with the given key. This is the most generic setter, you
     * may use specific setters, e.g., {@link #put(String, int)}, {@link #put(String, long)},
     * {@link #put(String, double)} or {@link #put(String, boolean)}. There may be additional setters, depending on
     * your chosen implementation
     * @param key the key to add a value to
     * @param value the value to put in the map
     */
    void put(String key, String value);
    default void put(String key, int value) {
        put(key, String.valueOf(value));
    }
    default void put(String key, double value) {
        put(key, String.valueOf(value));
    }
    default void put(String key, boolean value) {
        put(key, String.valueOf(value));
    }
    default void put(String key, long value) {
        put(key, String.valueOf(value));
    }


}
