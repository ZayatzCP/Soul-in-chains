package com.soulinchains.arcaneartifacts.client.baked;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the creature models baked from the designer's Blockbench files by {@code tools/bbmodel_entity.py}
 * ({@code assets/arcane_artifacts/entity_models/<name>.json}): named parts, each with a pivot and textured
 * quads already in entity model space.
 */
public final class BakedModels {
    private BakedModels() {
    }

    public static Map<String, BakedPart> load(ResourceManager resources, ResourceLocation id) {
        Map<String, BakedPart> parts = new HashMap<>();
        try (Reader reader = resources.getResourceOrThrow(id).openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("parts").entrySet()) {
                JsonObject part = entry.getValue().getAsJsonObject();
                JsonArray p = part.getAsJsonArray("pivot");
                List<BakedPart.Quad> quads = new ArrayList<>();
                for (JsonElement element : part.getAsJsonArray("quads")) {
                    JsonObject q = element.getAsJsonObject();
                    Vector3f[] vertices = new Vector3f[4];
                    float[][] uvs = new float[4][2];
                    for (int i = 0; i < 4; i++) {
                        JsonArray v = q.getAsJsonArray("v").get(i).getAsJsonArray();
                        vertices[i] = new Vector3f(v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat());
                        JsonArray uv = q.getAsJsonArray("uv").get(i).getAsJsonArray();
                        uvs[i][0] = uv.get(0).getAsFloat();
                        uvs[i][1] = uv.get(1).getAsFloat();
                    }
                    Vector3f normal = new Vector3f(vertices[1]).sub(vertices[0]).cross(new Vector3f(vertices[2]).sub(vertices[0]));
                    if (normal.lengthSquared() < 1.0E-8F) {
                        normal.set(0, -1, 0);
                    }
                    quads.add(new BakedPart.Quad(vertices, uvs, normal.normalize()));
                }
                parts.put(entry.getKey(), new BakedPart(new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()), quads));
            }
        } catch (Exception e) {
            ArcaneArtifacts.LOGGER.error("Could not load the creature model {}", id, e);
        }
        return parts;
    }

    public static BakedPart part(Map<String, BakedPart> parts, String name) {
        return parts.getOrDefault(name, BakedPart.EMPTY);
    }
}
