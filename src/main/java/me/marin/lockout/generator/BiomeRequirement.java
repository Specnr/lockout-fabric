package me.marin.lockout.generator;

import me.marin.lockout.LocateData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

import java.util.Map;

public interface BiomeRequirement {

    boolean isMet(Map<RegistryKey<Biome>, LocateData> biomes);

    void collectBiomes(java.util.Collection<RegistryKey<Biome>> collector);

}
