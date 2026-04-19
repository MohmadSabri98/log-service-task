package org.example;

import org.example.dto.ChunkResult;
import org.example.dto.ChunkTask;
import org.example.dto.ResultAggregator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static final int CHUNK_SIZE = 500;
    private static final int THREADS = 4;

    public static void main(String[] args) throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<ChunkResult>> futures = new ArrayList<>();
        ResultAggregator aggregator = new ResultAggregator();

        Path inputPath = Paths.get("src", "main", "resources", "input");

        if (Files.exists(inputPath)) {
            Files.list(inputPath)
                    .filter(p -> p.toString().endsWith(".log"))
                    .forEach(path -> {
                        try {
                            List<String> lines = Files.readAllLines(path);
                            aggregator.addTotalLines(lines.size(), path.getFileName().toString());

                            int chunkId = 0;
                            for (int i = 0; i < lines.size(); i += CHUNK_SIZE) {
                                List<String> chunk = lines.subList(i, Math.min(i + CHUNK_SIZE, lines.size()));
                                ChunkTask task = new ChunkTask(
                                        path.getFileName().toString(),
                                        chunkId++,
                                        chunk,
                                        i
                                );

                                futures.add((Future<ChunkResult>) pool.submit((Callable<ChunkResult>) task));
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to read file " + path + ": " + e.getMessage());
                        }
                    });
        } else {
            System.err.println("Input directory not found: " + inputPath.toAbsolutePath());
        }

        int completed = 0;
        for (Future<ChunkResult> f : futures) {
            try {
                aggregator.add(f.get());
                completed++;
                if (completed % 10 == 0) {
                    System.out.println("Completed " + completed + " / " + futures.size() + " chunks...");
                }
            } catch (Exception e) {
                System.err.println("Chunk failed: " + e.getMessage());
            }
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        aggregator.writeOutputs();
        System.out.println("Processing finished. Outputs written to output/ directory.");
    }
}