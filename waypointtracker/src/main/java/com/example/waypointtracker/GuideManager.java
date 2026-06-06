package com.example.waypointtracker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuideManager {
    private final Map<UUID, Long> lastGuide = new HashMap<>();
    private static final long COOLDOWN = 6000;

    public void sendGuide(UUID uuid, Vec3 target, String name, String reason) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastGuide.getOrDefault(uuid, 0L) < COOLDOWN) return;
        lastGuide.put(uuid, now);

        Vec3 p = mc.player.position();
        double dx = target.x - p.x;
        double dz = target.z - p.z;
        double dist = Math.hypot(dx, dz);
        String dir = getDirection(dx, dz);

        String msg = String.format(
            "§8[§b►§8] §eGuide: §fMove §b%s §7(%.0fm) §fto §a(%.0f, %.0f) §7| %s §c%s",
            dir, dist, target.x, target.z, reason, name
        );
        mc.player.sendSystemMessage(Component.literal(msg));
    }

    public void sendSuccess(UUID uuid, String name, double x, double z, String method, double confidence) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String color = confidence >= 0.95 ? "§a" : confidence >= 0.7 ? "§e" : "§c";
        String msg = String.format(
            "§8[§d✦§8] §7Tracker: %s%s §8» §f%s §7@ §a(%.0f, %.0f) §8| §7%.0f%% §7| §b%s",
            color, method, name, x, z, confidence * 100, method
        );
        mc.player.sendSystemMessage(Component.literal(msg));
    }

    private String getDirection(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(-dx, dz));
        if (deg < 0) deg += 360;
        if (deg >= 337.5 || deg < 22.5) return "South";
        if (deg < 67.5) return "South-West";
        if (deg < 112.5) return "West";
        if (deg < 157.5) return "North-West";
        if (deg < 202.5) return "North";
        if (deg < 247.5) return "North-East";
        if (deg < 292.5) return "East";
        return "South-East";
    }
}
