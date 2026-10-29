#!/usr/bin/env python3
"""Exercise a running app with its real local model; save a reviewable transcript."""
import argparse
import json
from pathlib import Path
import urllib.error
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--output", default=".local/acceptance.json")
    args = parser.parse_args()

    def request(method, path, payload=None):
        data = None if payload is None else json.dumps(payload).encode()
        req = urllib.request.Request(args.base_url.rstrip("/") + path, data=data,
                                     headers={"Content-Type": "application/json"}, method=method)
        with urllib.request.urlopen(req, timeout=600) as response:
            body = response.read()
            return json.loads(body) if body else None

    chat = request("POST", "/api/chats", {"title": "Local Ollama smoke test"})
    try:
        exchange = request("POST", f"/api/chats/{chat['id']}/messages",
                           {"content": "In one English sentence, what is constructor injection?"})
        if not exchange["chatMessages"][-1]["content"].strip():
            raise RuntimeError("The message endpoint returned no model answer")
        answer = request("POST", "/api/tutor/ask", {"question": "What is constructor injection?"})
        if not answer["sources"]:
            raise RuntimeError("The grounded answer has no retrieved sources")
        plan = request("POST", "/api/tutor/study-plan",
                       {"topic": "Spring Boot dependency injection", "level": "BEGINNER", "days": 2})
        if [stage["agent"] for stage in plan["stages"]] != ["researcher", "tutor", "reviewer"]:
            raise RuntimeError("The workflow did not return all three agents")
        if any(not stage["output"].strip() for stage in plan["stages"]):
            raise RuntimeError("An agent returned an empty result")
        if plan["plan"] != plan["stages"][-1]["output"]:
            raise RuntimeError("The final plan does not contain the reviewer output")
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps({"chat": exchange, "answer": answer, "studyPlan": plan}, indent=2) + "\n")
        print(f"PASS: local chat, grounded Q&A, and all three agents. Transcript: {output}")
    finally:
        request("DELETE", f"/api/chats/{chat['id']}")


if __name__ == "__main__":
    try:
        main()
    except (urllib.error.URLError, RuntimeError, KeyError) as error:
        raise SystemExit(f"Smoke test failed: {error}") from error
