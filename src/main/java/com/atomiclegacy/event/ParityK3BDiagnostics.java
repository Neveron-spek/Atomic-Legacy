package com.atomiclegacy.event;

import java.util.List;
import java.util.Optional;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.recipes.CentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.CrystallizerRecipe;
import com.atomiclegacy.inventory.recipes.ShredderRecipe;
import com.atomiclegacy.items.LegacyBedrockOreStageItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.CentrifugeBlockEntity;
import com.atomiclegacy.tileentity.CrystallizerBlockEntity;
import com.atomiclegacy.tileentity.ShredderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity K3: group B] Dedicated-server checks for the shredder, centrifuge and crystallizer against the pinned 1.7.10
 * TileEntityMachineShredder / TileEntityMachineCentrifuge / TileEntityMachineCrystallizer and their *Recipes classes.
 * SHREDDER_TABLE is generated from ShredderRecipes.registerDefaults + the registerPost ore-dictionary pass
 * (work/tmp-k3/B/gen_shredder.py). Machine runs happen on stone pads far from the origin.
 */
final class ParityK3BDiagnostics {

	private ParityK3BDiagnostics() {
	}

	private static final int BASE_X = 7_500;
	private static final int BASE_Y = 240;
	private static final int BASE_Z = 7_500;

