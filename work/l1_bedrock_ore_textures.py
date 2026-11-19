# Bakes ItemBedrockOreNew#registerIcons (TextureAtlasSpriteMutatable + RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, type.light, type.dark))
# into static textures: textures/item/bedrock_ore_mutated/<prefix>_<suffix>.png. Run from the l1 root.
from PIL import Image
T='src/main/resources/assets/atomiclegacy/textures/item/'
TYPES={'light':(0xFFFFFF,0x353535),'heavy':(0x868686,0x000000),'rare':(0xE6E6B6,0x1C1C00),
       'actinide':(0xC1C7BD,0x2B3227),'nonmetal':(0xAFAFAF,0x0F0F0F),'crystal':(0xE2FFFA,0x1E8A77)}
PREFIXES=['base','primary','crumbs','sulfuric','solvent','rad']
def comp(c,s): return (c>>s)&0xff
def shift(light,dark,bl,bd,v):
    pos=(v-bl)/(bd-bl)  # getPosFromComp(boundLighter, boundDarker, comp)
    return light+pos*(dark-light)   # getCompFromFunc(lighter, darker, interp)
for p in PREFIXES:
    for s,(light,dark) in TYPES.items():
        im=Image.open(T+'bedrock_ore_%s_%s.png'%(p,s)).convert('RGBA'); px=im.load()
        for x in range(im.width):
            for y in range(im.height):
                r,g,b,a=px[x,y]
                out=[int(shift(comp(light,sh),comp(dark,sh),comp(0xFFFFFF,sh),comp(0x505050,sh),v))&0xff for v,sh in ((r,16),(g,8),(b,0))]
                px[x,y]=(out[0],out[1],out[2],a)
        im.save(T+'bedrock_ore_mutated/%s_%s.png'%(p,s))
