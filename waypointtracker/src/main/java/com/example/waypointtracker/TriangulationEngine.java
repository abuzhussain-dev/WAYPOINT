package com.example.waypointtracker;

import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TriangulationEngine {
    private static final double MIN_SAMPLE_DISTANCE = 8.0;
    private static final double MIN_ANGLE_DELTA = Math.toRadians(2.0);

    public static class AzimuthSample {
        public final Vec3 playerPos;
        public final float angle;
        public final long time;
        public AzimuthSample(Vec3 pos, float angle) {
            this.playerPos = pos;
            this.angle = angle;
            this.time = System.currentTimeMillis();
        }
    }

    private final Map<UUID, List<AzimuthSample>> sampleMap = new ConcurrentHashMap<>();

    public void addSample(UUID uuid, Vec3 pos, float angle) {
        List<AzimuthSample> list = sampleMap.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (!list.isEmpty()) {
            AzimuthSample last = list.get(list.size() - 1);
            if (pos.distanceTo(last.playerPos) < MIN_SAMPLE_DISTANCE) {
                list.set(list.size() - 1, new AzimuthSample(pos, angle));
                return;
            }
        }
        list.add(new AzimuthSample(pos, angle));
        if (list.size() > 12) list.remove(0);
    }

    public Vec3 solve(UUID uuid) {
        List<AzimuthSample> list = sampleMap.get(uuid);
        if (list == null || list.size() < 2) return null;
        if (list.size() == 2) return solveTwoPoint(list);
        return solveLeastSquares(list);
    }

    private Vec3 solveTwoPoint(List<AzimuthSample> list) {
        AzimuthSample s1 = list.get(0);
        AzimuthSample s2 = list.get(list.size() - 1);
        double x1 = s1.playerPos.x, z1 = s1.playerPos.z;
        double x2 = s2.playerPos.x, z2 = s2.playerPos.z;
        double dx1 = -Math.sin(s1.angle);
        double dz1 =  Math.cos(s1.angle);
        double dx2 = -Math.sin(s2.angle);
        double dz2 =  Math.cos(s2.angle);
        double denom = dx1 * dz2 - dz1 * dx2;
        if (Math.abs(denom) < 1e-8) return null;
        double t = ((x2 - x1) * dz2 - (z2 - z1) * dx2) / denom;
        return new Vec3(x1 + t * dx1, 64.0, z1 + t * dz1);
    }

    private Vec3 solveLeastSquares(List<AzimuthSample> list) {
        int n = list.size();
        double a00 = 0, a01 = 0, a11 = 0;
        double b0 = 0, b1 = 0;
        for (AzimuthSample s : list) {
            double c = Math.cos(s.angle);
            double sn = Math.sin(s.angle);
            a00 += c * c;
            a01 += c * sn;
            a11 += sn * sn;
            double rhs = c * s.playerPos.x + sn * s.playerPos.z;
            b0 += c * rhs;
            b1 += sn * rhs;
        }
        double det = a00 * a11 - a01 * a01;
        if (Math.abs(det) < 1e-8) return null;
        double x = (b0 * a11 - a01 * b1) / det;
        double z = (a00 * b1 - b0 * a01) / det;
        return new Vec3(x, 64.0, z);
    }

    public Vec3 getGuideTarget(UUID uuid, Vec3 currentPos) {
        List<AzimuthSample> list = sampleMap.get(uuid);
        if (list == null || list.isEmpty()) return null;
        if (list.size() >= 2) {
            float delta = Math.abs(list.get(0).angle - list.get(list.size() - 1).angle);
            while (delta > (float) Math.PI) delta -= 2 * (float) Math.PI;
            if (Math.abs(delta) > MIN_ANGLE_DELTA) return null;
        }
        AzimuthSample first = list.get(0);
        double px = Math.cos(first.angle);
        double pz = Math.sin(first.angle);
        return new Vec3(currentPos.x + px * 80, currentPos.y, currentPos.z + pz * 80);
    }

    public void clear(UUID uuid) {
        sampleMap.remove(uuid);
    }

    public int getSampleCount(UUID uuid) {
        List<AzimuthSample> list = sampleMap.get(uuid);
        return list == null ? 0 : list.size();
    }
}
