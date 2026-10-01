#!/bin/bash
# ==============================================================================
# RailFlow macOS Launcher & Database Management Utility
# Double-click in Finder to launch RailFlow, or run with 'seed' argument:
#   ./RailFlow.command seed
# ==============================================================================

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "================================================================================"
echo "  🚆 RAILFLOW • Modern Train Ticket Management System"
echo "================================================================================"

# Ensure clean Java 17 bytecode regardless of IDE compiler settings.
# Without this, IntelliJ / VS Code may have written Java 21 class files that
# the exec plugin's embedded JVM (Java 17) cannot load.
echo "⚙️  Compiling with Java 17 target..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed. Aborting."
    exit 1
fi

# If 'seed' argument passed directly from command line:
if [ "$1" == "seed" ] || [ "$1" == "--seed" ] || [ "$1" == "db:seed" ]; then
    echo "🌱 Seeding RailFlow Master Database (Stations, Trains, Routes, Users)..."
    mvn exec:java -Dexec.args="seed"
    exit $?
fi

# Interactive menu with 4-second auto-default to Launch RailFlow
echo "Select execution mode:"
echo "  [1] Launch RailFlow Desktop Application (Default)"
echo "  [2] Seed / Re-initialize Database (Stations, Trains, Routes & Admin)"
echo "--------------------------------------------------------------------------------"

choice="1"
if read -t 4 -p "Enter choice [1 or 2, auto-launches [1] in 4s]: " user_choice; then
    if [ -n "$user_choice" ]; then
        choice="$user_choice"
    fi
else
    echo ""
    echo "⏱ Auto-launching RailFlow Desktop Application..."
fi

if [ "$choice" == "2" ] || [ "$choice" == "seed" ]; then
    echo ""
    echo "🌱 Running RailFlow Database Seeder..."
    mvn exec:java -Dexec.args="seed"
else
    echo ""
    echo "🚀 Starting RailFlow Application..."
    mvn exec:java
fi
