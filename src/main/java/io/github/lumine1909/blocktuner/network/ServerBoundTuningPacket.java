/*
 *     Copyright (c) 2022, xwjcool.
 *     Copyright (c) 2025, Lumine1909.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.lumine1909.blocktuner.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNullByDefault;

import static io.github.lumine1909.blocktuner.BlockTuner.id;

@NotNullByDefault
public record ServerBoundTuningPacket(BlockPos blockPos, int note) implements CustomPacketPayload {

    public static final Identifier ID = id("server_bound_tuning");
    public static final CustomPacketPayload.Type<ServerBoundTuningPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundTuningPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ServerBoundTuningPacket::blockPos,
        ByteBufCodecs.INT,
        ServerBoundTuningPacket::note,
        ServerBoundTuningPacket::new
    );

    public static void receive(ServerBoundTuningPacket payload, ServerPlayNetworking.Context context) {
        BlockPos pos = payload.blockPos();
        if (!context.player().isWithinBlockInteractionRange(pos, 1.0F)) {
            return;
        }
        int note = payload.note();
        ServerLevel world = context.player().level();

        if (world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) {
            return;
        }
        world.setBlock(pos, world.getBlockState(pos).setValue(NoteBlock.NOTE, note), 2 | 16);
        if (world.getBlockState(pos.above()).isAir()) {
            NoteBlock block = (NoteBlock) world.getBlockState(pos).getBlock();
            world.blockEvent(pos, block, 0, 0);
            world.gameEvent(context.player(), GameEvent.NOTE_BLOCK_PLAY, pos);
        }
        context.player().swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}