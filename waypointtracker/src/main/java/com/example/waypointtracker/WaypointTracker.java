package com.example.waypointtracker;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointTracker {
    private static final WaypointTracker INSTANCE = new WaypointTracker();
    public static WaypointTracker getInstance() { return INSTANCE; }

    private static final long STALE_TIMEOUT = 30000;

    private final Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
    private final Map<UUID, WaypointPin> activePins = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPacket = new ConcurrentHashMap<>();

    private final TriangulationEngine engine = new TriangulationEngine();
    private final WaypointDatabase database = new WaypointDatabase();
    private final GuideManager guide = new GuideManager();

    public static class WaypointPin {
        public final String name;
        public final double x, z;
        public final String method;
        public final double confidence;
        public final long time;
        public WaypointPin(String name, double x, double z, String method, double confidence) {
            this.name = name; this.x = x; this.z = z;
            this.method = method; this.confidence = confidence; this.time = System.currentTimeMillis();
        }
    }

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHud);
    }

    public void handleSetWaypoint(UUID uuid, Optional<BlockPos> pos, Optional<Float> angle, Optional<Component> label) {
        if (uuid == null) return;
        updateNameMap();
        String name = uuidToName.getOrDefault(uuid, uuid.toString().substring(0, 8));
        lastPacket.put(uuid, System.currentTimeMillis());

        if (pos.isPresent()) {
            BlockPos p = pos.get();
            double conf = 1.0;
            activePins.put(uuid, new WaypointPin(name, p.getX(), p.getZ(), "EXACT", conf));
            database.record(uuid, name, p.getX(), p.getZ(), "EXACT", conf);
            guide.sendSuccess(uuid, name, p.getX(), p.getZ(), "EXACT", conf);
            engine.clear(uuid);
            return;
        }

        if (angle.isPresent()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Vec3 cur = mc.player.position();
            engine.addSample(uuid, cur, angle.get());
            trySolve(uuid, name);
        }
    }

    public void handleResetWaypoint(UUID uuid) {
        activePins.remove(uuid);
        engine.clear(uuid);
        lastPacket.remove(uuid);
    }

    private void trySolve(UUID uuid, String name) {
        Vec3 est = engine.solve(uuid);
        if (est == null) return;

        int n = engine.getSampleCount(uuid);
        double conf = (n <= 2) ? 0.72 : Math.min(0.95, 0.35 + (n * 0.06));

        WaypointPin old = activePins.get(uuid);
        if (old != null) {
            double jump = Math.hypot(est.x - old.x, est.z - old.z);
            if (jump > 60.0) {
                engine.clear(uuid);
                return;
            }
        }

        String method = (n <= 2) ? "2-POINT" : "LEAST-SQUARES";
        activePins.put(uuid, new WaypointPin(name, est.x, est.z, method, conf));
        database.record(uuid, name, est.x, est.z, method, conf);
        guide.sendSuccess(uuid, name, est.x, est.z, method, conf);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Long>> it = lastPacket.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now - e.getValue() > STALE_TIMEOUT) {
                UUID uuid = e.getKey();
                activePins.remove(uuid);
                engine.clear(uuid);
                it.remove();
            }
        }

        for (UUID uuid : lastPacket.keySet()) {
            WaypointPin pin = activePins.get(uuid);
            if (pin != null && pin.confidence >= 0.85) continue;

            Vec3 guidePos = engine.getGuideTarget(uuid, mc.player.position());
            if (guidePos != null) {
                String reason = (pin == null) ? "Need 2nd sample" : "Improve lock";
                String name = uuidToName.getOrDefault(uuid, uuid.toString().substring(0, 8));
                guide.sendGuide(uuid, guidePos, name, reason);
            }
        }
    }

    private void onHud(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || activePins.isEmpty()) return;

        Font font = mc.font;
        int y = 10;
        for (WaypointPin pin : activePins.values()) {
            double dist = Math.hypot(mc.player.getX() - pin.x, mc.player.getZ() - pin.z);
            String col = pin.confidence >= 0.95 ? "§a" : pin.confidence >= 0.7 ? "§e" : "§c";
            String txt = String.format(
                "%s[✦] §f%s §7(%.0f, %.0f) §e[%.0fm] §8| §7%s §8| §7%.0f%%",
                col, pin.name, pin.x, pin.z, dist, pin.method, pin.confidence * 100
            );
            graphics.drawString(font, txt, 10, y, 0xFFFFFF, true);
            y += 12;
        }
    }

    private void updateNameMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.getConnection().getOnlinePlayers() == null) return;
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info.getProfile() == null || info.getProfile().getId() == null) continue;
            String n = info.getTabListDisplayName() != null
                ? info.getTabListDisplayName().getString()
                : info.getProfile().getName();
            if (n != null && !n.isEmpty()) uuidToName.put(info.getProfile().getId(), n);
        }
    }
}
