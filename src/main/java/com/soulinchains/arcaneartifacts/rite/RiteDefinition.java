package com.soulinchains.arcaneartifacts.rite;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One chalk rite, read from {@code data/<namespace>/arcane_rites/<name>.json}: the four sigils that call it, the smallest
 * field it needs, what is offered on the pedestal (and later, at the Heart's signal), an optional creature in a cage, and
 * what it brings.
 */
public record RiteDefinition(ResourceLocation id, Kind kind, Sigil main, Sigil south, Sigil west, Sigil east, int minField,
                             Offering offering, List<Offering> extraOfferings, int cageValue, int channelSeconds,
                             List<Summon> summons, boolean otherworldly, int lifeSeconds, List<Offering> creates,
                             Map<ResourceLocation, ResourceLocation> entityMap, Map<Item, Item> itemMap, int cleanse) {

    public enum Kind { SUMMON, CREATE, TRANSMUTE, CLEANSE }

    public record Offering(Item item, int count) {
        public boolean matches(ItemStack stack) {
            return stack.is(item) && stack.getCount() >= count;
        }

        public ItemStack stack() {
            return new ItemStack(item, count);
        }
    }

    public record Summon(EntityType<?> type, int count) {
    }

    public Component displayName() {
        return Component.translatable("arcane_artifacts.rite.name." + id.getPath());
    }

    public boolean needsCage() {
        return cageValue > 0;
    }

    /** Whether the four rite places of {@code site} call this rite. */
    public boolean calledBy(RiteSite site) {
        return site.at(RiteSite.Place.MAIN).is(main) && site.at(RiteSite.Place.SOUTH).is(south)
                && site.at(RiteSite.Place.WEST).is(west) && site.at(RiteSite.Place.EAST).is(east);
    }

    private static Sigil sigil(JsonObject json, String key) {
        Sigil sigil = Sigil.byId(GsonHelper.getAsString(json, key));
        if (sigil == null || sigil.modifier) {
            throw new IllegalArgumentException("Unknown base sigil for '" + key + "': " + GsonHelper.getAsString(json, key));
        }
        return sigil;
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            throw new IllegalArgumentException("Unknown item " + id);
        }
        return BuiltInRegistries.ITEM.get(key);
    }

    private static Offering offering(JsonObject json) {
        return new Offering(item(GsonHelper.getAsString(json, "item")), GsonHelper.getAsInt(json, "count", 1));
    }

    private static List<Offering> offerings(@Nullable JsonArray array) {
        List<Offering> list = new ArrayList<>();
        if (array != null) {
            for (JsonElement e : array) {
                list.add(offering(e.getAsJsonObject()));
            }
        }
        return list;
    }

    public static RiteDefinition parse(ResourceLocation id, JsonObject json) {
        Kind kind = Kind.valueOf(GsonHelper.getAsString(json, "kind").toUpperCase(java.util.Locale.ROOT));
        int minField = GsonHelper.getAsInt(json, "min_field", 9);
        JsonObject cage = GsonHelper.getAsJsonObject(json, "cage", null);
        List<Summon> summons = new ArrayList<>();
        boolean otherworldly = false;
        int life = 120;
        JsonObject summon = GsonHelper.getAsJsonObject(json, "summon", null);
        if (summon != null) {
            for (JsonElement e : GsonHelper.getAsJsonArray(summon, "entities")) {
                JsonObject o = e.getAsJsonObject();
                ResourceLocation type = ResourceLocation.parse(GsonHelper.getAsString(o, "type"));
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(type)) {
                    throw new IllegalArgumentException("Unknown entity " + type);
                }
                summons.add(new Summon(BuiltInRegistries.ENTITY_TYPE.get(type), GsonHelper.getAsInt(o, "count", 1)));
            }
            otherworldly = GsonHelper.getAsBoolean(summon, "otherworldly", false);
            life = GsonHelper.getAsInt(summon, "life_seconds", 120);
        }
        Map<ResourceLocation, ResourceLocation> entityMap = new LinkedHashMap<>();
        Map<Item, Item> itemMap = new LinkedHashMap<>();
        JsonObject transmute = GsonHelper.getAsJsonObject(json, "transmute", null);
        if (transmute != null) {
            JsonObject entities = GsonHelper.getAsJsonObject(transmute, "entities", new JsonObject());
            for (var e : entities.entrySet()) {
                entityMap.put(ResourceLocation.parse(e.getKey()), ResourceLocation.parse(e.getValue().getAsString()));
            }
            JsonObject items = GsonHelper.getAsJsonObject(transmute, "items", new JsonObject());
            for (var e : items.entrySet()) {
                itemMap.put(item(e.getKey()), item(e.getValue().getAsString()));
            }
            otherworldly = GsonHelper.getAsBoolean(transmute, "otherworldly", otherworldly);
        }
        return new RiteDefinition(id, kind, sigil(json, "main"), sigil(json, "south"), sigil(json, "west"), sigil(json, "east"), minField,
                offering(GsonHelper.getAsJsonObject(json, "offering")), offerings(GsonHelper.getAsJsonArray(json, "extra_offerings", null)),
                cage == null ? 0 : GsonHelper.getAsInt(cage, "min_value", 1), GsonHelper.getAsInt(json, "channel_seconds", 0),
                summons, otherworldly, life, offerings(GsonHelper.getAsJsonArray(json, "create", null)), entityMap, itemMap,
                GsonHelper.getAsInt(json, "cleanse", 0));
    }
}
