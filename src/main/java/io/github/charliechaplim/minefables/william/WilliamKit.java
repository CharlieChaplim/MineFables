package io.github.charliechaplim.minefables.william;

import com.mojang.brigadier.Command;
import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import io.github.charliechaplim.minefables.resource.ResourceService;
import io.github.charliechaplim.minefables.resource.ResourceType;
import io.github.charliechaplim.minefables.network.ResourceNetworking;
import io.github.charliechaplim.minefables.network.WilliamStatusPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Locale;

/** Sistema de William: somente servidor. O estado e os cooldowns usam NBT persistente. */
public final class WilliamKit {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("minefables", "william");
    private static final String ROOT = "minefables_william";
    private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath("minefables", "dragon_override_armor");
    private static final String TEAM = "mf_william_red";
    private static final long SECOND = 20L;
    private static final long IMPETUO_CD = 10 * SECOND;
    private static final long WALKER_CD = 30 * SECOND;
    private static final long OVERDRIVE_CD = 40 * SECOND;
    private static final long DIVE_CD = 5 * SECOND;
    private static final long SLOTS_CD = 30 * SECOND;
    private static final DustParticleOptions GOLD_SPARK = new DustParticleOptions(new Vector3f(1f, .69f, .09f), 1.45f);
    private static final DustParticleOptions BRIGHT_SPARK = new DustParticleOptions(new Vector3f(1f, .93f, .36f), 1.0f);

    private WilliamKit() {}

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(ROOT)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    public static int level(ServerPlayer p) { return Math.max(1, Math.min(5, data(p).getInt("level"))); }
    public static boolean hasArsenal(ServerPlayer p) { return isWilliam(p) && level(p) >= 3; }
    public static boolean isWilliam(ServerPlayer p) { return PlayerCharacterService.isUsing(p, ID); }
    public static boolean isResonating(ServerPlayer p) { return isWilliam(p) && data(p).getBoolean("resonance"); }
    public static boolean isUltimateActive(ServerPlayer p) {
        return isWilliam(p) && data(p).getBoolean("ultimate") && time(p) < data(p).getLong("ultimate_until");
    }

    /** Estado visual enviado pelo servidor: nivel, modo e tempo restantes. */
    public static void sync(ServerPlayer p) {
        if (!isWilliam(p)) {
            ResourceNetworking.sendWilliam(p, new WilliamStatusPayload(1, false, 0, false, 0));
            return;
        }
        CompoundTag tag = data(p);
        boolean isUltimate = isUltimateActive(p);
        int remaining = isUltimate ? (int) Math.min(3600L, Math.max(0L,
                (tag.getLong("ultimate_until") - time(p) + 19L) / 20L)) : 0;
        ResourceNetworking.sendWilliam(p, new WilliamStatusPayload(level(p), isUltimate,
                tag.getBoolean("resonance") ? tier(p) : 0,
                tag.getBoolean("impetuo_armed"), remaining));
    }

