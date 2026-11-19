#!/bin/bash
# DIAG : adresse fautive, carte mémoire autour de la pile du thread fautif, piles des threads.
for core in /tmp/core.*; do
	[ -f "$core" ] || { echo "DIAG aucun core dans /tmp"; continue; }
	echo "===== CORE $core ====="
	gdb -batch -nx -ex 'set pagination off' \
		-ex 'echo \n=== SIGINFO (faute finale, page de la faute initiale) ===\n' -ex 'p $_siginfo.si_code' -ex 'p $_siginfo._sifields._sigfault.si_addr' \
		-ex 'echo \n=== THREAD FAUTIF ===\n' -ex 'info registers rip rsp' -ex 'bt 12' \
		-ex 'echo \n=== FRAME JIT (avant le signal) ===\n' -ex 'frame 6' -ex 'info registers rip rsp rbp' -ex 'x/4i $pc' \
		-ex 'echo \n=== CARTE MEMOIRE ===\n' -ex 'info proc mappings' \
		"$DIAG_REAL_JAVA" "$core" > /tmp/gdb.out 2>&1
	sed -n '/=== SIGINFO/,/=== CARTE MEMOIRE/p' /tmp/gdb.out
	rsp=$(grep -m1 -A3 '=== FRAME JIT' /tmp/gdb.out | awk '/^rsp/ {print $2}')
	echo "=== MAPPINGS autour de rsp=$rsp (2 Mo de part et d'autre) ==="
	python3 - "$rsp" /tmp/gdb.out <<'PY'
import sys, re
rsp = int(sys.argv[1], 16)
for line in open(sys.argv[2]):
    m = re.match(r'\s*0x([0-9a-f]+)\s+0x([0-9a-f]+)\s', line)
    if m:
        lo, hi = int(m.group(1), 16), int(m.group(2), 16)
        if hi > rsp - (2 << 20) and lo < rsp + (2 << 20):
            print(('>>> ' if lo <= rsp < hi else '    ') + line.rstrip())
PY
	echo "=== /proc/sys/vm et THP ==="
	cat /sys/kernel/mm/transparent_hugepage/enabled /proc/sys/vm/max_map_count 2>/dev/null
	uname -r
done
