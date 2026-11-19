#!/usr/bin/env python3
"""[parity e2] Extracts slot tables of the 1.7.10 containers and GUI texture/size data into JSON.
Usage: e2_gui_extract.py <original inventory dir> <out json>"""
import json, os, re, sys

SRC = sys.argv[1]
OUT = sys.argv[2]


def strip_comments(s):
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    return re.sub(r'//[^\n]*', '', s)


def match_paren(s, i, o='(', c=')'):
    d = 0
    while i < len(s):
        if s[i] == o:
            d += 1
        elif s[i] == c:
            d -= 1
            if d == 0:
                return i
        i += 1
    raise ValueError('unbalanced')


def split_args(s):
    out, d, cur = [], 0, ''
    for ch in s:
        if ch in '([{':
            d += 1
        if ch in ')]}':
            d -= 1
        if ch == ',' and d == 0:
            out.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


PLAYER_RE = re.compile(r'(?i)invPlayer|playerInv|player\.inventory|inventoryPlayer|^inv$|^player$|^inventory$')


class Interp:
    def __init__(self, name, fields):
        self.name = name
        self.slots = []
        self.warn = []
        self.env = dict(fields)
        self.env['ArmorModHandler'] = type('C', (), dict(helmet_only=0, plate_only=1, legs_only=2, boots_only=3, servos=4, cladding=5, kevlar=6, extra=7, battery=8))

    def ev(self, e):
        e = e.strip().replace('inventory.getSizeInventory()', '40')
        e = re.sub(r'\(int\)\s*', '', e)
        e = e.replace('this.', '')
        e = re.sub(r'(?<![/])/(?![/])', '//', e)
        e = e.replace('&&', ' and ').replace('||', ' or ')
        e = re.sub(r'!(?!=)', ' not ', e)
        e = re.sub(r'\btrue\b', 'True', e)
        e = re.sub(r'\bfalse\b', 'False', e)
        return eval(e, {'__builtins__': {}}, self.env)

    def isint(self, e):
        try:
            v = self.ev(e)
            return isinstance(v, int) and not isinstance(v, bool)
        except Exception:
            return False

    def add(self, cls, args):
        # the slot index/x/y triple is the first run of three int-evaluable args
        for k in range(len(args) - 2):
            if all(self.isint(a) for a in args[k:k + 3]):
                inv = args[k - 1] if k > 0 else ''
                player = bool(PLAYER_RE.search(inv)) and cls != 'SlotPneumo'
                self.slots.append({'c': cls, 'p': 1 if player else 0, 'i': self.ev(args[k]), 'x': self.ev(args[k + 1]), 'y': self.ev(args[k + 2])})
                return
        self.warn.append('unresolved slot args: %s(%s)' % (cls, ', '.join(args)))

    def player_inv(self, a):
        v = [self.ev(x) for x in a[1:]]
        if len(v) == 1:
            v = [8, v[0], v[0] + 58]
        elif len(v) == 2:
            v = [v[0], v[1], v[1] + 58]
        for i in range(3):
            for j in range(9):
                self.slots.append({'c': 'SlotNonRetarded', 'p': 1, 'i': j + i * 9 + 9, 'x': v[0] + j * 18, 'y': v[1] + i * 18})
        for i in range(9):
            self.slots.append({'c': 'SlotNonRetarded', 'p': 1, 'i': i, 'x': v[0] + i * 18, 'y': v[2]})

    def grid(self, cls, a):
        ints = [x for x in a if self.isint(x)]
        v = [self.ev(x) for x in ints]
        if len(v) == 5:
            v.append(18)
        frm, x, y, rows, cols, size = v
        for r in range(rows):
            for c in range(cols):
                self.slots.append({'c': cls, 'p': 0, 'i': c + r * cols + frm, 'x': x + c * size, 'y': y + r * size})

    def run(self, s):
        i = 0
        while i < len(s):
            i = self.stmt(s, i, True)

    @staticmethod
    def skip_ws(s, i):
        while i < len(s) and s[i].isspace():
            i += 1
        return i

    def stmt(self, s, i, live):
        i = self.skip_ws(s, i)
        if i >= len(s):
            return i
        if s[i] == '{':
            j = match_paren(s, i, '{', '}')
            if live:
                self.run(s[i + 1:j])
            return j + 1
        m = re.match(r'for\s*\(', s[i:])
        if m:
            p = i + m.end() - 1
            q = match_paren(s, p)
            head = s[p + 1:q].split(';')
            body_start = q + 1
            end = self.stmt(s, body_start, False)
            if live:
                try:
                    im = re.match(r'\s*(?:(?:int|byte|short)\s+)?(\w+)\s*=\s*(.+)', head[0])
                    var = im.group(1)
                    self.env[var] = self.ev(im.group(2))
                    n = 0
                    while self.ev(head[1]) and n < 1000:
                        self.stmt(s, body_start, True)
                        u = head[2].strip()
                        if u in (var + '++', '++' + var):
                            self.env[var] += 1
                        elif u == var + '--':
                            self.env[var] -= 1
                        else:
                            um = re.match(r'%s\s*\+=\s*(.+)' % var, u)
                            self.env[var] += self.ev(um.group(1))
                        n += 1
                except Exception as ex:
                    self.warn.append('for loop: %s (%s)' % (s[p + 1:q].strip(), ex))
            return end
        m = re.match(r'if\s*\(', s[i:])
        if m:
            p = i + m.end() - 1
            q = match_paren(s, p)
            cond = s[p + 1:q]
            val = None
            if live:
                try:
                    val = bool(self.ev(cond))
                except Exception:
                    self.warn.append('conditional: if(%s) - branches skipped' % cond.strip())
            end = self.stmt(s, q + 1, bool(live and val))
            k = self.skip_ws(s, end)
            if s[k:k + 4] == 'else' and not s[k + 4:k + 5].isalnum():
                end = self.stmt(s, k + 4, bool(live and val is False))
            return end
        j = i
        d = 0
        while j < len(s):
            if s[j] in '({':
                d += 1
            elif s[j] in ')}':
                d -= 1
            elif s[j] == ';' and d == 0:
                break
            j += 1
        st = s[i:j].strip()
        if live and st:
            self.expr(st)
        return j + 1

    def expr(self, st):
        m = re.search(r'addSlotToContainer\s*\(\s*new\s+([\w.]+)\s*\(', st)
        if m:
            p = m.end() - 1
            q = match_paren(st, p)
            self.add(m.group(1).split('.')[-1], split_args(st[p + 1:q]))
            return
        m = re.match(r'(?:this\.)?(playerInv|addSlots|addOutputSlots|addTakeOnlySlots)\s*\(', st)
        if m:
            p = m.end() - 1
            q = match_paren(st, p)
            a = split_args(st[p + 1:q])
            try:
                if m.group(1) == 'playerInv':
                    self.player_inv(a)
                else:
                    self.grid({'addSlots': 'SlotNonRetarded', 'addOutputSlots': 'SlotCraftingOutput', 'addTakeOnlySlots': 'SlotTakeOnly'}[m.group(1)], a)
            except Exception as ex:
                self.warn.append('helper %s: %s' % (st, ex))
            return
        m = re.match(r'(?:final\s+)?(?:int|byte|short)\s+(\w+)\s*=\s*(.+)', st)
        if m:
            try:
                self.env[m.group(1)] = self.ev(m.group(2))
            except Exception:
                self.warn.append('unresolved local: ' + st)
            return
        m = re.match(r'(\w+)\s*(\+?=)\s*(.+)', st)
        if m and m.group(1) in self.env:
            try:
                v = self.ev(m.group(3))
                self.env[m.group(1)] = self.env[m.group(1)] + v if m.group(2) == '+=' else v
            except Exception:
                self.warn.append('unresolved assign: ' + st)
            return
        if 'addSlot' in st:
            self.warn.append('unparsed slot statement: ' + st[:120])


