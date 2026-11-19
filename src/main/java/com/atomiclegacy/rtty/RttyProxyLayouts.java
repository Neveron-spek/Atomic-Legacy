package com.atomiclegacy.rtty;

import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * [parity U2: ROR] Which dummy cells of a multiblock carried a TileEntityProxyCombo in 1.7.10.
 *
 * <p>Only a ProxyCombo dummy forwarded IRORValueProvider/IRORInteractive to its core, and BlockDummyable
 * created it only for the "extra" dummies (meta 6..11) set by makeExtra in fillSpace, unless the block
 * returned a ProxyCombo for every dummy (turrets, launch_pad_large). Offsets are the original
 * getAllPorts/makeExtra coordinates relative to the core: {@code core + nx*rot + nz*dir + (0, ny, 0)} with
 * {@code dir} the core facing and {@code rot = dir.getRotation(UP)} (= {@link Direction#getClockWise()}).
 * [parity b2: ROR] BlockDummyable.rotatePorts already subtracts {@code dir * getOffset()}, so getAllPorts cells added
 * to the placement point are core-relative; only raw makeExtra calls on the unshifted x/z needed re-basing
 * (heater_oilburner).</p>
 */
public final class RttyProxyLayouts {
    /** Every dummy cell is a proxy. */
    private static final int[][] ALL = new int[0][];
    /** No dummy cell is a proxy (createNewTileEntity returned null / another tile for dummies). */
    private static final int[][] NONE = new int[0][];
    /** Layouts written with absolute world axes (x, y, z) instead of rot/dir. */
    private static final Map<String, int[][]> ABSOLUTE = new HashMap<>();
    private static final Map<String, int[][]> LAYOUTS = new HashMap<>();

    static {
        // LaunchPad.fillSpace: makeExtra(x +- 1, y, z +- 1) in world axes
        ABSOLUTE.put("launch_pad", new int[][] {{1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}});
        LAYOUTS.put("launch_pad_large", ALL); // LaunchPadLarge: ProxyCombo().inventory() for every dummy
        LAYOUTS.put("launchpad_lambda", ports(new int[][] {{-2, 1, 7}, {-3, 1, 7}, {-4, 1, 7}, {-5, 1, 7}, {-6, 1, 7}}));
        LAYOUTS.put("launchpad_soyuz", ports(new int[][] {
                {1, 1, 2}, {-1, 1, 2}, {-7, 1, 2}, {-9, 1, 2},
                {1, 1, -10}, {-1, 1, -10}, {-7, 1, -10}, {-9, 1, -10},
                {-10, 1, 1}, {-10, 1, -1}, {-10, 1, -7}, {-10, 1, -9}}));
        LAYOUTS.put("cargo_elevator", NONE);
        // single-block TileEntities in 1.7.10 (IMultiblock dummies carried TileEntityDummy) or null dummies
        LAYOUTS.put("compact_launcher", NONE);
        LAYOUTS.put("launch_table", NONE);
        LAYOUTS.put("launch_pad_rusted", NONE);
        LAYOUTS.put("machine_tacz_ammo_assembler", NONE); // port-only block sharing the assembler class
        LAYOUTS.put("machine_chungus", ports(new int[][] {{0, 2, 4}, {0, 0, -10}, {2, 0, 0}, {-2, 0, 0}}));
        LAYOUTS.put("machine_boiler", ports(new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 3, 0}}));
        LAYOUTS.put("machine_industrial_boiler", ports(new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 4, 0}}));
        LAYOUTS.put("heater_heatex", ports(new int[][] {{1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}}));
        // HeaterOilburner: four ports plus makeExtra(x - dir, y + 1, z - dir). [parity b2: ROR] fillSpace receives
        // the placement point, the core is x + dir * o with o = -getOffset() = -1, so that cell is the one above the core.
        LAYOUTS.put("heater_oilburner", ports(new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}}));
        int[][] ring2 = {{-2, 0, -2}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2}, {2, 0, -2}, {-2, 0, -1}, {2, 0, -1}, {-2, 0, 0},
                {2, 0, 0}, {-2, 0, 1}, {2, 0, 1}, {-2, 0, 2}, {-1, 0, 2}, {0, 0, 2}, {1, 0, 2}, {2, 0, 2}};
        LAYOUTS.put("machine_purex", ports(ring2));
        LAYOUTS.put("machine_assembly_factory", ports(ring2)); // TileEntityProxyDyn extends TileEntityProxyCombo
        LAYOUTS.put("machine_chemical_factory", ports(concat(ring2, new int[][] {
                {-2, 2, -2}, {-2, 2, -1}, {-2, 2, 0}, {-2, 2, 1}, {-2, 2, 2}, {2, 2, -2}, {2, 2, -1}, {2, 2, 0}, {2, 2, 1}, {2, 2, 2}})));
        int[][] ring1 = {{-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {0, 0, -1}, {0, 0, 1}, {1, 0, -1}, {1, 0, 0}, {1, 0, 1}};
        LAYOUTS.put("machine_assembly_machine", ports(ring1));
        LAYOUTS.put("machine_chemical_plant", ports(ring1));
        LAYOUTS.put("machine_combustion_engine", ports(new int[][] {{1, 0, 0}, {-1, 0, 0}, {1, 0, -1}, {-1, 0, -1}}));
        LAYOUTS.put("machine_industrial_turbine", ports(new int[][] {{1, 0, 3}, {-1, 0, 3}, {1, 0, -1}, {-1, 0, -1}, {0, 2, 3}, {0, 2, -1}, {0, 1, -3}}));
        LAYOUTS.put("machine_turbinegas", ports(new int[][] {{1, 0, -1}, {1, 0, 1}, {-4, 0, -1}, {-4, 0, 1}, {4, 1, 0}, {-5, 1, 0}}));
        LAYOUTS.put("watz", ports(new int[][] {{2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2}, {2, 2, 0}, {-2, 2, 0}, {0, 2, 2}, {0, 2, -2}, {0, 2, 0}, {0, 0, 0}}));
        LAYOUTS.put("pa_detector", ports(new int[][] {{-4, 0, 0}, {-4, 1, 0}, {-4, -1, 0}, {-4, 0, 1}, {-4, 0, -1}}));
        LAYOUTS.put("pa_dipole", ports(new int[][] {{1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}}));
        LAYOUTS.put("pa_quadrupole", ports(new int[][] {{0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}}));
        LAYOUTS.put("pa_rfc", ports(new int[][] {{3, 1, 0}, {-3, 1, 0}, {0, 1, 0}, {3, -1, 0}, {-3, -1, 0}, {0, -1, 0}}));
        LAYOUTS.put("pa_source", ports(new int[][] {{4, 0, 0}, {0, 0, 1}, {2, 0, 1}, {-2, 0, 1}, {0, 0, -1}, {2, 0, -1}, {-2, 0, -1}, {0, -1, 0}, {2, -1, 0}, {-2, -1, 0}}));
        LAYOUTS.put("machine_battery_socket", ports(new int[][] {{1, 0, 0}, {0, 0, 0}, {0, 0, -1}, {1, 0, -1}}));
        LAYOUTS.put("machine_radgen", ports(new int[][] {{0, 0, -3}, {1, 0, 0}, {-1, 0, 0}}));
        LAYOUTS.put("machine_turbofan", ports(new int[][] {{0, 0, 1}, {-1, 0, 1}, {0, 0, -1}, {-1, 0, -1}}));
        LAYOUTS.put("machine_satlink", ports(new int[][] {{1, 0, 0}, {1, 0, -1}, {0, 0, 0}, {0, 0, -1}}));
        LAYOUTS.put("machine_excavator", ports(new int[][] {{1, 1, 3}, {-1, 1, 3}, {3, 1, 0}, {-3, 1, 0}}));
        LAYOUTS.put("icf", ports(new int[][] {{0, 5, 0}, {6, 3, 2}, {-6, 3, 2}, {6, 3, -2}, {-6, 3, -2}}));
        LAYOUTS.put("fusion_klystron", ports(new int[][] {{0, 2, 3}, {2, 0, 0}, {-2, 0, 0}}));
        LAYOUTS.put("fusion_mhdt", ports(new int[][] {{3, 0, 4}, {-3, 0, 4}, {0, 1, 7}}));
        LAYOUTS.put("fusion_plasma_forge", ports(new int[][] {{-2, 0, 5}, {-1, 0, 5}, {0, 0, 5}, {1, 0, 5}, {2, 0, 5},
                {-2, 0, -5}, {-1, 0, -5}, {0, 0, -5}, {1, 0, -5}, {2, 0, -5}}));
        LAYOUTS.put("fusion_torus", ports(new int[][] {{0, 4, 0}, {0, 0, 0}, {2, 0, 6}, {2, 4, 6}, {2, 0, -6}, {2, 4, -6},
                {-2, 0, 6}, {-2, 4, 6}, {-2, 0, -6}, {-2, 4, -6}, {6, 0, 0}, {6, 4, 0}, {6, 0, 2}, {6, 4, 2}, {6, 0, -2},
                {6, 4, -2}, {-6, 0, 0}, {-6, 4, 0}, {-6, 0, 2}, {-6, 4, 2}, {-6, 0, -2}, {-6, 4, -2}, {0, 0, 6}, {0, 4, 6},
                {0, 0, -6}, {0, 4, -6}}));
        LAYOUTS.put("machine_flare", ports(new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}}));
        LAYOUTS.put("machine_zirnox", ports(new int[][] {{2, 1, 0}, {2, 3, 0}, {-2, 1, 0}, {-2, 3, 0}, {0, 4, 0}}));
        // Turret*.createNewTileEntity: ProxyCombo for every dummy; TurretHowardDamaged: null
        for (String turret : new String[] {"turret_chekhov", "turret_friendly", "turret_jeremy", "turret_tauon", "turret_richard",
                "turret_howard", "turret_maxwell", "turret_fritz", "turret_arty", "turret_himars"}) {
            LAYOUTS.put(turret, ALL);
        }
        LAYOUTS.put("turret_howard_damaged", NONE);
        // [parity V1: ROR] TileEntityBarrel subclasses (ProxyCombo only on makeExtra cells): MachineOrbus /
        // MachineFluidTank / MachineBigAssTank getAllPorts, MachineBigAssTank9000 makeExtra(core +- 1|2, y, core +- 2|1)
        LAYOUTS.put("machine_orbus", ports(new int[][] {{0, 0, -1}, {0, 0, 0}, {1, 0, -1}, {1, 0, 0}, {0, 4, -1}, {0, 4, 0}, {1, 4, -1}, {1, 4, 0}}));
        LAYOUTS.put("machine_fluidtank", ports(new int[][] {{1, 0, -1}, {1, 0, 1}, {-1, 0, -1}, {-1, 0, 1}}));
        LAYOUTS.put("machine_bigasstank", ports(new int[][] {{0, 0, 6}, {0, 0, -6}}));
        ABSOLUTE.put("machine_bat9000", new int[][] {{1, 0, 2}, {-1, 0, 2}, {1, 0, -2}, {-1, 0, -2}, {2, 0, 1}, {-2, 0, 1}, {2, 0, -1}, {-2, 0, -1}});
    }

    private RttyProxyLayouts() {
    }

    private static int[][] ports(int[][] ports) {
        return ports;
    }

    private static int[][] concat(int[][] a, int[][] b) {
        int[][] result = new int[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /** Block path of the core, the key of the layout tables. */
    public static String id(BlockEntity core) {
        return BuiltInRegistries.BLOCK.getKey(core.getBlockState().getBlock()).getPath();
    }

    /** True when the 1.7.10 multiblock had a layout entry (otherwise every dummy keeps forwarding). */
    public static boolean hasLayout(String id) {
        return LAYOUTS.containsKey(id) || ABSOLUTE.containsKey(id);
    }

    /** The original proxy cells of {@code id} for a core at {@code core} facing {@code dir} (null for ALL). */
    public static BlockPos[] proxyCells(String id, BlockPos core, Direction dir) {
        int[][] absolute = ABSOLUTE.get(id);
        if (absolute != null) {
            BlockPos[] cells = new BlockPos[absolute.length];
            for (int i = 0; i < absolute.length; i++) cells[i] = core.offset(absolute[i][0], absolute[i][1], absolute[i][2]);
            return cells;
        }
        int[][] layout = LAYOUTS.get(id);
        if (layout == null || layout == ALL) return null;
        Direction rot = dir.getClockWise();
        BlockPos[] cells = new BlockPos[layout.length];
        for (int i = 0; i < layout.length; i++) {
            int nx = layout[i][0];
            int nz = layout[i][2];
            cells[i] = core.offset(nx * rot.getStepX() + nz * dir.getStepX(), layout[i][1], nx * rot.getStepZ() + nz * dir.getStepZ());
        }
        return cells;
    }

    /** Whether the dummy at {@code dummyPos} forwarded ROR to {@code core} in 1.7.10. */
    public static boolean isProxy(BlockEntity core, BlockPos dummyPos) {
        if (core instanceof RbmkComponentBlockEntity rbmk) return rbmk.hasRorProxyAt(dummyPos); // [parity T2: ROR]
        String id = id(core);
        if (!hasLayout(id)) return true;
        if (LAYOUTS.get(id) == ALL) return true;
        if (LAYOUTS.get(id) == NONE) return false;
        BlockPos[] cells = proxyCells(id, core.getBlockPos(), facing(core.getBlockState()));
        for (BlockPos cell : cells) {
            if (cell.equals(dummyPos)) return true;
        }
        return false;
    }

    /** The horizontal facing of a core block state (the 1.7.10 core meta - offset direction). */
    public static Direction facing(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty direction && property.getName().equals("facing")) {
                Direction value = state.getValue(direction);
                if (value.getAxis().isHorizontal()) return value;
            }
        }
        return Direction.NORTH;
    }
}
