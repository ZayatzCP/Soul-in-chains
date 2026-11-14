package com.soulinchains.arcaneartifacts.world.village;

/** What a Mycelinoid house is for: it decides the furniture and who lives there. */
public enum HouseType {
    /** The big house in the middle of the village: a long table for the whole colony. */
    HALL(4, 6, 2, 1),
    /** Where a Mycelinoid family sleeps and eats. */
    DWELLING(3, 5, 2, 0),
    /** Barrels of spare mushrooms. */
    STORAGE(3, 5, 1, 0),
    /** A mushroom garden on mushroom moss. */
    FARM(3, 5, 1, 0),
    /** A brewing stand and a cauldron: Mycelinoid alchemy. */
    BREWERY(3, 5, 1, 0),
    /** The Amanitoids' post. */
    GUARDHOUSE(3, 5, 0, 2);

    /** Outer radius of the stem (the walls). */
    public final int radius;
    /** Height of the room under the cap. */
    public final int height;
    public final int mycelinoids;
    public final int guards;

    HouseType(int radius, int height, int mycelinoids, int guards) {
        this.radius = radius;
        this.height = height;
        this.mycelinoids = mycelinoids;
        this.guards = guards;
    }

    public int capRadius() {
        return radius + 2;
    }

    /** The houses around the hall, in the order a village gets them. */
    public static final HouseType[] AROUND = {DWELLING, GUARDHOUSE, STORAGE, FARM, DWELLING, BREWERY, DWELLING};
}
