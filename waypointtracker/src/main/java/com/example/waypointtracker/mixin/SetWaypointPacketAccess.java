package com.example.waypointtracker.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetWaypointPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;
import java.util.UUID;

@Mixin(ClientboundSetWaypointPacket.class)
public interface SetWaypointPacketAccess {
    @Accessor("id")
    Either<UUID, ?> getId();

    @Accessor("pos")
    Optional<BlockPos> getPos();

    @Accessor("angle")
    Optional<Float> getAngle();

    @Accessor("label")
    Optional<Component> getLabel();
}
