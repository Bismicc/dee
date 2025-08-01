package de.somkat.iceboatracing.powerups;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public enum PowerUpType {
    BOOST("Boost", Material.GOLD_BLOCK, Particle.FLAME, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 5000),
    SHIELD("Shield", Material.LAPIS_BLOCK, Particle.ENCHANT, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 10000),
    BLOCKER("Blocker", Material.REDSTONE_BLOCK, Particle.SMOKE, Sound.BLOCK_ANVIL_PLACE, 15000);

    private final String displayName;
    private final Material displayMaterial;
    private final Particle particle;
    private final Sound sound;
    private final long defaultDuration;

    PowerUpType(String displayName, Material displayMaterial, Particle particle, Sound sound, long defaultDuration) {
        this.displayName = displayName;
        this.displayMaterial = displayMaterial;
        this.particle = particle;
        this.sound = sound;
        this.defaultDuration = defaultDuration;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    public Particle getParticle() {
        return particle;
    }

    public Sound getSound() {
        return sound;
    }

    public long getDefaultDuration() {
        return defaultDuration;
    }
}