"""e3: compare 1.7.10 workbench/furnace recipes (CraftingManager + crafting/*) with the port's JSON recipes.
Usage: python parity_e3_recipes.py [--json out.json]
Prints summary + diff lists. Static source parse with helper-method macro expansion."""
import json, re, sys, os
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ORIG = Path('C:/Users/Admin/Documents/ChatGPT/NeoNTM/work/original/src/main/java/com/hbm')
RECIPES = ROOT / 'src/main/resources/data/reinhardtshbm/recipe'
LANG = json.loads((ROOT / 'src/main/resources/assets/reinhardtshbm/lang/en_us.json').read_text(encoding='utf-8'))
PORT_IDS = {k.split('.', 2)[2] for k in LANG if k.startswith(('item.reinhardtshbm.', 'block.reinhardtshbm.')) and k.count('.') == 2}
PORT_IDS |= {p.stem for p in (ROOT / 'src/main/resources/assets/reinhardtshbm/models/item').glob('*.json')}
PORT_IDS |= {p.stem for p in (ROOT / 'src/main/resources/assets/reinhardtshbm/blockstates').glob('*.json')}
sys.path.insert(0, str(Path(__file__).parent))
try:
    from parity_e3_recipe_alias import ITEM_ALIAS, VANILLA, MAT_ALIAS, ORE_ALIAS, IGNORE_OUT, VARIANTS
except ImportError:
    ITEM_ALIAS, VANILLA, MAT_ALIAS, ORE_ALIAS, IGNORE_OUT = {}, {}, {}, {}, set()
try:
    import parity_f1_recipe_extra as F1X
    ITEM_ALIAS.update(F1X.ALIAS)
except ImportError:
    F1X = None

def strip_comments(s):
    out, i, n = [], 0, len(s)
    while i < n:
        c = s[i]
        if c == '"':
            j = i + 1
            while s[j] != '"':
                j += 2 if s[j] == '\\' else 1
            out.append(s[i:j + 1]); i = j + 1
        elif c == "'":
            j = i + 1
            while s[j] != "'":
                j += 2 if s[j] == '\\' else 1
            out.append(s[i:j + 1]); i = j + 1
        elif s.startswith('//', i):
            j = s.find('\n', i); j = n if j < 0 else j; i = j
        elif s.startswith('/*', i):
            j = s.find('*/', i); seg = s[i:j + 2]; out.append('\n' * seg.count('\n')); i = j + 2
        else:
            out.append(c); i += 1
    return ''.join(out)

def balanced(s, i, open_='(', close=')'):
    """s[i] == open_; return index of matching close."""
    depth, n = 0, len(s)
    while i < n:
        c = s[i]
        if c == '"' or c == "'":
            q = c; i += 1
            while s[i] != q:
                i += 2 if s[i] == '\\' else 1
        elif c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError('unbalanced')

def split_args(text):
    res, start, depth, i, n = [], 0, 0, 0, len(text)
    while i < n:
        c = text[i]
        if c == '"' or c == "'":
            q = c; i += 1
            while text[i] != q:
                i += 2 if text[i] == '\\' else 1
        elif c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
        elif c == ',' and depth == 0:
            res.append(text[start:i].strip()); start = i + 1
        i += 1
    if text[start:].strip():
        res.append(text[start:].strip())
    return res

# ---------- oredict frames ----------
odm = strip_comments((ORIG / 'inventory/OreDictManager.java').read_text(encoding='utf-8'))
FRAMES = {}
for m in re.finditer(r'Dict(?:Frame|Group)\s+(\w+)\s*=\s*new\s+Dict(?:Frame|Group)\((?:Compat[^?]*\?\s*"[^"]*"\s*:\s*)?"([^"]+)"', odm):
    FRAMES[m.group(1)] = m.group(2)
KEYS = dict(re.findall(r'String\s+(KEY_\w+)\s*=\s*"([^"]+)"', odm))
PREFIX = {'any': 'any', 'nugget': 'nugget', 'tiny': 'tiny', 'bolt': 'bolt', 'ingot': 'ingot', 'dustTiny': 'dustTiny', 'dust': 'dust', 'gem': 'gem',
          'crystal': 'crystal', 'plate': 'plate', 'plateCast': 'plateTriple', 'plateWelded': 'plateSextuple', 'heavyComp': 'plateSextuple',
          'wireFine': 'wireFine', 'wireDense': 'wireDense', 'shell': 'shell', 'pipe': 'ntmpipe', 'billet': 'billet', 'block': 'block', 'ore': 'ore',
          'fragment': 'bedrockorefragment', 'lightBarrel': 'barrelLight', 'heavyBarrel': 'barrelHeavy', 'lightReceiver': 'receiverLight',
          'heavyReceiver': 'receiverHeavy', 'mechanism': 'gunMechanism', 'stock': 'stock', 'grip': 'grip'}
TAGDIR = {'ingots': 'ingot', 'dusts': 'dust', 'plates': 'plate', 'nuggets': 'nugget', 'storage_blocks': 'block', 'gems': 'gem', 'crystals': 'crystal',
          'wires': 'wireFine', 'wires_dense': 'wireDense', 'bolts': 'bolt', 'pipes': 'ntmpipe', 'billets': 'billet', 'tiny_dusts': 'dustTiny', 'ores': 'ore',
          'dyes': 'dye', 'rods': 'stick', 'glass_blocks': 'blockGlass'}
GUN = {'barrel_heavy': 'barrelHeavy', 'barrel_light': 'barrelLight', 'grip': 'grip', 'mechanism': 'gunMechanism', 'receiver_heavy': 'receiverHeavy',
       'receiver_light': 'receiverLight', 'stock': 'stock'}

def canon_mat(s):
    s = re.sub(r'[^a-z0-9]', '', s.lower())
    return MAT_ALIAS.get(s, s)

ALLPRE = sorted(set(PREFIX.values()) | {'dye', 'stick', 'blockGlass', 'paneGlass', 'tiny'}, key=len, reverse=True)
def canon_key(key):
    for p in ALLPRE:
        if key.startswith(p) and len(key) > len(p) and key[len(p)].isupper():
            return '%s|%s' % (p, canon_mat(key[len(p):]))
    return key

ALTMAT = {}
for _m in re.finditer(r'DictFrame\s+\w+\s*=\s*new\s+DictFrame\(([^)]*)\)', odm):
    _names = re.findall(r'"([^"]+)"', _m.group(1))
    for _n in _names[1:]: ALTMAT.setdefault(_n, _names[0])
