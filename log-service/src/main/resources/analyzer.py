import sys
import json
import re

RULES = {
    "CMD_CHAINING": re.compile(r"(&&|\|\||;|`|(?<!\w)&(?!\w))"),
    "TRAVERSAL": re.compile(r"(\.\./|%2e%2e%2f)", re.IGNORECASE),
    "SQLI": re.compile(r"(or\s+1=1|union\s+select|sleep\s*\()", re.IGNORECASE),
    "SECRETS": re.compile(r"(Authorization:\s*Bearer|api_key=|secret=)", re.IGNORECASE),
    "PII": re.compile(r"\b[\w.-]+@[\w.-]+\.\w+\b")
}

def analyze(lines, file_name, chunk_id, start_line):
    counts = {k: 0 for k in RULES}
    flagged = []

    for idx, line in enumerate(lines):
        absolute_line = start_line + idx   
        for rule, pattern in RULES.items():
            if pattern.search(line):
                counts[rule] += 1
                flagged.append({
                    "file": file_name,
                    "line": absolute_line,
                    "rule": rule,
                    "excerpt": line.strip()
                })

    return {
        "file": file_name,
        "chunk_id": chunk_id,
        "counts": counts,
        "flagged": flagged
    }


if __name__ == "__main__":
    try:
        raw = sys.stdin.readline()   

        payload = json.loads(raw)

        result = analyze(
            payload["lines"],
            payload["file"],
            payload["chunk_id"],
            payload["start_line"]
        )

        print(json.dumps(result), flush=True)

    except Exception as e:
        print(json.dumps({"error": str(e)}), flush=True)
        sys.exit(1)