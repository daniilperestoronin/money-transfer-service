package com.moneytransferservice.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Repository<V> {

    private final Map<UUID, V> mapRepository = new ConcurrentHashMap<>();

    public UUID create(V v) {
        final var uuid = UUID.randomUUID();
        mapRepository.put(uuid, v);
        return uuid;
    }

    public Optional<V> read(UUID uuid) {
        return Optional.of(mapRepository.get(uuid));
    }

    public Optional<List<V>> readAll() {
        return Optional.of(new LinkedList<>(mapRepository.values()));
    }

    public void update(UUID uuid, V v) {
        mapRepository.put(uuid, v);
    }

    public void delete(UUID uuid) {
        mapRepository.remove(uuid);
    }
}
