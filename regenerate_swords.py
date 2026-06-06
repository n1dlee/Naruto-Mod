"""Regenerate chakra_blade and kusanagi icons in katana diagonal style (16x16)."""
from PIL import Image

OUT = "src/main/resources/assets/narutomod/textures/items/weapons/"

def chakra_blade():
    """Diagonal sword like katana but with cyan/blue chakra glow."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.putpixel

    # Blade pixels (diagonal, bottom-left to top-right) — cyan chakra blade
    # Blade core (bright cyan)
    blade_core = [
        (4, 13), (5, 12), (6, 11), (7, 10), (8, 9), (9, 8), (10, 7),
        (11, 6), (12, 5), (13, 4), (14, 3),
    ]
    # Blade edge (lighter)
    blade_edge = [
        (5, 13), (6, 12), (7, 11), (8, 10), (9, 9), (10, 8),
        (11, 7), (12, 6), (13, 5), (14, 4),
    ]
    # Blade highlight (white-ish)
    blade_highlight = [
        (13, 3), (12, 4), (11, 5),
    ]

    for x, y in blade_core:
        p((x, y), (60, 170, 230, 255))
    for x, y in blade_edge:
        p((x, y), (120, 210, 255, 255))
    for x, y in blade_highlight:
        p((x, y), (200, 240, 255, 255))

    # Tip
    p((15, 2), (180, 230, 255, 255))
    p((14, 2), (140, 200, 240, 200))

    # Glow aura around blade (subtle)
    glow_pixels = [
        (3, 13), (5, 11), (7, 9), (9, 7), (11, 5), (13, 3),
        (6, 13), (8, 11), (10, 9), (12, 7), (14, 5),
    ]
    for x, y in glow_pixels:
        p((x, y), (80, 180, 255, 50))

    # Guard (gold, horizontal bar)
    for i in range(3):
        p((3 + i, 14), (210, 180, 50, 255))
        p((5 + i, 14), (180, 150, 40, 255))
    p((4, 13), (220, 190, 60, 255))

    # Handle (dark teal wrapping)
    handle = [(2, 15), (3, 15), (1, 15)]
    for x, y in handle:
        p((x, y), (30, 80, 90, 255))
    p((2, 14), (40, 100, 110, 255))
    p((1, 14), (25, 65, 75, 255))

    img.save(OUT + "chakra_blade.png")
    print("  chakra_blade.png OK")

def kusanagi():
    """Diagonal sword — elegant white/silver blade with purple handle (Orochimaru style)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.putpixel

    # Blade core (silver/white, diagonal)
    blade_core = [
        (4, 13), (5, 12), (6, 11), (7, 10), (8, 9), (9, 8), (10, 7),
        (11, 6), (12, 5), (13, 4), (14, 3),
    ]
    blade_edge = [
        (5, 13), (6, 12), (7, 11), (8, 10), (9, 9), (10, 8),
        (11, 7), (12, 6), (13, 5), (14, 4),
    ]
    blade_dark = [
        (3, 13), (4, 12), (5, 11), (6, 10), (7, 9), (8, 8),
        (9, 7), (10, 6), (11, 5), (12, 4),
    ]

    for x, y in blade_core:
        p((x, y), (220, 225, 235, 255))
    for x, y in blade_edge:
        p((x, y), (245, 245, 255, 255))
    for x, y in blade_dark:
        p((x, y), (170, 175, 190, 255))

    # Tip (bright white)
    p((15, 2), (255, 255, 255, 255))
    p((14, 2), (235, 235, 245, 230))

    # Guard (ornate gold)
    for i in range(4):
        p((3 + i, 14), (220, 185, 55, 255))
    p((4, 13), (200, 170, 50, 255))
    p((3, 14), (240, 200, 70, 255))
    p((6, 14), (240, 200, 70, 255))

    # Handle (dark purple — Orochimaru)
    handle = [(1, 15), (2, 15), (3, 15)]
    for x, y in handle:
        p((x, y), (70, 30, 90, 255))
    p((2, 14), (85, 40, 110, 255))
    p((1, 14), (55, 20, 70, 255))

    # Subtle purple aura
    aura = [(6, 13), (8, 11), (10, 9), (12, 7), (14, 5)]
    for x, y in aura:
        p((x, y), (130, 60, 180, 40))

    img.save(OUT + "kusanagi.png")
    print("  kusanagi.png OK")

chakra_blade()
kusanagi()
print("Done — both 16x16 diagonal sword icons regenerated")
