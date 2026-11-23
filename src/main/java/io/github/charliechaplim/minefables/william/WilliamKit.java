package io.github.charliechaplim.minefables.william;

import com.mojang.brigadier.Command;
import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import io.github.charliechaplim.minefables.resource.ResourceService;
import io.github.charliechaplim.minefables.resource.ResourceType;
import io.github.charliechaplim.minefables.network.ResourceNetworking;
import io.github.charliechaplim.minefables.network.WilliamStatusPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.Blocks;
import io.github.charliechaplim.minefables.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.Locale;

/** Sistema de William: somente servidor. O estado e os cooldowns usam NBT persistente. */
public final class WilliamKit {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("minefables", "william");
    private static final String ROOT = "minefables_william";
    private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath("minefables", "dragon_override_armor");
    private static final String TEAM = "mf_william_red";
    private static final long SECOND = 20L;
    private static final long IMPETUO_CD = 5 * SECOND;
    private static final long WALKER_CD = 30 * SECOND;
    private static final long OVERDRIVE_CD = 20 * SECOND;
    private static final long DIVE_CD = 5 * SECOND;
    private static final long DIVE_MARKED_CD = 90 * SECOND;
    private static final long DRAGON_CD = 250 * SECOND;
    private static final long FIREBALL_CD = 10L; // 0.5 segundo
    private static final long STEAM_CD = 40L;
    private static final long ZONE_CD = 500 * SECOND;
    private static final ThreadLocal<Boolean> REPEATING = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, List<DelayedHit>> PENDING_HITS = new HashMap<>();
    private record DelayedHit(UUID targetId, ResourceKey<Level> dimension, float damage, long triggerTick, boolean sparks) {}
    public static boolean isRepeatDamage() { return REPEATING.get(); }
    private static final long SLOTS_CD = 15 * SECOND;
    private static final DustParticleOptions GOLD_SPARK = new DustParticleOptions(new Vector3f(1f, .69f, .09f), 1.45f);
    private static final DustParticleOptions BRIGHT_SPARK = new DustParticleOptions(new Vector3f(1f, .93f, .36f), 1.0f);

    private WilliamKit() {}

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(ROOT)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    public static int level(ServerPlayer p) { return Math.max(1, Math.min(6, data(p).getInt("level"))); }
    public static boolean hasArsenal(ServerPlayer p) { return false; } // Arsenal nao integra este rework
    public static boolean isWilliam(ServerPlayer p) { return PlayerCharacterService.isUsing(p, ID); }
    public static boolean isResonating(ServerPlayer p) { return isWilliam(p) && data(p).getBoolean("resonance"); }
    public static boolean isUltimateActive(ServerPlayer p) {
        return isWilliam(p) && data(p).getBoolean("ultimate") && time(p) < data(p).getLong("ultimate_until");
    }

    /** Estado visual enviado pelo servidor: nivel, modo e tempo restantes. */
    public static void sync(ServerPlayer p) {
        if (!isWilliam(p)) {
            ResourceNetworking.sendWilliam(p, new WilliamStatusPayload(1, false, 0, false, 0, 0, new int[6], new int[4]));
            return;
        }
        CompoundTag tag = data(p);
        boolean isUltimate = isUltimateActive(p);
        int remaining = isUltimate ? (int) Math.min(3600L, Math.max(0L,
                (tag.getLong("ultimate_until") - time(p) + 19L) / 20L)) : 0;
        String[] normals = {"resonance_ready","impetuo_ready","walker_ready","slots_ready","dive_ready","dragon_ready"};
        String[] ultimates = {"fireball_ready","steam_ready","overdrive_ready","zone_ready"};
        int[] a = new int[normals.length], b = new int[ultimates.length];
        long now = time(p);
        for (int i = 0; i < a.length; i++) a[i] = (int) Math.max(0, Math.min(20000, tag.getLong(normals[i]) - now));
        for (int i = 0; i < b.length; i++) b[i] = (int) Math.max(0, Math.min(20000, tag.getLong(ultimates[i]) - now));
        ResourceNetworking.sendWilliam(p, new WilliamStatusPayload(level(p), isUltimate,
                tag.getBoolean("resonance") ? tier(p) : 0, tag.getBoolean("impetuo_armed"),
                remaining, tag.getInt("temperature"), a, b));
    }

