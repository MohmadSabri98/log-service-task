package org.example.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
        // Find python path, use just python for Windows by default or fallback
        ProcessBuilder pb = new ProcessBuilder("python", "src/main/resources/analyzer.py");
        Process process = pb.start();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = Map.of(
                "file", file,
                "chunk_id", chunkId,
                "lines", lines
        );

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(mapper.writeValueAsString(payload));
        } catch (IOException e) {
            process.destroyForcibly();
            throw new RuntimeException("Failed to send input to python process: " + e.getMessage(), e);
        }

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.readLine();
        }

        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python process timeout after 10 seconds");
        }

        int exit = process.exitValue();
        if (exit != 0) {
            // capture error stream
            String errorMsg = "";
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorMsg = errReader.readLine();
            }
            throw new RuntimeException("Python failed with exit code " + exit + ". Error: " + errorMsg);
        }

        if (output == null || output.trim().isEmpty()) {
            throw new RuntimeException("Python process returned no output");
        }

        return mapper.readValue(output, ChunkResult.class);
    }
}
