package io.github.charliechaplim.minefables.william;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * VFX de William em coordenadas do mundo, transmitidos pelo servidor a todos
 * os observadores. Nao escreve texto na tela e nao altera dano/cooldowns.
 * Mantem o custo dos efeitos limitado, inclusive durante ticks de canalizacao.
 */
public final class WilliamVfx {
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.76f, 0.14f), 1.5f);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.95f, 0.09f, 0.07f), 1.5f);
    private static final DustParticleOptions AMBER = new DustParticleOptions(new Vector3f(1f, 0.32f, 0.06f), 1.4f);
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.35f, 0.92f, 1f), 1.4f);
    private static final DustParticleOptions VOID = new DustParticleOptions(new Vector3f(0.12f, 0.07f, 0.25f), 1.5f);
    private static final double TAU = Math.PI * 2;

    private WilliamVfx() {}

    private static void dot(ServerLevel level, ParticleOptions p, Vec3 pos) {
        level.sendParticles(p, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    private static void play(ServerLevel level, BlockPos pos,
                             net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Circulo 3D horizontal; pode ser composto com altura para espirais. */
    private static void circle(ServerLevel level, Vec3 center, double radius, int points,
                               ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double a = TAU * i / points;
            dot(level, particle, center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
        }
    }

    public static void resonance(ServerPlayer p, int tier) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        int count = 22 + tier * 8;
        for (int i=0; i<count; i++) {
            double a = TAU * i / count;
            double r = 0.65 + 0.14 * tier;
            double y = 0.25 + 1.65 * i / (double)count;
            dot(level, i % 4 == 0 ? ParticleTypes.END_ROD : RED,
                    c.add(Math.cos(a * (tier + 1)) * r, y, Math.sin(a * (tier + 1)) * r));
        }
        circle(level, c.add(0, 0.12, 0), 0.9 + tier * 0.1, 24, RED);
        play(level, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.85f, 0.8f + tier * 0.17f);
    }

    public static void resonanceIdle(ServerPlayer p, int tier, long tick) {
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < 5 + tier * 2; i++) {
            double a = tick * 0.14 + TAU * i / (5 + tier * 2);
            double y = 0.3 + ((i * 3 + tick / 4) % 16) * 0.095;
            dot(level, i % 3 == 0 ? ParticleTypes.END_ROD : RED,
                    p.position().add(Math.cos(a) * 0.67, y, Math.sin(a) * 0.67));
        }
    }

    public static void resonanceOff(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        circle(level, p.position().add(0, .55, 0), .8, 22, ParticleTypes.SMOKE);
        level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 16, .4, .55, .4, .02);
        play(level, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, .5f, 1.55f);
    }

    public static void impetuoImpact(ServerLevel level, Vec3 c, boolean cursed) {
        circle(level, c, cursed ? 1.35 : 0.95, cursed ? 42 : 30, cursed ? CYAN : GOLD);
        circle(level, c.add(0, 0.15, 0), 0.48, 20, GOLD);
        level.sendParticles(cursed ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                c.x, c.y, c.z, 32, .42, .48, .42, .095);
        play(level, BlockPos.containing(c), SoundEvents.ANVIL_LAND, .45f, cursed ? 1.55f : 1.9f);
    }

    public static void link(ServerLevel level, Vec3 pos) {
        circle(level, pos.add(0, .1, 0), .85, 36, CYAN);
        circle(level, pos.add(0, 1.3, 0), .6, 30, ParticleTypes.END_ROD);
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + .7, pos.z,
                30, .45, .65, .45, .15);
        play(level, BlockPos.containing(pos), SoundEvents.ENDERMAN_TELEPORT, .7f, 1.65f);
    }

    public static void walkerTravel(ServerLevel originLevel, Vec3 origin,
                                    ServerLevel destLevel, Vec3 destination) {
        for (int i=0; i<35; i++) {
            double t = i / 34.0;
            double a = t * TAU * 2.5;
            dot(originLevel, CYAN, origin.add(Math.cos(a) * (1-t) * .8, .4 + t*1.5, Math.sin(a) * (1-t) * .8));
            dot(destLevel, ParticleTypes.END_ROD,
                    destination.add(Math.cos(a) * (1-t) * .7, .3+t*1.3, Math.sin(a) * (1-t) * .7));
        }
        play(originLevel, BlockPos.containing(origin), SoundEvents.ENDERMAN_TELEPORT, .85f, 1.25f);
    }

    public static void citation(ServerPlayer p, boolean appearing) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0, 1.0, 0);
        circle(level, c, 1.2, 40, appearing ? GOLD : ParticleTypes.END_ROD);
        circle(level, c.add(0, -.65, 0), .8, 28, appearing ? ParticleTypes.ENCHANT : CYAN);
        level.sendParticles(appearing ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.END_ROD,
                c.x, c.y, c.z, appearing ? 45 : 24, .65, .6, .65, .13);
        play(level, p.blockPosition(), appearing ? SoundEvents.EVOKER_CAST_SPELL : SoundEvents.ENDERMAN_TELEPORT,
                .9f, appearing ? 1.4f : .8f);
    }

    public static void diveMark(ServerPlayer p, boolean set) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        circle(level, c.add(0, .12, 0), 1.25, 32, set ? VOID : ParticleTypes.SMOKE);
        level.sendParticles(set ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL,
                c.x, c.y+1, c.z, 45, .55, .8, .55, .13);
        play(level, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, .75f, set ? .6f : 1.35f);
    }

    public static void channel(ServerPlayer p, long tick) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        for (int i=0; i<18; i++) {
            double t = i / 17.0;
            double a = tick * .19 + 5 * Math.PI * t;
            double r = .35 + t * 1.65;
            dot(level, i % 3 == 0 ? ParticleTypes.SQUID_INK : VOID,
                    c.add(Math.cos(a) * r, 0.2 + t * 2.4, Math.sin(a) * r));
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y+1.3, c.z, 5, .3, .4, .3, .1);
    }

    public static void diveTravel(ServerLevel from, Vec3 start, ServerLevel to, Vec3 dest) {
        circle(from, start.add(0,1,0), .8, 36, VOID);
        circle(to, dest.add(0,1,0), .8, 36, CYAN);
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x,start.y+1,start.z,50,.3,.8,.3,.11);
        to.sendParticles(ParticleTypes.PORTAL, dest.x,dest.y+1,dest.z,50,.3,.8,.3,.11);
        play(from, BlockPos.containing(start), SoundEvents.ENDERMAN_TELEPORT, 1f, .75f);
        play(to, BlockPos.containing(dest), SoundEvents.ENDERMAN_TELEPORT, .9f, 1.3f);
    }

    public static void dragonStart(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        for(int i=0; i<96; i++) {
            double t=i/95.0;
            double a=t*TAU*4;
            double r = 2.6 - t * 1.5;
            dot(level, i%5==0 ? ParticleTypes.END_ROD : i%2==0 ? RED : AMBER,
                    c.add(Math.cos(a)*r, .15+t*3.6, Math.sin(a)*r));
        }
        circle(level, c.add(0, .15, 0), 2.75, 60, AMBER);
        level.sendParticles(ParticleTypes.FLAME,c.x,c.y+1.0,c.z,65,.9,1.1,.9,.09);
        play(level,p.blockPosition(),SoundEvents.ENDER_DRAGON_GROWL,1.1f,1.25f);
    }

    public static void dragonHeat(ServerPlayer p, int temperature) {
        ServerLevel level = p.serverLevel();
        if (temperature <= 0) return;
        Vec3 c = p.position().add(0, 1, 0);
        circle(level, c, .65 + temperature * .17, 16 + 6 * temperature,
                temperature >= 4 ? RED : AMBER);
        level.sendParticles(temperature >= 4 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                c.x, c.y, c.z, temperature * 6, .5, .65, .5, .07);
        if (temperature == 4) play(level,p.blockPosition(),SoundEvents.BLAZE_HURT,.95f,.6f);
    }

    public static void fireballTrail(ServerLevel level, Vec3 center, long tick) {
        level.sendParticles(AMBER,center.x,center.y,center.z,4,.18,.18,.18,.02);
        level.sendParticles(ParticleTypes.FLAME,center.x,center.y,center.z,8,.15,.15,.15,.025);
        if (tick%3==0) circle(level,center,.32,12,GOLD);
    }

    public static void fireballBurst(ServerLevel level, Vec3 center, boolean hit) {
        circle(level,center,1.35,48,AMBER);
        circle(level,center,.75,32,GOLD);
        level.sendParticles(ParticleTypes.FLAME,center.x,center.y,center.z,hit ? 65 : 36,.6,.6,.6,.09);
        level.sendParticles(ParticleTypes.SMOKE,center.x,center.y,center.z,hit ? 22 : 10,.4,.4,.4,.06);
        play(level,BlockPos.containing(center),SoundEvents.GENERIC_EXPLODE.value(),.65f,1.3f);
    }

    public static void steam(ServerPlayer p, Vec3 from, Vec3 to) {
        ServerLevel level = p.serverLevel();
        Vec3 direction = to.subtract(from);
        for (int i=0;i<=30;i++) {
            double t=i/30.0;
            Vec3 here=from.add(direction.scale(t)).add(0,1,0);
            dot(level,i%4==0 ? ParticleTypes.END_ROD : ParticleTypes.CLOUD,
                    here.add(Math.sin(i*1.8)*.35,Math.cos(i*1.8)*.2,Math.cos(i*1.5)*.35));
        }
        circle(level,to.add(0,1,0),1.4,40,ParticleTypes.CLOUD);
        level.sendParticles(ParticleTypes.POOF,to.x,to.y+1,to.z,55,.65,.6,.65,.16);
        play(level,BlockPos.containing(from),SoundEvents.FIRE_EXTINGUISH,1.2f,1.25f);
    }

    public static void overload(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        circle(level,c.add(0,.4,0),7.5,92,AMBER);
        circle(level,c.add(0,2.6,0),5.4,70,ParticleTypes.FLAME);
        circle(level,c.add(0,4.2,0),3.1,48,ParticleTypes.SOUL_FIRE_FLAME);
        for (int i=0;i<90;i++) {
            double t = i / 89.0;
            double a=t*TAU*3.0;
            double r=7.5*(1-t)+.4;
            dot(level,i%5==0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME,
                    c.add(Math.cos(a)*r,.4+t*5.0,Math.sin(a)*r));
        }
        play(level,p.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),1.2f,.7f);
    }

    public static void zoneOpen(ServerLevel level, Vec3 center) {
        for (int j=0;j<3;j++) {
            for (int i=0;i<64;i++) {
                double a=TAU*i/64.0 + j*.45;
                double r=1.7+j*1.25;
                double h=.2+j*.7 + Math.sin(a*3)*.35;
                dot(level,i%4==0 ? ParticleTypes.END_ROD : CYAN,
                        center.add(Math.cos(a)*r,h,Math.sin(a)*r));
            }
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,center.x,center.y+1.2,center.z,70,2.2,1.5,2.2,.15);
        play(level,BlockPos.containing(center),SoundEvents.END_PORTAL_SPAWN,1.0f,1.25f);
    }

    public static void zoneReappear(ServerPlayer player) {
        ServerLevel level=player.serverLevel();
        Vec3 c=player.position().add(0,1,0);
        circle(level,c,.9,32,CYAN);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,c.x,c.y,c.z,32,.4,.5,.4,.14);
        play(level,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,.8f,1.5f);
    }
}
