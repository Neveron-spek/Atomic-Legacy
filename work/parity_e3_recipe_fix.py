"""e3: plan/apply ingredient rewrites so port crafting recipes use the ingredient set of the 1.7.10 recipe."""
import sys, json, re
from collections import Counter, defaultdict
apply = '--apply' in sys.argv
sys.argv_orig = list(sys.argv)
sys.argv = ['x', 'none']
import parity_e3_recipes as m
REG = {v[2:] for v in m.VANILLA.values() if v.startswith('I:')}
for _p in m.RECIPES.rglob('*.json'):
    _t = _p.read_text(encoding='utf-8-sig')
    for _mm in re.finditer(r'\{\s*"(?:item|id)":\s*"(reinhardtshbm:[a-z0-9_]+)"\s*(?:,\s*"count":\s*\d+\s*)?\}', _t): REG.add(_mm.group(1))
REG |= {'reinhardtshbm:' + k.split('.', 2)[2] for k in m.LANG if k.startswith(('item.reinhardtshbm.', 'block.reinhardtshbm.')) and k.count('.') == 2}
for _c in ('item_catalog.txt', 'block_catalog.txt'):
    REG |= {'reinhardtshbm:' + x.strip() for x in (m.ROOT / 'src/main/resources/legacy/reinhardtshbm' / _c).read_text(encoding='utf-8').split() if re.fullmatch(r'[a-z0-9_]+', x.strip())}
VARJSON = {}
def _walk(o):
    if isinstance(o, dict):
        if 'components' in o and ('items' in o or 'item' in o):
            VARJSON.setdefault(m.ing_token(o), o)
        for v in o.values(): _walk(v)
    elif isinstance(o, list):
        for v in o: _walk(v)
for _p in m.RECIPES.glob('*.json'):
    try: _j = json.loads(_p.read_text(encoding='utf-8-sig'))
    except Exception: continue
    if 'crafting' in _j.get('type', ''): _walk(_j.get('key', {})); _walk(_j.get('ingredients', []))
SKIP_CUR = ('T:ae2:', 'I:reinhardtshbm:plate_cast_cdalloy', 'I:reinhardtshbm:falloutitem', 'I:reinhardtshbm:sand_mix_boron', 'A:[I:reinhardtshbm:bdcl', 'T:c:ores/', 'R:{"base"', 'A:[I:minecraft:allium')
def tag_for(key):
    so = m.ODICT.get(key)
    cands = []
    if so:
        for tag in m.TAGS:
            if m.pmembers(tag) == so: cands.append(tag)
        ck = m.canon_key(key)
        for c in cands:
            if m.canon_tag(c) == ck: return c
        if cands: return sorted(cands, key=len)[0]
    al = m.ORE_ALIAS.get(key)
    if al:
        return al[0] if not al[0].startswith('minecraft:') or al[0] in ('minecraft:planks', 'minecraft:wooden_slabs', 'minecraft:logs', 'minecraft:saplings', 'minecraft:leaves', 'minecraft:wool') else 'ITEM:' + al[0]
    return None
def port_ing(tok):
    if tok.startswith('O:container'):
        mm = re.fullmatch(r'O:container(\d+)(\w+)', tok)
        return {'type': 'reinhardtshbm:legacy_fluid_container', 'fluid': mm.group(2), 'amount': int(mm.group(1))}
    if tok.startswith('O:wild'):
        return [{'item': x} for x in sorted(m.ODICT[tok[2:]])]
    if tok.startswith('O:'):
        t = tag_for(tok[2:])
        if t is None:
            so = m.ODICT.get(tok[2:])
            if so and len(so) == 1 and next(iter(so)) in REG: return {'item': next(iter(so))}
            return None
        return {'item': t[5:]} if t.startswith('ITEM:') else {'tag': t}
    if tok.startswith('T:'):
        return {'tag': tok[2:]}
    if tok.startswith('I:'):
        if tok in VARJSON: return VARJSON[tok]
        if (tok[2:] in REG or tok.startswith('I:minecraft:')) and not re.search(r'[#?~*]', tok): return {'item': tok[2:]}
        if tok.startswith('I:reinhardtshbm:battery_pack_'):
            v = tok[len('I:reinhardtshbm:battery_pack_'):]
            order = ['battery_redstone', 'battery_lead', 'battery_lithium', 'battery_sodium', 'battery_schrabidium', 'battery_quantum']
            return {'type': 'reinhardtshbm:battery', 'items': ['reinhardtshbm:battery_pack'], 'components': {'minecraft:custom_data': {'variant': v}, 'minecraft:custom_model_data': order.index(v)}}
        mm = re.fullmatch(r'I:reinhardtshbm:(fluid_tank_full|fluid_barrel_full|canister_full)_(\w+)', tok)
        if mm: return {'type': 'neoforge:components', 'items': ['reinhardtshbm:' + mm.group(1)], 'components': {'minecraft:custom_data': {'fluid': mm.group(2)}}}
        return None
    return None