def ore_token(key):
    if key.startswith('tiny') and key[4:5].isupper(): key = 'nugget' + key[4:]
    for p in ALLPRE:
        if key.startswith(p) and key[len(p):] in ALTMAT:
            key = p + ALTMAT[key[len(p):]]; break
    return 'O:' + key

def canon_tag(tag):
    ns, path = tag.split(':', 1)
    parts = path.split('/')
    if ns == 'c' and len(parts) == 2 and parts[0] in TAGDIR:
        pre, name = TAGDIR[parts[0]], parts[1]
        if pre == 'plate' and name.startswith('cast_'):
            pre, name = 'plateTriple', name[5:]
        elif pre == 'plate' and name.startswith('welded_'):
            pre, name = 'plateSextuple', name[7:]
        elif pre == 'plate' and name.endswith('_cast'):
            pre, name = 'plateTriple', name[:-5]
        return '%s|%s' % (pre, canon_mat(name))
    if ns == 'c' and len(parts) == 3 and parts[0] == 'gun_parts':
        return '%s|%s' % (GUN[parts[1]], canon_mat(parts[2]))
    return tag

def tag_token(tag):
    return 'T:' + tag

# ---------- original expression evaluation ----------
def port_item(name, variant=None):
    """1.7.10 field name (+variant) -> port id token"""
    key = name if variant is None else '%s#%s' % (name, variant)
    def ns(x): return 'I:' + (x if ':' in x else 'reinhardtshbm:' + x)
    if key in ITEM_ALIAS:
        return ns(ITEM_ALIAS[key])
    if variant is None or variant == '0' and name in PORT_IDS and (name + '_0') not in PORT_IDS:
        return ns(ITEM_ALIAS.get(name, name))
    if variant == '*':
        return 'I:reinhardtshbm:%s#*' % name
    v = variant.lower()
    base = ITEM_ALIAS.get(name, name)
    for vv in VARIANTS.get(v, [v]) + [v]:
        cand = '%s_%s' % (base, vv)
        if cand in PORT_IDS:
            return 'I:reinhardtshbm:' + cand
    if re.fullmatch(r'\d+', v):
        return 'I:reinhardtshbm:%s#%s' % (base, v)
    return 'I:reinhardtshbm:%s_%s' % (base, VARIANTS.get(v, [v])[0])

def ev_item(expr):
    """-> (kind, name) where kind in mod/van/None"""
    expr = expr.strip()
    m = re.fullmatch(r'Item\.getItemFromBlock\((.*)\)', expr)
    if m:
        expr = m.group(1).strip()
    m = re.fullmatch(r'(ModItems|ModBlocks|Items|Blocks)\.(\w+)', expr)
    if m and m.group(1) == 'ModBlocks' and F1X and m.group(2) in F1X.BLOCK_ALIAS:
        return ('mod', F1X.BLOCK_ALIAS[m.group(2)])
    if m:
        return ('mod' if m.group(1).startswith('Mod') else 'van' + m.group(1)[0], m.group(2))
    return (None, expr)

def ev_meta(expr):
    expr = expr.strip()
    if re.fullmatch(r'-?\d+', expr):
        return expr
    if 'WILDCARD' in expr:
        return '*'
    m = re.fullmatch(r'(?:[\w.]+\.)?(\w+)\.ordinal\(\)', expr)
    if m:
        return m.group(1)
    m = re.fullmatch(r'Fluids\.(\w+)\.getID\(\)', expr)
    if m:
        return m.group(1)
    m = re.fullmatch(r'(?:Mats\.)?MAT_(\w+)\.id', expr)
    if m:
        return m.group(1)
    m = re.fullmatch(r'(?:[\w.]+\.)?([A-Z][A-Z0-9_]+)', expr)
    if m:
        return m.group(1)
    return '?' + expr

def mk(kind, name, meta):
    if kind is None:
        return 'R:' + name + ('#' + meta if meta else '')
    if kind == 'mod':
        return port_item(name, meta)
    key = '%s.%s' % (kind[3], name) + ('#' + meta if meta not in (None,) else '')
    if key not in VANILLA and meta == '0': key = '%s.%s' % (kind[3], name)
    if key not in VANILLA and meta == '0' and False: pass
    if key in VANILLA:
        return VANILLA[key]
    if meta == '*' and '%s.%s' % (kind[3], name) in VANILLA:
        return VANILLA['%s.%s' % (kind[3], name)]
    return 'V:' + key

def ev(expr):
    """-> (token, count)"""
    e = expr.strip()
    while e.startswith('(') and balanced(e, 0) == len(e) - 1:
        e = e[1:-1].strip()
    if re.fullmatch(r'"[^"]*"', e):
        return ore_token(e[1:-1]), 1
    if e in KEYS:
        return ore_token(KEYS[e]), 1
    m = re.fullmatch(r'(\w+)\.(\w+)\(\)', e)
    if m and m.group(1) in FRAMES and m.group(2) in PREFIX:
        return ore_token(PREFIX[m.group(2)] + FRAMES[m.group(1)]), 1
    m = re.fullmatch(r'new ItemStack\((.*)\)', e, re.S)
    if m:
        a = split_args(m.group(1))
        kind, name = ev_item(a[0])
        cnt = a[1] if len(a) > 1 else '1'
        meta = ev_meta(a[2]) if len(a) > 2 else None
        return mk(kind, name, meta), cnt
    m = re.fullmatch(r'(?:DictFrame\.|OreDictManager\.DictFrame\.)?fromOne\((.*)\)', e, re.S)
    if m:
        a = split_args(m.group(1))
        kind, name = ev_item(a[0])
        return mk(kind, name, ev_meta(a[1])), (a[2] if len(a) > 2 else '1')
    m = re.fullmatch(r'Mats\.MAT_(\w+)\.make\((.*)\)', e, re.S)
    if m:
        a = split_args(m.group(2))
        kind, name = ev_item(a[0])
        return mk(kind, name, m.group(1)), (a[1] if len(a) > 1 else '1')
    m = re.fullmatch(r'Item(?:Battery|SelfcontainedBattery)?\.get(?:Empty|Full)Battery\((.*)\)', e, re.S)
    if m:
        kind, name = ev_item(m.group(1))
        return mk(kind, name, None), 1
    m = re.fullmatch(r'Fluids\.(\w+)\.getDict\(([\d_]+)\)', e)
    if m:
        return 'O:container%s%s' % (m.group(2).replace('_', ''), m.group(1).lower()), 1
    m = re.fullmatch(r'(ModItems\.\w+)\.stackFromEnum\((.*)\)', e, re.S)
    if m:
        a = split_args(m.group(2))
        kind, name = ev_item(m.group(1))
        return mk(kind, name, ev_meta(a[-1])), (a[0] if len(a) > 1 else '1')
    m = re.fullmatch(r'EnumBatteryPack\.(\w+)\.stack\(\)', e)
    if m:
        return mk('mod', 'battery_pack', m.group(1)), 1
    m = re.fullmatch(r'(?:ItemToolAbilityFueled|ItemBlowtorch)\.getEmptyTool\((.*)\)', e)
    if m:
        return ev(m.group(1))
    m = re.fullmatch(r'ItemHot\.heatUp\((.*)\)', e, re.S)
    if m:
        return ev(m.group(1))
    m = re.fullmatch(r'ItemScraps\.create\(new MaterialStack\(Mats\.MAT_(\w+), MaterialShapes\.INGOT\.q\((\d+)\)\)\)', e)
    if m:   # f1: amount (q*72) checked by hand in parity-f1.md
        return mk('mod', 'scraps', m.group(1)), 1
    if e == 'GunB92Cell.getFullCell()':
        return 'I:reinhardtshbm:gun_b92_ammo', 1
    kind, name = ev_item(e)
    return mk(kind, name, None), 1

