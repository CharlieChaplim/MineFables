package io.github.charliechaplim.minefables.william;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Primeira versao da Zona Fabular: dimensao compartilhada, sem efeitos de arena. */
public final class WilliamZone {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("minefables", "zona_fabular"));
    private static final String SOURCE = "mf_zona_origem";
    private WilliamZone() {}

    public static boolean isInside(ServerPlayer p) { return p.serverLevel().dimension().equals(DIMENSION); }

    public static boolean enter(ServerPlayer user) {
        if (isInside(user)) return exitGroup(user);
        ServerLevel dest = user.getServer().getLevel(DIMENSION);
        if (dest == null) {
            user.displayClientMessage(Component.literal("Dimensao minefables:zona_fabular indisponivel."), false);
            return false;
        }
        Vec3 center = user.position();
        // 10 x 10 na horizontal; altura de +/-5 para incluir entidades proximas.
        AABB region = AABB.ofSize(center, 10, 10, 10);
        List<Entity> selected = new ArrayList<>(user.serverLevel().getEntities(user, region, Entity::isAlive));
        selected.add(user);
        int count = transfer(selected, dest, center);
        return count > 0;
    }

    public static boolean exit(ServerPlayer p) { return exitGroup(p); }

    public static boolean exitGroup(ServerPlayer user) {
        if (!isInside(user)) {
            user.displayClientMessage(Component.literal("Voce nao esta na Zona Fabular."), true);
            return false;
        }
        Vec3 center = user.position();
        List<Entity> selected = new ArrayList<>(user.serverLevel().getEntities(user,
                AABB.ofSize(center, 10, 10, 10), e -> e.getPersistentData().contains(SOURCE)));
        selected.add(user);
        int count = 0;
        for (Entity entity : selected) if (returnEntity(entity)) count++;
        return count > 0;
    }

    private static int transfer(List<Entity> entities, ServerLevel dest, Vec3 sourceCenter) {
        int moved = 0;
        for (Entity entity : entities) {
            if (!(entity.level() instanceof ServerLevel origin) || !entity.isAlive()
                    || entity.getPersistentData().contains(SOURCE)) continue;
            CompoundTag back = new CompoundTag();
            back.putString("dimension", origin.dimension().location().toString());
            back.putDouble("x", entity.getX());
            back.putDouble("y", entity.getY());
            back.putDouble("z", entity.getZ());
            entity.getPersistentData().put(SOURCE, back);
            // Dimensao com piso plano. Mantem deslocamento relativo do grupo.
            double x = entity.getX();
            double z = entity.getZ();
            int surface = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(x, 0, z).getX(), BlockPos.containing(x, 0, z).getZ());
            Vec3 target = new Vec3(x, surface + 1.0, z);
            entity.stopRiding();
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(dest, target.x, target.y, target.z, player.getYRot(), player.getXRot());
                player.displayClientMessage(Component.literal("ZONA FABULAR — use /minefables_william sair_zona para retornar."), false);
                moved++;
            } else if (entity.changeDimension(new DimensionTransition(dest, target, Vec3.ZERO,
                    entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING)) != null) {
                moved++;
            }
        }
        return moved;
    }

    private static boolean returnEntity(Entity entity) {
        CompoundTag nbt = entity.getPersistentData();
        if (!nbt.contains(SOURCE)) return false;
        CompoundTag back = nbt.getCompound(SOURCE);
        ResourceLocation id = ResourceLocation.tryParse(back.getString("dimension"));
        if (id == null || entity.getServer() == null) return false;
        ServerLevel dest = entity.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (dest == null) return false;
        Vec3 point = new Vec3(back.getDouble("x"), back.getDouble("y"), back.getDouble("z"));
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(dest, point.x, point.y, point.z, player.getYRot(), player.getXRot());
            player.getPersistentData().remove(SOURCE);
        } else {
            Entity relocated = entity.changeDimension(new DimensionTransition(dest, point, Vec3.ZERO,
                    entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
            if (relocated == null) return false;
            relocated.getPersistentData().remove(SOURCE);
        }
        return true;
    }
}
