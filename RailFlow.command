#!/bin/bash
# Double-click this script in Finder to launch RailFlow without opening terminal manually
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
mvn exec:java
