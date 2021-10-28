package net.lintim.model.impl;

import net.lintim.model.OD;
import net.lintim.model.ODPair;

import java.util.*;
import java.util.stream.Collectors;

public class MapOD implements OD {

    private Map<Integer, Map<Integer, Double>> matrix;

    public MapOD() {
        this.matrix = new HashMap<>();
    }

    @Override
    public double getValue(int origin, int destination) {
        return this.matrix.getOrDefault(origin, new HashMap<>()).getOrDefault(destination, 0.);
    }

    @Override
    public void setValue(int origin, int destination, double newValue) {
        this.matrix.putIfAbsent(origin, new HashMap<>());
        this.matrix.get(origin).put(destination, newValue);
    }

    @Override
    public double computeNumberOfPassengers() {
        return matrix.values().stream().map(m -> m.values().stream().mapToDouble(Double::doubleValue).sum())
            .mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public Collection<ODPair> getODPairs() {
        return matrix.entrySet().stream()
            .flatMap(oe -> oe.getValue().entrySet().stream()
                .filter(de -> de.getValue() > 0)
                .map(de -> new ODPair(oe.getKey(), de.getKey(), de.getValue())))
            .collect(Collectors.toList());
    }
}
