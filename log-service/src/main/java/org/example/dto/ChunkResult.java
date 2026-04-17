package org.example.dto;

import java.util.*;

public class ChunkResult {
    public String file;
    public int chunk_id;
    public Map<String, Integer> counts;
    public List<FlaggedLine> flagged;
}