# ---------- original ore dictionary -> port ids ----------
ODICT = defaultdict(set)
def strip_tok(t):
    return t[2:] if t.startswith('I:') else t
def od_add(key, expr):
    e = expr.strip()
    if re.fullmatch(r'[a-z]\w*', e):
        e = 'ModItems.' + e
    else:
        e = re.sub(r'\(\s*([a-z]\w*)\s*([,)])', lambda m: '(ModItems.%s%s' % (m.group(1), m.group(2)), e)
    m = re.fullmatch(r'(?:DictFrame\.)?fromAll\(\s*(?:ModItems\.)?(\w+)\s*,.*\)', e)
    if m:
        base = ITEM_ALIAS.get(m.group(1), m.group(1))
        ODICT[key] |= {'reinhardtshbm:' + x for x in PORT_IDS if x.startswith(base + '_')}
        return
    tok = ev(e)[0]
    if tok.startswith('I:reinhardtshbm:') and tok.endswith('#*'):
        base = tok[16:-2]
        ODICT[key] |= {'reinhardtshbm:' + x for x in PORT_IDS if x == base or x.startswith(base + '_')}
        return
    ODICT[key].add(strip_tok(tok))
SHAPE_M = {'any': ['any'], 'nugget': ['nugget', 'tiny'], 'ingot': ['ingot'], 'dustSmall': ['dustTiny'], 'dust': ['dust'], 'gem': ['gem'], 'crystal': ['crystal'],
           'plate': ['plate'], 'plateCast': ['plateTriple'], 'billet': ['billet'], 'block': ['block'], 'ore': ['ore'], 'oreNether': ['oreNether']}
reg = odm[odm.index('public static void registerOres()'):odm.index('public static void registerGroups()')]
for stmt in reg.split(';'):
    st = stmt.strip()
    m = re.match(r'(?:.*\{\s*)?(\w+)\s*((?:\.\w+\(.*\)\s*)+)$', st, re.S)
    if m and m.group(1) in FRAMES:
        chain, i = m.group(2), 0
        cre = re.compile(r'\.(\w+)\(')
        while i < len(chain):
            mm = cre.match(chain, i)
            if not mm:
                i += 1; continue
            end = balanced(chain, mm.end() - 1)
            if mm.group(1) in SHAPE_M:
                for a in split_args(chain[mm.end():end]):
                    for pre in SHAPE_M[mm.group(1)]:
                        od_add(pre + FRAMES[m.group(1)], a)
            i = end + 1
        continue
    m = re.search(r'OreDictionary\.registerOre\((.*)\)$', st, re.S)
    if m:
        a = split_args(m.group(1))
        key = KEYS.get(a[0], a[0].strip('"'))
        if re.fullmatch(r'\w+', key) and len(a) == 2 and key not in ('name', 'dyeName') and not re.search(r'\bi\)|mat\.id|ore\.ordinal', a[1]):
            od_add(key, a[1])
for i, d in enumerate(["Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"]):
    for it in ('chemical_dye', 'crayon'):
        ODICT['dye' + d].add(strip_tok(port_item(it, str(i))))
for n in ('fuelCoke', 'coke'):
    ODICT[n] |= {'reinhardtshbm:coke_coal', 'reinhardtshbm:coke_lignite', 'reinhardtshbm:coke_petroleum'}
for i in range(16):
    ODICT['anyConcrete'].add('reinhardtshbm:concrete_colored#%d' % i)
for i in range(8):
    ODICT['anyConcrete'].add('reinhardtshbm:concrete_colored_ext#%d' % i)
mats_src = strip_comments((ORIG / 'inventory/material/Mats.java').read_text(encoding='utf-8'))
AUTOGEN = {'CASTPLATE': ('plateTriple', 'plate_cast', True), 'WELDEDPLATE': ('plateSextuple', 'plate_welded', True), 'DENSEWIRE': ('wireDense', 'wire_dense', True),
           'BOLT': ('bolt', 'bolt', False), 'INGOT': ('ingot', 'ingot_raw', False), 'NUGGET': ('nugget', 'nugget_raw', False), 'SHELL': ('shell', 'shell', False),
           'PIPE': ('ntmpipe', 'pipe', False), 'FRAGMENT': ('bedrockorefragment', 'bedrock_ore_fragment', False), 'WIRE': ('wireFine', 'wire_fine', False),
           'LIGHTBARREL': ('barrelLight', 'part_barrel_light', False), 'HEAVYBARREL': ('barrelHeavy', 'part_barrel_heavy', False),
           'LIGHTRECEIVER': ('receiverLight', 'part_receiver_light', False), 'HEAVYRECEIVER': ('receiverHeavy', 'part_receiver_heavy', False),
           'MECHANISM': ('gunMechanism', 'part_mechanism', False), 'STOCK': ('stock', 'part_stock', False), 'GRIP': ('grip', 'part_grip', False)}
MAT_FRAME = {}
for m in re.finditer(r'NTMMaterial\s+MAT_(\w+)\s*=\s*(\w+)\(([^;]*);', mats_src):
    name, maker, rest = m.group(1), m.group(2), m.group(3)
    try:
        a = split_args(rest[:balanced('(' + rest, 0) - 1])
    except Exception:
        continue
    frame = a[1].strip() if len(a) > 1 else None
    MAT_FRAME[name] = frame
    ag = re.search(r'setAutogen\(([^)]*)\)', rest)
    if not ag or frame not in FRAMES: continue
    for sh in [x.strip() for x in ag.group(1).split(',')]:
        if sh in AUTOGEN:
            pre, item, need_smelt = AUTOGEN[sh]
            if need_smelt and maker != 'makeSmeltable': continue
            ODICT[pre + FRAMES[frame]].add(strip_tok(port_item(item, name)))
