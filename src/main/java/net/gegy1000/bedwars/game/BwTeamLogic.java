package net.gegy1000.bedwars.game;

import net.gegy1000.bedwars.game.modifiers.BwGameTriggers;
import net.gegy1000.gl.game.GameTeam;
import net.gegy1000.gl.world.BlockBounds;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public final class BwTeamLogic {
    private final BedWars game;

    BwTeamLogic(BedWars game) {
        this.game = game;
    }

    public void applyEnchantments(GameTeam team) {
        this.game.state.participantsFor(team).forEach(participant -> {
            ServerPlayerEntity player = participant.player();
            if (player != null) {
                this.game.playerLogic.applyEnchantments(player, participant);
            }
        });
    }

    public boolean canRespawn(BwState.Participant participant) {
        return this.tryRespawn(participant) != null;
    }

    @Nullable
    public BwMap.TeamSpawn tryRespawn(BwState.Participant participant) {
        BwState.TeamState teamState = this.game.state.getTeam(participant.team);
        if (teamState != null && teamState.hasBed) {
            return this.game.map.getTeamSpawn(participant.team);
        }

        return null;
    }

    public void onBedBroken(ServerPlayerEntity player, BlockPos pos) {
        GameTeam destroyerTeam = null;

        BwState.Participant participant = this.game.state.getParticipant(player);
        if (participant != null) {
            destroyerTeam = participant.team;
        }

        Bed bed = this.findBed(pos);
        if (bed == null || bed.team.equals(destroyerTeam)) {
            return;
        }

        bed.bounds.iterate().forEach(p -> this.game.world.setBlockState(p, Blocks.AIR.getDefaultState(), 0b100010));
        this.game.broadcast.broadcastBedBroken(player, bed.team, destroyerTeam);
        game.triggerModifiers(BwGameTriggers.BED_BROKEN);

        BwState.TeamState teamState = this.game.state.getTeam(bed.team);
        if (teamState != null) {
            teamState.hasBed = false;
        }

        this.game.scoreboardLogic.markDirty();
    }

    @Nullable
    private Bed findBed(BlockPos pos) {
        for (GameTeam team : this.game.config.getTeams()) {
            BwMap.TeamRegions teamRegions = this.game.map.getTeamRegions(team);
            BlockBounds bed = teamRegions.bed;
            if (bed != null && bed.contains(pos)) {
                return new Bed(team, bed);
            }
        }
        return null;
    }

    private static class Bed {
        final GameTeam team;
        final BlockBounds bounds;

        Bed(GameTeam team, BlockBounds bounds) {
            this.team = team;
            this.bounds = bounds;
        }
    }
}