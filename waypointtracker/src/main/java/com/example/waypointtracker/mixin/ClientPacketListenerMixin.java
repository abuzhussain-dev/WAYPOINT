package com.example.waypointtracker.mixin;

import com.example.waypointtracker.WaypointTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundResetWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundSetWaypointPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetWaypoint", at = @At("HEAD"))
    private void onSetWaypoint(ClientboundSetWaypointPacket packet, CallbackInfo ci) {
        SetWaypointPacketAccess access = (SetWaypointPacketAccess) packet;
        UUID uuid = access.getId().left().orElse(null);
        if (uuid == null) return;
        WaypointTracker.getInstance().handleSetWaypoint(uuid, access.getPos(), access.getAngle(), access.getLabel());
    }

    @Inject(method = "handleResetWaypoint", at = @At("HEAD"))
    private void onResetWaypoint(ClientboundResetWaypointPacket packet, CallbackInfo ci) {
        ResetWaypointPacketAccess access = (ResetWaypointPacketAccess) packet;
        UUID uuid = access.getId().left().orElse(null);
        if (uuid != null) WaypointTracker.getInstance().handleResetWaypoint(uuid);
    }
}
