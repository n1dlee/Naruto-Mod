package com.sekwah.narutomod.util;

/**
 * One size for every final form in the mod.
 *
 * Complete Body Susanoo and the Kurama avatar are peers - in the story they meet as equals,
 * and in this world they have to be able to fight each other. They were not: the player's
 * forms were scaled for spectacle (a 63-block fox, a 17.5-block Susanoo that grew a further
 * 35% with the power surge) while the boss's were scaled to its real thirteen-block hitbox.
 * Standing next to each other the difference read as the boss being broken.
 *
 * The boss's hitbox is the binding constraint, because it is real: a fox drawn four times
 * taller than the box you can actually hit is the same bug as a fox drawn as two floating
 * claws. So the size lives here once, every renderer derives its scale from it, and the
 * boss's hitbox and headroom check follow the same number.
 */
public final class GiantForm {

    /**
     * How tall a final form stands, in blocks.
     *
     * Chosen a little above where the player's Susanoo already was, so nothing anyone had
     * gets visibly smaller, and far below where the player's Kurama was - sixty-three blocks
     * is taller than the render distance is useful at and cannot be seen from the ground.
     */
    public static final float HEIGHT_BLOCKS = 18.0f;

    /** Clear blocks needed overhead before a giant can rise, with a little slack. */
    public static final int HEADROOM = 19;

    private GiantForm() {
    }
}
