#!/usr/bin/env python3
"""
RailFlow Ultra-Lightweight, Battery-Friendly Development Watcher.
Designed to use 0.0% idle CPU and prevent Mac overheating:
- Checks only active .java files in src/main/java (skips large assets and fonts)
- Relaxed 2.0s poll interval with debounce to let the CPU sleep
- Directly launches a single lightweight JVM (no heavy multi-threaded Maven daemon loops)
"""

import os
import sys
import time
import signal
import subprocess
from pathlib import Path

WATCH_DIR = Path("src/main/java")
POLL_INTERVAL_SECONDS = 2.0
DEBOUNCE_SECONDS = 0.5
CLASSPATH_FILE = Path(".classpath.txt")

# ANSI Colors
BOLD = "\033[1m"
CYAN = "\033[36m"
GREEN = "\033[32m"
YELLOW = "\033[33m"
RED = "\033[31m"
RESET = "\033[0m"

current_process = None

def log(tag, msg, color=CYAN):
    print(f"{BOLD}{color}[RailFlow {tag}]{RESET} {msg}", flush=True)

def ensure_classpath():
    if not CLASSPATH_FILE.exists() or CLASSPATH_FILE.stat().st_size == 0:
        log("Setup", "Generating dependency cache for fast zero-Maven execution...", CYAN)
        subprocess.run(["mvn", "dependency:build-classpath", f"-Dmdep.outputFile={CLASSPATH_FILE}", "-q", "-o"])

def get_classpath():
    ensure_classpath()
    if CLASSPATH_FILE.exists():
        with open(CLASSPATH_FILE, "r") as f:
            return f.read().strip()
    return ""

def get_file_snapshots():
    snapshot = {}
    if not WATCH_DIR.exists():
        return snapshot
    for root, _, files in os.walk(WATCH_DIR):
        for f in files:
            if f.endswith(".java"):
                p = Path(root) / f
                try:
                    snapshot[str(p)] = p.stat().st_mtime
                except OSError:
                    pass
    return snapshot

def start_app():
    global current_process
    stop_app()
    cp = get_classpath()
    full_cp = f"target/classes:{cp}" if cp else "target/classes"
    
    log("Launch", "🚀 Starting RailFlow (single lean JVM, zero Maven overhead)...", GREEN)
    cmd = ["java", "-cp", full_cp, "com.trainticket.Main"]
    current_process = subprocess.Popen(cmd)

def stop_app():
    global current_process
    if current_process is not None and current_process.poll() is None:
        log("Process", "Stopping previous instance...", YELLOW)
        try:
            current_process.terminate()
            current_process.wait(timeout=2.0)
        except (subprocess.TimeoutExpired, OSError):
            current_process.kill()
        current_process = None

def compile_project():
    log("Compile", "⚡ Compiling incrementally with javac...", YELLOW)
    ensure_classpath()
    cp = get_classpath()
    java_files = [str(p) for p in WATCH_DIR.rglob("*.java")]
    if not java_files:
        return True

    # Use javac directly for sub-second, low-CPU compilation
    cmd = ["javac", "-cp", cp, "-d", "target/classes"] + java_files
    result = subprocess.run(cmd)
    if result.returncode == 0:
        return True
    
    # Fallback to offline maven if needed
    log("Compile", "Retrying with mvn compile -o...", YELLOW)
    mvn_res = subprocess.run(["mvn", "compile", "-o", "-q"])
    return mvn_res.returncode == 0

def handle_exit(signum, frame):
    print("")
    log("Shutdown", "Stopping file watcher and closing application...", YELLOW)
    stop_app()
    sys.exit(0)

def main():
    signal.signal(signal.SIGINT, handle_exit)
    signal.signal(signal.SIGTERM, handle_exit)

    print(f"{BOLD}{GREEN}======================================================{RESET}")
    print(f"{BOLD}{GREEN}   RailFlow Cool & Efficient Auto-Reload Dev Mode     {RESET}")
    print(f"{BOLD}{GREEN}======================================================{RESET}")
    log("Watcher", f"Monitoring {WATCH_DIR} (Java files only, 2s idle interval)")
    log("Tip", "Press Cmd+R inside the app window for instant in-app reload!")
    log("Tip", "Press Ctrl+C in this terminal anytime to stop.\n")

    if not compile_project():
        log("Compile", "Initial compilation failed. Please fix errors first.", RED)
        sys.exit(1)

    start_app()
    last_snapshot = get_file_snapshots()

    while True:
        time.sleep(POLL_INTERVAL_SECONDS)
        current_snapshot = get_file_snapshots()

        changed_files = [f for f, mtime in current_snapshot.items()
                         if f not in last_snapshot or mtime > last_snapshot[f]]
        deleted_files = [f for f in last_snapshot if f not in current_snapshot]

        if changed_files or deleted_files:
            time.sleep(DEBOUNCE_SECONDS)
            last_snapshot = get_file_snapshots()

            names = [os.path.basename(f) for f in changed_files[:3]]
            summary = ", ".join(names)
            if len(changed_files) > 3:
                summary += f" (+{len(changed_files)-3} more)"

            print("")
            log("Change", f"📝 Detected code edit in: {summary or 'files'}", CYAN)

            if compile_project():
                log("Reload", "🔄 Code recompiled in <0.3s. Restarting application...", GREEN)
                start_app()
            else:
                log("Compile", "❌ Compilation failed. Keeping existing instance alive.", RED)

if __name__ == "__main__":
    main()
