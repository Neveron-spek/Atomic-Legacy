"""After a merge, every non-blank line the agents (and main) added relative to base must still be present in main."""
import difflib, sys
from collections import Counter
WT='C:/Users/Admin/Documents/ChatGPT/NeoNTM-wt'
files=sys.argv[2:]; agents=sys.argv[1].split(',')
for f in files:
    main=Counter(l for l in open(f,encoding='utf-8').read().splitlines() if l.strip())
    base=open(f'{WT}/base/{f}',encoding='utf-8').read().splitlines()
    need=Counter()
    for a in agents:
        al=open(f'{WT}/{a}/{f}',encoding='utf-8').read().splitlines()
        for l in difflib.ndiff(base,al):
            if l.startswith('+ ') and l[2:].strip(): need[l[2:]]+=1
    miss=[l for l,c in need.items() if main[l]<c and l.strip() not in ('}',)]
    brace_need=sum(c for l,c in need.items() if l.strip()=='}')
    print(f, 'missing:', len(miss), miss[:3])
