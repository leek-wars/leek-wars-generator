#!/bin/bash
# DIAG : pile du thread fautif et de tous les threads, pour chaque core.
for core in /tmp/core.*; do
	[ -f "$core" ] || { echo "DIAG aucun core dans /tmp"; continue; }
	echo "===== CORE $core ====="
	gdb -batch -nx -ex 'set pagination off' -ex 'info sharedlibrary' -ex 'echo \n=== THREAD FAUTIF ===\n' -ex 'info registers rip rsp' -ex 'bt 60' \
		-ex 'echo \n=== TOUS LES THREADS ===\n' -ex 'thread apply all bt 12' "$DIAG_REAL_JAVA" "$core" 2>&1 | grep -v '^\[New LWP' | head -1500
done