GROUPS = {}
for m in re.finditer(r'DictGroup\s+(\w+)\s*=\s*new\s+DictGroup\("(\w+)"\s*,\s*([^)]*)\)', odm):
    GROUPS[m.group(1)] = (m.group(2), [x.strip() for x in m.group(3).split(',')])
PRE_OF = {'INGOT': 'ingot', 'DUST': 'dust', 'BLOCK': 'block', 'GRIP': 'grip', 'STOCK': 'stock', 'CASTPLATE': 'plateTriple', 'WELDEDPLATE': 'plateSextuple',
          'LIGHTBARREL': 'barrelLight', 'HEAVYBARREL': 'barrelHeavy', 'LIGHTRECEIVER': 'receiverLight', 'HEAVYRECEIVER': 'receiverHeavy', 'NUGGET': 'nugget', 'ANY': 'any'}
rg = odm[odm.index('public static void registerGroups()'):]
rg = rg[:rg.index('recursionBrake')]
for st in rg.split(';'):
    m = re.match(r'\s*(?:.*\{\s*)?(\w+)((?:\s*\.addPrefix\(\w+,\s*\w+\))+)', st, re.S)
    if not m or m.group(1) not in GROUPS: continue
    gname, gm = GROUPS[m.group(1)]
    for sh, flag in re.findall(r'addPrefix\((\w+),\s*(\w+)\)', m.group(2)):
        pre = PRE_OF[sh]
        for member in gm:
            if member in FRAMES:
                ODICT[pre + gname] |= ODICT.get(pre + FRAMES[member], set())
            elif member in KEYS:
                ODICT[pre + gname] |= ODICT.get(KEYS[member], set())
from parity_e3_recipe_alias import WILD
for _k, _v in WILD.items(): ODICT[_k] = {'minecraft:' + x for x in _v}
ODICT = dict(ODICT)

# ---------- collect original calls ----------
BASE = {'addRecipeAuto': 'shaped', 'addShapelessAuto': 'shapeless', 'GameRegistry.addRecipe': 'shaped', 'GameRegistry.addShapelessRecipe': 'shapeless',
        'GameRegistry.addSmelting': 'smelt', 'CraftingManager.addRecipeAuto': 'shaped', 'CraftingManager.addShapelessAuto': 'shapeless'}
FILES = ['main/CraftingManager.java'] + ['crafting/%s.java' % n for n in
         ['ArmorRecipes', 'ConsumableRecipes', 'MineralRecipes', 'PowderRecipes', 'RodRecipes', 'SmeltingRecipes', 'ToolRecipes', 'WeaponRecipes']]
SRC = {f: strip_comments((ORIG / f).read_text(encoding='utf-8')) for f in FILES}
HELPERS = defaultdict(list)  # name -> [(params, body)]
for f, s in SRC.items():
    for m in re.finditer(r'public static void (\w+)\(([^)]*)\)\s*\{', s):
        name = m.group(1)
        if name in ('register', 'registerInit', 'mainRegistry', 'AddCraftingRec', 'reg2', 'crumple', 'addRecipeAuto', 'addShapelessAuto', 'AddSmeltingRec'):
            continue
        end = balanced(s, m.end() - 1)
        params = [tuple(p.strip().replace('...', '@VA').rsplit(' ', 1)) for p in m.group(2).split(',') if p.strip()]
        HELPERS[name].append((params, s[m.end():end], f))

def arg_type(a):
    a = a.strip()
    if a.startswith(('new ItemStack', 'DictFrame.fromOne', 'fromOne', 'Mats.')):
        return 'ItemStack'
    if a.startswith(('ModBlocks.', 'Blocks.')):
        return 'Block'
    if a.startswith(('ModItems.', 'Items.')):
        return 'Item'
    if a.startswith('"') or re.fullmatch(r'\w+\.\w+\(\)', a):
        return 'String'
    if a in FRAMES:
        return 'DictFrame'
    return '?'

CALL_RE = re.compile(r'(?<![\w.])((?:GameRegistry\.|CraftingManager\.|MineralRecipes\.|RodRecipes\.|ToolRecipes\.|ArmorRecipes\.)?\w+)\s*\(')
calls = []  # dict(kind,args,conds,file,line,via)

def block_context(s, pos):
    """enclosing block headers + same-statement prefix"""
    heads, stack, i, last = [], [], 0, 0
    while i < pos:
        c = s[i]
        if c == '"' or c == "'":
            q = c; i += 1
            while s[i] != q:
                i += 2 if s[i] == '\\' else 1
        elif c == '{':
            stack.append(s[last:i].strip()); last = i + 1
        elif c == '}':
            if stack: stack.pop()
            last = i + 1
        elif c == ';':
            last = i + 1
        elif c in '([':
            i = balanced(s, i)
        i += 1
    ctx = [h for h in stack if re.match(r'(if|else|for|while)\b', h) or h.startswith('} else')]
    pre = s[last:pos].strip()
    if pre:
        ctx.append(pre)
    return ctx

def process(text, file, base_line, conds, via, depth=0, top=None):
    for m in CALL_RE.finditer(text):
        full = m.group(1)
        short = full.split('.')[-1]
        if full in BASE or short in HELPERS and (full == short or full.split('.')[0] in ('MineralRecipes', 'RodRecipes', 'ToolRecipes', 'ArmorRecipes')):
            try:
                end = balanced(text, m.end() - 1)
            except Exception:
                continue
            # skip method declarations
            if re.search(r'static void\s*$', text[:m.start()][-14:]):
                continue
            args = split_args(text[m.end():end])
            line = base_line if top is None else top
            if depth == 0:
                line = text.count('\n', 0, m.start()) + 1
                c2 = conds + block_context(text, m.start())
            else:
                c2 = conds + block_context(text, m.start())
            if full in BASE:
                calls.append(dict(kind=BASE[full], args=args, conds=c2, file=file, line=line, via=via))
            elif depth < 4:
                types = [arg_type(a) for a in args]
                best = None
                for params, body, hf in HELPERS[short]:
                    varargs = params and params[-1][0].endswith('@VA')
                    if len(params) != len(args) and not (varargs and len(args) >= len(params) - 1):
                        continue
                    score = sum(1 for (pt, _), t in zip(params, types) if t == pt or pt == 'Object') - (1 if varargs and len(params) != len(args) else 0)
                    if best is None or score > best[0]:
                        best = (score, params, body, hf, varargs)
                if best is None:
                    continue
                _, params, body, hf, varargs = best
                b = body
                for idx, (pt, pn) in enumerate(params):
                    if varargs and idx == len(params) - 1 and not (len(args) == len(params) and args[idx].startswith('new ')):
                        rep = 'new Object[] {' + ', '.join(args[idx:]) + '}'
                        b = re.sub(r'for\s*\(\s*String\s+(\w+)\s*:\s*%s\s*\)' % pn, lambda mm: '/*VARARG %s*/ for(String %s : %s)' % (pn, mm.group(1), rep), b)
                    else:
                        rep = args[idx]
                    b = re.sub(r'(?<![\w.])%s(?!\w)' % re.escape(pn), lambda _m: rep, b)
                process(b, file, line, c2, via + [short], depth + 1, line)

