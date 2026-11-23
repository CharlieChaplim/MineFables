package io.github.charliechaplim.minefables.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import io.github.charliechaplim.minefables.client.render.FulgorGeoRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.joml.Vector3f;

/** Lanca Fulgor: usa encantamentos de espada, mas possui alcance adicional. */
public final class FulgorItem extends SwordItem implements GeoItem {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final net.minecraft.resources.ResourceLocation REACH_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minefables", "fulgor_reach");
    public static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.73f, 0.05f), 1.4f);

    public FulgorItem(Item.Properties properties) { super(Tiers.DIAMOND, properties); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Modelo GeckoLib estatico por enquanto; sem animacao disparada.
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private FulgorGeoRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new FulgorGeoRenderer();
                return renderer;
            }
        });
    }

    public static Item.Properties properties() {
        return new Item.Properties().durability(4064)
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 6, -2.6f)
                        .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                                new AttributeModifier(REACH_ID, 1.5d, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND));
    }

    /** A probabilidade afeta apenas golpes primarios (nao repeticoes programadas). */
    public static void onDamage(LivingDamageEvent.Pre event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p ||
                !p.getMainHandItem().is(ModItems.FULGOR.get()) ||
                event.getEntity() == p || event.getNewDamage() <= 0f ||
                io.github.charliechaplim.minefables.william.WilliamKit.isRepeatDamage()) return;
        if (p.getRandom().nextFloat() < 0.30f) {
            event.setNewDamage(event.getNewDamage() * 1.30f);
            if (event.getEntity().level() instanceof ServerLevel level) {
                level.sendParticles(GOLD, event.getEntity().getX(), event.getEntity().getY() + 1.0,
                        event.getEntity().getZ(), 34, .35, .55, .35, .12);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        event.getEntity().getX(), event.getEntity().getY() + 1,
                        event.getEntity().getZ(), 24, .35, .4, .35, .09);
                level.playSound(null, event.getEntity().blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.PLAYERS, .4f, 1.55f);
            }
        }
    }
}
