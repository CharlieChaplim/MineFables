package io.github.charliechaplim.minefables.william;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Collections;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/** Armas de curta duracao. O item anterior e armazenado no NBT do jogador. */
public final class WilliamCrazySlots {
    private static final String ROOT = "minefables_william";
    private static final String MARK = "minefables_crazy_slots";
    private WilliamCrazySlots() {}

    private static CompoundTag data(ServerPlayer p) { return p.getPersistentData().getCompound(ROOT); }

    public static boolean isTemporary(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null && custom.copyTag().getBoolean(MARK);
    }

    public static boolean summon(ServerPlayer p, long now) {
        CompoundTag tag = data(p);
        if (tag.contains("slots_saved")) {
            if (tag.getLong("slots_until") > now) {
                return false;
            }
            restore(p);
        }
        // Guarda o item original no disco (e nao em uma variavel volatil).
        ItemStack original = p.getMainHandItem().copy();
        tag.put("slots_saved", original.saveOptional(p.registryAccess()));
        tag.putInt("slots_index", p.getInventory().selected);
        int selection = p.getRandom().nextInt(6);
        ItemStack weapon = makeWeapon(p, selection);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(MARK, true);
        marker.putUUID("owner", p.getUUID());
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        p.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        if (selection == 5) {
            ItemStack arrow = new ItemStack(Items.ARROW);
            arrow.set(DataComponents.CUSTOM_DATA, CustomData.of(marker.copy()));
            if (!p.getInventory().add(arrow)) p.drop(arrow, false);
        }
        tag.putLong("slots_until", now + 300); // 15 segundos
        tag.putInt("slots_weapon", selection);
        WilliamVfx.citation(p, true);
        return true;
    }

    private static ItemStack makeWeapon(ServerPlayer p, int type) {
        ItemStack stack = switch (type) {
            case 0 -> new ItemStack(Items.CROSSBOW);
            case 1 -> new ItemStack(Items.NETHERITE_SWORD);
            case 2 -> new ItemStack(Items.NETHERITE_AXE);
            case 3 -> new ItemStack(Items.TRIDENT);
            case 4 -> new ItemStack(Items.MACE);
            default -> new ItemStack(Items.BOW);
        };
        switch (type) {
            case 0 -> {
                enchant(p, stack, Enchantments.PIERCING, 4);
                enchant(p, stack, Enchantments.MULTISHOT, 1);
                ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
                // Oito explosoes (potencia ofensiva 8), nao apenas duracao de voo 8.
                FireworkExplosion explosion = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL,
                        new IntArrayList(new int[]{0xFF5500}), new IntArrayList(), true, false);
                rocket.set(DataComponents.FIREWORKS, new Fireworks(1, Collections.nCopies(8, explosion)));
                stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(rocket));
            }
            case 1 -> { enchant(p, stack, Enchantments.SHARPNESS, 5);
                enchant(p, stack, Enchantments.SWEEPING_EDGE, 3);
                enchant(p, stack, Enchantments.FIRE_ASPECT, 2); }
            case 2 -> { enchant(p, stack, Enchantments.SHARPNESS, 5);
                enchant(p, stack, Enchantments.EFFICIENCY, 5); }
            case 3 -> { enchant(p, stack, Enchantments.IMPALING, 5);
                enchant(p, stack, Enchantments.CHANNELING, 1);
                enchant(p, stack, Enchantments.LOYALTY, 3); }
            case 4 -> { enchant(p, stack, Enchantments.BREACH, 4);
                enchant(p, stack, Enchantments.WIND_BURST, 3); }
            default -> { enchant(p, stack, Enchantments.FLAME, 1);
                enchant(p, stack, Enchantments.PUNCH, 2);
                enchant(p, stack, Enchantments.POWER, 5); }
        }
        return stack;
    }

    private static void enchant(ServerPlayer p, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        var registry = p.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getHolderOrThrow(key), level);
    }

    public static void tick(ServerPlayer p, long now) {
        CompoundTag tag = data(p);
        if (!tag.contains("slots_saved")) return;
        // Nao permite retirar a arma da hotbar para levar a um bau.
        if (now >= tag.getLong("slots_until") ||
                p.getInventory().selected != tag.getInt("slots_index") ||
                p.containerMenu != p.inventoryMenu) restore(p);
    }

    /** Elimina equipamentos temporarios que reaparecam (ex.: tridente Loyalty). */
    public static void purgeExpired(ServerPlayer p) {
        CompoundTag tag = data(p);
        if (tag.contains("slots_saved")) return;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (isTemporary(p.getInventory().getItem(i)))
                p.getInventory().setItem(i, ItemStack.EMPTY);
        }
    }

    public static void restore(ServerPlayer p) {
        CompoundTag tag = data(p);
        if (!tag.contains("slots_saved")) return;
        // Remove todas as copias restantes de armas temporarias do inventario.
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (isTemporary(p.getInventory().getItem(i))) p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        // Recupera o item original no slot que estava selecionado ao invocar.
        int slot = tag.getInt("slots_index");
        ItemStack original = ItemStack.parseOptional(p.registryAccess(), tag.getCompound("slots_saved"));
        if (slot >= 0 && slot < 9) {
            ItemStack occupant = p.getInventory().getItem(slot);
            if (occupant.isEmpty()) p.getInventory().setItem(slot, original);
            else if (!original.isEmpty() && !p.getInventory().add(original)) p.drop(original, false);
        } else if (!original.isEmpty() && !p.getInventory().add(original)) p.drop(original, false);
        WilliamVfx.citation(p, false);
        // O tempo de recarga comeca quando a arma some, nao ao conjurar.
        // Tambem protege restauracoes antecipadas (troca de slot/menu/logout).
        long now = p.getServer().overworld().getGameTime();
        tag.putLong("slots_ready", now + 15 * 20L);
        tag.remove("slots_saved");
        tag.remove("slots_until");
        tag.remove("slots_index");
        tag.remove("slots_weapon");
    }
}
