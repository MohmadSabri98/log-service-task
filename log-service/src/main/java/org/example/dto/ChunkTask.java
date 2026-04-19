package org.example.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ChunkTask implements Callable<ChunkResult> {

    private final String file;
    private final int chunkId;
    private final List<String> lines;
    private final int startLine ;
    private final String taskPath;
    public ChunkTask(String file, int chunkId, List<String> lines,int startLine,String taskPath) {
        this.file = file;
        this.chunkId = chunkId;
        this.lines = lines;
        this.startLine=startLine;
        this.taskPath= taskPath;
    }

    @Override
    public ChunkResult call() throws Exception {

        ProcessBuilder pb = new ProcessBuilder("python", taskPath);
        Process process = pb.start();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = Map.of(
                "file", file,
                "chunk_id", chunkId,
                "lines", lines,
                "start_line",startLine

        );

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(mapper.writeValueAsString(payload));
        }

        ExecutorService single = Executors.newSingleThreadExecutor();

        Future<String> outputFuture = single.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        });

        String output;
        try {
            output = outputFuture.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            process.destroyForcibly();
            single.shutdownNow();
            throw new RuntimeException("Python timeout after 10s");
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Python failed with exit code " + exit);
        }

        single.shutdown();

        return mapper.readValue(output, ChunkResult.class);
    }
}
