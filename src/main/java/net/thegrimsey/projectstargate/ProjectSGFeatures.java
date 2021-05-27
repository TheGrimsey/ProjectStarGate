package net.thegrimsey.projectstargate;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Blocks;
import net.minecraft.structure.rule.BlockMatchRuleTest;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;

import java.util.function.Predicate;

public class ProjectSGFeatures {
    private static ConfiguredFeature<?, ?> NAQUADAH_ORE_OVERWORLD = Feature.ORE.configure(
            new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,
                                ProjectSGBlocks.NAQUADAH_ORE.getDefaultState(),
                                3))
            .rangeOf(16)
            .spreadHorizontally()
            .repeat(3);

    private static ConfiguredFeature<?, ?> NAQUADAH_ORE_NETHER = Feature.ORE.configure(
            new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_NETHER,
                    ProjectSGBlocks.NAQUADAH_ORE_NETHER.getDefaultState(),
                    6))
            .rangeOf(128)
            .spreadHorizontally()
            .repeat(3);

    private static ConfiguredFeature<?, ?> NAQUADAH_ORE_END = Feature.ORE.configure(
            new OreFeatureConfig(new BlockMatchRuleTest(Blocks.END_STONE),
                    ProjectSGBlocks.NAQUADAH_ORE_END.getDefaultState(),
                    9))
            .rangeOf(128)
            .spreadHorizontally()
            .repeat(3);

    public static void registerFeatures()
    {
        registerFeature("naquadah_ore_overworld", NAQUADAH_ORE_OVERWORLD, BiomeSelectors.foundInOverworld());
        registerFeature("naquadah_ore_nether", NAQUADAH_ORE_NETHER, BiomeSelectors.foundInTheNether());
        registerFeature("naquadah_ore_end", NAQUADAH_ORE_END, BiomeSelectors.foundInTheEnd());
    }

    static void registerFeature(String id, ConfiguredFeature<?, ?> feature, Predicate<BiomeSelectionContext> biomeSelector)
    {
        RegistryKey<ConfiguredFeature<?,?>> registryKey = RegistryKey.of(Registry.CONFIGURED_FEATURE_WORLDGEN, new Identifier(ProjectStarGate.MODID, id));
        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, registryKey.getValue(), feature);
        BiomeModifications.addFeature(biomeSelector, GenerationStep.Feature.UNDERGROUND_ORES, registryKey);
    }
}