for f, s in SRC.items():
    # only top-level registration bodies (helpers are expanded at call sites)
    for m in re.finditer(r'public static void (\w+)\(([^)]*)\)\s*\{', s):
        if m.group(1) in HELPERS or m.group(1) in ('addRecipeAuto', 'addShapelessAuto'):
            continue
        end = balanced(s, m.end() - 1)
        body = s[:m.end()].count('\n')
        seg = '\n' * body + s[m.end():end]
        process(seg, f, 0, [], [])

def flags(conds):
    t = ' '.join(conds)
    fl = []
    if re.search(r'!\s*GeneralConfig\.enable528', t) or re.search(r'enable528[^{]*\)\s*\{?\s*$', t) and 'else' in t and False:
        fl.append('not528')
    elif 'enable528' in t:
        fl.append('528')
    if 'else' in t:
        fl.append('else')
    if 'enableLBSM' in t: fl.append('lbsm')
    if 'enableExpensiveMode' in t: fl.append('expensive')
    if re.search(r'\bfor\s*\(', t): fl.append('loop')
    if 'isModLoaded' in t or 'Loader.' in t: fl.append('compat')
    if 'enableBabyMode' in t: fl.append('baby')
    return fl

orig = []
unresolved = []
PATCONST = {}
for _f, _s in SRC.items():
    for _m in re.finditer(r'String\[\]\s+(\w+)\s*=\s*(new String\[\]\s*\{[^}]*\})', _s):
        PATCONST[_m.group(1)] = _m.group(2)
def expand_loops(c):
    variants = [list(c['args'])]
    for h in c['conds']:
        reps = F1X.unroll(c['file'], h) if F1X else None
        if reps is not None:
            variants = [[F1X.apply(x, rep) for x in a] for a in variants for rep in reps]
            continue
        m = re.search(r'for\s*\(\s*\w+\s+(\w+)\s*:\s*new\s+\w+\[\]\s*\{(.*)\}\s*\)', h, re.S)
        vals = None
        if m:
            var, vals = m.group(1), split_args(m.group(2))
        else:
            m = re.search(r'for\s*\(\s*int\s+(\w+)\s*=\s*(\d+)\s*;\s*\w+\s*<\s*(\d+)\s*;', h)
            if m:
                var, vals = m.group(1), [str(i) for i in range(int(m.group(2)), int(m.group(3)))]
        if vals:
            variants = [[re.sub(r'(?<![\w.])%s(?!\w)' % var, lambda _m: v, x) for x in a] for a in variants for v in vals]
    return variants
calls2 = []
for c in calls:
    for a in expand_loops(c):
        c2 = dict(c); c2['args'] = a; calls2.append(c2)
for c in calls2:
    a = c['args']
    if len(a) == 1:
        mm = re.fullmatch(r'new Shape(d|less)OreRecipe\((.*)\)', a[0], re.S)
        if mm:
            a = split_args(mm.group(2)); c['kind'] = 'shaped' if mm.group(1) == 'd' else 'shapeless'
        mm = re.fullmatch(r'new ContainerUpgradeCraftingHandler\((.*)\)', a[0], re.S)
        if mm:
            a = split_args(mm.group(1)); c['kind'] = 'shaped'
    if len(a) == 2 and a[1].startswith('new Object[]'):
        a = [a[0]] + split_args(a[1][a[1].index('{') + 1:a[1].rindex('}')])
    fl = flags(c['conds'])
    try:
        if c['kind'] == 'smelt':
            intok, _ = ev(a[0]); out, cnt = ev(a[1])
            rec = dict(kind='smelt', out=out, count=str(cnt), ings=Counter({intok: 1}), xp=a[2].rstrip('Ff'), pattern=None)
        else:
            out, cnt = ev(a[0])
            rest = a[1:]
            if len(rest) == 1 and rest[0].startswith('new String[]'):
                pass
            if c['kind'] == 'shaped':
                rest = [PATCONST.get(x, x) for x in rest]
                rows = []
                while rest and (re.fullmatch(r'"[^"]*"', rest[0]) or rest[0].startswith('new String[]')):
                    if rest[0].startswith('new String[]'):
                        rows += [x[1:-1] for x in split_args(rest[0][rest[0].index('{') + 1:rest[0].rindex('}')])]
                    else:
                        rows.append(rest[0][1:-1])
                    rest = rest[1:]
                keymap = {}
                for i in range(0, len(rest) - 1, 2):
                    ch = rest[i].strip("'")
                    keymap[ch] = ev(rest[i + 1])[0]
                ings = Counter()
                for r in rows:
                    for ch in r:
                        if ch != ' ':
                            ings[keymap.get(ch, 'R:?' + ch)] += 1
                pat = tuple(tuple(keymap.get(ch, 'R:?' + ch) if ch != ' ' else '' for ch in r) for r in rows)
                rec = dict(kind='shaped', out=out, count=str(cnt), ings=ings, pattern=pat)
            else:
                ings = Counter()
                for x in rest:
                    ings[ev(x)[0]] += 1
                rec = dict(kind='shapeless', out=out, count=str(cnt), ings=ings, pattern=None)
    except Exception as ex:
        unresolved.append((c, 'EXC ' + repr(ex))); continue
    rec.update(file=c['file'].split('/')[-1], line=c['line'], flags=fl, conds=c['conds'], via=c['via'])
    toks = [rec['out']] + list(rec['ings'])
    if any(t.startswith('R:') for t in toks) or not re.fullmatch(r'\d+', rec['count']):
        unresolved.append((c, 'RAW ' + ' '.join(t for t in toks if t.startswith('R:')) + ' cnt=' + rec['count'])); continue
    orig.append(rec)

