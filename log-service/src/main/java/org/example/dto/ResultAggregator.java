package org.example.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class ResultAggregator {

    private int totalLines = 0;
    private int totalFlagged = 0;

    private Map<String, Integer> ruleCounts = new HashMap<>();
    private Map<String, Integer> fileCounts = new HashMap<>();

    private List<FlaggedLine> allFlagged = new ArrayList<>();

    public void add(ChunkResult result) {

        result.counts.forEach((k, v) ->
                ruleCounts.merge(k, v, Integer::sum));

        totalFlagged += result.flagged.size();

        fileCounts.merge(result.file,
                result.flagged.size(),
                Integer::sum);

        allFlagged.addAll(result.flagged);
    }

    public void writeOutputs() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> report = new HashMap<>();
        report.put("total_flagged", totalFlagged);
        report.put("counts_by_rule", ruleCounts);

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