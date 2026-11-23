package io.github.charliechaplim.minefables.william;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Vec3i;
import java.util.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Vertice Vazio: arena instanciada numa dimensao vazia, com entradas seguras.
 * O template NBT de 228x76x240 e instalado automaticamente, uma unica vez.
 * Um clash com outros usuarios de dominio ainda nao possui protocolo/criterio definido.
 */
public final class WilliamZone {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("minefables", "zona_fabular"));
    private static final String SOURCE = "mf_zona_origem";
    private static final String OWNER = "mf_zona_caster";
    // The End comeca em Y=0. O marcador antigo em Y=-60 nunca era salvo.
    // Fora dos limites X/Z da arena para nao interferir na queda pelo void central.
    private static final BlockPos INSTALL_MARKER = new BlockPos(256, 5, 256);
    // Duas amostras comprovadas no NBT original, apos placement em (-114, 12, -120).
    // Reconhecem arenas instaladas pela versao antiga, cujo marcador era invalido.
    private static final BlockPos ARENA_SAMPLE_LEFT = new BlockPos(-67, 75, -1);
    private static final BlockPos ARENA_SAMPLE_RIGHT = new BlockPos(66, 73, 5);
    private static final long DURATION_TICKS = 2400; // 120 segundos exatos
    private static UUID caster;
    private static long expires;
    // Os pontos X/Z ficam em lados opostos da espiral original.
    // O Y e descoberto no terreno existente, nunca por plataformas artificiais.
    private static final BlockPos LEFT_ANCHOR = new BlockPos(-68, 90, 0);
    private static final BlockPos RIGHT_ANCHOR = new BlockPos(68, 90, 0);
    private static final int ENTRY_RADIUS = 25;
    private static final int LANDING_SEARCH_RADIUS = 20;
    private static final int TOP_SCAN_Y = 94;
    private static final int BOTTOM_SCAN_Y = 15;
    // Centro da estrutura NBT: template 228x76x240 instalado em (-114,12,-120).
    // Regiao de saida por coordenadas fisicas X/Z, independente da altura do void.
    private static final double CENTER_MIN = -5.0;
    private static final double CENTER_MAX = 5.0;
    // E possivel chegar a Y=20 antes de ser resgatado; nao usamos a altura letal do End.
    private static final double FALL_RESCUE_Y = 18.0;
    private WilliamZone() {}

    public static boolean isInside(ServerPlayer p) { return p.serverLevel().dimension().equals(DIMENSION); }

    /**
     * Apenas o conjurador que ainda possui o vinculo de entrada pode
     * encerrar a Zona ao recastar a ultimate. A verificacao usa o UUID
     * persistido, inclusive se o estado em memoria tiver sido recuperado.
     */
    public static boolean isCasterInside(ServerPlayer p) {
        if (!isInside(p) || !p.getPersistentData().contains(SOURCE)) return false;
        CompoundTag home = p.getPersistentData().getCompound(SOURCE);
        return home.hasUUID(OWNER) && p.getUUID().equals(home.getUUID(OWNER));
    }

    /** Apenas o conjurador recebe imunidade a queda dentro da Zona. */
    public static boolean cancelFallDamage(ServerPlayer p) {
        if (!isInside(p) || !p.getPersistentData().contains(SOURCE)) return false;
        CompoundTag home = p.getPersistentData().getCompound(SOURCE);
        return home.hasUUID(OWNER) && p.getUUID().equals(home.getUUID(OWNER));
    }

    /**
     * Morte real: um participante morto sai da sessao. Se for o conjurador,
     * libera a dimensao, devolve os sobreviventes e inicia a recarga de 500s.
     * NUNCA teleporta o jogador morto no evento de morte.
     * Pode ser chamada de novo no Clone como salvaguarda (idempotente).
     */
    public static void onPlayerDeath(ServerPlayer dead) {
        if (!isInside(dead) || !dead.getPersistentData().contains(SOURCE)) return;
        CompoundTag home = dead.getPersistentData().getCompound(SOURCE);
        if (!home.hasUUID(OWNER)) {
            dead.getPersistentData().remove(SOURCE);
            return;
        }
        UUID group = home.getUUID(OWNER);
        boolean diedAsCaster = group.equals(dead.getUUID());
        // A morte deve retirar a referencia mesmo antes de o Minecraft criar o novo player.
        dead.getPersistentData().remove(SOURCE);
        if (!diedAsCaster) return;

        // Coletar antes de teleportar: changeDimension altera as colecoes internas.
        List<Entity> survivors = getParticipants(dead.serverLevel(), group, dead);
        for (Entity participant : survivors) returnEntity(participant);
        if (group.equals(caster)) {
            caster = null;
            expires = 0;
        }
        WilliamKit.finishZoneCooldownOnDeath(dead);
    }

    /** Encerra sessoes penduradas caso o jogador renasca em outro contexto. */
    public static void recoverAfterRespawn(ServerPlayer respawned) {
        if (!respawned.getUUID().equals(caster)) return;
        UUID group = caster;
        ServerLevel zone = respawned.getServer().getLevel(DIMENSION);
        if (zone != null) {
            for (Entity participant : getParticipants(zone, group, null)) returnEntity(participant);
        }
        caster = null;
        expires = 0;
        WilliamKit.finishZoneCooldownOnDeath(respawned);
    }

    private static List<Entity> getParticipants(ServerLevel level, UUID group, Entity exclude) {
        List<Entity> participants = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity == exclude || !entity.getPersistentData().contains(SOURCE)) continue;
            CompoundTag source = entity.getPersistentData().getCompound(SOURCE);
            if (source.hasUUID(OWNER) && source.getUUID(OWNER).equals(group)) participants.add(entity);
        }
        return participants;
    }

    /**
     * O comando OP continua disponivel apenas como manutencao opcional.
     * A primeira tentativa de usar a Zona instala a arena automaticamente.
     */
    public static boolean installArena(ServerPlayer admin) {
        ServerLevel dest = admin.getServer().getLevel(DIMENSION);
        if (dest == null) {
            admin.sendSystemMessage(Component.literal("Vertice Vazio: dimensao nao disponivel."));
            return false;
        }
        if (arenaReady(dest)) {
            admin.sendSystemMessage(Component.literal("Vertice Vazio ja instalado; nao sera sobrescrito."));
            return true;
        }
        return ensureArena(admin, dest);
    }

    /**
     * Evita reinstalar a estrutura, inclusive em mundos criados pela versao anterior.
     * A assinatura usa dois blocos REAIS do NBT, nao apenas um marcador artificial.
     */
    private static boolean arenaReady(ServerLevel dest) {
        if (dest.getBlockState(INSTALL_MARKER).is(Blocks.BEDROCK)) return true;
        if (isVortexBlock(dest, ARENA_SAMPLE_LEFT) && isVortexBlock(dest, ARENA_SAMPLE_RIGHT)) {
            // Recuperacao do marcador: o anterior ficava abaixo do limite do End.
            dest.setBlockAndUpdate(INSTALL_MARKER, Blocks.BEDROCK.defaultBlockState());
            return true;
        }
        return false;
    }

    /**
     * So chamado quando o mundo ainda nao possui a estrutura.
     * Os chunks do template sao carregados uma vez, antes de qualquer teleporte.
     */
    private static boolean ensureArena(ServerPlayer requester, ServerLevel dest) {
        if (arenaReady(dest)) return true;
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath("minefables", "vertice_vazio");
        Optional<StructureTemplate> loaded = dest.getStructureManager().get(templateId);
        if (loaded.isEmpty()) {
            requester.sendSystemMessage(Component.literal("Vertice Vazio: template nao encontrado: " + templateId));
            return false;
        }
        StructureTemplate template = loaded.get();
        Vec3i dimensions = template.getSize();
        BlockPos origin = new BlockPos(-dimensions.getX() / 2, 12, -dimensions.getZ() / 2);
        int minX = origin.getX() >> 4, maxX = (origin.getX() + dimensions.getX() - 1) >> 4;
        int minZ = origin.getZ() >> 4, maxZ = (origin.getZ() + dimensions.getZ() - 1) >> 4;
        int chunks = (maxX - minX + 1) * (maxZ - minZ + 1);
        if (chunks > 256) {
            requester.sendSystemMessage(Component.literal("Vertice Vazio excede o limite de chunks: " + chunks));
            return false;
        }
        requester.sendSystemMessage(Component.literal(
                "Preparando Vertice Vazio pela primeira vez (" + chunks + " chunks)."));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) dest.getChunk(x, z);
        }
        boolean placed = template.placeInWorld(dest, origin, origin,
                new StructurePlaceSettings().setIgnoreEntities(true), RandomSource.create(), 2);
        if (!placed || !isVortexBlock(dest, ARENA_SAMPLE_LEFT)
                || !isVortexBlock(dest, ARENA_SAMPLE_RIGHT)) {
            requester.sendSystemMessage(Component.literal("Falha ao instalar automaticamente o Vertice Vazio: estrutura incompleta."));
            return false;
        }
        // Persistencia valida: Y=5 existe no End, ao contrario do antigo Y=-60.
        if (!dest.setBlockAndUpdate(INSTALL_MARKER, Blocks.BEDROCK.defaultBlockState())) {
            requester.sendSystemMessage(Component.literal("Vertice Vazio: estrutura instalada, mas nao foi possivel salvar o marcador."));
            return false;
        }
        requester.sendSystemMessage(Component.literal("Vertice Vazio instalado automaticamente."));
        return true;
    }

    public static boolean enter(ServerPlayer user) {
        if (isInside(user) || caster != null) return false;
        ServerLevel dest = user.getServer().getLevel(DIMENSION);
        if (dest == null) {
            user.displayClientMessage(Component.literal("Vértice Vazio indisponível."), true);
            return false;
        }
        // Instala quando necessario e recupera a estrutura preexistente da versao antiga.
        if (!ensureArena(user, dest)) return false;

        // Raio real de 25 blocos em todas as direcoes, nao uma caixa de 10.
        Vec3 center = user.position();
        AABB reach = AABB.ofSize(center, ENTRY_RADIUS * 2.0, ENTRY_RADIUS * 2.0, ENTRY_RADIUS * 2.0);
        List<Entity> members = new ArrayList<>();
        members.add(user);
        members.addAll(user.serverLevel().getEntities(user, reach,
                e -> e.isAlive() && e.position().distanceToSqr(center) <= ENTRY_RADIUS * ENTRY_RADIUS
                        && !e.getPersistentData().contains(SOURCE)));

        // Resolve os destinos ANTES de mover qualquer um: se houver poucos pontos livres,
        // nada entra na dimensao e ninguem e largado no void.
        Set<BlockPos> reserved = new HashSet<>();
        List<Vec3> destinations = collectVortexLandings(dest, LEFT_ANCHOR, 1, reserved);
        if (destinations.size() != 1) {
            user.displayClientMessage(Component.literal("Vértice Vazio: sem ponto seguro para o conjurador."), false);
            return false;
        }
        List<Vec3> targets = collectVortexLandings(dest, RIGHT_ANCHOR, members.size() - 1, reserved);
        if (targets.size() != members.size() - 1) {
            user.displayClientMessage(Component.literal("Vértice Vazio: não há blocos seguros suficientes para "
                    + members.size() + " entidades. Ninguém foi teleportado."), false);
            return false;
        }
        destinations.addAll(targets);

        UUID id = user.getUUID();
        caster = id;
        expires = user.getServer().overworld().getGameTime() + DURATION_TICKS;
        WilliamVfx.zoneOpen(user.serverLevel(), center);
        for (int i = 0; i < members.size(); i++) {
            transfer(members.get(i), dest, destinations.get(i), id);
        }
        WilliamVfx.zoneOpen(dest, new Vec3(0, 65, 0));
        return true;
    }

    /**
     * Procura o topo de blocos REAIS do NBT (azuis ou lanternas), sempre com
     * dois blocos de ar para nao sufocar os participantes. Nunca coloca obsidiana.
     */
    private static Vec3 findVortexLanding(ServerLevel level, BlockPos anchor, Set<BlockPos> reserved) {
        List<Vec3> found = collectVortexLandings(level, anchor, 1, reserved);
        return found.isEmpty() ? null : found.get(0);
    }

    private static List<Vec3> collectVortexLandings(ServerLevel level, BlockPos anchor, int limit,
                                                    Set<BlockPos> reserved) {
        List<Vec3> found = new ArrayList<>();
        if (limit <= 0) return found;
        // Um unico percurso pela regiao. Evita revarrer milhares de blocos para cada mob.
        for (int r = 0; r <= LANDING_SEARCH_RADIUS && found.size() < limit; r++) {
            for (int dz = -r; dz <= r && found.size() < limit; dz++) {
                for (int dx = -r; dx <= r && found.size() < limit; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = anchor.getX() + dx;
                    int z = anchor.getZ() + dz;
                    for (int y = TOP_SCAN_Y; y >= BOTTOM_SCAN_Y; y--) {
                        BlockPos floor = new BlockPos(x, y, z);
                        if (!isVortexBlock(level, floor) || !level.isEmptyBlock(floor.above())
                                || !level.isEmptyBlock(floor.above(2))) continue;
                        BlockPos feet = floor.above();
                        boolean near = reserved.stream().anyMatch(other -> other.distSqr(feet) < 9.0);
                        if (near) continue;
                        reserved.add(feet);
                        found.add(Vec3.atBottomCenterOf(feet));
                        break;
                    }
                }
            }
        }
        return found;
    }

    private static boolean isVortexBlock(ServerLevel level, BlockPos floor) {
        var state = level.getBlockState(floor);
        return (state.is(Blocks.BLUE_WOOL) || state.is(Blocks.BLUE_CONCRETE)
                || state.is(Blocks.BLUE_GLAZED_TERRACOTTA) || state.is(Blocks.SEA_LANTERN))
                && state.isFaceSturdy(level, floor, Direction.UP);
    }

    private static boolean transfer(Entity entity,ServerLevel dest,Vec3 to,UUID group) {
        if(entity.getPersistentData().contains(SOURCE) || !(entity.level() instanceof ServerLevel from))return false;
        CompoundTag home=new CompoundTag();
        home.putString("dimension",from.dimension().location().toString());
        home.putDouble("x",entity.getX());home.putDouble("y",entity.getY());home.putDouble("z",entity.getZ());
        home.putUUID(OWNER,group);
        entity.getPersistentData().put(SOURCE,home);
        entity.stopRiding();
        if(entity instanceof ServerPlayer p) {
            p.teleportTo(dest,to.x,to.y,to.z,p.getYRot(),p.getXRot());
        }else{
            Entity result=entity.changeDimension(new DimensionTransition(dest,to,Vec3.ZERO,
                    entity.getYRot(),entity.getXRot(),DimensionTransition.DO_NOTHING));
            if(result==null)return false;
            result.getPersistentData().put(SOURCE,home);
        }
        return true;
    }

    /**
     * Gatilho da Zona em X/Z [-5,5]. Para o conjurador, o resgate
     * pelo centro ocorre em Y<=20, preservando a queda intencional.
     * Os outros participantes encerram a Zona ao entrar no quadrado.
     * Quedas fora do centro so sao resgatadas abaixo de Y=18.
     */
    public static void tickParticipant(ServerPlayer p, long now) {
        if (!isInside(p) || !p.getPersistentData().contains(SOURCE)) return;

        boolean isCaster = p.getUUID().equals(caster);
        // A duracao tem prioridade inclusive se o conjurador estiver no centro.
        if (isCaster && now >= expires) {
            exitGroup(p);
            return;
        }
        boolean inExitSquare = p.getX() >= CENTER_MIN && p.getX() <= CENTER_MAX
                && p.getZ() >= CENTER_MIN && p.getZ() <= CENTER_MAX;

        // Outro jogador alcancou a abertura central: todos voltam para casa.
        if (inExitSquare && !isCaster) {
            exitGroup(p);
            return;
        }

        // William pode usar o centro para surgir atras de um participante.
        // Fora do centro, a protecao continua valendo para quedas acidentais.
        if (isCaster && ((inExitSquare && p.getY() <= 20.0) || p.getY() < FALL_RESCUE_Y)) {
            List<Entity> others = p.serverLevel().getEntities(p,
                    AABB.ofSize(new Vec3(0, 90, 0), 300, 300, 300),
                    entity -> entity.isAlive() && entity instanceof net.minecraft.world.entity.LivingEntity
                            && entity.getPersistentData().contains(SOURCE)
                            && entity.getPersistentData().getCompound(SOURCE).getUUID(OWNER).equals(caster));
            Vec3 safe = null;
            if (!others.isEmpty()) {
                Entity other = others.get(p.getRandom().nextInt(others.size()));
                Vec3 facing = other.getLookAngle().normalize();
                Vec3 behind = other.position().subtract(facing.scale(2.5));
                safe = findVortexLanding(p.serverLevel(),
                        new BlockPos((int) Math.floor(behind.x), 90, (int) Math.floor(behind.z)),
                        new HashSet<>());
                if (safe == null) safe = findVortexLanding(p.serverLevel(), RIGHT_ANCHOR, new HashSet<>());
            }
            if (safe == null) safe = findVortexLanding(p.serverLevel(), LEFT_ANCHOR, new HashSet<>());
            if (safe != null) {
                p.teleportTo(safe.x, safe.y, safe.z);
                WilliamVfx.zoneReappear(p);
            }
            p.resetFallDistance();
            return;
        }

        // Jogador fora do centro caiu da estrutura: volta para uma superficie
        // valida e continua participando. Nao pode se esconder abaixo da arena.
        if (!isCaster && p.getY() < FALL_RESCUE_Y) {
            Vec3 safe = findVortexLanding(p.serverLevel(), RIGHT_ANCHOR, new HashSet<>());
            if (safe != null) p.teleportTo(safe.x, safe.y, safe.z);
            p.resetFallDistance();
        }

    }

    public static boolean cancelVoidDamage(ServerPlayer p) {
        return isInside(p) && p.getUUID().equals(caster) && p.getY() < 20;
    }

    public static boolean exit(ServerPlayer p) {return exitGroup(p);}
    public static boolean exitGroup(ServerPlayer p) {
        if (!isInside(p) || !p.getPersistentData().contains(SOURCE)) return false;
        CompoundTag home = p.getPersistentData().getCompound(SOURCE);
        if (!home.hasUUID(OWNER)) return false;
        UUID group = home.getUUID(OWNER);
        List<Entity> participants = getParticipants(p.serverLevel(), group, null);
        if (!participants.contains(p)) participants.add(p);
        for (Entity participant : participants) returnEntity(participant);
        if (group.equals(caster)) {
            caster = null;
            expires = 0;
        }
        // O primeiro jogador a sair pode nao ser o conjurador: localizar pelo UUID,
        // independentemente de onde esteja, para nao perder o cooldown.
        ServerPlayer owner = p.getServer().getPlayerList().getPlayer(group);
        if (owner != null) WilliamKit.finishZoneCooldown(owner);
        return true;
    }

    private static void returnEntity(Entity entity) {
        CompoundTag home=entity.getPersistentData().getCompound(SOURCE);
        ResourceLocation id=ResourceLocation.tryParse(home.getString("dimension"));
        if(id==null || entity.getServer()==null)return;
        ServerLevel dest=entity.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,id));
        if(dest==null)return;
        Vec3 to=new Vec3(home.getDouble("x"),home.getDouble("y"),home.getDouble("z"));
        if(entity instanceof ServerPlayer player) {
            player.teleportTo(dest,to.x,to.y,to.z,player.getYRot(),player.getXRot());
            player.getPersistentData().remove(SOURCE);
            player.resetFallDistance();
        } else {
            Entity moved=entity.changeDimension(new DimensionTransition(dest,to,Vec3.ZERO,
                    entity.getYRot(),entity.getXRot(),DimensionTransition.DO_NOTHING));
            if(moved!=null)moved.getPersistentData().remove(SOURCE);
        }
    }
}