	/** input output count (R: = atomiclegacy, M: = minecraft). */
	private static final String[] SHREDDER_TABLE = {
		"M:anvil R:powder_iron 31",
		"M:apple M:sugar 1",
		"M:black_terracotta M:clay_ball 4",
		"M:black_wool M:string 4",
		"M:blue_terracotta M:clay_ball 4",
		"M:blue_wool M:string 4",
		"M:brick M:clay_ball 1",
		"M:brick_stairs M:clay_ball 3",
		"M:bricks M:clay_ball 4",
		"M:brown_terracotta M:clay_ball 4",
		"M:brown_wool M:string 4",
		"M:carrot M:sugar 1",
		"M:chiseled_quartz_block R:powder_quartz 4",
		"M:clay M:clay_ball 4",
		"M:coal R:powder_coal 1",
		"M:coal_block R:powder_coal 9",
		"M:coal_ore R:powder_coal 2",
		"M:cobbled_deepslate M:gravel 1",
		"M:cobblestone M:gravel 1",
		"M:copper_block R:powder_copper 9",
		"M:copper_ingot R:powder_copper 1",
		"M:copper_ore R:powder_copper 2",
		"M:creeper_head R:biomass 4",
		"M:cyan_terracotta M:clay_ball 4",
		"M:cyan_wool M:string 4",
		"M:deepslate M:gravel 1",
		"M:deepslate_coal_ore R:powder_coal 2",
		"M:deepslate_copper_ore R:powder_copper 2",
		"M:deepslate_diamond_ore R:gravel_diamond 2",
		"M:deepslate_emerald_ore R:powder_emerald 2",
		"M:deepslate_gold_ore R:powder_gold 2",
		"M:deepslate_iron_ore R:powder_iron 2",
		"M:deepslate_lapis_ore R:powder_lapis 2",
		"M:deepslate_redstone_ore M:redstone 2",
		"M:diamond R:powder_diamond 1",
		"M:diamond_block R:powder_diamond 9",
		"M:diamond_ore R:gravel_diamond 2",
		"M:dirt R:dust 1",
		"M:emerald R:powder_emerald 1",
		"M:emerald_block R:powder_emerald 9",
		"M:emerald_ore R:powder_emerald 2",
		"M:enchanted_book R:powder_magic 1",
		"M:fermented_spider_eye R:powder_poison 3",
		"M:flower_pot M:clay_ball 3",
		"M:glowstone M:glowstone_dust 4",
		"M:glowstone_dust R:dust 1",
		"M:gold_block R:powder_gold 9",
		"M:gold_ingot R:powder_gold 1",
		"M:gold_ore R:powder_gold 2",
		"M:gravel M:sand 1",
		"M:gray_terracotta M:clay_ball 4",
		"M:gray_wool M:string 4",
		"M:green_terracotta M:clay_ball 4",
		"M:green_wool M:string 4",
		"M:iron_block R:powder_iron 9",
		"M:iron_ingot R:powder_iron 1",
		"M:iron_ore R:powder_iron 2",
		"M:lapis_block R:powder_lapis 9",
		"M:lapis_lazuli R:powder_lapis 1",
		"M:lapis_ore R:powder_lapis 2",
		"M:light_blue_terracotta M:clay_ball 4",
		"M:light_blue_wool M:string 4",
		"M:light_gray_terracotta M:clay_ball 4",
		"M:light_gray_wool M:string 4",
		"M:lime_terracotta M:clay_ball 4",
		"M:lime_wool M:string 4",
		"M:magenta_terracotta M:clay_ball 4",
		"M:magenta_wool M:string 4",
		"M:nether_quartz_ore R:powder_quartz 2",
		"M:obsidian R:gravel_obsidian 1",
		"M:orange_terracotta M:clay_ball 4",
		"M:orange_wool M:string 4",
		"M:packed_ice R:powder_ice 1",
		"M:pink_terracotta M:clay_ball 4",
		"M:pink_wool M:string 4",
		"M:player_head R:biomass 4",
		"M:poisonous_potato R:powder_poison 1",
		"M:purple_terracotta M:clay_ball 4",
		"M:purple_wool M:string 4",
		"M:quartz R:powder_quartz 1",
		"M:quartz_block R:powder_quartz 4",
		"M:quartz_pillar R:powder_quartz 4",
		"M:quartz_slab R:powder_quartz 2",
		"M:quartz_stairs R:powder_quartz 3",
		"M:red_terracotta M:clay_ball 4",
		"M:red_wool M:string 4",
		"M:redstone R:dust 1",
		"M:redstone_block M:redstone 4",
		"M:redstone_ore M:redstone 2",
		"M:sand R:dust 2",
		"M:sandstone M:sand 4",
		"M:sandstone_stairs M:sand 6",
		"M:skeleton_skull R:biomass 4",
		"M:stone M:gravel 1",
		"M:stone_bricks M:gravel 1",
		"M:sugar_cane M:sugar 3",
		"M:terracotta M:clay_ball 4",
		"M:tnt M:gunpowder 5",
		"M:white_terracotta M:clay_ball 4",
		"M:white_wool M:string 4",
		"M:wither_skeleton_skull R:biomass 4",
		"M:yellow_terracotta M:clay_ball 4",
		"M:yellow_wool M:string 4",
		"M:zombie_head R:biomass 4",
		"R:bedrock_ore_new M:gravel 1", // 1.7.10 ModItems.bedrock_ore (ItemBedrockOreNew) is bedrock_ore_new here
		"R:block_actinium R:powder_actinium 9",
		"R:block_aluminium R:powder_aluminium 9",
		"R:block_asbestos R:powder_asbestos 9",
		"R:block_australium R:powder_australium 9",
		"R:block_bakelite R:powder_bakelite 9",
		"R:block_beryllium R:powder_beryllium 9",
		"R:block_bismuth R:powder_bismuth 9",
		"R:block_boron R:powder_boron 9",
		"R:block_cadmium R:powder_cadmium 9",
		"R:block_cobalt R:powder_cobalt 9",
		"R:block_coltan R:powder_coltan_ore 9",
		"R:block_combine_steel R:powder_combine_steel 9",
		"R:block_desh R:powder_desh 9",
		"R:block_dineutronium R:powder_dineutronium 9",
		"R:block_dura_steel R:powder_dura_steel 9",
		"R:block_euphemium R:powder_euphemium 9",
		"R:block_fluorite R:fluorite 4",
		"R:block_lanthanium R:powder_lanthanium 9",
		"R:block_lead R:powder_lead 9",
		"R:block_lithium R:powder_lithium 9",
		"R:block_magnetized_tungsten R:powder_magnetized_tungsten 9",
		"R:block_meteor R:powder_meteorite 10",
		"R:block_neptunium R:powder_neptunium 9",
		"R:block_niobium R:powder_niobium 9",
		"R:block_niter R:niter 4",
		"R:block_plutonium R:powder_plutonium 9",
		"R:block_polonium R:powder_polonium 9",
		"R:block_polymer R:powder_polymer 9",
		"R:block_ra226 R:powder_ra226 9",
		"R:block_red_copper R:powder_red_copper 9",
		"R:block_red_phosphorus R:powder_fire 4",
		"R:block_schrabidate R:powder_schrabidate 9",
		"R:block_schrabidium R:powder_schrabidium 9",
		"R:block_slag R:powder_cement 4",
		"R:block_steel R:powder_steel 9",
		"R:block_sulfur R:sulfur 4",
		"R:block_tantalium R:powder_tantalium 9",
		"R:block_tcalloy R:powder_tcalloy 9",
		"R:block_thorium R:powder_thorium 9",
		"R:block_titanium R:powder_titanium 9",
		"R:block_tungsten R:powder_tungsten 9",
		"R:block_uranium R:powder_uranium 9",
		"R:block_zirconium R:powder_zirconium 9",
		"R:boxcar R:powder_steel 32",
		"R:brick_concrete M:gravel 1",
		"R:brick_concrete_broken M:gravel 1",
		"R:brick_concrete_cracked M:gravel 1",
		"R:brick_concrete_mossy M:gravel 1",
		"R:brick_light M:clay_ball 4",
		"R:brick_obsidian R:gravel_obsidian 1",
		"R:can_empty R:powder_aluminium 2",
		"R:chunk_ore_rare R:powder_desh_mix 1",
		"R:coal_infernal R:powder_coal 2",
		"R:coil_copper R:powder_red_copper 1",
		"R:coil_copper_torus R:powder_red_copper 2",
		"R:coil_gold R:powder_gold 1",
		"R:coil_gold_torus R:powder_gold 2",
		"R:coil_magnetized_tungsten R:powder_magnetized_tungsten 1",
		"R:coil_tungsten R:powder_tungsten 1",
		"R:concrete M:gravel 1",
		"R:concrete_smooth M:gravel 1",
		"R:crate_iron R:powder_iron 8",
		"R:crate_steel R:powder_steel 8",
		"R:crate_tungsten R:powder_tungsten 36",
		"R:crystal_aluminium R:powder_aluminium 3",
		"R:crystal_beryllium R:powder_beryllium 3",
		"R:crystal_coal R:powder_coal 3",
		"R:crystal_cobalt R:powder_cobalt 3",
		"R:crystal_copper R:powder_copper 3",
		"R:crystal_diamond R:powder_diamond 3",
		"R:crystal_fluorite R:fluorite 8",
		"R:crystal_gold R:powder_gold 3",
		"R:crystal_iron R:powder_iron 3",
		"R:crystal_lapis R:powder_lapis 8",
		"R:crystal_lead R:powder_lead 3",
		"R:crystal_lithium R:powder_lithium 3",
		"R:crystal_niter R:niter 8",
		"R:crystal_phosphorus R:powder_fire 8",
		"R:crystal_plutonium R:powder_plutonium 3",
		"R:crystal_rare R:powder_desh_mix 2",
		"R:crystal_redstone M:redstone 8",
		"R:crystal_schrabidium R:powder_schrabidium 3",
		"R:crystal_schraranium R:nugget_schrabidium 3",
		"R:crystal_starmetal R:powder_dura_steel 6",
		"R:crystal_sulfur R:sulfur 8",
		"R:crystal_thorium R:powder_thorium 3",
		"R:crystal_titanium R:powder_titanium 3",
		"R:crystal_trixite R:powder_plutonium 6",
		"R:crystal_tungsten R:powder_tungsten 3",
		"R:crystal_uranium R:powder_uranium 3",
		"R:debris_concrete R:scrap_nuclear 2",
		"R:debris_element R:scrap_nuclear 4",
		"R:debris_exchanger R:powder_steel 3",
		"R:debris_graphite R:powder_coal 1",
		"R:debris_metal R:powder_steel_tiny 3",
		"R:debris_shrapnel R:powder_steel_tiny 5",
		"R:deco_pipe R:powder_steel 1",
		"R:deco_pipe_framed R:powder_steel 1",
		"R:deco_pipe_framed_green R:powder_steel 1",
		"R:deco_pipe_framed_green_rusted R:powder_steel 1",
		"R:deco_pipe_framed_marked R:powder_steel 1",
		"R:deco_pipe_framed_red R:powder_steel 1",
		"R:deco_pipe_framed_rusted R:powder_steel 1",
		"R:deco_pipe_green R:powder_steel 1",
		"R:deco_pipe_green_rusted R:powder_steel 1",
		"R:deco_pipe_marked R:powder_steel 1",
		"R:deco_pipe_quad R:powder_steel 1",
		"R:deco_pipe_quad_green R:powder_steel 1",
		"R:deco_pipe_quad_green_rusted R:powder_steel 1",
		"R:deco_pipe_quad_marked R:powder_steel 1",
		"R:deco_pipe_quad_red R:powder_steel 1",
		"R:deco_pipe_quad_rusted R:powder_steel 1",
		"R:deco_pipe_red R:powder_steel 1",
		"R:deco_pipe_rim R:powder_steel 1",
		"R:deco_pipe_rim_green R:powder_steel 1",
		"R:deco_pipe_rim_green_rusted R:powder_steel 1",
		"R:deco_pipe_rim_marked R:powder_steel 1",
		"R:deco_pipe_rim_red R:powder_steel 1",
		"R:deco_pipe_rim_rusted R:powder_steel 1",
		"R:deco_pipe_rusted R:powder_steel 1",
		"R:dirt_dead R:scrap_oil 1",
		"R:dirt_oily R:scrap_oil 1",
		"R:drive_disk_broken R:powder_polymer 4",
		"R:drive_flash_broken R:powder_polymer 2",
		"R:dungeon_chain R:powder_steel_tiny 1",
		"R:dust R:dust 1",
		"R:dust_tiny R:dust_tiny 1",
		"R:fluorite R:dust 1",
		"R:fragment_actinium R:powder_actinium_tiny 1",
		"R:fragment_boron R:powder_boron_tiny 1",
		"R:fragment_cerium R:powder_cerium_tiny 1",
		"R:fragment_cobalt R:powder_cobalt_tiny 1",
		"R:fragment_coltan R:powder_coltan_ore 1",
		"R:fragment_lanthanium R:powder_lanthanium_tiny 1",
		"R:fragment_meteorite R:powder_meteorite_tiny 1",
		"R:fragment_neodymium R:powder_neodymium_tiny 1",
		"R:fragment_niobium R:powder_niobium_tiny 1",
		"R:gem_tantalium R:powder_tantalium 1",
		"R:gravel_diamond R:powder_diamond 2",
		"R:ingot_actinium R:powder_actinium 1",
		"R:ingot_aluminium R:powder_aluminium 1",
		"R:ingot_asbestos R:powder_asbestos 1",
		"R:ingot_au198 R:powder_au198 1",
		"R:ingot_australium R:powder_australium 1",
		"R:ingot_bakelite R:powder_bakelite 1",
		"R:ingot_beryllium R:powder_beryllium 1",
		"R:ingot_bismuth R:powder_bismuth 1",
		"R:ingot_boron R:powder_boron 1",
		"R:ingot_cadmium R:powder_cadmium 1",
		"R:ingot_calcium R:powder_calcium 1",
		"R:ingot_co60 R:powder_co60 1",
		"R:ingot_cobalt R:powder_cobalt 1",
		"R:ingot_combine_steel R:powder_combine_steel 1",
		"R:ingot_desh R:powder_desh 1",
		"R:ingot_dineutronium R:powder_dineutronium 1",
		"R:ingot_dura_steel R:powder_dura_steel 1",
		"R:ingot_euphemium R:powder_euphemium 1",
		"R:ingot_lanthanium R:powder_lanthanium 1",
		"R:ingot_lead R:powder_lead 1",
		"R:ingot_magnetized_tungsten R:powder_magnetized_tungsten 1",
		"R:ingot_neptunium R:powder_neptunium 1",
		"R:ingot_niobium R:powder_niobium 1",
		"R:ingot_plutonium R:powder_plutonium 1",
		"R:ingot_polonium R:powder_polonium 1",
		"R:ingot_polymer R:powder_polymer 1",
		"R:ingot_ra226 R:powder_ra226 1",
		"R:ingot_raw_borax R:powder_borax 1",
		"R:ingot_raw_neodymium R:powder_neodymium 1",
		"R:ingot_raw_redstone M:redstone 1",
		"R:ingot_raw_sodium R:powder_sodium 1",
		"R:ingot_raw_strontium R:powder_strontium 1",
		"R:ingot_red_copper R:powder_red_copper 1",
		"R:ingot_schrabidate R:powder_schrabidate 1",
		"R:ingot_schrabidium R:powder_schrabidium 1",
		"R:ingot_schraranium R:nugget_schrabidium 2",
		"R:ingot_sr90 R:powder_sr90 1",
		"R:ingot_steel R:powder_steel 1",
		"R:ingot_tantalium R:powder_tantalium 1",
		"R:ingot_tcalloy R:powder_tcalloy 1",
		"R:ingot_th232 R:powder_thorium 1",
		"R:ingot_titanium R:powder_titanium 1",
		"R:ingot_tungsten R:powder_tungsten 1",
		"R:ingot_uranium R:powder_uranium 1",
		"R:ingot_zirconium R:powder_zirconium 1",
		"R:lignite R:powder_lignite 1",
		"R:lithium R:powder_lithium 1",
		"R:meteor_brick R:powder_meteorite 1",
		"R:meteor_brick_chiseled R:powder_meteorite 1",
		"R:meteor_brick_cracked R:powder_meteorite 1",
		"R:meteor_brick_mossy R:powder_meteorite 1",
		"R:meteor_pillar R:powder_meteorite 1",
		"R:meteor_polished R:powder_meteorite 1",
		"R:niter R:dust 1",
		"R:ore_aluminium R:chunk_ore_cryolite 2",
		"R:ore_asbestos R:powder_asbestos 2",
		"R:ore_australium R:powder_australium 2",
		"R:ore_beryllium R:powder_beryllium 2",
		"R:ore_cobalt R:powder_cobalt 2",
		"R:ore_coltan R:powder_coltan_ore 2",
		"R:ore_deepslate_aluminium R:chunk_ore_cryolite 2",
		"R:ore_deepslate_asbestos R:powder_asbestos 2",
		"R:ore_deepslate_australium R:powder_australium 2",
		"R:ore_deepslate_beryllium R:powder_beryllium 2",
		"R:ore_deepslate_cobalt R:powder_cobalt 2",
		"R:ore_deepslate_coltan R:powder_coltan_ore 2",
		"R:ore_deepslate_fluorite R:fluorite 2",
		"R:ore_deepslate_lead R:powder_lead 2",
		"R:ore_deepslate_lignite R:powder_lignite 2",
		"R:ore_deepslate_niter R:niter 2",
		"R:ore_deepslate_oil_empty M:gravel 1",
		"R:ore_deepslate_rare R:powder_desh_mix 1",
		"R:ore_deepslate_schrabidium R:powder_schrabidium 2",
		"R:ore_deepslate_sulfur R:sulfur 2",
		"R:ore_deepslate_thorium R:powder_thorium 2",
		"R:ore_deepslate_titanium R:powder_titanium 2",
		"R:ore_deepslate_tungsten R:powder_tungsten 2",
		"R:ore_deepslate_uranium R:powder_uranium 2",
		"R:ore_deepslate_uranium_scorched R:powder_uranium 2",
		"R:ore_fluorite R:fluorite 2",
		"R:ore_gneiss_asbestos R:powder_asbestos 2",
		"R:ore_gneiss_gold R:powder_gold 2",
		"R:ore_gneiss_iron R:powder_iron 2",
		"R:ore_gneiss_lithium R:powder_lithium 2",
		"R:ore_gneiss_schrabidium R:powder_schrabidium 2",
		"R:ore_gneiss_uranium R:powder_uranium 2",
		"R:ore_gneiss_uranium_scorched R:powder_uranium 2",
		"R:ore_lead R:powder_lead 2",
		"R:ore_lignite R:powder_lignite 2",
		"R:ore_nether_cobalt R:powder_cobalt 2",
		"R:ore_nether_fire R:powder_fire 6",
		"R:ore_nether_plutonium R:powder_plutonium 2",
		"R:ore_nether_schrabidium R:powder_schrabidium 2",
		"R:ore_nether_sulfur R:sulfur 2",
		"R:ore_nether_tungsten R:powder_tungsten 2",
		"R:ore_nether_uranium R:powder_uranium 2",
		"R:ore_nether_uranium_scorched R:powder_uranium 2",
		"R:ore_niter R:niter 2",
		"R:ore_oil_empty M:gravel 1",
		"R:ore_rare R:powder_desh_mix 1",
		"R:ore_schrabidium R:powder_schrabidium 2",
		"R:ore_sellafield_diamond R:gravel_diamond 2",
		"R:ore_sellafield_emerald R:powder_emerald 2",
		"R:ore_sellafield_schrabidium R:powder_schrabidium 2",
		"R:ore_sellafield_uranium_scorched R:powder_uranium 2",
		"R:ore_sulfur R:sulfur 2",
		"R:ore_tektite_osmiridium R:powder_tektite 1",
		"R:ore_thorium R:powder_thorium 2",
		"R:ore_titanium R:powder_titanium 2",
		"R:ore_tungsten R:powder_tungsten 2",
		"R:ore_uranium R:powder_uranium 2",
		"R:ore_uranium_scorched R:powder_uranium 2",
		"R:pipes_steel R:powder_steel 27",
		"R:plate_aluminium R:powder_aluminium 1",
		"R:plate_combine_steel R:powder_combine_steel 1",
		"R:plate_copper R:powder_copper 1",
		"R:plate_dura_steel R:powder_dura_steel 1",
		"R:plate_gold R:powder_gold 1",
		"R:plate_iron R:powder_iron 1",
		"R:plate_lead R:powder_lead 1",
		"R:plate_schrabidium R:powder_schrabidium 1",
		"R:plate_steel R:powder_steel 1",
		"R:plate_titanium R:powder_titanium 1",
		"R:powder_actinium R:dust 1",
		"R:powder_actinium_tiny R:dust_tiny 1",
		"R:powder_aluminium R:dust 1",
		"R:powder_asbestos R:dust 1",
		"R:powder_astatine R:dust 1",
		"R:powder_at209 R:dust 1",
		"R:powder_au198 R:dust 1",
		"R:powder_australium R:dust 1",
		"R:powder_bakelite R:dust 1",
		"R:powder_beryllium R:dust 1",
		"R:powder_bismuth R:dust 1",
		"R:powder_borax R:dust 1",
		"R:powder_boron R:dust 1",
		"R:powder_boron_tiny R:dust_tiny 1",
		"R:powder_bromine R:dust 1",
		"R:powder_cadmium R:dust 1",
		"R:powder_caesium R:dust 1",
		"R:powder_calcium R:dust 1",
		"R:powder_cerium R:dust 1",
		"R:powder_cerium_tiny R:dust_tiny 1",
		"R:powder_chlorocalcite R:dust 1",
		"R:powder_co60 R:dust 1",
		"R:powder_coal R:dust 1",
		"R:powder_coal_tiny R:dust_tiny 1",
		"R:powder_cobalt R:dust 1",
		"R:powder_cobalt_tiny R:dust_tiny 1",
		"R:powder_coltan_ore R:dust 1",
		"R:powder_combine_steel R:dust 1",
		"R:powder_copper R:dust 1",
		"R:powder_cs137 R:dust 1",
		"R:powder_cs137_tiny R:dust_tiny 1",
		"R:powder_desh R:dust 1",
		"R:powder_diamond R:dust 1",
		"R:powder_dineutronium R:dust 1",
		"R:powder_dura_steel R:dust 1",
		"R:powder_emerald R:dust 1",
		"R:powder_euphemium R:dust 1",
		"R:powder_fire R:dust 1",
		"R:powder_gold R:dust 1",
		"R:powder_i131 R:dust 1",
		"R:powder_i131_tiny R:dust_tiny 1",
		"R:powder_iodine R:dust 1",
		"R:powder_iron R:dust 1",
		"R:powder_lanthanium R:dust 1",
		"R:powder_lanthanium_tiny R:dust_tiny 1",
		"R:powder_lapis R:powder_cobalt_tiny 1",
		"R:powder_lead R:dust 1",
		"R:powder_lignite R:dust 1",
		"R:powder_limestone R:dust 1",
		"R:powder_lithium R:dust 1",
		"R:powder_lithium_tiny R:dust_tiny 1",
		"R:powder_magnetized_tungsten R:dust 1",
		"R:powder_molysite R:dust 1",
		"R:powder_neodymium R:dust 1",
		"R:powder_neodymium_tiny R:dust_tiny 1",
		"R:powder_neptunium R:dust 1",
		"R:powder_niobium R:dust 1",
		"R:powder_niobium_tiny R:dust_tiny 1",
		"R:powder_plutonium R:dust 1",
		"R:powder_polonium R:dust 1",
		"R:powder_polymer R:dust 1",
		"R:powder_quartz R:dust 1",
		"R:powder_ra226 R:dust 1",
		"R:powder_red_copper R:dust 1",
		"R:powder_schrabidate R:dust 1",
		"R:powder_schrabidium R:dust 1",
		"R:powder_sodium R:dust 1",
		"R:powder_sr90 R:dust 1",
		"R:powder_sr90_tiny R:dust_tiny 1",
		"R:powder_steel R:dust 1",
		"R:powder_steel_tiny R:dust_tiny 1",
		"R:powder_strontium R:dust 1",
		"R:powder_tantalium R:dust 1",
		"R:powder_tcalloy R:dust 1",
		"R:powder_thorium R:dust 1",
		"R:powder_titanium R:dust 1",
		"R:powder_tungsten R:dust 1",
		"R:powder_uranium R:dust 1",
		"R:powder_xe135 R:dust 1",
		"R:powder_xe135_tiny R:dust_tiny 1",
		"R:powder_zirconium R:dust 1",
		"R:sand_dirty R:scrap_oil 1",
		"R:sand_dirty_red R:scrap_oil 1",
		"R:scrap R:dust 1",
		"R:sellafield_slaked M:gravel 1",
		"R:steel_beam R:powder_steel_tiny 3",
		"R:steel_corner R:powder_steel_tiny 18",
		"R:steel_grate R:powder_steel_tiny 3",
		"R:steel_poles R:powder_steel_tiny 2",
		"R:steel_roof R:powder_steel_tiny 9",
		"R:steel_scaffold R:powder_steel_tiny 4",
		"R:steel_scaffold_red R:powder_steel_tiny 4",
		"R:steel_scaffold_white R:powder_steel_tiny 4",
		"R:steel_scaffold_yellow R:powder_steel_tiny 4",
		"R:steel_wall R:powder_steel_tiny 9",
		"R:stone_cracked R:scrap_oil 1",
		"R:stone_gneiss R:powder_lithium_tiny 1",
		"R:stone_porous R:scrap_oil 1",
		"R:stone_resource_asbestos R:powder_asbestos 2",
		"R:stone_resource_limestone R:powder_limestone 4",
		"R:stone_resource_sulfur R:sulfur 2",
		"R:sulfur R:dust 1",
	};

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		try {
			shredderTable(level, problems, cases);
			centrifugeRecipes(level, problems, cases);
			crystallizerRecipes(level, problems, cases);
			shredderRun(level, problems, cases);
			centrifugeRun(level, problems, cases);
			crystallizerRun(level, problems, cases);
		} catch(RuntimeException e) {
			AtomicLegacy.LOGGER.error("Parity K3B diagnostics crashed", e);
			problems.add("K3B diagnostics crashed: " + e);
		}
		return cases[0];
	}

	private static Item item(String id) {
		String full = id.startsWith("R:") ? "atomiclegacy:" + id.substring(2) : id.startsWith("M:") ? "minecraft:" + id.substring(2) : id;
		ResourceLocation rl = ResourceLocation.parse(full);
		return BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.get(rl) : null;
	}

	private static ItemStack shred(ServerLevel level, ItemStack in) {
		Optional<RecipeHolder<ShredderRecipe>> r = level.getRecipeManager().getRecipeFor(HbmRecipeTypes.SHREDDER.get(), new SingleRecipeInput(in), level);
		return r.map(h -> h.value().assemble(new SingleRecipeInput(in), level.registryAccess())).orElse(ItemStack.EMPTY);
	}

	private static void shredderTable(ServerLevel level, List<String> problems, int[] cases) {
		for(String row : SHREDDER_TABLE) {
			String[] p = row.split(" ");
			Item in = item(p[0]);
			Item out = item(p[1]);
			if(in == null || out == null) {
				check(false, problems, cases, "shredder table item missing: " + row);
				continue;
			}
			ItemStack res = shred(level, new ItemStack(in));
			check(res.is(out) && res.getCount() == Integer.parseInt(p[2]), problems, cases,
					"shredder " + p[0] + " -> " + res + " expected " + p[1] + " x" + p[2]);
		}
		ItemStack stage = LegacyBedrockOreStageItem.stackFor(item("R:ore_centrifuged"), LegacyBedrockOreStageItem.Type.URANIUM);
		ItemStack enriched = shred(level, stage);
		check(enriched.is(item("R:ore_enriched")) && LegacyBedrockOreStageItem.type(enriched) == LegacyBedrockOreStageItem.Type.URANIUM,
				problems, cases, "shredder ore_centrifuged(uranium) -> ore_enriched(uranium), got " + enriched);
		check(shred(level, new ItemStack(Items.NETHER_BRICK)).isEmpty(), problems, cases, "nether brick: no shredder recipe (machine gives scrap)");
	}

	private static Optional<RecipeHolder<CentrifugeRecipe>> spin(ServerLevel level, ItemStack in) {
		return level.getRecipeManager().getRecipeFor(HbmRecipeTypes.CENTRIFUGE.get(), new CentrifugeRecipe.Input(in), level);
	}

	private static void centrifugeRecipes(ServerLevel level, List<String> problems, int[] cases) {
		Optional<RecipeHolder<CentrifugeRecipe>> r = spin(level, LegacyBedrockOreStageItem.stackFor(item("R:ore_bedrock"), LegacyBedrockOreStageItem.Type.IRON));
		check(r.isPresent() && r.get().value().results().size() == 4 && r.get().value().results().get(0).is(item("R:ore_centrifuged")),
				problems, cases, "centrifuge ore_bedrock(iron) -> 4x ore_centrifuged(iron)");
		r = spin(level, new ItemStack(Blocks.IRON_ORE));
		check(r.isPresent() && r.get().value().results().size() == 4 && r.get().value().results().get(3).is(Items.GRAVEL),
				problems, cases, "centrifuge iron ore -> powder_iron x3 + gravel");
		Item ilmenite = item("R:chunk_ore_ilmenite");
		Item titaniumOre = item("R:ore_titanium");
		if(titaniumOre != null) {
			r = spin(level, new ItemStack(titaniumOre));
			check(ilmenite != null && r.isPresent() && r.get().value().results().get(1).is(ilmenite) && r.get().value().results().get(1).getCount() == 2,
					problems, cases, "centrifuge titanium ore: second output 2x chunk_ore_ilmenite");
		}
		Item schraranium = item("R:ingot_schraranium");
		check(schraranium == null || spin(level, new ItemStack(schraranium)).isEmpty(), problems, cases, "no centrifuge recipe for ingot_schraranium");
	}

	private static String describe(Optional<RecipeHolder<CrystallizerRecipe>> r) {
		return r.map(h -> h.id() + " " + h.value().result() + " " + h.value().duration() + " " + h.value().productivity()).orElse("none");
	}

	private static Optional<RecipeHolder<CrystallizerRecipe>> crystal(ServerLevel level, ItemStack in, String fluid) {
		HbmFluidStack acid = new HbmFluidStack(HbmFluids.byName(fluid).orElse(HbmFluids.none()), Integer.MAX_VALUE, 0);
		return level.getRecipeManager().getRecipeFor(HbmRecipeTypes.CRYSTALLIZER.get(), new CrystallizerRecipe.Input(in, acid), level);
	}

	private static void crystallizerRecipes(ServerLevel level, List<String> problems, int[] cases) {
		Optional<RecipeHolder<CrystallizerRecipe>> r = crystal(level, new ItemStack(Blocks.IRON_ORE), "peroxide");
		check(r.isPresent() && r.get().value().duration() == 600 && r.get().value().acid().amount() == 500 && Math.abs(r.get().value().productivity() - 0.05F) < 1e-6F,
				problems, cases, "crystallizer iron ore: peroxide 500, 600 t, prod 0.05");
		r = crystal(level, new ItemStack(Blocks.SAND), "peroxide");
		check(r.isPresent() && r.get().value().result().is(item("R:ingot_fiberglass")) && r.get().value().duration() == 100
				&& Math.abs(r.get().value().productivity() - 0.15F) < 1e-6F, problems, cases, "crystallizer sand: fiberglass, 100 t, prod 0.15; got " + describe(r));
		r = crystal(level, new ItemStack(Blocks.COAL_BLOCK), "peroxide");
		check(r.isPresent() && r.get().value().result().is(item("R:block_graphite")), problems, cases, "crystallizer coal block -> graphite block");
		r = crystal(level, new ItemStack(item("R:scrap_plastic_board_blank")), "peroxide");
		check(r.isPresent() && r.get().value().acid().amount() == 500, problems, cases, "crystallizer scrap_plastic uses peroxide 500");
		r = crystal(level, LegacyBedrockOreStageItem.stackFor(item("R:ore_separated"), LegacyBedrockOreStageItem.Type.GOLD), "nitric_acid");
		check(r.isPresent() && r.get().value().result().is(item("R:ore_nitrated")) && r.get().value().duration() == 200,
				problems, cases, "crystallizer ore_separated + nitric acid -> ore_nitrated, 200 t");
	}

	private static <T extends BlockEntity> T place(ServerLevel level, BlockPos pos, BlockState state, Class<T> type) {
		level.getChunkAt(pos);
		for(int dx = -2; dx <= 2; dx++) for(int dz = -2; dz <= 2; dz++) level.setBlock(pos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
		level.setBlock(pos, state, 3);
		BlockEntity be = level.getBlockEntity(pos);
		return type.isInstance(be) ? type.cast(be) : null;
	}

	private static void shredderRun(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = new BlockPos(BASE_X, BASE_Y, BASE_Z);
		ShredderBlockEntity be = place(level, pos, HbmBlocks.MACHINE_SHREDDER.get().defaultBlockState(), ShredderBlockEntity.class);
		if(be == null) {
			check(false, problems, cases, "shredder block entity");
			return;
		}
		be.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
		be.setItem(ShredderBlockEntity.LEFT_BLADE_SLOT, new ItemStack(item("R:blades_steel")));
		be.setItem(ShredderBlockEntity.RIGHT_BLADE_SLOT, new ItemStack(item("R:blades_steel")));
		be.applyPower(0L, ShredderBlockEntity.ENERGY_CAPACITY);
		for(int i = 0; i < 59; i++) ShredderBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(9).isEmpty(), problems, cases, "shredder: nothing after 59 ticks (processingSpeed 60)");
		ShredderBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(9).is(item("R:powder_iron")) && be.getItem(9).getCount() == 1 && be.getItem(0).getCount() == 1, problems, cases,
				"shredder: tick 60 -> 1 powder_iron, 1 ingot left; got " + be.getItem(9) + " / " + be.getItem(0));
		check(be.getRequestedInput() == 60L * 5L, problems, cases, "shredder: 5 HE per tick (300 per cycle), used " + be.getRequestedInput());
		check(be.getItem(ShredderBlockEntity.LEFT_BLADE_SLOT).getDamageValue() == 1 && be.getItem(ShredderBlockEntity.RIGHT_BLADE_SLOT).getDamageValue() == 1,
				problems, cases, "shredder: each blade loses 1 durability per cycle");
		be.setItem(ShredderBlockEntity.RIGHT_BLADE_SLOT, ItemStack.EMPTY);
		for(int i = 0; i < 60; i++) ShredderBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(9).getCount() == 1, problems, cases, "shredder: no work without both blades");
		check(!be.canPlaceItemThroughFace(ShredderBlockEntity.BATTERY_SLOT, new ItemStack(Items.IRON_INGOT), Direction.UP), problems, cases,
				"shredder: battery slot not insertable by automation");
		level.removeBlock(pos, false);
	}

	private static void centrifugeRun(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = new BlockPos(BASE_X + 32, BASE_Y, BASE_Z);
		CentrifugeBlockEntity be = place(level, pos, HbmBlocks.MACHINE_CENTRIFUGE.get().defaultBlockState(), CentrifugeBlockEntity.class);
		if(be == null) {
			check(false, problems, cases, "centrifuge block entity");
			return;
		}
		be.setItem(0, new ItemStack(Blocks.IRON_ORE, 2));
		be.applyPower(0L, CentrifugeBlockEntity.MAX_POWER);
		for(int i = 0; i < 199; i++) CentrifugeBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(2).isEmpty(), problems, cases, "centrifuge: nothing after 199 ticks (processingSpeed 200)");
		CentrifugeBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(2).is(item("R:powder_iron")) && be.getItem(3).is(item("R:powder_iron")) && be.getItem(4).is(item("R:powder_iron"))
				&& be.getItem(5).is(Items.GRAVEL), problems, cases, "centrifuge: outputs in slots 2..5 in recipe order");
		// 1.7.10 charges consumption while progress > 0 at the start of a tick: 199 x 200 HE per recipe
		check(be.getRequestedInput() == 199L * 200L, problems, cases, "centrifuge: 199 x 200 HE per recipe, used " + be.getRequestedInput());
		be.applyPower(0L, CentrifugeBlockEntity.MAX_POWER);
		be.setItem(6, new ItemStack(item("R:upgrade_speed_3")));
		be.setItem(7, new ItemStack(item("R:upgrade_speed_3")));
		for(int i = 0; i < 50; i++) CentrifugeBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		// speed level capped at 3: speed 4, consumption 800 -> 50 ticks, 49 charged
		check(be.getItem(2).getCount() == 2 && be.getRequestedInput() == 49L * 800L, problems, cases,
				"centrifuge: speed capped at 3 (50 ticks, 49 x 800 HE), used " + be.getRequestedInput() + ", out " + be.getItem(2));
		level.removeBlock(pos, false);
	}

	private static void crystallizerRun(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = new BlockPos(BASE_X + 64, BASE_Y, BASE_Z);
		CrystallizerBlockEntity be = place(level, pos, HbmBlocks.MACHINE_CRYSTALLIZER.get().defaultBlockState(), CrystallizerBlockEntity.class);
		if(be == null) {
			check(false, problems, cases, "crystallizer block entity");
			return;
		}
		HbmFluidTank tank = be.acidTank();
		tank.setType(HbmFluids.byName("peroxide").orElse(HbmFluids.none()));
		tank.setAmount(1000);
		be.setItem(0, new ItemStack(Blocks.IRON_ORE, 2));
		be.applyPower(0L, CrystallizerBlockEntity.MAX_POWER);
		for(int i = 0; i < 600; i++) CrystallizerBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(2).isEmpty(), problems, cases, "crystallizer: nothing after 600 ticks (progress > duration)");
		CrystallizerBlockEntity.tick(level, pos, level.getBlockState(pos), be);
		check(be.getItem(2).is(item("R:crystal_iron")) && tank.amount() == 500 && be.getItem(0).getCount() == 1, problems, cases,
				"crystallizer: tick 601 -> crystal_iron, 500 mB peroxide used; got " + be.getItem(2) + ", tank " + tank.amount());
		check(be.getRequestedInput() == 601L * 1000L, problems, cases, "crystallizer: 1000 HE per tick, used " + be.getRequestedInput());
		check(be.getSlotsForFace(Direction.UP).length == 2, problems, cases, "crystallizer: automation slots {0, 2}");
		level.removeBlock(pos, false);
	}

	private static void check(boolean condition, List<String> problems, int[] cases, String message) {
		cases[0]++;
		if(!condition) problems.add("K3B: " + message);
	}
}