plans = []; skipped = []
pairs = [(o, p, 'narrowed') for o, p in m.result['narrowed']] + [(x[0], x[1], 'changed') for x in m.result['changed']]
for o, p, why in pairs:
    if o['kind'] == 'smelt' or o['out'] != p['out'] or o['count'] != p['count'] or o['kind'] != p['kind']:
        skipped.append((o, p, 'out/count/kind')); continue
    f = m.RECIPES / p['file']
    j = json.loads(f.read_text(encoding='utf-8-sig'))
    changes = []
    ok = True
    if o['kind'] == 'shaped':
        op, pp = o['pattern'], p['pattern']
        if not (len(op) == len(pp) and all(len(a) == len(b) for a, b in zip(op, pp))):
            op = m.mirror(op)
        if not (len(op) == len(pp) and all(len(a) == len(b) for a, b in zip(op, pp))) or any(bool(s) != bool(t) for a, b in zip(op, pp) for s, t in zip(a, b)):
            skipped.append((o, p, 'shape')); continue
        keymap = {}
        for ra, rb, rowtxt in zip(op, pp, j['pattern']):
            for s, t, ch in zip(ra, rb, rowtxt):
                if not s: continue
                if keymap.setdefault(ch, s) != s: ok = False
        if not ok:
            skipped.append((o, p, 'keyconflict')); continue
        for ch, s in keymap.items():
            cur = m.ing_token(j['key'][ch])
            if m.tok_eq(s, cur) == 2: continue
            if cur.startswith(SKIP_CUR) or cur.startswith('R:{"base"'): continue
            ni = port_ing(s)
            if ni is None: ok = False; changes.append((s, cur, None)); continue
            changes.append((s, cur, (ch, ni)))
    else:
        a = Counter(o['ings']); b = Counter(p['ings'])
        for t in list(a):
            for u in list(b):
                if a[t] > 0 and b[u] > 0 and m.tok_eq(t, u) == 2:
                    k = min(a[t], b[u]); a[t] -= k; b[u] -= k
        a = +a; b = +b
        if len(a) != len(b) or sorted(a.values()) != sorted(b.values()) or len(set(a.values())) != len(a) and len(a) > 1:
            skipped.append((o, p, 'shapeless-ambiguous %s | %s' % (dict(a), dict(b)))); continue
        bycount = {n: t for t, n in b.items()}
        for s, n in a.items():
            cur = bycount[n]
            if cur.startswith(SKIP_CUR): continue
            ni = port_ing(s)
            if ni is None: ok = False; changes.append((s, cur, None)); continue
            changes.append((s, cur, (cur, ni)))
    if not changes: continue
    if not ok:
        skipped.append((o, p, 'noingredient ' + '; '.join('%s<-%s' % (c[0], c[1]) for c in changes if c[2] is None))); continue
    plans.append((o, p, why, changes, j, f))
agg = defaultdict(list)
for o, p, why, changes, j, f in plans:
    for s, cur, how in changes:
        agg[(s, cur, json.dumps(how[1], sort_keys=True))].append(p['file'])
if not apply and '--gen' not in sys.argv_orig and '--gen-write' not in sys.argv_orig:
    for (s, cur, ni), files in sorted(agg.items(), key=lambda x: -len(x[1])):
        print(len(files), s, '<=', cur, '::', ni, '|', ','.join(x[:-5] for x in files[:4]))
    print('--notes')
    for k in sorted(m.EQ_NOTES):
        if k.startswith('tagset'): print(k)
    print('--skipped', len(skipped))
    for o, p, why in skipped:
        print('SKIP', p['file'], '|', why[:230])
