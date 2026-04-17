package org.example;

import org.example.dto.ChunkResult;
import org.example.dto.ChunkTask;
import org.example.dto.ResultAggregator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static final int CHUNK_SIZE = 3;
    private static final int THREADS = 4;

    public static void main(String[] args) throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<ChunkResult>> futures = new ArrayList<>();

        Files.list(Paths.get("input"))
                .filter(p -> p.toString().endsWith(".log"))
                .forEach(path -> {
                    try {
                        List<String> lines = Files.readAllLines(path);
                        int chunkId = 0;

                        for (int i = 0; i < lines.size(); i += CHUNK_SIZE) {
                            List<String> chunk = lines.subList(i, Math.min(i + CHUNK_SIZE, lines.size()));

                            ChunkTask task = new ChunkTask(
                                    path.getFileName().toString(),
                                    chunkId++,
                                    chunk
                            );

                            futures.add((Future<ChunkResult>) pool.submit((Runnable) task));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        ResultAggregator aggregator = new ResultAggregator();

        for (Future<ChunkResult> f : futures) {
            try {
                aggregator.add(f.get());
            } catch (Exception e) {
                System.err.println("Chunk failed: " + e.getMessage());
            }
        }

        pool.shutdown();

        aggregator.writeOutputs();
    }
}