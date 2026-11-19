package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyCrateLogic;
import com.atomiclegacy.blocks.rail.IRailNTM;
import com.atomiclegacy.blocks.rail.IRailNTM.MoveContext;
import com.atomiclegacy.blocks.rail.IRailNTM.RailCheckType;
import com.atomiclegacy.blocks.rail.IRailNTM.RailContext;
import com.atomiclegacy.blocks.rail.RailDummyableBlock;
import com.atomiclegacy.blocks.rail.RailStandardSwitchBlock;
import com.atomiclegacy.entity.cart.MinecartCrateEntity;
import com.atomiclegacy.entity.cart.MinecartDestroyerEntity;
import com.atomiclegacy.entity.cart.NtmMinecartEntity;
import com.atomiclegacy.items.LegacyItemComponents;
import com.atomiclegacy.items.ModMinecartItem;
import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.items.ModMinecartItem.EnumMinecart;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.train.BoundingBoxDummyEntity;
import com.atomiclegacy.train.CargoTramEntity;
import com.atomiclegacy.train.CargoTramTrailerEntity;
import com.atomiclegacy.train.LegacyVec3;
import com.atomiclegacy.train.RailCarEntity;
import com.atomiclegacy.train.TrainItem;
import com.atomiclegacy.train.TrainMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * [parity S1] Dedicated-server checks for the train / cart batch against the pinned 1.7.10 sources: ModBlocks rail
 * properties, BlockDummyable footprints and removal, RailStandardSwitch / Buffer / Ramp / Curve travel, TrainCargoTram
 * movement with the passive brake, coupling into one LogicalTrainUnit, cargo inventories, the electric charge slot,
 * ItemModMinecart variants, EntityMinecartDestroyer filters, EntityMinecartCrate drops and the ToolRecipes cart recipes.
 * Block pads near x = 27000, y = 200; entities in the spawn chunks at y = 250.
 * Not covered: rendering, GUIs, the trainInv key, client interpolation.
 */
final class ParityS1Diagnostics {

    private ParityS1Diagnostics() {
    }

