package com.atomiclegacy.render.anim;

/** 1.7.10 com.hbm.render.anim.AnimationEnums; ordinals are sent over the network. */
public final class AnimationEnums {
    private AnimationEnums() {
    }

    public enum GunAnimation {
        RELOAD,			//either a full reload or start of a reload
        RELOAD_CYCLE,	//animation that plays for every individual round (for shotguns and similar single round loading weapons)
        RELOAD_END,		//animation for transitioning from our RELOAD_CYCLE to idle
        CYCLE,			//animation for every firing cycle
        CYCLE_EMPTY,	//animation for the final shot in the magazine
        CYCLE_DRY,		//animation for trying to fire, but no round is available
        ALT_CYCLE,		//animation for alt fire cycles
        SPINUP,			//animation for actionstart
        SPINDOWN,		//animation for actionend
        EQUIP,			//animation for drawing the weapon
        INSPECT,		//animation for inspecting the weapon
        JAMMED,			//animation for jammed weapons
    }

    public enum ToolAnimation {
        SWING,
        EQUIP,
    }
}