    private static void title(ServerPlayer p, String header, String sub, ChatFormatting color) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(4, 34, 8));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.WHITE)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(header).withStyle(color)));
    }

    /** Aneis luminosos de particulas, renderizados para todos os jogadores proximos. */
    private static void ring(ServerLevel level, Vec3 center, double radius, int amount, ParticleOptions particle) {
        for (int i = 0; i < amount; i++) {
            double angle = i * Math.PI * 2.0 / amount;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
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
        String roman = tier == 3 ? "III" : tier == 2 ? "II" : "I";
        title(p, "RESSONÂNCIA " + roman,
                tier == 3 ? "Potência máxima — Impétuo disponível" :
                        tier == 2 ? "Velocidade I desbloqueada" : "Resistência I ativada",
                ChatFormatting.RED);
        ring(p.serverLevel(), p.position().add(0, 1.0, 0), 1.2, 28, ParticleTypes.END_ROD);
        p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1,
                p.getZ(), 25, .6, .6, .6, .03);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1f, .8f + tier * .15f);
    }
    private static long time(ServerPlayer p) { return p.getServer().overworld().getGameTime(); }

    public static boolean readPage(ServerPlayer p) {
        if (!isWilliam(p)) { say(p, "Somente William pode ler estas paginas."); return false; }
        int lvl = level(p);
        if (lvl >= 5) { say(p, "Todas as paginas de William ja foram recuperadas."); return false; }
        data(p).putInt("level", lvl + 1);
        say(p, "Pagina recuperada! William: nivel " + (lvl + 1) + "/5.");
        sync(p);
        return true;
    }

    private static boolean require(ServerPlayer p, int lvl) {
        if (!isWilliam(p)) return false;
        if (level(p) < lvl) { say(p, "Requer pagina de William: nivel " + lvl + "."); return false; }
        return true;
    }

    private static boolean available(CompoundTag tag, String name, long now, long cd, ServerPlayer p) {
        long left = tag.getLong(name) - now;
        if (left > 0) { say(p, "Recarga: " + ((left + 19) / 20) + "s."); return false; }
        return true;
    }

    private static void say(ServerPlayer p, String message) { p.displayClientMessage(Component.literal(message), true); }

    public static boolean resonance(ServerPlayer p) {
        if (!require(p, 1)) return false;
        CompoundTag tag = data(p);
        if (tag.getBoolean("resonance")) {
            stopResonance(p);
            say(p, "Ressoar [Eficiência] desativado.");
            return true;
        }
        if (ResourceService.get(p, ResourceType.STAMINA) <= 0) { say(p, "Estamina insuficiente."); return false; }
        tag.putBoolean("resonance", true);
        tag.putLong("resonance_start", time(p));
        tag.putInt("resonance_tier", 1);
        setRedTeam(p, true);
        applyResonance(p);
        tierReached(p, 1);
        say(p, "Ressoar [Eficiência] — potência I.");
        sync(p);
        return true;
    }

    private static int tier(ServerPlayer p) {
        long ticks = Math.max(0, time(p) - data(p).getLong("resonance_start"));
        return ticks >= 800 ? 3 : ticks >= 400 ? 2 : 1;
    }

    private static void applyResonance(ServerPlayer p) {
        int tier = tier(p);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, tier == 3 ? 1 : 0, false, false));
        if (tier >= 2) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 0, false, false));
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
        tag.remove("resonance_start");
        tag.remove("resonance_tier");
        p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        p.removeEffect(MobEffects.MOVEMENT_SPEED);
        p.removeEffect(MobEffects.GLOWING);
        setRedTeam(p, false);
        sync(p);
    }

    public static boolean impetuar(ServerPlayer p) {
        if (!require(p, 2)) return false;
        CompoundTag tag = data(p);
        if (tag.getBoolean("impetuo_armed")) {
            say(p, "Impétuo já preparado. Acerte um golpe para consumi-lo.");
            return false;
        }
        if (!isResonating(p) || tier(p) != 3) {
            say(p, "Impétuo requer Ressonância na potência máxima (40s)."); return false;
        }
        long now = time(p);
        if (!available(tag, "impetuo_ready", now, IMPETUO_CD, p)) return false;
        if (!ResourceService.trySpend(p, ResourceType.STAMINA, 10)) {
            say(p, "Estamina insuficiente (10)."); return false;
        }
        // A recarga inicia no IMPACTO, nao enquanto o golpe esta preparado.
        // Sem cronometro: persiste ate que um golpe valido cause dano.
        tag.putBoolean("impetuo_armed", true);
        // Aro 3D desenhado no cliente, preso ao punho: sem partículas espalhadas pelo corpo.
        ResourceNetworking.sendImpetuo(p, true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0f, 1.5f);
        title(p, "IMPÉTUO PREPARADO", "O próximo golpe recebe um segundo dano", ChatFormatting.GOLD);
        sync(p);
        return true;
    }

    public static void onDamage(LivingDamageEvent.Pre event) {
        Entity entity = event.getSource().getEntity();
        if (!(entity instanceof ServerPlayer p) || !isWilliam(p) || event.getNewDamage() <= 0
                || event.getEntity() == p) return;
        CompoundTag tag = data(p);
        if (isUltimateActive(p))
            event.getEntity().setRemainingFireTicks(Math.max(event.getEntity().getRemainingFireTicks(), 100));

        if (tag.getBoolean("impetuo_armed")) {
            tag.remove("impetuo_armed");
            tag.putLong("impetuo_ready", time(p) + IMPETUO_CD);
            ResourceNetworking.sendImpetuo(p, false);
            // Simula o segundo dano efetivo depois das reducoes do primeiro.
            // Nao cria outro evento LivingDamageEvent, knockback ou status adicional.
            event.setNewDamage(event.getNewDamage() * 2f);
            if (event.getEntity().level() instanceof ServerLevel level) {
                impetuoBurst(p, event.getEntity());
                level.playSound(null, event.getEntity().blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS, 1.5f, .7f);
            }
            say(p, "IMPÉTUO! Golpe executado.");
            sync(p);
        }
    }

    public static boolean walker(ServerPlayer p) {
        if (!require(p, 4)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (!available(tag, "walker_ready", now, WALKER_CD, p)) return false;
        // Primeiro prioriza o bau alvejado com a mira. Caso contrario, envia o item da mao.
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(5));
        BlockHitResult hit = p.serverLevel().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        BlockPos pos = hit.getBlockPos();
        ChestBlockEntity lookedChest = p.serverLevel().getBlockEntity(pos) instanceof ChestBlockEntity chest ? chest : null;
        if (lookedChest != null) {
            if (!ResourceService.trySpend(p, ResourceType.STAMINA, 40)) { say(p, "Estamina insuficiente (40)."); return false; }
            tag.putString("chest_dimension", p.serverLevel().dimension().location().toString());
            tag.putLong("chest_pos", pos.asLong());
            tag.putBoolean("chest_linked", true);
            tag.putLong("walker_ready", now + WALKER_CD);
            ring(p.serverLevel(), Vec3.atCenterOf(pos).add(0, .5, 0), 1.2, 30, ParticleTypes.END_ROD);
            p.serverLevel().sendParticles(ParticleTypes.ENCHANT, pos.getX() + .5,
                    pos.getY() + 1.0, pos.getZ() + .5, 45, .5, .5, .5, .3);
            p.serverLevel().playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.PLAYERS, .9f, 1.35f);
            say(p, "Andarilho da Luz: baú vinculado.");
            return true;
        }
        if (!tag.getBoolean("chest_linked")) { say(p, "Olhe para um bau a ate 5 blocos primeiro."); return false; }
        ItemStack carried = p.getMainHandItem();
        if (carried.isEmpty() || WilliamCrazySlots.isTemporary(carried)) { say(p, "Segure um item permanente na mao principal."); return false; }
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("chest_dimension"));
        if (dim == null) return false;
        ServerLevel remote = p.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dim));
        if (remote == null) { say(p, "A dimensao do bau nao existe mais."); return false; }
        BlockPos remotePos = BlockPos.of(tag.getLong("chest_pos"));
        // Forca o carregamento do chunk: funciona mesmo a longas distancias.
        remote.getChunk(remotePos.getX() >> 4, remotePos.getZ() >> 4);
        if (!(remote.getBlockEntity(remotePos) instanceof ChestBlockEntity chest)) {
            tag.putBoolean("chest_linked", false);
            say(p, "Bau removido: vinculo desfeito.");
            return false;
        }
        if (!canFit(chest, carried)) { say(p, "Bau cheio: transferencia cancelada sem cobrar Estamina."); return false; }
        if (!ResourceService.trySpend(p, ResourceType.STAMINA, 40)) { say(p, "Estamina insuficiente (40)."); return false; }
        Vec3 castPosition = p.position().add(0, 1.1, 0);
        insert(chest, carried);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        chest.setChanged();
        tag.putLong("walker_ready", now + WALKER_CD);
        ring(p.serverLevel(), castPosition, .9, 28, ParticleTypes.END_ROD);
        p.serverLevel().sendParticles(ParticleTypes.PORTAL, castPosition.x,
                castPosition.y, castPosition.z, 60, .3, .4, .3, .25);
        Vec3 destinationEffect = Vec3.atCenterOf(remotePos).add(0, .6, 0);
        ring(remote, destinationEffect, 1.0, 28, ParticleTypes.END_ROD);
        remote.sendParticles(ParticleTypes.ENCHANT, destinationEffect.x,
                destinationEffect.y, destinationEffect.z, 45, .4, .4, .4, .3);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, .8f, 1.65f);
        say(p, "Andarilho da Luz: itens enviados ao baú vinculado.");
        return true;
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

    public static boolean dragon(ServerPlayer p) {
        if (!require(p, 5)) return false;
        CompoundTag tag = data(p);
        if (tag.getBoolean("ultimate")) { say(p, "Dragon Override ja esta ativo."); return false; }
        tag.putBoolean("ultimate", true);
        tag.putLong("ultimate_until", time(p) + 60 * SECOND);
        tag.putInt("dive_step", 0);
        tag.putBoolean("had_flight", p.getAbilities().mayfly);
        p.getAbilities().mayfly = true;
        p.onUpdateAbilities();
        var armor = p.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR_ID);
            armor.addTransientModifier(new AttributeModifier(ARMOR_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS, 1.8f, .85f);
        ring(p.serverLevel(), p.position().add(0, .25, 0), 2.6, 60, ParticleTypes.FLAME);
        ring(p.serverLevel(), p.position().add(0, 1.4, 0), 1.5, 48, ParticleTypes.END_ROD);
        p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1,
                p.getZ(), 120, 1.3, 1.1, 1.3, .08);
        title(p, "DRAGON OVERRIDE", "Ultimate ativada — 60 segundos", ChatFormatting.GOLD);
        say(p, "DRAGON OVERRIDE: 60 segundos!");
        ResourceNetworking.sendDragon(p, true);
        sync(p);
        return true;
    }

    private static boolean active(ServerPlayer p) {
        if (!require(p, 5)) return false;
        CompoundTag t = data(p);
        if (!t.getBoolean("ultimate") || time(p) >= t.getLong("ultimate_until")) {
            say(p, "Ative Dragon Override primeiro."); return false;
        }
        return true;
    }

    public static boolean overdrive(ServerPlayer p) {
        if (!active(p)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (!available(tag, "overdrive_ready", now, OVERDRIVE_CD, p)) return false;
        tag.putLong("overdrive_ready", now + OVERDRIVE_CD);
        tag.putLong("ultimate_until", tag.getLong("ultimate_until") + 30 * SECOND);
        ResourceService.set(p, ResourceType.STAMINA, 0);
        List<LivingEntity> targets = p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(10), living -> living != p);
        for (LivingEntity target : targets) target.setRemainingFireTicks(Math.max(100, target.getRemainingFireTicks()));
        p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 250, 9, 2, 9, .09);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2f, .7f);
        title(p, "OVERDRIVE", "+30 segundos — Estamina zerada", ChatFormatting.RED);
        say(p, "OVERDRIVE: +30s, Estamina zerada; chamas em raio de 10 blocos.");
        sync(p);
        return true;
    }

    public static boolean dive(ServerPlayer p) {
        if (!active(p)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (!available(tag, "dive_ready", now, DIVE_CD, p)) return false;
        int step = tag.getInt("dive_step");
        if (step > 3) { say(p, "Mergulhar atingiu o limite de Estamina desta ultimate."); return false; }
        int cost = 10 << step;
        int distance = 10 * (step + 1);
        if (ResourceService.get(p, ResourceType.STAMINA) < cost) { say(p, "Estamina insuficiente (" + cost + ")."); return false; }
        Vec3 direction = p.getLookAngle().normalize();
        Vec3 from = p.position();
        Vec3 to = from.add(direction.scale(distance));
        var hit = p.serverLevel().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(direction.scale(distance)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double range = Math.min(distance, hit.getLocation().distanceTo(p.getEyePosition()) - .8);
        Vec3 destination = null;
        for (double d = range; d >= 1; d -= .5) {
            Vec3 candidate = from.add(direction.scale(d));
            if (p.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(candidate))
                    && p.serverLevel().noCollision(p, p.getBoundingBox().move(candidate.subtract(from)))) {
                destination = candidate; break;
            }
        }
        if (destination == null) { say(p, "Sem destino seguro para Mergulhar."); return false; }
        if (!ResourceService.trySpend(p, ResourceType.STAMINA, cost)) return false;
        p.serverLevel().sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 50, .5, 1, .5, .1);
        p.teleportTo(destination.x, destination.y, destination.z);
        p.resetFallDistance();
        p.serverLevel().sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1, destination.z, 50, .5, 1, .5, .1);
        tag.putInt("dive_step", step + 1);
        tag.putLong("dive_ready", now + DIVE_CD);
        say(p, String.format(Locale.ROOT, "Mergulhar: alcance %d / custo %d.", distance, cost));
        return true;
    }

    public static boolean slots(ServerPlayer p) {
        if (!active(p)) return false;
        CompoundTag tag = data(p);
        long now = time(p);
        if (!available(tag, "slots_ready", now, SLOTS_CD, p)) return false;
        if (ResourceService.get(p, ResourceType.STAMINA) < 30) { say(p, "Estamina insuficiente (30)."); return false; }
        if (!WilliamCrazySlots.summon(p, now)) return false;
        ResourceService.trySpend(p, ResourceType.STAMINA, 30);
        tag.putLong("slots_ready", now + SLOTS_CD);
        return true;
    }

    public static boolean zone(ServerPlayer p) {
        // A saida nao pode depender da Ultimate permanecer ativa.
        if (WilliamZone.isInside(p)) return WilliamZone.exitGroup(p);
        if (!active(p)) return false;
        return WilliamZone.enter(p);
    }

    public static void endUltimate(ServerPlayer p) {
        CompoundTag tag = data(p);
        if (!tag.getBoolean("ultimate")) return;
        tag.putBoolean("ultimate", false);
        p.getAbilities().mayfly = tag.getBoolean("had_flight") || p.isCreative() || p.isSpectator();
        if (!p.getAbilities().mayfly) p.getAbilities().flying = false;
        p.onUpdateAbilities();
        var armor = p.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(ARMOR_ID);
        WilliamCrazySlots.restore(p);
        say(p, "Dragon Override encerrado.");
        ResourceNetworking.sendDragon(p, false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, .75f, .8f);
        sync(p);
    }

    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        // Limpeza defensiva mesmo apos trocar a Fabula via ferramentas de debug.
        WilliamCrazySlots.purgeExpired(p);
        if (!isWilliam(p)) return;
        long now = time(p);
        CompoundTag tag = data(p);
        if (tag.getBoolean("ultimate") && now >= tag.getLong("ultimate_until")) endUltimate(p);
        WilliamCrazySlots.tick(p, now);
        if (tag.getBoolean("resonance")) {
            int currentTier = tier(p);
            if (currentTier > tag.getInt("resonance_tier")) {
                tag.putInt("resonance_tier", currentTier);
                tierReached(p, currentTier);
                sync(p);
            }
            if (now % 20 == 0) {
                if (!ResourceService.trySpend(p, ResourceType.STAMINA, 1)) {
                    stopResonance(p);
                    say(p, "Ressonância encerrada: Estamina esgotada.");
                } else applyResonance(p);
            }
        }
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
            // Em um reinicio nao continuamos ultimates nem buffs temporarios.
            if (tag.getBoolean("ultimate")) endUltimate(p);
            if (tag.getBoolean("resonance")) stopResonance(p);
            WilliamCrazySlots.restore(p);
            if (isWilliam(p) && tag.getBoolean("impetuo_armed")) ResourceNetworking.sendImpetuo(p, true);
            sync(p);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            endUltimate(p);
            stopResonance(p);
            ResourceNetworking.sendImpetuo(p, false);
        }
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer next) || !(event.getOriginal() instanceof ServerPlayer original)) return;
        CompoundTag old = original.getPersistentData().getCompound(ROOT);
        CompoundTag copied = old.copy();
        copied.putBoolean("resonance", false);
        copied.putBoolean("ultimate", false);
        copied.remove("impetuo_armed");
        copied.remove("resonance_start");
        copied.remove("resonance_tier");
        next.getPersistentData().put(ROOT, copied);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
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

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("minefables_william")
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CompoundTag tag = data(p);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "William nivel " + level(p) + "/5 | Ressonancia " +
                                    (tag.getBoolean("resonance") ? "ativa (" + tier(p) + ")" : "inativa") +
                                    " | Dragon Override " + (tag.getBoolean("ultimate") ? "ativo" : "inativo")), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("sair_zona").executes(ctx ->
                        WilliamZone.exit(ctx.getSource().getPlayerOrException()) ? 1 : 0)));
    }
}