    private static final int BASE_X = 27_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 27_000;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();
    private static final List<Entity> SPAWNED = new ArrayList<>();

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            properties(problems, cases);
            footprints(level, problems, cases);
            travel(level, problems, cases);
            recipes(server, problems, cases);
            items(problems, cases);
            trains(level, problems, cases);
            carts(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity S1 diagnostics crashed", e);
            problems.add("S1 diagnostics crashed: " + e);
        } finally {
            List<Entity> dummies = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof BoundingBoxDummyEntity dummy && SPAWNED.contains(dummy.train)) dummies.add(entity);
            }
            for (Entity entity : dummies) entity.discard();
            for (Entity entity : SPAWNED) entity.discard();
            SPAWNED.clear();
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    private static final String[] RAILS = {"rail_narrow_straight", "rail_narrow_curve", "rail_large_straight", "rail_large_straight_short",
            "rail_large_curve", "rail_large_curve_7", "rail_large_curve_9", "rail_large_ramp", "rail_large_buffer", "rail_large_switch",
            "rail_large_switch_flipped"};
    /** Block count of each footprint: BlockDummyable dimensions plus the hand-written checkRequirement/fillSpace parts. */
    private static final int[] FOOTPRINT = {5, 25, 10, 2, 14, 22, 27, 20, 10, 56, 56};

    /// ModBlocks: setHardness(5.0F).setResistance(10.0F), Material.iron, default step sound, setCreativeTab(null) ///
    private static void properties(List<String> problems, int[] cases) {
        for (String id : RAILS) {
            Block block = block(id);
            check(block instanceof RailDummyableBlock, problems, cases, id + " is a BlockDummyable rail");
            check(block.defaultDestroyTime() == 5.0F, problems, cases, id + " hardness " + block.defaultDestroyTime());
            check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
            check(block.defaultBlockState().requiresCorrectToolForDrops(), problems, cases, id + " Material.iron needs a tool");
            check(block.defaultBlockState().getSoundType() == SoundType.STONE, problems, cases, id + " uses the default stone step sound");
            check(block.asItem() != Items.AIR, problems, cases, id + " has its ItemBlock");
            check(((IRailNTM) block).getGauge(null, 0, 0, 0) == (id.startsWith("rail_narrow") ? IRailNTM.TrackGauge.NARROW : IRailNTM.TrackGauge.STANDARD),
                    problems, cases, id + " gauge");
        }
    }

    /// onBlockPlacedBy + fillSpace, then breakBlock / destroyIfOrphan ///
    private static void footprints(ServerLevel level, List<String> problems, int[] cases) {
        for (int i = 0; i < RAILS.length; i++) {
            Block block = block(RAILS[i]);
            BlockPos pos = pad(level);
            place(level, block, pos, 0F);
            int count = count(level, block, pos);
            checkEq(count, FOOTPRINT[i], problems, cases, RAILS[i] + " footprint (yaw 0)");
            BlockPos core = core(level, block, pos);
            check(core != null && RailDummyableBlock.meta(level, core.getX(), core.getY(), core.getZ()) == Direction.NORTH.get3DDataValue() + 10,
                    problems, cases, RAILS[i] + " core meta = NORTH + offset");
            if (core != null) {
                // break one dummy: the chain to the core and every orphan go with it
                BlockPos dummy = null;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-16, -1, -16), pos.offset(16, 2, 16))) {
                    if (level.getBlockState(p).getBlock() == block && !p.equals(core)) {
                        dummy = p.immutable();
                        break;
                    }
                }
                if (dummy != null) level.removeBlock(dummy, false);
                checkEq(count(level, block, pos), 0, problems, cases, RAILS[i] + " fully removed after breaking a dummy");
            }
        }
        // occupied space: the placement is refunded and nothing is placed
        BlockPos pos = pad(level);
        level.setBlock(pos.offset(0, 0, 3), Blocks.STONE.defaultBlockState(), 3);
        place(level, block("rail_large_straight"), pos, 0F);
        checkEq(count(level, block("rail_large_straight"), pos), 0, problems, cases, "rail_large_straight refuses an obstructed footprint");
    }

    /// RailStandardCurveBase / Ramp / Buffer / Switch geometry ///
    private static void travel(ServerLevel level, List<String> problems, int[] cases) {
        // curve, dir NORTH: axis (cX + 5, cZ), radius 4, entry at (cX + 1, cZ)
        BlockPos pos = pad(level);
        place(level, block("rail_large_curve"), pos, 0F);
        LegacyVec3 end = RailCarEntity.getRelPosAlongRail(pos, 6.0D, IRailNTM.TrackGauge.STANDARD, level,
                LegacyVec3.of(pos.getX() + 1.0D, pos.getY() + 0.1875D, pos.getZ()), 0F, new MoveContext(RailCheckType.CORE, 0));
        double ex = pos.getX() + 5 - 4 * Math.cos(1.5D);
        double ez = pos.getZ() + 4 * Math.sin(1.5D);
        check(end != null && Math.abs(end.xCoord - ex) < 0.01D && Math.abs(end.zCoord - ez) < 0.01D && Math.abs(end.yCoord - (pos.getY() + 0.1875D)) < 1.0E-6D,
                problems, cases, "6 blocks along rail_large_curve end on the radius-4 arc (" + end + ", expected " + ex + ", " + ez + ")");

        // curve_9: radius 8, a quarter is 4 pi long; overshooting past the end runs into air -> null
        BlockPos pos9 = pad(level);
        place(level, block("rail_large_curve_9"), pos9, 0F);
        LegacyVec3 past = RailCarEntity.getRelPosAlongRail(pos9, 8 * Math.PI / 2 + 0.5D, IRailNTM.TrackGauge.STANDARD, level,
                LegacyVec3.of(pos9.getX() + 1.0D, pos9.getY() + 0.1875D, pos9.getZ()), 0F, new MoveContext(RailCheckType.CORE, 0));
        check(past == null, problems, cases, "leaving rail_large_curve_9 into air derails (null)");

        // ramp, dir EAST: rises one block over five towards -X, y = cY + (cX + 0.5 - x + 2.5) / 5
        BlockPos ramp = pad(level);
        place(level, block("rail_large_ramp"), ramp, 90F);
        BlockPos rampCore = core(level, block("rail_large_ramp"), ramp);
        if (rampCore != null) {
            IRailNTM rail = (IRailNTM) block("rail_large_ramp");
            LegacyVec3 mid = rail.getTravelLocation(level, rampCore.getX(), rampCore.getY(), rampCore.getZ(), rampCore.getX() + 0.5D, rampCore.getY(), rampCore.getZ() + 1.0D,
                    1, 0, 0, 0, new RailContext(), new MoveContext(RailCheckType.CORE, 0));
            check(Math.abs(mid.yCoord - (rampCore.getY() + 0.5D + 0.1875D)) < 1.0E-9D, problems, cases, "rail_large_ramp centre height cY + 0.5 + 0.1875, got " + mid.yCoord);
            check(Math.abs(mid.zCoord - (rampCore.getZ() + 1.0D)) < 1.0E-9D, problems, cases, "rail_large_ramp track between core and extra column");
        } else {
            check(false, problems, cases, "rail_large_ramp core found");
        }

        // buffer, dir EAST: stop window [cX + 1 + bogie, cX + 3]; dir NORTH keeps the original x/z comparison slip
        BlockPos buffer = pad(level);
        place(level, block("rail_large_buffer"), buffer, 90F);
        BlockPos bc = core(level, block("rail_large_buffer"), buffer);
        IRailNTM bufferRail = (IRailNTM) block("rail_large_buffer");
        if (bc != null) {
            MoveContext inside = new MoveContext(RailCheckType.FRONT, 0);
            bufferRail.getTravelLocation(level, bc.getX(), bc.getY(), bc.getZ(), bc.getX() + 2.0D, bc.getY(), bc.getZ() + 1.0D, 1, 0, 0, 0.1D, new RailContext(), inside);
            MoveContext outside = new MoveContext(RailCheckType.FRONT, 0);
            bufferRail.getTravelLocation(level, bc.getX(), bc.getY(), bc.getZ(), bc.getX() + 0.5D, bc.getY(), bc.getZ() + 1.0D, 1, 0, 0, 0.1D, new RailContext(), outside);
            check(!inside.collision && outside.collision, problems, cases, "rail_large_buffer (EAST) collides only outside its stop window");
        }
        BlockPos bufferN = pad(level);
        place(level, block("rail_large_buffer"), bufferN, 0F);
        BlockPos bn = core(level, block("rail_large_buffer"), bufferN);
        if (bn != null) {
            MoveContext ctx = new MoveContext(RailCheckType.FRONT, 0);
            bufferRail.getTravelLocation(level, bn.getX(), bn.getY(), bn.getZ(), bn.getX() + 1.0D, bn.getY(), bn.getZ() + 1.5D, 0, 0, 1, 0.1D, new RailContext(), ctx);
            check(ctx.collision, problems, cases, "rail_large_buffer (NORTH) compares the Z clamp with xCoord like 1.7.10");
        }

        // switch, dir EAST: past the junction only the selected RailDef can be crossed; right click toggles the core
        BlockPos sw = pad(level);
        RailStandardSwitchBlock switchBlock = (RailStandardSwitchBlock) block("rail_large_switch");
        place(level, switchBlock, sw, 90F);
        BlockPos sc = core(level, switchBlock, sw);
        if (sc != null) {
            LegacyVec3 from = LegacyVec3.of(sc.getX() + 8.5D, sc.getY(), sc.getZ() + 0.5D);
            LegacyVec3 to = LegacyVec3.of(sc.getX() + 7.5D, sc.getY(), sc.getZ() + 0.5D);
            RailStandardSwitchBlock.RailDef main = switchBlock.railDefs.get(0);
            RailStandardSwitchBlock.RailDef side = switchBlock.railDefs.get(1);
            check(switchBlock.canCross(level, sc.getX(), sc.getY(), sc.getZ(), from, to, main)
                    && !switchBlock.canCross(level, sc.getX(), sc.getY(), sc.getZ(), from, to, side), problems, cases, "unswitched rail_large_switch only allows the main line");
            FakePlayer fake = FakePlayerFactory.getMinecraft(level);
            fake.setShiftKeyDown(false);
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            BlockPos far = sc.offset(-6, 0, 0);
            level.getBlockState(far).useWithoutItem(level, fake, new BlockHitResult(Vec3.atCenterOf(far), Direction.UP, far, false));
            check(RailStandardSwitchBlock.isSwitched(level, sc), problems, cases, "clicking any part toggles TileEntityRailSwitch.isSwitched on the core");
            check(!switchBlock.canCross(level, sc.getX(), sc.getY(), sc.getZ(), from, to, main)
                    && switchBlock.canCross(level, sc.getX(), sc.getY(), sc.getZ(), from, to, side), problems, cases, "switched rail_large_switch only allows the side line");
        }
    }

    /// ToolRecipes 157-169 ///
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        var manager = server.getRecipeManager();
        Object[][] rows = {{"cart_wood_empty", EnumCartBase.WOOD, EnumMinecart.EMPTY}, {"cart_steel_empty", EnumCartBase.STEEL, EnumMinecart.EMPTY},
                {"cart_painted_empty", EnumCartBase.PAINTED, EnumMinecart.EMPTY}, {"cart_steel_destroyer", EnumCartBase.STEEL, EnumMinecart.DESTROYER},
                {"cart_wood_powder", EnumCartBase.WOOD, EnumMinecart.POWDER}, {"cart_wood_semtex", EnumCartBase.WOOD, EnumMinecart.SEMTEX}};
        for (Object[] row : rows) {
            var holder = manager.byKey(AtomicLegacy.id((String) row[0]));
            ItemStack out = holder.map(h -> h.value().getResultItem(server.registryAccess())).orElse(ItemStack.EMPTY);
            check(out.is(HbmItems.CART.get()) && ModMinecartItem.getBaseType(out) == row[1] && ModMinecartItem.getType(out) == row[2],
                    problems, cases, "recipe " + row[0] + " result " + (out.isEmpty() ? "missing" : ModMinecartItem.getBaseType(out) + "/" + ModMinecartItem.getType(out)));
        }
        for (String shadowed : new String[] {"cart_painted_destroyer", "cart_steel_powder", "cart_painted_powder", "cart_steel_semtex", "cart_painted_semtex"}) {
            check(manager.byKey(AtomicLegacy.id(shadowed)).isEmpty(), problems, cases, shadowed + " is shadowed by the first registered recipe (NBT-blind ingredient) and absent");
        }
        var painted = manager.byKey(AtomicLegacy.id("cart_painted_empty"));
        check(painted.isPresent() && painted.get().value().getIngredients().stream()
                        .anyMatch(ing -> ing.test(ModMinecartItem.createCartItem(EnumCartBase.WOOD, EnumMinecart.EMPTY))),
                problems, cases, "the painted cart accepts any EMPTY cart (damage-only match)");
        var crate = manager.byKey(AtomicLegacy.id("cart_vanilla_crate"));
        if (crate.isPresent() && crate.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe) {
            ItemStack filled = new ItemStack(HbmBlocks.CRATE_STEEL.get());
            CompoundTag data = new CompoundTag();
            data.put("slot0", new ItemStack(Items.DIAMOND, 3).save(server.registryAccess()));
            LegacyCrateLogic.setData(filled, data);
            CraftingInput input = CraftingInput.of(1, 2, List.of(filled, new ItemStack(Items.MINECART)));
            check(recipe.matches(input, server.overworld()), problems, cases, "crate cart: crate_steel over a minecart");
            ItemStack result = recipe.assemble(input, server.registryAccess());
            check(ModMinecartItem.getType(result) == EnumMinecart.CRATE && LegacyCrateLogic.data(result).contains("slot0"),
                    problems, cases, "crate cart recipe copies the crate NBT (func_92100_c)");
        } else {
            check(false, problems, cases, "cart_vanilla_crate recipe present");
        }
    }

    /// ItemModMinecart / ItemTrain ///
    private static void items(List<String> problems, int[] cases) {
        checkEq(new ItemStack(HbmItems.CART.get()).getMaxStackSize(), 4, problems, cases, "cart max stack");
        checkEq(new ItemStack(HbmItems.TRAIN.get()).getMaxStackSize(), 1, problems, cases, "train max stack");
        ItemStack plain = new ItemStack(HbmItems.CART.get());
        check(ModMinecartItem.getBaseType(plain) == EnumCartBase.VANILLA && ModMinecartItem.getType(plain) == EnumMinecart.EMPTY,
                problems, cases, "cart without data is a VANILLA EMPTY cart");
        int variants = 0;
        for (EnumMinecart cart : EnumMinecart.values()) for (EnumCartBase base : EnumCartBase.values()) if (cart.supportsBase(base)) variants++;
        checkEq(variants, 12, problems, cases, "ItemModMinecart.getSubItems variants");
        checkEq(ModMinecartItem.createCartItem(EnumCartBase.PAINTED, EnumMinecart.SEMTEX).getOrDefault(LegacyItemComponents.CART_BASE.get(), -1), 3,
                problems, cases, "createCartItem stores cartBase");
        check(ModMinecartItem.createCartItem(EnumCartBase.STEEL, EnumMinecart.DESTROYER).getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("item.atomiclegacy.cart.destroyer"), problems, cases, "cart name item.cart.<type>");
    }

    /// TrainCargoTram / TrainCargoTramTrailer: movement, coupling, inventory, charge ///
    private static void trains(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos spawn = level.getSharedSpawnPos();
        int x0 = spawn.getX() - 12;
        int y = 250;
        int z0 = spawn.getZ() + 8;
        for (int dx = -5; dx <= 30; dx++) for (int dz = -2; dz <= 3; dz++) for (int dy = -1; dy <= 3; dy++) {
            level.setBlock(new BlockPos(x0 + dx, y + dy, z0 + dz), dy == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        Block straight = block("rail_large_straight");
        for (int i = 0; i < 5; i++) {
            // yaw 90 -> dir EAST, core = clicked x - 2, footprint x +-2, z core..core + 1
            place(level, straight, new BlockPos(x0 + i * 5 + 2, y, z0), 90F);
        }
        checkEq(countBox(level, straight, new BlockPos(x0 - 3, y, z0 - 1), new BlockPos(x0 + 23, y, z0 + 2)), 50, problems, cases, "5 chained rail_large_straight");

        CargoTramEntity tram = spawnTrain(level, TrainItem.EnumTrainType.CARGO_TRAM, x0 + 10.5D, y, z0 + 1.0D);
        check(tram != null, problems, cases, "ItemTrain places the cargo tram on standard gauge");
        if (tram == null) return;
        check(Math.abs(tram.getY() - (y + 0.1875D)) < 1.0E-6D && Math.abs(tram.getZ() - (z0 + 1.0D)) < 1.0E-6D, problems, cases,
                "tram snapped to the track centre line (" + tram.position() + ")");

        tram.tick(); // generates the LTU and the three bounding dummies
        check(tram.ltu != null && tram.ltu.trains().length == 1, problems, cases, "a lone tram is its own LogicalTrainUnit");
        checkEq(level.getEntitiesOfClass(BoundingBoxDummyEntity.class, tram.getBoundingBox().inflate(3)).size(), 3, problems, cases, "TrainCargoTram bounding dummies");

        // no driver: engineSpeed *= getPassivBrake() (0.95) every tick, max powered speed 0.5
        double startX = tram.getX();
        tram.engineSpeed = 0.4D;
        double expected = 0;
        double speed = 0.4D;
        for (int t = 0; t < 20; t++) {
            speed *= 0.95D;
            expected += speed;
            tram.tick();
            RailCarEntity.updateMotion(level);
        }
        double moved = Math.abs(tram.getX() - startX);
        check(tram.isOnRail && Math.abs(moved - expected) < 1.0E-3D, problems, cases,
                "tram coasted " + moved + " blocks in 20 ticks with the 0.95 passive brake, expected " + expected);
        check(Math.abs(tram.getZ() - (z0 + 1.0D)) < 1.0E-6D && Math.abs(tram.getY() - (y + 0.1875D)) < 1.0E-6D, problems, cases, "tram stays on the rail line");
        check(Math.abs(Math.abs(LegacyVec3.wrapAngleTo180(tram.getYRot())) - 90.0F) < 1.0E-3D, problems, cases, "tram yaw follows the X track (+-90), got " + tram.getYRot());

        // inventory and charge slot
        checkEq(tram.getContainerSize(), 29, problems, cases, "TrainCargoTram slots");
        checkEq(tram.getMaxPower(), 1000, problems, cases, "TrainCargoTram max power (10 * 100)");
        tram.setItem(28, new ItemStack(HbmItems.BATTERY_CREATIVE.get()));
        tram.tick();
        checkEq(tram.getPower(), 1000, problems, cases, "creative battery in slot 28 fills the tram");
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.getInventory().clearContent();
        fake.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 5));
        TrainMenus.CargoTram menu = new TrainMenus.CargoTram(0, fake.getInventory(), tram);
        menu.quickMoveStack(fake, 29); // first player inventory row
        check(tram.getItem(0).is(Items.COBBLESTONE), problems, cases, "shift-click moves non-batteries into slots 0-27");

        // coupling: a trailer whose back coupling meets the tram's coupling point (2.75 + 2.75)
        CargoTramTrailerEntity trailer = spawnTrain(level, TrainItem.EnumTrainType.CARGO_TRAM_TRAILER, tram.getX() + 5.5D, y, z0 + 1.0D);
        check(trailer != null, problems, cases, "ItemTrain places the cargo tram trailer");
        if (trailer == null) return;
        checkEq(trailer.getContainerSize(), 45, problems, cases, "TrainCargoTramTrailer slots");
        trailer.tick();
        tram.tick();
        RailCarEntity.updateMotion(level);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HbmItems.COUPLING_TOOL.get()));
        boolean coupled = tram.interactFirst(fake);
        check(coupled && (tram.coupledFront == trailer || tram.coupledBack == trailer) && (trailer.coupledFront == tram || trailer.coupledBack == tram),
                problems, cases, "coupling_tool couples tram and trailer within 1 block of their coupling points");
        tram.tick();
        trailer.tick();
        RailCarEntity.updateMotion(level);
        check(tram.ltu != null && tram.ltu == trailer.ltu && tram.ltu.trains().length == 2, problems, cases, "coupled cars share one LogicalTrainUnit");
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fake.getInventory().clearContent();
    }

    /// EntityMinecartNTM family ///
    private static void carts(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos spawn = level.getSharedSpawnPos();
        double x = spawn.getX() + 0.5D;
        double y = 251.0D;
        double z = spawn.getZ() - 10.5D;

        MinecartDestroyerEntity destroyer = (MinecartDestroyerEntity) ModMinecartItem.createMinecart(level, x, y, z,
                ModMinecartItem.createCartItem(EnumCartBase.STEEL, EnumMinecart.DESTROYER));
        level.addFreshEntity(destroyer);
        SPAWNED.add(destroyer);
        check(destroyer.getBase() == EnumCartBase.STEEL && destroyer.getContainerSize() == 18 && !destroyer.canPlaceItem(0, new ItemStack(Items.STONE)),
                problems, cases, "EntityMinecartDestroyer: 18 filter slots, steel base, no automated insertion");
        destroyer.setItem(9, new ItemStack(Items.STONE));
        ItemEntity junk = new ItemEntity(level, x + 1, y, z, new ItemStack(Items.STONE, 7));
        ItemEntity kept = new ItemEntity(level, x - 1, y, z, new ItemStack(Items.DIRT, 7));
        level.addFreshEntity(junk);
        level.addFreshEntity(kept);
        SPAWNED.add(junk);
        SPAWNED.add(kept);
        destroyer.tickCount = 5;
        destroyer.tick();
        check(junk.isRemoved() && !kept.isRemoved(), problems, cases, "destroyer deletes dropped items matching a wildcard filter every 5 ticks");

        MinecartCrateEntity crate = (MinecartCrateEntity) ModMinecartItem.createMinecart(level, x, y, z + 3, ModMinecartItem.createCartItem(EnumCartBase.VANILLA, EnumMinecart.CRATE));
        level.addFreshEntity(crate);
        SPAWNED.add(crate);
        checkEq(crate.getContainerSize(), 54, problems, cases, "EntityMinecartCrate slots (9 * 6)");
        check(crate.getDisplayBlockState().is(HbmBlocks.CRATE_STEEL.get()), problems, cases, "crate cart displays crate_steel");
        crate.setItem(4, new ItemStack(Items.GOLD_INGOT, 2));
        crate.destroy(level.damageSources().generic());
        ItemStack dropped = ItemStack.EMPTY;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(x - 3, y - 3, z, x + 3, y + 3, z + 6))) {
            if (item.getItem().is(HbmItems.CART.get())) dropped = item.getItem();
            SPAWNED.add(item);
        }
        check(ModMinecartItem.getType(dropped) == EnumMinecart.CRATE && LegacyCrateLogic.data(dropped).contains("slot4"),
                problems, cases, "a broken crate cart drops its contents inside the crate cart item");

        NtmMinecartEntity ore = ModMinecartItem.createMinecart(level, x, y, z, ModMinecartItem.createCartItem(EnumCartBase.WOOD, EnumMinecart.EMPTY));
        check(!ore.canBeRidden() && ore.getCartItem().is(HbmItems.CART.get()) && ModMinecartItem.getBaseType(ore.getCartItem()) == EnumCartBase.WOOD,
                problems, cases, "EntityMinecartOre is not rideable and drops its own wood cart");
    }

    @SuppressWarnings("unchecked")
    private static <T extends RailCarEntity> T spawnTrain(ServerLevel level, TrainItem.EnumTrainType type, double x, int y, double z) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setYRot(-90F);
        ItemStack stack = TrainItem.stackFor(HbmItems.TRAIN.get(), type);
        fake.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos rail = BlockPos.containing(x, y, z);
        UseOnContext context = new UseOnContext(fake, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(x, y + 0.125D, z), Direction.UP, rail, false));
        stack.getItem().useOn(context);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Class<? extends RailCarEntity> cls = type == TrainItem.EnumTrainType.CARGO_TRAM ? CargoTramEntity.class : CargoTramTrailerEntity.class;
        for (RailCarEntity car : level.getEntitiesOfClass(cls, new AABB(x - 1, y - 1, z - 1, x + 1, y + 2, z + 1))) {
            if (!SPAWNED.contains(car)) {
                SPAWNED.add(car);
                return (T) car;
            }
        }
        return null;
    }

    private static void place(ServerLevel level, Block block, BlockPos pos, float yaw) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setYRot(yaw);
        level.setBlock(pos, block.defaultBlockState(), 3);
        block.setPlacedBy(level, pos, level.getBlockState(pos), fake, new ItemStack(block));
        fake.getInventory().clearContent();
    }

    private static BlockPos core(ServerLevel level, Block block, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-16, -1, -16), pos.offset(16, 2, 16))) {
            if (level.getBlockState(p).getBlock() == block && RailDummyableBlock.meta(level, p.getX(), p.getY(), p.getZ()) >= 12) return p.immutable();
        }
        return null;
    }

    private static int count(ServerLevel level, Block block, BlockPos pos) {
        return countBox(level, block, pos.offset(-16, -1, -16), pos.offset(16, 2, 16));
    }

    private static int countBox(ServerLevel level, Block block, BlockPos from, BlockPos to) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(from, to)) if (level.getBlockState(p).getBlock() == block) n++;
        return n;
    }

    private static Block block(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("S1: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static BlockPos pad(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 6) * 48, BASE_Y, BASE_Z + (index / 6) * 48);
        for (int cx = (pos.getX() - 20) >> 4; cx <= (pos.getX() + 20) >> 4; cx++) {
            for (int cz = (pos.getZ() - 20) >> 4; cz <= (pos.getZ() + 20) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -18; dx <= 18; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -18; dz <= 18; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }
}
