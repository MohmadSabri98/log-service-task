package org.example.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class ChunkTask implements Callable<ChunkResult> {

    private final String file;
    private final int chunkId;
    private final List<String> lines;

    public ChunkTask(String file, int chunkId, List<String> lines) {
        this.file = file;
        this.chunkId = chunkId;
        this.lines = lines;
    }

    @Override
    public ChunkResult call() throws Exception {

        ProcessBuilder pb = new ProcessBuilder("python3", "python/analyzer.py");
        Process process = pb.start();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = Map.of(
                "file", file,
                "chunk_id", chunkId,
                "lines", lines
        );

        // send input
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(mapper.writeValueAsString(payload));
        }

        // read output
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.readLine();
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Python failed");
        }

        return mapper.readValue(output, ChunkResult.class);
    }
}