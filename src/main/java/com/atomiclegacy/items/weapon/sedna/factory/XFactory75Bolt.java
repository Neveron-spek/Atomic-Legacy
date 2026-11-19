package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * [weapons batch E1] 1.7.10 XFactory75Bolt: b75, b75_inc, b75_exp and gun_bolter. The three rounds keep the
 * BulletConfig defaults (velocity 10, life 30, no gravity, standard ricochet and entity hit); the damage and
 * piercing values are those of the ammo_standard B75 types.
 */
public class XFactory75Bolt {

	/** LAMBDA_TINY_EXPLODE: shooter protection for 3 ticks, Lego.tinyExplode(bullet, mop, 2F). */
	public static final BulletImpact LAMBDA_TINY_EXPLODE = (bullet, hit) -> XFactory50.standardExplode(bullet,
			hit instanceof EntityHitResult ehr ? ehr.getEntity() : null, hit.getLocation(),
			hit instanceof BlockHitResult bhr ? bhr.getDirection() : null, bullet.baseDamage());
	/** LAMBDA_INC: living entities get HbmLivingProps.phosphorus raised to at least 300, the standard hit follows. */
	public static final BulletImpact LAMBDA_INC = XFactory12ga.LAMBDA_PHOSPHORUS;
	/** LAMBDA_STANDARD_EXPLODE: Lego.standardExplode(bullet, mop, 5F), the bullet dies (no shooter protection). */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE = (bullet, hit) -> {
		XFactory12ga.standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), 5F, 1F);
		return true;
	};

	static {
		StandardAmmoType.B75.setOnImpact(LAMBDA_TINY_EXPLODE);
		StandardAmmoType.B75_INC.setOnImpact(LAMBDA_INC);
		StandardAmmoType.B75_EXP.setOnImpact(LAMBDA_STANDARD_EXPLODE);
	}

	// casing75 = SpentCasing(STRAIGHT).setColor(COLOR_CASE_BRASS).setScale(2F, 2F, 1.5F), registered as "b75", "b75inc", "b75exp"
	public static final BulletConfig b75 = new BulletConfig(StandardAmmoType.B75).setCasingEffect(OrchestrasE1.CASING_B75);
	public static final BulletConfig b75_inc = new BulletConfig(StandardAmmoType.B75_INC).setCasingEffect(OrchestrasE1.CASING_B75);
	public static final BulletConfig b75_exp = new BulletConfig(StandardAmmoType.B75_EXP).setCasingEffect(OrchestrasE1.CASING_B75);

	public static ItemGunBaseNT createBolter(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SPECIAL, new GunConfig()
				.dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::bolterDamage).delay(2).auto(true).spread(0.005F).reload(40).jam(55).sound(HbmSoundEvents.GUN_POWDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 30).addConfigs(b75, b75_inc, b75_exp))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_BOLT))
				.setupStandardConfiguration()
				.anim(LAMBDA_BOLTER_ANIMS).orchestra(OrchestrasE1.ORCHESTRA_BOLTER)
				).setDefaultAmmo(StandardAmmoType.B75, 15);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
		Lego.handleStandardSmoke(ctx.entity, stack, 2000, 0.05D, 1.1D, 0);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_BOLT = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_BOLTER_ANIMS = (stack, type) -> {
		switch(type) {
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(1, 0, 0, 25).addPos(0, 0, 0, 75));
		case RELOAD: return new BusAnimation()
				.addBus("TILT", new BusAnimationSequence().addPos(1, 0, 0, 250).addPos(1, 0, 0, 1500).addPos(0, 0, 0, 250))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 1, 500).addPos(1, 0, 1, 500).addPos(0, 0, 0, 500));
		case JAMMED: return new BusAnimation()
				.addBus("TILT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250).addPos(1, 0, 0, 700).addPos(0, 0, 0, 250))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(0.6, 0, 0, 250).addPos(0, 0, 0, 250));
		}

		return null;
	};
}
