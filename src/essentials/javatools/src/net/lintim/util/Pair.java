package net.lintim.util;

/**
 * As the name says, a pair of two objects that have the same type.
 *
 * @param <T> The type of the two objects.
 */
public class Pair<T> {
    public T first;
    public T second;

    public Pair(T first, T second){
        this.first = first;
        this.second = second;
    }
}