# ---------- port recipes ----------
def comp_suffix(item, comp):
    if not comp: return ''
    cd = comp.get('minecraft:custom_data')
    if isinstance(cd, str):
        m = re.fullmatch(r'\{(meta|crayon|variant|waste_class):(\d+)\}', cd.replace(' ', ''))
        if m: return '#' + m.group(2)
        return ''
    if isinstance(cd, dict):
        keys = [k for k in cd if k != 'spentFuelPoolCooling']
        if len(keys) == 1:
            v = cd[keys[0]]
            if keys[0] == 'meta': return '#' + str(v)
            if keys[0] == 'post': return '#1'
            if isinstance(v, str):
                base = str(item).split(':')[-1]
                if v.startswith(base + '_'): v = v[len(base) + 1:]
                elif v == base: return ''
                return '_' + v
        return ''
    for k in ('reinhardtshbm:drone_type', 'reinhardtshbm:cart_type'):
        if k in comp: return '#' + str(comp[k])
    return ''

def ing_token(o):
    if isinstance(o, list):
        ts = sorted(ing_token(x) for x in o)
        return ts[0] if len(set(ts)) == 1 else 'A:[' + ','.join(ts) + ']'
    if isinstance(o, str):
        return 'I:' + o
    if 'tag' in o:
        return tag_token(o['tag'])
    if o.get('type') == 'reinhardtshbm:legacy_fluid_container':
        return 'O:container%s%s' % (o.get('amount'), o.get('fluid'))
    if o.get('type') == 'neoforge:compound':
        return ing_token(o.get('children', []))
    if 'item' in o and 'components' in o:
        return 'I:' + o['item'] + comp_suffix(o['item'], o['components'])
    if o.get('type') == 'neoforge:components' or ('items' in o and 'components' in o):
        items = o.get('items')
        if isinstance(items, list):
            items = items[0] if len(items) == 1 else '[' + ','.join(sorted(items)) + ']'
        return 'I:' + str(items) + comp_suffix(items, o.get('components', {}))
    if 'item' in o:
        return 'I:' + o['item']
    return 'R:' + json.dumps(o, sort_keys=True)[:80]

def cond_flags(j):
    t = json.dumps(j.get('neoforge:conditions', []))
    fl = []
    neg = 'neoforge:not' in t
    if 'enable_528_mode' in t: fl.append('not528' if neg else '528')
    if 'less_bullshit_mode' in t: fl.append('notlbsm' if neg else 'lbsm')
    if 'enable_expensive_mode' in t: fl.append('notexpensive' if neg else 'expensive')
    if 'mod_loaded' in t: fl.append('compat')
    other = re.findall(r'"type": "([^"]+)"', t)
    for o in other:
        if o not in ('neoforge:not', 'reinhardtshbm:enable_528_mode', 'reinhardtshbm:less_bullshit_mode', 'reinhardtshbm:enable_expensive_mode', 'neoforge:mod_loaded'):
            fl.append(o.split(':')[-1])
    return fl

port = []
for p in sorted(RECIPES.rglob('*.json')):
    try:
        j = json.loads(p.read_text(encoding='utf-8-sig'))
    except Exception as ex:
        print('BAD JSON', p, ex); continue
    t = j.get('type', '')
    rel = p.relative_to(RECIPES).as_posix()
    if t in ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless'):
        res = j['result']
        rid = res.get('id') or res.get('item')
        out = 'I:' + rid + comp_suffix(rid, res.get('components', {}))
        if t.endswith('shaped'):
            key = {k: ing_token(v) for k, v in j['key'].items()}
            ings = Counter()
            for r in j['pattern']:
                for ch in r:
                    if ch != ' ':
                        ings[key[ch]] += 1
            pat = tuple(tuple(key[ch] if ch != ' ' else '' for ch in r) for r in j['pattern'])
            port.append(dict(kind='shaped', out=out, count=str(res.get('count', 1)), ings=ings, pattern=pat, file=rel, flags=cond_flags(j)))
        else:
            ings = Counter(ing_token(x) for x in j['ingredients'])
            port.append(dict(kind='shapeless', out=out, count=str(res.get('count', 1)), ings=ings, pattern=None, file=rel, flags=cond_flags(j)))
    elif t in ('minecraft:smelting', 'reinhardtshbm:hot_smelting'):
        res = j['result']
        rid = res.get('id') if isinstance(res, dict) else res
        port.append(dict(kind='smelt', out='I:' + rid, count=str(res.get('count', 1)) if isinstance(res, dict) else '1',
                         ings=Counter({ing_token(j['ingredient']): 1}), xp=str(j.get('experience', 0)), time=j.get('cookingtime', 200),
                         pattern=None, file=rel, flags=cond_flags(j)))

# tag membership (port) for tag<->item relaxation
TAGS = {}
for base in ['c', 'reinhardtshbm', 'minecraft']:
    d = ROOT / 'src/main/resources/data' / base / 'tags/item'
    if d.exists():
        for p in d.rglob('*.json'):
            try:
                vals = json.loads(p.read_text(encoding='utf-8-sig')).get('values', [])
            except Exception:
                continue
            TAGS[base + ':' + p.relative_to(d).as_posix()[:-5]] = [v if isinstance(v, str) else v.get('id') for v in vals]
def members(tag, seen=()):
    out = set()
    for v in TAGS.get(tag, []):
        if v is None: continue
        if v.startswith('#'):
            if v[1:] not in seen: out |= members(v[1:], seen + (tag,))
        else:
            out.add(v)
    return out
def pmembers(tag):
    return {m for m in members(tag) if m.split(':')[0] in ('reinhardtshbm', 'minecraft')}

def mirror(pat):
    return tuple(tuple(reversed(r)) for r in pat)

EQ_NOTES = defaultdict(set)
def tok_eq(o, p):
    """2 equal, 1 soft (set differs but same concept / narrowed), 0 different"""
    if o == p: return 2
    if o.startswith('I:') and o.endswith('#*') and p.startswith(o[:-2]): return 2
    if o.startswith('I:') and p.startswith('I:'):
        return 0
    so = ODICT.get(o[2:]) if o.startswith('O:') else {o[2:]}
    if p.startswith('A:['):
        return 2 if so and so == {x[2:] for x in p[3:-1].split(',')} else 0
    if p.startswith('T:'):
        tag = p[2:]
        if o.startswith('O:') and tag in ORE_ALIAS.get(o[2:], ()): return 2
        sp = pmembers(tag)
        if so and so == sp: return 2
        if o.startswith('O:') and canon_key(o[2:]) == canon_tag(tag):
            if so is None or not sp: return 2      # builtin/vanilla material, nothing to compare
            if so <= sp and all(x.startswith('minecraft:') for x in sp - so): return 2
            EQ_NOTES['tagset %s vs %s: orig-only %s port-only %s' % (o, p, sorted(so - sp), sorted(sp - so))].add(1)
            return 1
        if so and sp and so == sp: return 2
        if so and sp and (so < sp or sp < so):
            EQ_NOTES['%s %s vs %s: orig-only %s port-only %s' % ('widened' if so < sp else 'narrowed', o, p, sorted(so - sp), sorted(sp - so))].add(1)
            return 1
        return 0
    if p.startswith('I:'):
        it = p[2:]
        if it in ORE_ALIAS.get(o[2:], ()): return 2
        if so is None: return 0
        if so == {it}: return 2
        if it in so:
            EQ_NOTES['narrowed %s vs %s: orig %s' % (o, p, sorted(so))].add(1)
            return 1
    return 0

