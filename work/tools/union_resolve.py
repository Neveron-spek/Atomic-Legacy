"""Resolve git merge-file conflicts that are pure adjacent insertions by keeping main then agent side."""
import re, sys, os
for m in sys.argv[1:]:
    t = open(m, encoding='utf-8').read()
    assert '|||||||' not in t
    t2 = re.sub(r'^<<<<<<< main\n|^=======\n|^>>>>>>> [a-z]\d\n', '', t, flags=re.M)
    assert '<<<<<<<' not in t2 and '>>>>>>>' not in t2
    dst = m[:-len('.merge')]
    open(dst, 'w', encoding='utf-8', newline='').write(t2); os.remove(m); print('resolved', dst)
