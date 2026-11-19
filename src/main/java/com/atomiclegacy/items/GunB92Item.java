package com.atomiclegacy.items;

import java.util.List;

import com.atomiclegacy.entity.B92BeamEntity;
import com.atomiclegacy.entity.SoliniumCloudEntity;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * [parity c1] 1.7.10 com.hbm.items.weapon.GunB92 (old, non-sedna gun). The stack NBT ints "animation" and "energy" are
 * kept under the same names in CUSTOM_DATA. Adaptations: the charge/animation counters are advanced on the server only
 * (1.7.10 ran onUpdate on both sides and the server copy won); damageItem(1) had no effect (no max damage) and is
 * dropped; the Forge ArrowNock/ArrowLoose events (whose cancellation the original ignored) are not posted. The first
 * tooltip lines depend on MainRegistry.polaroidID (AtomicLegacy.polaroidID, parity J1).
 */
public class GunB92Item extends Item {

	private final RandomSource rand = RandomSource.create();

	public GunB92Item(Properties properties) {
		super(properties.stacksTo(1).rarity(Rarity.UNCOMMON)
				.attributes(ItemAttributeModifiers.builder()
						.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3.5D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
						.build()));
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity living, int timeLeft) {
		if(!(living instanceof Player player) || player.isShiftKeyDown()) return;
		int j = this.getUseDuration(stack, living) - timeLeft;

		if(j < 10.0D) {
			return;
		}

		if(!world.isClientSide) {
			for(int i = 0; i < getPower(stack); i++) {
				B92BeamEntity beam = new B92BeamEntity(world, player, 3.0F);

				float divergence = i * 0.2F;

				if(divergence > 1F)
					divergence = 1F;

				if(i > 0) {
					beam.setDeltaMovement(beam.getDeltaMovement().add(rand.nextGaussian() * divergence, rand.nextGaussian() * divergence, rand.nextGaussian() * divergence));
				}

				world.addFreshEntity(beam);
			}

			world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.WEAPON_SPARK_SHOOT.get(), SoundSource.PLAYERS, 5.0F, 1.0F);

			setAnim(stack, 1);
			setPower(stack, 0);
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
		if(world.isClientSide) return;
		int j = getAnim(stack);

		if(j > 0) {
			if(j < 30)
				setAnim(stack, j + 1);
			else
				setAnim(stack, 0);

			if(j == 15) {
				world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundEvents.WEAPON_B92_RELOAD.get(), SoundSource.PLAYERS, 2F, 0.9F);
				setPower(stack, getPower(stack) + 1);

				if(getPower(stack) > 10) {

					setPower(stack, 0);
					fleija((ServerLevel) world, entity.getX(), entity.getY(), entity.getZ(), 50);
				}
			}
		}
	}

	/**
	 * EntityNukeExplosionMK3.statFacFleija(world, x, y, z, range) with random.explode (100, 0.9-1.0) and an
	 * EntityCloudFleijaRainbow of the same size; nothing happens next to a field disturber (the entity came back dead).
	 */
	public static boolean fleija(ServerLevel world, double x, double y, double z, int range) {
		if(NukeExplosionManager.isFleijaSuppressed(world, BlockPos.containing(x, y, z))) return false;
		NukeExplosionManager.scheduleFleijaNukeWithoutCloud(world, x, y, z, range);
		world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 100.0F, world.random.nextFloat() * 0.1F + 0.9F);
		SoliniumCloudEntity cloud = HbmEntityTypes.SOLINIUM_CLOUD.get().create(world);
		if(cloud != null) {
			cloud.setRainbow(true);
			cloud.initialize(range);
			cloud.moveTo(x, y, z);
			world.addFreshEntity(cloud);
		}
		return true;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if(!player.isShiftKeyDown() && getPower(stack) > 0) {
			if(getAnim(stack) == 0) {
				player.startUsingItem(hand);
				return InteractionResultHolder.consume(stack);
			}
		} else {
			if(getAnim(stack) == 0 && !world.isClientSide) {
				setAnim(stack, 1);
			}
		}

		return InteractionResultHolder.pass(stack);
	}

	@Override
	public int getEnchantmentValue() {
		return 1;
	}

	/** 1.7.10 ItemRenderer kept the equipped item when only the NBT changed. */
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || oldStack.getItem() != newStack.getItem();
	}

	@Override
	public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
		return oldStack.getItem() != newStack.getItem();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		if(com.atomiclegacy.AtomicLegacy.polaroidID == 11) {
			list.add(Component.literal("A weapon that came from the stars."));
			list.add(Component.literal("It screams for murder."));
		} else if(com.atomiclegacy.AtomicLegacy.polaroidID == 18) {
			list.add(Component.literal("One could turn the gun into a bomb"));
			list.add(Component.literal("by overloading the capacitors..."));
		} else {
			list.add(Component.literal("Stay away from me compootur!"));
		}
		list.add(Component.literal(""));
		list.add(Component.literal("Projectiles explode on impact."));
		list.add(Component.literal("Sneak while holding the right mouse button"));
		list.add(Component.literal("to charge additional energy."));
		list.add(Component.literal("The more energy is stored, the less accurate"));
		list.add(Component.literal("the beams become."));
		list.add(Component.literal("Only up to ten charges may be stored."));
		list.add(Component.literal(""));
		list.add(Component.literal("\"It's nerf or nothing!\""));
		list.add(Component.literal(""));
		list.add(Component.literal("[LEGENDARY WEAPON]"));
	}

	static int getInt(ItemStack stack, String key) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? 0 : data.copyTag().getInt(key);
	}

	static void setInt(ItemStack stack, String key, int value) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if(data != null && data.contains(key) && data.copyTag().getInt(key) == value) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, value));
	}

	public static int getAnim(ItemStack stack) { return getInt(stack, "animation"); }
	public static void setAnim(ItemStack stack, int i) { setInt(stack, "animation", i); }
	public static int getPower(ItemStack stack) { return getInt(stack, "energy"); }
	public static void setPower(ItemStack stack, int i) { setInt(stack, "energy", i); }

	public static float getRotationFromAnim(ItemStack stack) {
		float rad = 0.0174533F;
		rad *= 7.5F;
		int i = getAnim(stack);

		if(i < 10)
			return 0;
		i -= 10;

		if(i < 6)
			return rad * i;
		if(i > 14)
			return rad * (5 - (i - 15));
		return rad * 5;
	}

	public static float getTransFromAnim(ItemStack stack) {
		float i = getAnim(stack);

		if(i < 10)
			return 0;
		i -= 10;

		if(i > 4 && i < 10)
			return (i - 5) * 0.05F;

		if(i > 9 && i < 15)
			return (10 * 0.05F) - ((i - 5) * 0.05F);

		return 0;
	}
}