def ings_match(a, b):
    """multiset match with relaxations; returns 2 exact / 1 narrowed / 0"""
    if sum(a.values()) != sum(b.values()): return 0
    b = Counter(b); q = 2
    for t, n in a.items():
        if b.get(t, 0) >= n:
            b[t] -= n; continue
        need = n - b.get(t, 0); b[t] = 0
        for u in list(b):
            if b[u] > 0 and need > 0:
                e = tok_eq(t, u)
                if e:
                    k = min(need, b[u]); b[u] -= k; need -= k; q = min(q, e)
        if need: return 0
    return q

def fmt(r):
    return '%s x%s <= %s' % (r['out'], r['count'], ', '.join('%s*%d' % (k, v) for k, v in sorted(r['ings'].items())))
def run_match(orig):
    by_out = defaultdict(list)
    for i, p in enumerate(port):
        by_out[(p['kind'], p['out'])].append(i)
    used = set()
    result = dict(matched=0, narrowed=[], pattern=[], count=[], cond=[], xp=[], missing=[], extra=[], changed=[])
    pending = []
    for r in orig:
        if r['out'] in IGNORE_OUT: continue
        cands = [i for i in by_out.get((r['kind'], r['out']), []) if i not in used]
        if r['kind'] != 'smelt':
            other = 'shapeless' if r['kind'] == 'shaped' else 'shaped'
            cands2 = [i for i in by_out.get((other, r['out']), []) if i not in used]
        else:
            cands2 = []
        best = None
        for i in cands + cands2:
            p = port[i]
            q = ings_match(r['ings'], p['ings'])
            if not q: continue
            score = q * 10 + (5 if p['count'] == r['count'] else 0) + (2 if p['kind'] == r['kind'] else 0)
            if r['pattern'] is not None and p['pattern'] is not None:
                def peq(x, y):
                    return len(x) == len(y) and all(len(a) == len(b) and all((s == t) or (s and t and tok_eq(s, t)) for s, t in zip(a, b)) for a, b in zip(x, y))
                if peq(r['pattern'], p['pattern']) or peq(mirror(r['pattern']), p['pattern']): score += 3
            of = set(f for f in r['flags'] if f in ('528', 'not528', 'lbsm', 'expensive'))
            pf = set(f for f in p['flags'] if f in ('528', 'not528', 'lbsm', 'expensive'))
            if of == pf: score += 1
            if best is None or score > best[0]: best = (score, i, q)
        if best is None:
            pending.append(r); continue
        score, i, q = best; used.add(i); p = port[i]
        rest = score - q * 10
        ok = True
        if q == 1: result['narrowed'].append((r, p)); ok = False
        if p['count'] != r['count']: result['count'].append((r, p)); ok = False
        if p['kind'] != r['kind'] or (r['pattern'] is not None and not (rest & 3 == 3 or (rest - 5 if p['count'] == r['count'] else rest) >= 5)):
            # recompute explicitly
            pass
        if r['kind'] != 'smelt':
            if p['kind'] != r['kind']:
                result['pattern'].append((r, p, 'kind')); ok = False
            elif r['pattern'] is not None:
                def peq(x, y):
                    return len(x) == len(y) and all(len(a) == len(b) and all((s == t) or (s and t and tok_eq(s, t)) for s, t in zip(a, b)) for a, b in zip(x, y))
                if not (peq(r['pattern'], p['pattern']) or peq(mirror(r['pattern']), p['pattern'])):
                    result['pattern'].append((r, p, 'shape')); ok = False
        else:
            if abs(float(r['xp']) - float(p['xp'])) > 1e-6: result['xp'].append((r, p)); ok = False
            if p.get('time', 200) != 200: result['xp'].append((r, p)); ok = False
        of = set(f for f in r['flags'] if f in ('528', 'not528', 'lbsm', 'expensive'))
        pf = set(f for f in p['flags'] if f in ('528', 'not528', 'lbsm', 'expensive'))
        if of != pf or 'else' in r['flags']:
            result['cond'].append((r, p))
        if ok: result['matched'] += 1
    # pair leftovers by output
    left_port = defaultdict(list)
    for i, p in enumerate(port):
        if i not in used: left_port[p['out']].append(i)
    for r in pending:
        lst = [i for i in left_port.get(r['out'], []) if i not in used and (port[i]['kind'] == 'smelt') == (r['kind'] == 'smelt')]
        if lst:
            # closest by multiset distance
            def dist(i):
                a, b = r['ings'], port[i]['ings']
                return sum(((a - b) + (b - a)).values())
            i = min(lst, key=dist); used.add(i)
            result['changed'].append((r, port[i]))
        else:
            result['missing'].append(r)
    for i, p in enumerate(port):
        if i not in used: result['extra'].append(p)

    return result, None

LEARNED = {}
def base_name(tok):
    return re.split(r'[#{~]', tok[2:])[0]
def apply_alias(recs):
    out = []
    for r in recs:
        r2 = dict(r)
        r2['out'] = LEARNED.get(r['out'], r['out'])
        r2['ings'] = Counter()
        for t, n in r['ings'].items(): r2['ings'][LEARNED.get(t, t)] += n
        if r['pattern'] is not None: r2['pattern'] = tuple(tuple(LEARNED.get(t, t) for t in row) for row in r['pattern'])
        out.append(r2)
    return out
_seen = set(); _d = []
for _r in orig:
    _k = (_r['kind'], _r['out'], _r['count'], tuple(sorted(_r['ings'].items())), _r['pattern'], tuple(f for f in _r['flags'] if f in ('528', 'not528', 'lbsm', 'expensive', 'else')))
    if _k in _seen: continue
    _seen.add(_k); _d.append(_r)
