package de.somkat.iceboatracing.powerups;

import de.somkat.iceboatracing.race.RacePlayer;
import org.bukkit.entity.Player;

public class PowerUp {

    private PowerUpType type;
    private RacePlayer owner;
    private long startTime;
    private long duration;
    private boolean active;

    public PowerUp(PowerUpType type, RacePlayer owner, long duration) {
        this.type = type;
        this.owner = owner;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.active = true;
    }

    public PowerUp(PowerUpType type, RacePlayer owner) {
        this(type, owner, type.getDefaultDuration());
    }

    public void activate() {
        if (!active) return;

        Player player = owner.getPlayer();

        switch (type) {
            case BOOST:
                applyBoost(player);
                break;
            case SHIELD:
                applyShield(player);
                break;
            case BLOCKER:
                applyBlocker(player);
                break;
        }

        // Play sound and particles
        player.playSound(player.getLocation(), type.getSound(), 1.0f, 1.0f);
        player.getWorld().spawnParticle(type.getParticle(), player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    private void applyBoost(Player player) {
        if (owner.getBoat() != null) {
            // Apply speed boost to boat
            owner.getBoat().setVelocity(owner.getBoat().getVelocity().multiply(2.5));
        }
    }

    private void applyShield(Player player) {
        // Shield effect - protection from blockers and obstacles
        // This would be handled in the collision/damage listeners
    }

    private void applyBlocker(Player player) {
        // Create obstacle/slowdown effect for other players
        // This would spawn blocks or effects behind the player
    }

    public void deactivate() {
        active = false;

        // Remove any ongoing effects
        switch (type) {
            case SHIELD:
                // Remove shield effects
                break;
            case BLOCKER:
                // Remove blocker effects
                break;
        }
    }

    public boolean isExpired() {
        return !active || (System.currentTimeMillis() - startTime) >= duration;
    }

    public long getRemainingTime() {
        if (!active) return 0;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }

    public double getRemainingPercent() {
        if (!active || duration == 0) return 0.0;
        return (double) getRemainingTime() / duration;
    }

    // Getters
    public PowerUpType getType() {
        return type;
    }

    public RacePlayer getOwner() {
        return owner;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isActive() {
        return active;
    }
}