elif apply:
    n = 0
    for o, p, why, changes, j, f in plans:
        if o['kind'] == 'shaped':
            for s, cur, (ch, ni) in changes: j['key'][ch] = ni
        else:
            for s, cur, (curtok, ni) in changes:
                j['ingredients'] = [ni if m.ing_token(x) == curtok else x for x in j['ingredients']]
        f.write_text(json.dumps(j, indent=2, ensure_ascii=False) + '\n', encoding='utf-8'); n += 1
    print('rewritten', n)

# ---------- generation of missing recipes ----------
def gen_missing(do_write):
    RES = {}
    for _p in m.RECIPES.glob('*.json'):
        try: _j = json.loads(_p.read_text(encoding='utf-8-sig'))
        except Exception: continue
        if 'crafting' in _j.get('type', '') or _j.get('type') == 'minecraft:smelting':
            r = _j.get('result')
            if isinstance(r, dict) and r.get('components'):
                RES.setdefault('I:' + r['id'] + m.comp_suffix(r['id'], r['components']), {k: v for k, v in r.items() if k != 'count'})
    made, failed = [], []
    for r in m.result['missing']:
        fl = set(r['flags'])
        if fl & {'lbsm', 'expensive', 'else', 'loop', 'compat', 'baby'}:
            failed.append((r, 'flags ' + ','.join(sorted(fl)))); continue
        out = r['out']
        if re.search(r'depth_|hev_battery|:fallout|crate_desh|crate_tungsten|:safe$|oc_cable_paintable|minecraft:copper_', out) or any('stone_depth' in t for t in r['ings']):
            failed.append((r, 'skip-list')); continue
        if out in RES: res = dict(RES[out])
        elif out.startswith('I:') and (out[2:] in REG or out.startswith('I:minecraft:')) and not re.search(r'[#?~]', out): res = {'id': out[2:]}
        else:
            failed.append((r, 'output ' + out)); continue
        res['count'] = int(r['count'])
        j = {}
        if '528' in fl: j['neoforge:conditions'] = [{'type': 'reinhardtshbm:enable_528_mode'}]
        if 'not528' in fl: j['neoforge:conditions'] = [{'type': 'neoforge:not', 'value': {'type': 'reinhardtshbm:enable_528_mode'}}]
        bad = None
        if r['kind'] == 'smelt':
            tok = next(iter(r['ings']))
            ing = port_ing(tok)
            if ing is None: failed.append((r, 'ingredient ' + tok)); continue
            j.update({'type': 'minecraft:smelting', 'category': 'misc', 'ingredient': ing, 'result': res, 'experience': float(r['xp']), 'cookingtime': 200})
        elif r['kind'] == 'shaped':
            letters = {}; key = {}; rows = []
            for row in r['pattern']:
                s = ''
                for t in row:
                    if not t: s += ' '; continue
                    if t not in letters:
                        ing = port_ing(t)
                        if ing is None: bad = t; break
                        letters[t] = 'ABCDEFGHIJ'[len(letters)]; key[letters[t]] = ing
                    s += letters[t]
                if bad: break
                rows.append(s)
            if bad: failed.append((r, 'ingredient ' + bad)); continue
            j.update({'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': rows, 'key': key, 'result': res})
        else:
            ings = []
            for t, n in r['ings'].items():
                ing = port_ing(t)
                if ing is None: bad = t; break
                ings += [ing] * n
            if bad: failed.append((r, 'ingredient ' + bad)); continue
            j.update({'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ings, 'result': res})
        name = '%s_%s_%s' % (re.sub(r'[^a-z0-9_]', '_', out.split(':')[-1]), r['file'][:-5].lower().replace('recipes', '').replace('manager', ''), r['line'])
        if r['kind'] == 'smelt': name = 'smelting_' + name
        f = m.RECIPES / (name + '.json'); k = 2
        while f.exists() or any(x[0] == f for x in made):
            f = m.RECIPES / ('%s_%d.json' % (name, k)); k += 1
        made.append((f, j, r))
    if do_write:
        for f, j, r in made: f.write_text(json.dumps(j, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('generated', len(made), 'failed', len(failed))
    for r, why in failed: print('NOGEN', m.fmt(r).replace('I:reinhardtshbm:', ':')[:150], '{%s:%s}' % (r['file'], r['line']), '|', why)
    return made
if '--gen' in sys.argv_orig or '--gen-write' in sys.argv_orig:
    gen_missing('--gen-write' in sys.argv_orig)