def ctor_bodies(src, cls):
    out = []
    for m in re.finditer(r'public\s+%s\s*\(' % re.escape(cls), src):
        p = m.end() - 1
        q = match_paren(src, p)
        b = src.index('{', q)
        e = match_paren(src, b, '{', '}')
        out.append((src[p + 1:q], src[b + 1:e]))
    return out


def transfer_summary(src):
    m = re.search(r'ItemStack\s+transferStackInSlot\s*\(', src)
    if not m:
        return None
    b = src.index('{', m.end())
    e = match_paren(src, b, '{', '}')
    return re.sub(r'\s+', ' ', src[b:e + 1])


containers = {}
cdir = os.path.join(SRC, 'container')
for fn in sorted(os.listdir(cdir)):
    if not fn.endswith('.java'):
        continue
    cls = fn[:-5]
    src = strip_comments(open(os.path.join(cdir, fn), encoding='utf-8', errors='replace').read())
    sup = re.search(r'class\s+%s(?:<[^>]*>)?\s+extends\s+([\w.]+)' % cls, src)
    fields = {}
    for fm in re.finditer(r'(?:static\s+final|final\s+static)\s+int\s+(\w+)\s*=\s*(-?\d+)\s*;', src):
        fields[fm.group(1)] = int(fm.group(2))
    entry = {'super': sup.group(1) if sup else None, 'ctors': []}
    for params, body in ctor_bodies(src, cls):
        it = Interp(cls, fields)
        it.run(body)
        sm = re.search(r'super\s*\(([^;]*)\)\s*;', body)
        entry['ctors'].append({'params': re.sub(r'\s+', ' ', params), 'slots': it.slots, 'warn': it.warn, 'superArgs': sm.group(1) if sm else None})
    entry['transfer'] = transfer_summary(src)
    containers[cls] = entry

guis = {}
gdir = os.path.join(SRC, 'gui')
for fn in sorted(os.listdir(gdir)):
    if not fn.endswith('.java'):
        continue
    cls = fn[:-5]
    src = strip_comments(open(os.path.join(gdir, fn), encoding='utf-8', errors='replace').read())
    tex = re.findall(r'(textures/gui/[\w/.\-]+\.png)', src)
    xs = re.findall(r'\bxSize\s*=\s*(\d+)', src)
    ys = re.findall(r'\bySize\s*=\s*(\d+)', src)
    cont = re.findall(r'new\s+(Container\w+)\s*\(', src)
    sup = re.search(r'class\s+%s(?:<[^>]*>)?\s+extends\s+([\w.]+)' % cls, src)
    guis[cls] = {'super': sup.group(1) if sup else None, 'textures': sorted(set(tex)), 'xSize': [int(v) for v in xs], 'ySize': [int(v) for v in ys], 'containers': sorted(set(cont))}

json.dump({'source': 'HbmMods/Hbm-s-Nuclear-Tech-GIT@936c60c4c07dc10aef81f75d800ad04282f08e62', 'containers': containers, 'guis': guis}, open(OUT, 'w', encoding='utf-8'), indent=0, sort_keys=True)
nw = sum(1 for c in containers.values() for k in c['ctors'] if k['warn'])
print('containers', len(containers), 'guis', len(guis), 'ctors with warnings', nw)
for n, c in containers.items():
    for k in c['ctors']:
        for w in k['warn']:
            print(' ', n, '|', w)
    if not c['ctors']:
        print(' ', n, '| no ctor')