    /** Dissolve o aro da mão e projeta estilhaços dourados no impacto. */
    private static void impetuoBurst(ServerPlayer attacker, LivingEntity target) {
        ServerLevel level = attacker.serverLevel();
        // Posição aproximada do punho, compatível com destros e canhotos.
        Vec3 forward = attacker.getLookAngle();
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        double side = attacker.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        Vec3 hand = attacker.position().add(0, attacker.getEyeHeight() * .68, 0)
                .add(forward.scale(.42)).add(right.scale(.29 * side));
        // O efeito parte da mão, não é um anel em volta do corpo.
        for (int i = 0; i < 20; i++) {
            double angle = 2.0 * Math.PI * i / 20;
            double speed = .08 + .05 * ((i % 4) / 3.0);
            level.sendParticles(i % 4 == 0 ? BRIGHT_SPARK : GOLD_SPARK,
                    hand.x, hand.y, hand.z, 0,
                    Math.cos(angle) * speed, .025 + (i % 3) * .025,
                    Math.sin(angle) * speed, 1.0);
        }
        Vec3 hit = target.position().add(0, target.getBbHeight() * .55, 0);
        for (int i = 0; i < 36; i++) {
            double angle = 2.0 * Math.PI * i / 36;
            double speed = .095 + (i % 5) * .011;
            level.sendParticles(i % 5 == 0 ? BRIGHT_SPARK : GOLD_SPARK,
                    hit.x, hit.y, hit.z, 0,
                    Math.cos(angle) * speed, (i % 7 - 3) * .026,
                    Math.sin(angle) * speed, 1.0);
        }
        level.sendParticles(BRIGHT_SPARK, hit.x, hit.y, hit.z, 12,
                .16, .18, .16, .045);
    }

    private static void tierReached(ServerPlayer p, int tier) {
        WilliamVfx.resonance(p, tier);
    }

    private static long time(ServerPlayer p) { return p.getServer().overworld().getGameTime(); }

    public static boolean readPage(ServerPlayer p) {
        if (!isWilliam(p)) {  return false; }
        int lvl = level(p);
        if (lvl >= 6) {  return false; }
        data(p).putInt("level", lvl + 1);
        
        sync(p);
        return true;
    }

    private static boolean require(ServerPlayer p, int lvl) {
        if (!isWilliam(p)) return false;
        if (level(p) < lvl) {  return false; }
        return true;
    }

    private static boolean available(CompoundTag tag, String name, long now, long cd, ServerPlayer p) {
        long left = tag.getLong(name) - now;
        if (left > 0) {  return false; }
        return true;
    }


    public static boolean resonance(ServerPlayer p) {
        if (!require(p, 1)) return false;
        CompoundTag tag = data(p);
        if (tag.getBoolean("resonance")) {
            stopResonance(p);
            
            return true;
        }
        if (!available(tag, "resonance_ready", time(p), 0, p)) return false;
        if (!isUltimateActive(p) && ResourceService.get(p, ResourceType.STAMINA) <= 0) {
             return false;
        }
        tag.putBoolean("resonance", true);
        tag.putLong("resonance_start", time(p));
        tag.putInt("resonance_tier", 1);
        setRedTeam(p, true);
        applyResonance(p);
        tierReached(p, 1);
        
        sync(p);
        return true;
    }

    private static int tier(ServerPlayer p) {
        long ticks = Math.max(0, time(p) - data(p).getLong("resonance_start"));
        return ticks >= 400 ? 3 : ticks >= 200 ? 2 : 1;
    }

