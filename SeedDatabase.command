#!/bin/bash
# ==============================================================================
# RailFlow Database Seeder & Schema Initializer
# Double-click this script in Finder to seed/reseed the train database
# ==============================================================================

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "================================================================================"
echo "  🌱 RAILFLOW • Database Seeder & Master Data Initializer"
echo "================================================================================"
echo "This will create tables and seed stations, trains, routes, and admin user."
echo ""

mvn exec:java -Dexec.args="seed"

echo ""
echo "Press any key to close this terminal window..."
read -n 1 -s
