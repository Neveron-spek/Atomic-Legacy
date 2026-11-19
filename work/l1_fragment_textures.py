# Bakes ItemAutogen(FRAGMENT)#registerIcons for bedrock_ore_fragment_<mat>: TextureAtlasSpriteMutatable over
# items/bedrock_ore_fragment with RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, solidColorLight, solidColorDark)
# (Mats.java). Run from the l1 root; original sources in ../../NeoNTM/work/original.
import re, os, json
from PIL import Image
ORIG='C:/Users/Admin/Documents/ChatGPT/NeoNTM/work/original/src/main/java/com/hbm/inventory/material/Mats.java'
A='src/main/resources/assets/atomiclegacy/'
mats={}
for l in open(ORIG,encoding='utf-8'):
    m=re.search(r'MAT_(\w+)\s*=\s*make(?:Non)?Smeltable\(([^,]+),\s*\w+,\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+)\)',l)
    if m: mats[m.group(1).lower()]=(int(m.group(3),16),int(m.group(4),16),int(m.group(5),16))
def comp(c,s): return (c>>s)&0xff
def shift(light,dark,bl,bd,v): return light+(v-bl)/(bd-bl)*(dark-light)
src=Image.open(A+'textures/item/bedrock_ore_fragment.png').convert('RGBA')
for f in sorted(os.listdir(A+'models/item')):
    m=re.fullmatch(r'bedrock_ore_fragment_(\w+)\.json',f)
    if not m: continue
    name=m.group(1)
    if name=='bismuth': continue  # textureOverrides: bedrock_ore_fragment_bismuth
    light,dark,molten=mats[name]
    if light==dark: print('no autogen icon', name); continue
    im=src.copy(); px=im.load()
    for x in range(im.width):
        for y in range(im.height):
            r,g,b,a=px[x,y]
            px[x,y]=tuple(int(shift(comp(light,s),comp(dark,s),comp(0xFFFFFF,s),comp(0x505050,s),v))&0xff for v,s in ((r,16),(g,8),(b,0)))+(a,)
    im.save(A+'textures/item/material/bedrock_ore_fragment_%s.png'%name)
    json.dump({'parent':'minecraft:item/generated','textures':{'layer0':'atomiclegacy:item/material/bedrock_ore_fragment_%s'%name}},open(A+'models/item/'+f,'w'),indent=2)
    print('baked',name)
