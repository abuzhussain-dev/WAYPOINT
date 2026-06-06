package com.example.waypointtracker.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.protocol.game.ClientboundResetWaypointPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ClientboundResetWaypointPacket.class)
public interface ResetWaypointPacketAccess {
    @Accessor("id")
    Either<UUID, ?> getId();
}
