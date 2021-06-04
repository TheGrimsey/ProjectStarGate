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
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.heightprovider.UniformHeightProvider;

import java.util.function.Predicate;

public class ProjectSGFeatures {
    private static final ConfiguredFeature<?, ?> NAQUADAH_ORE_OVERWORLD = Feature.ORE.configure(
            new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,
                                ProjectSGBlocks.NAQUADAH_ORE.getDefaultState(),
                                3))
            .decorate(Decorator.RANGE.configure(new RangeDecoratorConfig(UniformHeightProvider.create(YOffset.fixed(0), YOffset.fixed(16)))))
            .spreadHorizontally()
            .repeat(3);

    private static final ConfiguredFeature<?, ?> NAQUADAH_ORE_NETHER = Feature.ORE.configure(
            new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_NETHER,
                    ProjectSGBlocks.NAQUADAH_ORE_NETHER.getDefaultState(),
                    6))
            .decorate(Decorator.RANGE.configure(new RangeDecoratorConfig(UniformHeightProvider.create(YOffset.fixed(0), YOffset.fixed(128)))))
            .spreadHorizontally()
            .repeat(3);

    private static final ConfiguredFeature<?, ?> NAQUADAH_ORE_END = Feature.ORE.configure(
            new OreFeatureConfig(new BlockMatchRuleTest(Blocks.END_STONE),
                    ProjectSGBlocks.NAQUADAH_ORE_END.getDefaultState(),
                    9))
            .decorate(Decorator.RANGE.configure(new RangeDecoratorConfig(UniformHeightProvider.create(YOffset.fixed(0), YOffset.fixed(128)))))
            .spreadHorizontally()
            .repeat(3);

    public static void registerFeatures()
    {
        registerFeature("naquadah_ore_overworld", NAQUADAH_ORE_OVERWORLD, BiomeSelectors.foundInOverworld());
        registerFeature("naquadah_ore_nether", NAQUADAH_ORE_NETHER, BiomeSelectors.foundInTheNether());
        registerFeature("naquadah_ore_end", NAQUADAH_ORE_END, BiomeSelectors.foundInTheEnd());
    }

    static void registerFeature(String id, ConfiguredFeature<?, ?> feature, Predicate<BiomeSelectionContext> biomesSelector)
    {
        RegistryKey<ConfiguredFeature<?,?>> registryKey = RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier(ProjectStarGate.MODID, id));
        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, registryKey.getValue(), feature);
        BiomeModifications.addFeature(biomesSelector, GenerationStep.Feature.UNDERGROUND_ORES, registryKey);
    }
}
