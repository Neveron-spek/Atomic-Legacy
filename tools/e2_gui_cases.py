#!/usr/bin/env python3
"""[parity e2] Builds /legacy/atomiclegacy/parity-e2-gui-slots.json: 1.7.10 container slot tables paired with port menu
layouts (run-building-audit/parity-e2-menus.tsv written by ParityE2Diagnostics).
Pairing: explicit entries of tools/e2_gui_map.json first, then exact layout matches.
Usage: e2_gui_cases.py <orig.json> <menus.tsv> <map.json> <out.json> [report.txt]"""
import json, sys, re

orig = json.load(open(sys.argv[1], encoding='utf-8'))
manual = json.load(open(sys.argv[3], encoding='utf-8'))
port = {}
for line in open(sys.argv[2], encoding='utf-8'):
    parts = line.rstrip('\n').split('\t')
    if len(parts) < 4:
        continue
    slots = [s.split(',') for s in parts[3].split(';') if s]
    port[parts[0]] = {'type': parts[1], 'cls': parts[2], 'slots': [(s[0], int(s[1]), int(s[2]), int(s[3]), int(s[4]), int(s[5]), int(s[6])) for s in slots]}


def osig(slots):
    return ';'.join('%s%d,%d' % ('p' if s['p'] else 'm', s['x'], s['y']) for s in slots)


def psig(slots):
    return ';'.join('%s%d,%d' % ('p' if s[1] else 'm', s[3], s[4]) for s in slots)


bysig = {}
for k, v in port.items():
    bysig.setdefault(psig(v['slots']), []).append(k)

cases, report = [], []
matched_port = set()
for name, c in sorted(orig['containers'].items()):
    if name in manual.get('skip', {}):
        report.append('SKIP\t%s\t%s' % (name, manual['skip'][name]))
        continue
    for n, ctor in enumerate(c['ctors']):
        if not ctor['slots']:
            continue
        label = name if len(c['ctors']) == 1 else '%s#%d' % (name, n)
        if label in manual.get('skip', {}):
            report.append('SKIP\t%s\t%s' % (label, manual['skip'][label]))
            continue
        keys = manual.get('map', {}).get(label)
        how = 'manual'
        if keys is None:
            keys = bysig.get(osig(ctor['slots']))
            how = 'layout'
        if not keys and any(x['c'] == 'SlotDeprecated' for x in ctor['slots']):
            # SlotDeprecated is invisible (func_111238_b false) and rejects items: the port may leave such slots out
            ctor = dict(ctor, slots=[x for x in ctor['slots'] if x['c'] != 'SlotDeprecated'])
            keys = bysig.get(osig(ctor['slots']))
            how = 'layout without SlotDeprecated'
        if not keys:
            report.append('UNMATCHED\t%s\t%d slots' % (label, len(ctor['slots'])))
            continue
        if isinstance(keys, str):
            keys = [keys]
        if how != 'manual' and len(keys) > 1:
            import difflib
            want = re.sub(r'^container(machine)?', '', name.lower())
            keys = sorted(keys, key=lambda k: -difflib.SequenceMatcher(None, want, re.sub(r'^(type|block):|@.*$|_', '', k)).ratio())
        key = keys[0]
        matched_port.update(keys)
        cases.append({'key': key, 'container': label, 'slots': [[s['c'], s['p'], s['i'], s['x'], s['y']] for s in ctor['slots']]})
        p = port.get(key)
        status = 'OK'
        if p is None:
            status = 'NO PORT LAYOUT'
        elif psig(p['slots']) != osig(ctor['slots']):
            status = 'DIFF count %d/%d' % (len(ctor['slots']), len(p['slots']))
            for i, (a, b) in enumerate(zip(ctor['slots'], p['slots'])):
                if (a['p'], a['x'], a['y']) != (b[1], b[3], b[4]):
                    status += ' first@%d orig %s %d,%d port %d,%d' % (i, a['c'], a['x'], a['y'], b[3], b[4])
                    break
        else:
            bad = [i for i, (a, b) in enumerate(zip(ctor['slots'], p['slots'])) if a['c'] in ('SlotTakeOnly', 'SlotCraftingOutput', 'SlotSmelting', 'SlotDeprecated') and b[5]]
            if bad:
                status = 'TAKEONLY accepts items at %s' % bad
            plain = [i for i, (a, b) in enumerate(zip(ctor['slots'], p['slots'])) if a['c'] == 'Slot' and not a['p'] and not b[5]]
            if plain:
                status += ' (plain Slot rejects stone at %s)' % plain
            pat = [i for i, (a, b) in enumerate(zip(ctor['slots'], p['slots'])) if a['c'] == 'SlotPattern' and b[6]]
            if pat:
                status += ' (SlotPattern pickup allowed at %s)' % pat
        report.append('%s\t%s\t%s\t%s\t%s' % (status, label, how, key, ','.join(keys[1:6])))
for k, v in port.items():
    if k not in matched_port and k.startswith('type:'):
        report.append('PORT-ONLY\t%s\t%s\t%d slots' % (k, v['cls'], len(v['slots'])))
json.dump({'source': orig['source'], 'cases': cases}, open(sys.argv[4], 'w', encoding='utf-8'), separators=(',', ':'))
if len(sys.argv) > 5:
    open(sys.argv[5], 'w', encoding='utf-8').write('\n'.join(report) + '\n')
import collections
print(len(cases), 'cases;', collections.Counter(r.split('\t')[0].split(' ')[0] for r in report))
