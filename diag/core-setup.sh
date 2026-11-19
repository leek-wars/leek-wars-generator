#!/bin/bash
# DIAG : cores écrits dans /tmp (pas apport), gdb pour les lire. Runner jetable.
if [ -n "$CI" ]; then
	sudo sysctl -w kernel.core_pattern=/tmp/core.%e.%p
	command -v gdb > /dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq gdb > /dev/null; }
fi
cat /proc/sys/kernel/core_pattern
grep -m1 'model name' /proc/cpuinfo
grep -o -m1 -w 'avx512[a-z_]*' /proc/cpuinfo | sort -u | tr '\n' ' '; echo
