package org.example.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ResultAggregator {

    private int totalLines = 0;
    private int totalFlagged = 0;
    private Set<String> scannedFiles = new HashSet<>();

    private Map<String, Integer> ruleCounts = new HashMap<>();
    private Map<String, Integer> fileCounts = new HashMap<>();

    private List<FlaggedLine> allFlagged = new ArrayList<>();

    public void addTotalLines(int lines, String fileName) {
        this.totalLines += lines;
        this.scannedFiles.add(fileName);
    }

    public synchronized void add(ChunkResult result) {
        result.counts.forEach((k, v) ->
                ruleCounts.merge(k, v, Integer::sum));

        totalFlagged += result.flagged.size();

        fileCounts.merge(result.file,
                result.flagged.size(),
                Integer::sum);

        allFlagged.addAll(result.flagged);
    }

    public void writeOutputs() throws Exception {
        Files.createDirectories(Paths.get("output"));

        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> topFiles = fileCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("file", e.getKey());
                    map.put("flagged", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scanned_files", scannedFiles.size());
        report.put("total_lines", totalLines);
        report.put("total_flagged", totalFlagged);
        report.put("counts_by_rule", ruleCounts);
        report.put("top_files", topFiles);

        mapper.writeValue(new File("output/report.json"), report);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("output/flagged.jsonl"))) {

            for (FlaggedLine f : allFlagged) {
                writer.write(mapper.writeValueAsString(f));
                writer.newLine();
            }
        }
    }
}