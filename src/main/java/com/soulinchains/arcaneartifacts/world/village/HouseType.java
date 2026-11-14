package com.soulinchains.arcaneartifacts.world.village;

/** What a piece of a Mycelinoid village is: a house with a purpose, or one of the buildings between them. */
public enum HouseType {
    /** The big house in the middle of the village: a long table for the whole colony. */
    HALL(4, 6, 2, 1, true),
    /** Where a Mycelinoid family sleeps and eats. */
    DWELLING(3, 5, 2, 0, true),
    /** Barrels of spare mushrooms. */
    STORAGE(3, 5, 1, 0, true),
    /** A mushroom garden on mushroom moss. */
    FARM(3, 5, 1, 0, true),
    /** A brewing stand and a cauldron: Mycelinoid alchemy. */
    BREWERY(3, 5, 1, 0, true),
    /** The Amanitoids' post. */
    GUARDHOUSE(3, 5, 0, 2, true),
    /** The village well: water under a little roof of mushroom planks. */
    WELL(2, 4, 0, 0, false),
    /** A tall thin mushroom with a ladder inside and a lookout on its cap. */
    WATCHTOWER(2, 11, 0, 1, false),
    /** Market stalls under plank awnings, barrels of goods behind the counters. */
    MARKET(4, 4, 2, 0, false),
    /** A fenced garden of every cave mushroom, with a little huge mushroom in the middle. */
    GARDEN(4, 5, 1, 0, false),
    /** A lamp post by the path. */
    LAMP(1, 4, 0, 0, false);

    /** Outer radius of the stem (the walls), or half the size of the building. */
    public final int radius;
    /** Height of the room under the cap, or of the building. */
    public final int height;
    public final int mycelinoids;
    public final int guards;
    public final boolean house;

    HouseType(int radius, int height, int mycelinoids, int guards, boolean house) {
        this.radius = radius;
        this.height = height;
        this.mycelinoids = mycelinoids;
        this.guards = guards;
        this.house = house;
    }

    public int capRadius() {
        return this == WATCHTOWER ? radius + 2 : house ? radius + 2 : radius + 1;
    }

    /** The houses around the hall, in the order a village gets them. */
    public static final HouseType[] AROUND = {DWELLING, GUARDHOUSE, STORAGE, FARM, DWELLING, BREWERY, DWELLING};
    /** The buildings on the outer ring, in the order a village gets them. */
    public static final HouseType[] OUTSKIRTS = {WATCHTOWER, MARKET, GARDEN};
}