DUPES = len(orig) - len(_d)
orig = _d
ORIG0 = orig
for _round in range(6):
    orig = apply_alias(ORIG0)
    result, _ = run_match(orig)
    learned = {}
    lo = result['missing'] + [x[0] for x in result['changed']]
    lp = result['extra'] + [x[1] for x in result['changed']]
    for r in lo:
        if r['out'].startswith('I:minecraft'): continue
        bn = base_name(r['out'])
        c = [q for q in lp if q['kind'] == r['kind'] and q['count'] == r['count'] and q['out'] != r['out'] and q['out'].startswith('I:reinhardtshbm')
             and (base_name(q['out']).startswith(bn[:max(4, len(bn) - 12)]) or bn.startswith(base_name(q['out']))) and ings_match(r['ings'], q['ings']) == 2]
        outs = {q['out'] for q in c}
        if len(outs) == 1:
            o = outs.pop()
            if learned.get(r['out'], o) == o and o not in learned.values(): learned[r['out']] = o
    if not learned: break
    LEARNED.update(learned)

if __name__ == '__main__':
    mode = sys.argv[1] if len(sys.argv) > 1 else 'summary'
    n_by = Counter(r['kind'] for r in orig)
    print('orig parsed', dict(n_by), 'unresolved', len(unresolved), '| port', dict(Counter(p['kind'] for p in port)))
    print({k: (v if isinstance(v, int) else len(v)) for k, v in result.items()})
    def src(r): return '%s:%s%s%s' % (r['file'], r['line'], ' via ' + '/'.join(r['via']) if r.get('via') else '', ' [' + ','.join(r['flags']) + ']' if r['flags'] else '')
    if mode == 'learned':
        for k, v in sorted(LEARNED.items()): print(k, '=>', v)
    if mode == 'notes':
        for k in sorted(EQ_NOTES): print(k)
    if mode == 'unresolved':
        for c, why in unresolved:
            print(c['file'].split('/')[-1], c['line'], why[:150], '| conds', ' ; '.join(x[:60] for x in c['conds'])[:150])
    elif mode == 'tokens':
        cnt = Counter()
        for r in result['missing'] + [x[0] for x in result['changed']]:
            for t in [r['out']] + list(r['ings']):
                if t.startswith('V:'): cnt[t] += 1
                elif t.startswith('I:reinhardtshbm:') and t[16:].split('#')[0].split('~')[0] not in PORT_IDS: cnt[t] += 1
                elif t.startswith('O:') and t[2:] not in ODICT and t[2:] not in ORE_ALIAS: cnt[t] += 1
        for t, n in cnt.most_common(400): print(n, t)
    elif mode in result:
        for x in result[mode]:
            if isinstance(x, tuple):
                r, p = x[0], x[1]
                print('ORIG %s  {%s}%s\n PORT %s  {%s} [%s]%s' % (fmt(r), src(r), ' ' + x[2] if len(x) > 2 else '', fmt(p), p['file'], ','.join(p['flags']),
                      (' xp %s/%s t=%s' % (r.get('xp'), p.get('xp'), p.get('time')) if r['kind'] == 'smelt' else '')))
                if mode == 'pattern':
                    print('   ', r['pattern'], '\n   ', p['pattern'])
            elif 'line' in x:
                print('MISSING %s %s  {%s}%s' % (x['kind'], fmt(x), src(x), ' xp ' + x['xp'] if x['kind'] == 'smelt' else ''))
            else:
                print('EXTRA %s %s  {%s} [%s]' % (x['kind'], fmt(x), x['file'], ','.join(x['flags'])))
    if mode == 'pairs':
        cnt = Counter()
        for r, p in result['changed']:
            a = set(r['ings']) - set(p['ings']); b = set(p['ings']) - set(r['ings'])
            if len(a) == 1 and len(b) == 1: cnt[(a.pop(), b.pop())] += 1
            elif len(a) == 2 and len(b) == 2: cnt[(tuple(sorted(a)), tuple(sorted(b)))] += 1
        for (a, b), n in cnt.most_common(int(sys.argv[2]) if len(sys.argv) > 2 else 300): print(n, a, '=>', b)
    if mode == 'review':
        import difflib
        def sh(t): return t.replace('I:reinhardtshbm:', ':').replace('I:minecraft:', 'm:').replace('T:c:', '#').replace('T:minecraft:', '#m:')
        def diff(r, p):
            a = Counter(r['ings']); b = Counter(p['ings'])
            for t in list(a):
                for u in list(b):
                    if a[t] > 0 and b[u] > 0 and tok_eq(t, u) == 2:
                        k = min(a[t], b[u]); a[t] -= k; b[u] -= k
            return +a, +b
        lo = result['missing'] + [x[0] for x in result['changed']]
        lp = result['extra'] + [x[1] for x in result['changed']]
        usedp = set()
        rows = []
        for r in lo:
            best = None
            for j, q in enumerate(lp):
                if j in usedp or (q['kind'] == 'smelt') != (r['kind'] == 'smelt'): continue
                a, b = diff(r, q)
                d = sum(a.values()) + sum(b.values())
                sim = difflib.SequenceMatcher(None, base_name(r['out']), base_name(q['out'])).ratio()
                if sim < 0.6: continue
                score = d - 3 * sim + (0 if q['count'] == r['count'] else 0.5)
                if best is None or score < best[0]: best = (score, j, a, b, d)
            if best and best[4] <= max(2, sum(r['ings'].values()) // 2 + 1):
                _, j, a, b, d = best; usedp.add(j); q = lp[j]
                rows.append('PAIR %s x%s {%s:%s%s} ~ %s x%s {%s%s} | orig-only: %s | port-only: %s%s' % (sh(r['out']), r['count'], r['file'][:-5], r['line'], ' ' + ','.join(r['flags']) if r['flags'] else '',
                    sh(q['out']), q['count'], q['file'], ' ' + ','.join(q['flags']) if q['flags'] else '',
                    ', '.join('%s*%d' % (sh(k), v) for k, v in a.items()), ', '.join('%s*%d' % (sh(k), v) for k, v in b.items()),
                    (' | xp %s/%s' % (r.get('xp'), q.get('xp')) if r['kind'] == 'smelt' else '')))
            else:
                rows.append('MISS %s %s {%s:%s%s}' % (r['kind'], sh(fmt(r)), r['file'][:-5], r['line'], ' ' + ','.join(r['flags']) if r['flags'] else ''))
        for j, q in enumerate(lp):
            if j not in usedp: rows.append('EXTRA %s %s {%s%s}' % (q['kind'], sh(fmt(q)), q['file'], ' ' + ','.join(q['flags']) if q['flags'] else ''))
        open(sys.argv[2], 'w', encoding='utf-8').write('\n'.join(rows))
        print(Counter(x.split()[0] for x in rows))
