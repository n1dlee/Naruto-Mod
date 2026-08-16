package com.sekwah.narutomod.clan;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every clan a player can be, in one place.
 *
 * This existed as four parallel arrays in the selection screen and a separate hard-coded set
 * in the packet that validates the choice. They drifted, as parallel lists do: Akimichi,
 * Inuzuka, Yamanaka and Aburame each had working jutsu, chat messages and failure strings,
 * and none of them appeared in either list - so four clans' worth of implemented content was
 * reachable only by an operator command.
 *
 * One enum drives the buttons, the server-side whitelist and anything added later. Adding a
 * clan is one row here, and it is impossible to add it to the screen without also making it
 * valid to select.
 *
 * WARNING: the id string is written to player NBT. Rename ids only with a migration.
 */
public enum NinjaClan {

    UZUMAKI("uzumaki", "Uzumaki", "Chakra x1.5, Regen x2"),
    UCHIHA("uchiha", "Uchiha", "Fire jutsu +30% damage"),
    HYUGA("hyuga", "Hyuga", "Melee attack +30%"),
    NARA("nara", "Nara", "Movement speed +20%"),
    HARUNO("haruno", "Haruno", "HP regen 0.5/sec"),
    SENJU("senju", "Senju", "Wood Release, +20% HP"),

    // Implemented but previously unreachable. Their jutsu already gate on these exact ids -
    // see BaikaAbility, GatsugaAbility, MindDisturbanceAbility and KikaichuSwarmAbility.
    AKIMICHI("akimichi", "Akimichi", "Expansion jutsu, Human Boulder"),
    INUZUKA("inuzuka", "Inuzuka", "Ninken partner, Gatsuga"),
    YAMANAKA("yamanaka", "Yamanaka", "Mind techniques"),
    ABURAME("aburame", "Aburame", "Kikaichu insect swarm");

    private final String id;
    private final String displayName;
    private final String description;

    NinjaClan(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String id() {
        return this.id;
    }

    /** ASCII only - a non-ASCII glyph switches the shader font page mid-string. */
    public String displayName() {
        return this.displayName;
    }

    public String description() {
        return this.description;
    }

    public ResourceLocation icon() {
        return new ResourceLocation("narutomod", "textures/gui/clans/" + this.id + ".png");
    }

    /** The server-side whitelist, derived rather than written down a second time. */
    public static final Set<String> VALID_IDS =
            Arrays.stream(values()).map(NinjaClan::id).collect(Collectors.toUnmodifiableSet());

    public static boolean isValid(String id) {
        return id != null && VALID_IDS.contains(id);
    }
}