    private static void applyResonance(ServerPlayer p) {
        int tier = tier(p);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, tier == 3 ? 1 : 0, false, false));
        if (tier >= 2) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, tier == 3 ? 1 : 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45, 0, false, false));
    }

    private static void setRedTeam(ServerPlayer p, boolean enabled) {
        Scoreboard board = p.serverLevel().getScoreboard();
        String playerName = p.getScoreboardName();
        CompoundTag tag = data(p);
        if (enabled) {
            PlayerTeam prior = board.getPlayersTeam(playerName);
            tag.putString("previous_team", prior == null ? "" : prior.getName());
            PlayerTeam team = board.getPlayerTeam(TEAM);
            if (team == null) team = board.addPlayerTeam(TEAM);
            team.setColor(ChatFormatting.RED);
            board.addPlayerToTeam(playerName, team);
        } else {
            PlayerTeam active = board.getPlayersTeam(playerName);
            if (active == null || !TEAM.equals(active.getName())) return;
            board.removePlayerFromTeam(playerName, active);
            String previous = tag.getString("previous_team");
            if (!previous.isBlank()) {
                PlayerTeam original = board.getPlayerTeam(previous);
                if (original != null) board.addPlayerToTeam(playerName, original);
            }
            tag.remove("previous_team");
        }
    }

    public static void stopResonance(ServerPlayer p) {
        CompoundTag tag = data(p);
        if (!tag.getBoolean("resonance")) return;
        tag.putBoolean("resonance", false);
        tag.putLong("resonance_ready", time(p) + 10 * SECOND);
        tag.remove("resonance_start");
        tag.remove("resonance_tier");
        p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        p.removeEffect(MobEffects.MOVEMENT_SPEED);
        p.removeEffect(MobEffects.GLOWING);
        setRedTeam(p, false);
        WilliamVfx.resonanceOff(p);
        sync(p);
    }

    public static boolean impetuar(ServerPlayer p) {
        if (!require(p, 2)) return false;
        CompoundTag tag = data(p);
        if (tag.getBoolean("impetuo_armed")) {
            
            return false;
        }
        long now = time(p);
        if (!available(tag, "impetuo_ready", now, IMPETUO_CD, p)) return false;
        if (!ResourceService.trySpend(p, ResourceType.STAMINA, 15)) {
             return false;
        }
        // A recarga inicia no IMPACTO, nao enquanto o golpe esta preparado.
        // Sem cronometro: persiste ate que um golpe valido cause dano.
        tag.putBoolean("impetuo_armed", true);
        // Aro 3D desenhado no cliente, preso ao punho: sem partículas espalhadas pelo corpo.
        ResourceNetworking.sendImpetuo(p, true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0f, 1.5f);
        sync(p);
        return true;
    }

    public static void onDamage(LivingDamageEvent.Pre event) {
        // Dano sofrido durante a canalizacao cancela o teleporte, antes do impacto.
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                && WilliamZone.cancelVoidDamage(victim)) {
            event.setNewDamage(0);return;
        }
        // O conjurador nao recebe dano de queda dentro do Vertice Vazio.
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)
                && WilliamZone.cancelFallDamage(victim)) {
            victim.resetFallDistance();
            event.setNewDamage(0);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim && data(victim).getLong("dive_channel_until") > 0) {
            data(victim).remove("dive_channel_until");
            
            sync(victim);
        }
        Entity direct = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer p) || !isWilliam(p) || event.getNewDamage() <= 0
                || event.getEntity() == p || REPEATING.get()) return;
        if (isUltimateActive(p)) event.getEntity().setRemainingFireTicks(Math.max(100,
                event.getEntity().getRemainingFireTicks()));
        // So um golpe fisico basico consome o Impetuo. Projeteis, magia e dano indireto nao.
        if (direct != p || !data(p).getBoolean("impetuo_armed")) return;
        CompoundTag tag = data(p);
        tag.remove("impetuo_armed");
        tag.putLong("impetuo_ready", time(p) + IMPETUO_CD);
        ResourceNetworking.sendImpetuo(p, false);
        float damage = event.getNewDamage();
        WilliamVfx.impetuoImpact(p.serverLevel(), event.getEntity().position()
                .add(0, event.getEntity().getBbHeight() * .55, 0), false);
        List<DelayedHit> queue = PENDING_HITS.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        queue.add(new DelayedHit(event.getEntity().getUUID(), p.serverLevel().dimension(), damage, time(p) + 20, false));
        if (p.getMainHandItem().is(ModItems.FULGOR.get()))
            queue.add(new DelayedHit(event.getEntity().getUUID(), p.serverLevel().dimension(), damage * 1.3f, time(p) + 30, true));
        
        sync(p);
    }

    private static void executeDelayedHits(ServerPlayer p, long now) {
        List<DelayedHit> pending = PENDING_HITS.get(p.getUUID());
        if (pending == null) return;
        var iterator = pending.iterator();
        while (iterator.hasNext()) {
            DelayedHit hit = iterator.next();
            if (hit.triggerTick > now) continue;
            iterator.remove();
            ServerLevel level = p.getServer().getLevel(hit.dimension);
            if (level == null || !(level.getEntity(hit.targetId) instanceof LivingEntity target) || !target.isAlive()) continue;
            REPEATING.set(true);
            try {
                // Novo hit real: permite resistencias/protecao normalmente; nao ativa novos procs.
                target.hurt(p.damageSources().playerAttack(p), hit.damage);
                if (hit.sparks) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + .8,
                            target.getZ(), 40, .5, .5, .5, .12);
                }
                impetuoBurst(p, target);
                WilliamVfx.impetuoImpact(level, target.position()
                        .add(0, target.getBbHeight() * .55, 0), hit.sparks);
            } finally { REPEATING.set(false); }
        }
        if (pending.isEmpty()) PENDING_HITS.remove(p.getUUID());
    }

    public static boolean walker(ServerPlayer p) {
        if (!require(p, 3)) return false;
        CompoundTag tag = data(p);
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = p.serverLevel().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(5)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        BlockPos pos = hit.getBlockPos();
        Container aimed = getStorage(p.serverLevel(), pos);
        // Apontar para um armazenamento altera o vinculo de graca, sem gastar recarga.
        if (aimed != null && !p.isShiftKeyDown()) {
            tag.putString("chest_dimension", p.serverLevel().dimension().location().toString());
            tag.putLong("chest_pos", pos.asLong());
            tag.putBoolean("chest_linked", true);
            WilliamVfx.link(p.serverLevel(), Vec3.atCenterOf(pos));
            
            return true;
        }
        if (!tag.getBoolean("chest_linked")) {  return false; }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("chest_dimension"));
        if (id == null) return false;
        ServerLevel remote = p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (remote == null) return false;
        BlockPos linked = BlockPos.of(tag.getLong("chest_pos"));
        remote.getChunk(linked.getX() >> 4, linked.getZ() >> 4);
        Container storage = getStorage(remote, linked);
        if (storage == null) { tag.putBoolean("chest_linked", false);  return false; }
        if (p.isShiftKeyDown()) {
            // Interface de baus vanilla 27/54 slots a distancia; outros tamanhos exigem menu proprio.
            if (storage.getContainerSize() == 27 || storage.getContainerSize() == 54) {
                WilliamVfx.link(p.serverLevel(), p.position());
                // ChestMenu testa a distância de stillValid a cada tick: usa uma janela remota
                // delegando todos os itens ao inventário verdadeiro, mas sem limite de distância.
                Container remoteView = new RemoteContainer(storage);
                p.openMenu(new SimpleMenuProvider((idMenu, inv, player) -> remoteView.getContainerSize() == 54
                        ? ChestMenu.sixRows(idMenu, inv, remoteView)
                        : ChestMenu.threeRows(idMenu, inv, remoteView), Component.literal("Caminhante da Luz")));
                return true;
            }
            
            return false;
        }
        long now = time(p);
        if (!available(tag, "walker_ready", now, WALKER_CD, p)) return false;
        ItemStack carried = p.getMainHandItem();
        if (carried.isEmpty() || WilliamCrazySlots.isTemporary(carried)) return false;
        if (!canFit(storage, carried)) {  return false; }
        if (!ResourceService.trySpend(p, ResourceType.STAMINA, 40)) return false;
        insert(storage, carried);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        storage.setChanged();
        tag.putLong("walker_ready", now + WALKER_CD);
        WilliamVfx.walkerTravel(p.serverLevel(), p.position(), remote, Vec3.atCenterOf(linked));
        
        sync(p);
        return true;
    }

    /** Janela remota. Não duplica os itens do inventário original. */
    private static final class RemoteContainer implements Container {
        private final Container delegate;
        private RemoteContainer(Container delegate) { this.delegate = delegate; }
        @Override public int getContainerSize() { return delegate.getContainerSize(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return delegate.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return delegate.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return delegate.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { delegate.setItem(slot, stack); }
        @Override public void setChanged() { delegate.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { delegate.clearContent(); }
        @Override public int getMaxStackSize() { return delegate.getMaxStackSize(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return delegate.canPlaceItem(slot, stack); }
    }

    private static Container getStorage(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) return ChestBlock.getContainer(chest, state, level, pos, true);
        if (level.getBlockEntity(pos) instanceof Container container) return container;
        return null;
    }

    private static boolean canFit(Container chest, ItemStack stack) {
        int free = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack slot = chest.getItem(i);
            if (slot.isEmpty()) free += Math.min(stack.getMaxStackSize(), chest.getMaxStackSize());
            else if (ItemStack.isSameItemSameComponents(slot, stack))
                free += Math.max(0, Math.min(slot.getMaxStackSize(), chest.getMaxStackSize()) - slot.getCount());
            if (free >= stack.getCount()) return true;
        }
        return false;
    }

    private static void insert(Container chest, ItemStack stack) {
        int left = stack.getCount();
        for (int i = 0; i < chest.getContainerSize() && left > 0; i++) {
            ItemStack slot = chest.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int amount = Math.min(left, Math.max(0, Math.min(slot.getMaxStackSize(), chest.getMaxStackSize()) - slot.getCount()));
                slot.grow(amount); left -= amount;
            }
        }
        for (int i = 0; i < chest.getContainerSize() && left > 0; i++) {
            if (chest.getItem(i).isEmpty()) {
                int amount = Math.min(left, Math.min(stack.getMaxStackSize(), chest.getMaxStackSize()));
                ItemStack inserted = stack.copy(); inserted.setCount(amount);
                chest.setItem(i, inserted); left -= amount;
            }
        }
    }

    public static boolean slots(ServerPlayer p) {
        if (!require(p, 4)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (!available(tag, "slots_ready", now, SLOTS_CD, p)) return false;
        if (ResourceService.get(p, ResourceType.STAMINA) < 30) return false;
        if (!WilliamCrazySlots.summon(p, now)) return false;
        ResourceService.trySpend(p, ResourceType.STAMINA, 30);
        // O cooldown da Citacao inicia em WilliamCrazySlots.restore(),
        // quando a arma temporaria desaparece (inclusive termino antecipado).
        sync(p);
        return true;
    }

    public static boolean dive(ServerPlayer p) {
        if (!require(p, 5)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (p.isShiftKeyDown()) {
            if (tag.getBoolean("dive_marked")) {
                tag.putBoolean("dive_marked", false);
                WilliamVfx.diveMark(p, false);
                
            } else {
                tag.putBoolean("dive_marked", true);
                tag.putString("dive_dimension", p.serverLevel().dimension().location().toString());
                tag.putDouble("dive_x", p.getX());tag.putDouble("dive_y", p.getY());tag.putDouble("dive_z", p.getZ());
                WilliamVfx.diveMark(p, true);
                
            }
            return true;
        }
        if (tag.getBoolean("dive_marked")) {
            if (!available(tag, "dive_marked_ready", now, DIVE_MARKED_CD, p)) return false;
            if (tag.getLong("dive_channel_until") > now) return false;
            tag.putLong("dive_channel_until", now + 5 * SECOND);
            WilliamVfx.channel(p, now);
            
            return true;
        }
        if (!available(tag, "dive_ready", now, DIVE_CD, p)) return false;
        Vec3 origin = p.position(); Vec3 direction = p.getLookAngle().normalize();
        BlockHitResult wall = p.serverLevel().clip(new ClipContext(p.getEyePosition(),
                p.getEyePosition().add(direction.scale(25)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double distance = Math.min(25, wall.getLocation().distanceTo(p.getEyePosition()) - 1);
        Vec3 safe = null;
        for (double d = distance; d > 1; d -= .5) {
            Vec3 candidate = origin.add(direction.scale(d));
            if (p.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(candidate)) &&
                    p.serverLevel().noCollision(p, p.getBoundingBox().move(candidate.subtract(origin)))) { safe=candidate;break; }
        }
        if (safe == null || !ResourceService.trySpend(p, ResourceType.STAMINA, 20)) return false;
        p.teleportTo(safe.x, safe.y, safe.z);p.resetFallDistance();
        tag.putLong("dive_ready", now + DIVE_CD);
        WilliamVfx.diveTravel(p.serverLevel(), origin, p.serverLevel(), safe);
        sync(p);return true;
    }

    private static void completeDive(ServerPlayer p, CompoundTag tag, long now) {
        tag.remove("dive_channel_until");
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("dive_dimension"));
        ServerLevel target = dim == null ? null : p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (target == null) { return; }
        Vec3 dst = new Vec3(tag.getDouble("dive_x"),tag.getDouble("dive_y"),tag.getDouble("dive_z"));
        target.getChunk(BlockPos.containing(dst).getX() >> 4,BlockPos.containing(dst).getZ() >>4);
        if (!target.noCollision(p, p.getBoundingBox().move(dst.subtract(p.position())))) {  return; }
        ResourceService.set(p, ResourceType.STAMINA, 0);
        tag.putLong("dive_marked_ready", now + DIVE_MARKED_CD);
        ServerLevel originLevel = p.serverLevel();
        Vec3 originPos = p.position();
        p.teleportTo(target,dst.x,dst.y,dst.z,p.getYRot(),p.getXRot());
        WilliamVfx.diveTravel(originLevel, originPos, target, dst);
        p.resetFallDistance(); sync(p);
    }

    public static boolean dragon(ServerPlayer p) {
        if (!require(p, 6)) return false;
        // A Zona encerra a Reescrita Draconica quando e conjurada. Enquanto
        // William for o conjurador dentro dela, recastar esta habilidade
        // FINALIZA a sessao — nunca reativa a transformacao nem a interrompe
        // apenas por cooldown. exitGroup aplica os 500 s ao encerrar a Zona.
        if (WilliamZone.isCasterInside(p)) {
            return WilliamZone.exitGroup(p);
        }
        CompoundTag tag = data(p);long now=time(p);
        if (isUltimateActive(p) || !available(tag,"dragon_ready",now, DRAGON_CD,p)) return false;
        tag.putBoolean("ultimate",true);tag.putLong("ultimate_until",now+60*SECOND);
        tag.putInt("temperature",0);
        refreshDragonFireResistance(p);
        tag.putBoolean("had_flight",p.getAbilities().mayfly);
        p.getAbilities().mayfly=true;p.onUpdateAbilities();
        var armor=p.getAttribute(Attributes.ARMOR);
        if (armor!=null) {armor.removeModifier(ARMOR_ID);armor.addTransientModifier(new AttributeModifier(ARMOR_ID,10.0,AttributeModifier.Operation.ADD_VALUE));}
        ResourceNetworking.sendDragon(p,true);
        WilliamVfx.dragonStart(p);
        sync(p);return true;
    }

    /** Mantem o efeito apenas durante a ultimate, sem substituir pocoes de maior duracao. */
    private static void refreshDragonFireResistance(ServerPlayer p) {
        MobEffectInstance current = p.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current == null || current.getDuration() < 25) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        }
    }

    private static boolean active(ServerPlayer p) {
        if (!require(p,6) || !isUltimateActive(p)) {return false;}return true;
    }

    private static boolean heat(ServerPlayer p) {
        if (data(p).getInt("temperature") >= 4) {return false;}return true;
    }

    private static final String FIREBALL_PROJECTILE_MARK = "minefables_draconic_fireball";

    /** Projétil real. O impacto é interceptado para NUNCA colocar fogo no terreno. */
    public static boolean fireball(ServerPlayer p) {
        if (!active(p) || !heat(p)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (!available(tag, "fireball_ready", now, FIREBALL_CD, p) ||
                !ResourceService.trySpend(p, ResourceType.STAMINA, 25)) return false;
        Vec3 direction = p.getLookAngle().normalize();
        SmallFireball projectile = new SmallFireball(p.serverLevel(), p, direction);
        projectile.getPersistentData().putBoolean(FIREBALL_PROJECTILE_MARK, true);
        projectile.setPos(p.getX() + direction.x * 0.8,
                p.getEyeY() - 0.18 + direction.y * 0.8,
                p.getZ() + direction.z * 0.8);
        projectile.setDeltaMovement(direction.scale(1.05));
        // 0 anula a propulsao do projetil vanilla e ele perde velocidade a cada tick.
        // 0.06 equilibra aproximadamente o arrasto de 0.95 do SmallFireball.
        projectile.accelerationPower = 0.06;
        p.serverLevel().addFreshEntity(projectile);
        tag.putLong("fireball_ready", now + FIREBALL_CD);
        tag.putInt("temperature", tag.getInt("temperature") + 1);
        WilliamVfx.dragonHeat(p, tag.getInt("temperature"));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1f, 1.2f);
        sync(p);
        return true;
    }

    /** Remove o projétil no impacto; evita o comportamento vanilla de colocar blocos de fogo. */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof SmallFireball projectile) ||
                !projectile.getPersistentData().getBoolean(FIREBALL_PROJECTILE_MARK) ||
                !(projectile.level() instanceof ServerLevel level)) return;
        event.setCanceled(true);
        Vec3 impact = event.getRayTraceResult().getLocation();
        if (event.getRayTraceResult() instanceof EntityHitResult hit &&
                hit.getEntity() != projectile.getOwner()) {
            Entity target = hit.getEntity();
            target.hurt(projectile.damageSources().fireball(projectile, projectile.getOwner()), 7f);
            target.setRemainingFireTicks(Math.max(80, target.getRemainingFireTicks()));
        }
        WilliamVfx.fireballBurst(level, impact, true);
        projectile.discard();
    }

    /** Trilha controlada pelo servidor e expiracao ao atravessar o vazio sem atingir nada. */
    public static void onProjectileTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof SmallFireball fireball)
                || !(fireball.level() instanceof ServerLevel level)
                || !fireball.getPersistentData().getBoolean(FIREBALL_PROJECTILE_MARK)
                || fireball.isRemoved()) return;
        if (fireball.tickCount >= 70 || fireball.isInWater()) {
            WilliamVfx.fireballBurst(level, fireball.position(), false);
            fireball.discard();
            return;
        }
        if (fireball.tickCount % 2 == 0) {
            WilliamVfx.fireballTrail(level, fireball.position(), fireball.tickCount);
        }
    }

    public static boolean steam(ServerPlayer p) {
        if(!active(p)||!heat(p))return false;
        CompoundTag tag=data(p);long now=time(p);
        if(!available(tag,"steam_ready",now,STEAM_CD,p))return false;
        Vec3 start=p.position(), direction=p.getLookAngle().normalize(), safe=null;
        for(double d=5;d>=.5;d-=.5){Vec3 v=start.add(direction.scale(d));
            if(p.serverLevel().noCollision(p,p.getBoundingBox().move(v.subtract(start)))){safe=v;break;}}
        if(safe==null || !ResourceService.trySpend(p,ResourceType.STAMINA,25))return false;
        p.teleportTo(safe.x,safe.y,safe.z);p.resetFallDistance();
        WilliamVfx.steam(p, start, safe);
        for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(2),e->e!=p))
            target.push(direction.x*2.2, .8+Math.max(0,direction.y),direction.z*2.2);
        tag.putLong("steam_ready",now+STEAM_CD);tag.putInt("temperature",tag.getInt("temperature")+1);
        WilliamVfx.dragonHeat(p,tag.getInt("temperature"));
        sync(p);return true;
    }

    public static boolean overload(ServerPlayer p) {
        if(!active(p))return false;
        CompoundTag tag=data(p);long now=time(p);
        if(!available(tag,"overdrive_ready",now,OVERDRIVE_CD,p))return false;
        tag.putLong("overdrive_ready",now+OVERDRIVE_CD);
        tag.putLong("ultimate_until",tag.getLong("ultimate_until")+5*SECOND);
        ResourceService.set(p,ResourceType.STAMINA,0);tag.putInt("temperature",0);
        // Vortice de aproximadamente 15 blocos de diametro: tres bracos
        // espirais de fogo vanilla. Mantem um miolo livre para o conjurador.
        for (int x=-8; x<=8; x++) for (int z=-8; z<=8; z++) {
            double radius = Math.hypot(x,z);
            if (radius < 2.4 || radius > 7.6) continue;
            double spiral = Math.atan2(z,x) + radius * 0.53;
            // Tres bracos com espessura controlada, nao um quadrado de chamas.
            if (Math.cos(3.0 * spiral) < 0.48) continue;
            BlockPos at = p.blockPosition().offset(x,0,z);
            if (p.serverLevel().isEmptyBlock(at)
                    && p.serverLevel().getBlockState(at.below()).isSolidRender(p.serverLevel(),at.below()))
                p.serverLevel().setBlockAndUpdate(at,Blocks.FIRE.defaultBlockState());
        }
        tag.putLong("overload_vortex_until", now+40L);
        renderOverloadVortex(p, now);
        WilliamVfx.overload(p);
        sync(p);return true;
    }

    /** Particulas animadas em espiral por dois segundos apos Sobrecarga. */
    private static void renderOverloadVortex(ServerPlayer p,long now) {
        ServerLevel level = p.serverLevel();
        double phase = (now % 40) * 0.32;
        for (int arm = 0; arm < 3; arm++) {
            for (int i=0; i<48; i++) {
                double progress = i / 47.0;
                double radius = 0.9 + 6.6 * progress;
                double angle = phase + arm * Math.PI * 2.0 / 3.0 + progress * Math.PI * 3.0;
                double x = p.getX() + Math.cos(angle) * radius;
                double y = p.getY() + 0.25 + (1.0-progress)*3.5;
                double z = p.getZ() + Math.sin(angle) * radius;
                level.sendParticles(i % 4 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        x,y,z,1,0,0,0,0.015);
            }
        }
    }

    public static boolean zone(ServerPlayer p) {
        if (!active(p) || !heat(p) || !available(data(p),"zone_ready",time(p),0,p))return false;
        if (!WilliamZone.enter(p))return false;
        // Zona acorta a ultimate; recarga de 500s apenas apos fim da Zona.
        data(p).putBoolean("zone_pending_cooldown",true);
        endUltimate(p);
        sync(p);return true;
    }

    public static void finishZoneCooldown(ServerPlayer p) {
        if (!data(p).getBoolean("zone_pending_cooldown")) return;
        data(p).putBoolean("zone_pending_cooldown", false);
        data(p).putLong("dragon_ready", time(p) + ZONE_CD);
        sync(p);
    }

    /** Morte do conjurador encerra a Zona imediatamente e inicia 500s de recarga. */
    public static void finishZoneCooldownOnDeath(ServerPlayer p) {
        CompoundTag tag = data(p);
        tag.putBoolean("zone_pending_cooldown", false);
        tag.putBoolean("ultimate", false);
        tag.putLong("dragon_ready", time(p) + ZONE_CD);
        // O novo jogador recebe este NBT em PlayerEvent.Clone.
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer dead) WilliamZone.onPlayerDeath(dead);
    }

    public static void endUltimate(ServerPlayer p) {
        CompoundTag tag = data(p);
        if (!tag.getBoolean("ultimate")) return;
        tag.putBoolean("ultimate", false);
        if (!tag.getBoolean("zone_pending_cooldown"))
            tag.putLong("dragon_ready",time(p)+DRAGON_CD);
        p.getAbilities().mayfly = tag.getBoolean("had_flight") || p.isCreative() || p.isSpectator();
        if (!p.getAbilities().mayfly) p.getAbilities().flying = false;
        p.onUpdateAbilities();
        var armor = p.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(ARMOR_ID);
        WilliamCrazySlots.restore(p);
        
        ResourceNetworking.sendDragon(p, false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, .75f, .8f);
        sync(p);
    }

    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        // Tick da Zona para TODOS os players. Participantes de outras Fabulas
        // tambem podem encerrar a Zona ao cair no centro do vortice.
        long now = time(p);
        WilliamZone.tickParticipant(p, now);
        // Limpeza defensiva mesmo apos trocar a Fabula via ferramentas de debug.
        WilliamCrazySlots.purgeExpired(p);
        if (!isWilliam(p)) return;
        CompoundTag tag = data(p);
        executeDelayedHits(p,now);
        if (tag.getLong("overload_vortex_until") > now && now % 2L == 0L)
            renderOverloadVortex(p,now);
        if (tag.getLong("overload_vortex_until") > 0 && now >= tag.getLong("overload_vortex_until"))
            tag.remove("overload_vortex_until");
        if (tag.getLong("dive_channel_until") > 0) {
            if (now >= tag.getLong("dive_channel_until")) completeDive(p,tag,now);
            else if (now % 4 == 0) WilliamVfx.channel(p,now);
        }
        if (tag.getBoolean("ultimate") && now >= tag.getLong("ultimate_until")) endUltimate(p);
        if (isUltimateActive(p) && now % 20 == 0) refreshDragonFireResistance(p);
        WilliamCrazySlots.tick(p, now);
        if (tag.getBoolean("resonance")) {
            int currentTier = tier(p);
            if (currentTier > tag.getInt("resonance_tier")) {
                tag.putInt("resonance_tier", currentTier);
                tierReached(p, currentTier);
                sync(p);
            }
            if (now % 20 == 0) {
                if (!isUltimateActive(p) && !ResourceService.trySpend(p, ResourceType.STAMINA, 1)) {
                    stopResonance(p);
                    
                } else applyResonance(p);
            }
        }
        if (tag.getBoolean("resonance") && now % 8 == 0)
            WilliamVfx.resonanceIdle(p, tier(p), now);
        if (isUltimateActive(p) && now % 10 == 0) {
            p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1,
                    p.getZ(), 8, .35, .65, .35, .01);
            p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(),
                    p.getY() + .35, p.getZ(), 4, .3, .2, .3, .005);
        }
        // Contagem da HUD para o proprio jogador; heartbeat visual para observadores.
        if (now % 10 == 0) {
            sync(p);
            if (isUltimateActive(p)) ResourceNetworking.sendDragon(p, true);
            if (tag.getBoolean("impetuo_armed") && now % 20 == 0) {
                ResourceNetworking.sendImpetuo(p, true);
            }
        }
    }

    /** Novos observadores recebem a transformacao sem esperar o heartbeat. */
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher
                && event.getTarget() instanceof ServerPlayer target) {
            if (isUltimateActive(target)) ResourceNetworking.sendDragonTo(watcher, target);
            if (isWilliam(target) && data(target).getBoolean("impetuo_armed")) {
                ResourceNetworking.sendImpetuoTo(watcher, target);
            }
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            CompoundTag tag = data(p);
            // Recuperacao segura apos logout/crash dentro da Zona.
            if (WilliamZone.isInside(p)) {
                WilliamZone.exitGroup(p);
                finishZoneCooldown(p);
            }
            // Logout, crash ou morte durante a Zona nao deixam a recarga travada.
            if (tag.getBoolean("zone_pending_cooldown")) finishZoneCooldown(p);
            // Em um reinicio nao continuamos ultimates nem buffs temporarios.
            if (tag.getBoolean("ultimate")) endUltimate(p);
            tag.remove("dive_channel_until");
            if (tag.getBoolean("resonance")) stopResonance(p);
            WilliamCrazySlots.restore(p);
            if (isWilliam(p) && tag.getBoolean("impetuo_armed")) ResourceNetworking.sendImpetuo(p, true);
            sync(p);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            if (WilliamZone.isInside(p)) WilliamZone.exitGroup(p);
            endUltimate(p);
            stopResonance(p);
            PENDING_HITS.remove(p.getUUID());
            ResourceNetworking.sendImpetuo(p, false);
        }
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer next) || !(event.getOriginal() instanceof ServerPlayer original)) return;
        // Salvaguarda caso outro mod altere a sequencia dos eventos de morte.
        // onPlayerDeath pode ser chamada novamente: a marca SOURCE ja foi removida.
        if (event.isWasDeath()) WilliamZone.onPlayerDeath(original);
        CompoundTag old = original.getPersistentData().getCompound(ROOT);
        CompoundTag copied = old.copy();
        copied.putBoolean("resonance", false);
        copied.putBoolean("ultimate", false);
        copied.remove("impetuo_armed");
        copied.remove("resonance_start");
        copied.remove("resonance_tier");
        copied.remove("dive_channel_until");
        next.getPersistentData().put(ROOT, copied);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            WilliamZone.recoverAfterRespawn(p);
            // Se a morte ocorreu durante a Zona, a recarga permanece no clone.
            if (data(p).getBoolean("zone_pending_cooldown")) finishZoneCooldownOnDeath(p);
            ResourceNetworking.sendImpetuo(p, false);
            sync(p);
        }
    }

    public static void reset(ServerPlayer p) {
        endUltimate(p);
        stopResonance(p);
        WilliamCrazySlots.restore(p);
        p.getPersistentData().remove(ROOT);
        ResourceNetworking.sendImpetuo(p, false);
        sync(p);
    }

    /**
     * Comando de depuracao (OP): reinicia todas as recargas do William.
     * Se a Citacao ainda estiver com uma arma temporaria, restaura primeiro
     * o item original; assim o cooldown nao sera recriado ao terminar a Citacao.
     */
    public static void resetCooldowns(ServerPlayer player) {
        CompoundTag tag = data(player);
        WilliamCrazySlots.restore(player);
        String[] readyKeys = {
                "resonance_ready", "impetuo_ready", "walker_ready",
                "slots_ready", "dive_ready", "dive_marked_ready", "dragon_ready",
                "fireball_ready", "steam_ready", "overdrive_ready", "zone_ready"
        };
        for (String key : readyKeys) tag.remove(key);
        // Uma Zona em andamento poderia recolocar a recarga ao terminar.
        // Reset OP tambem cancela essa recarga pendente, sem fechar a Zona.
        tag.remove("zone_pending_cooldown");
        sync(player);
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("minefables_william")
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CompoundTag tag = data(p);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "William nivel " + level(p) + "/6 | Ressonancia " +
                                    (tag.getBoolean("resonance") ? "ativa (" + tier(p) + ")" : "inativa") +
                                    " | Dragon Override " + (tag.getBoolean("ultimate") ? "ativo" : "inativo")), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reset_cooldowns")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer target = ctx.getSource().getPlayerOrException();
                            resetCooldowns(target);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Cooldowns de William zerados para " + target.getGameProfile().getName() + "."), true);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("jogador", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "jogador");
                            resetCooldowns(target);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Cooldowns de William zerados para " + target.getGameProfile().getName() + "."), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("sair_zona").requires(source -> source.hasPermission(2)).executes(ctx ->
                        WilliamZone.exit(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("instalar_zona").requires(source -> source.hasPermission(2)).executes(ctx ->
                        WilliamZone.installArena(ctx.getSource().getPlayerOrException()) ? 1 : 0)));
    }
}
