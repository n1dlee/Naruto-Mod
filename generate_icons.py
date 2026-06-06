"""Generate 16x16 pixel art icons for Naruto mod weapons."""
from PIL import Image

OUT = "src/main/resources/assets/narutomod/textures/items/weapons/"

def chakra_blade():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.putpixel
    # Handle (brown)
    for y in range(12, 16):
        for x in range(7, 9):
            p((x, y), (120, 80, 40, 255))
    # Guard (gold)
    for x in range(5, 11):
        p((x, 11), (200, 170, 50, 255))
    # Blade (cyan/chakra blue glow)
    blade_colors = [
        (100, 180, 255, 255),  # outer
        (140, 210, 255, 255),  # mid
        (200, 240, 255, 255),  # center bright
    ]
    for y in range(1, 11):
        p((7, y), blade_colors[2])
        p((8, y), blade_colors[1])
        if y > 2:
            p((6, y), blade_colors[0])
    # Tip
    p((7, 0), blade_colors[1])
    # Glow pixels
    p((5, 4), (80, 160, 255, 100))
    p((9, 6), (80, 160, 255, 100))
    p((5, 8), (80, 160, 255, 80))
    p((9, 3), (80, 160, 255, 80))
    img.save(OUT + "chakra_blade.png")

def kusanagi():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.putpixel
    # Handle (dark purple)
    for y in range(12, 16):
        for x in range(7, 9):
            p((x, y), (80, 40, 100, 255))
    # Guard (gold ornate)
    for x in range(5, 11):
        p((x, 11), (220, 190, 60, 255))
    p((5, 10), (220, 190, 60, 255))
    p((10, 10), (220, 190, 60, 255))
    # Blade (white/silver)
    for y in range(1, 11):
        p((7, y), (240, 240, 250, 255))
        p((8, y), (210, 215, 225, 255))
    # Blade edge highlight
    for y in range(2, 10):
        p((6, y), (200, 200, 215, 255))
    # Tip
    p((7, 0), (250, 250, 255, 255))
    # Subtle purple glow (Orochimaru vibe)
    p((5, 3), (140, 80, 200, 60))
    p((9, 5), (140, 80, 200, 60))
    p((9, 7), (140, 80, 200, 50))
    img.save(OUT + "kusanagi.png")

def smoke_bomb():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.putpixel
    # Main sphere (dark gray/black)
    sphere = [
        (6,4),(7,4),(8,4),(9,4),
        (5,5),(6,5),(7,5),(8,5),(9,5),(10,5),
        (4,6),(5,6),(6,6),(7,6),(8,6),(9,6),(10,6),(11,6),
        (4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7),
        (4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8),
        (5,9),(6,9),(7,9),(8,9),(9,9),(10,9),
        (6,10),(7,10),(8,10),(9,10),
    ]
    for x, y in sphere:
        shade = 50 + (y - 4) * 5
        p((x, y), (shade, shade, shade, 255))
    # Highlight
    p((6, 5), (90, 90, 90, 255))
    p((7, 5), (100, 100, 100, 255))
    # Fuse on top
    p((7, 3), (160, 120, 60, 255))
    p((7, 2), (160, 120, 60, 255))
    p((8, 2), (180, 140, 70, 255))
    # Spark
    p((8, 1), (255, 200, 50, 255))
    p((9, 1), (255, 150, 30, 200))
    # Smoke wisps
    p((3, 5), (120, 120, 120, 80))
    p((12, 7), (120, 120, 120, 80))
    p((3, 8), (100, 100, 100, 60))
    p((12, 5), (100, 100, 100, 60))
    # Bottom shadow
    for x in range(5, 11):
        p((x, 11), (30, 30, 30, 40))
    img.save(OUT + "smoke_bomb.png")

def soldier_pill():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.putpixel
    # Pill shape (red capsule, horizontal)
    # Top half (brighter red)
    pill_top = [
        (5,6),(6,6),(7,6),(8,6),(9,6),(10,6),
        (4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7),
    ]
    for x, y in pill_top:
        p((x, y), (200, 40, 40, 255))
    # Bottom half (darker red)
    pill_bot = [
        (4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8),
        (5,9),(6,9),(7,9),(8,9),(9,9),(10,9),
    ]
    for x, y in pill_bot:
        p((x, y), (160, 30, 30, 255))
    # Center line (divider)
    for x in range(4, 12):
        if 4 <= x <= 11:
            p((x, 7), (180, 35, 35, 255))
    p((7, 7), (220, 220, 220, 255))
    p((8, 7), (220, 220, 220, 255))
    # Highlight
    p((6, 6), (240, 80, 80, 255))
    p((7, 6), (240, 80, 80, 255))
    # Rounded ends
    p((3, 7), (180, 35, 35, 200))
    p((3, 8), (140, 25, 25, 200))
    p((12, 7), (180, 35, 35, 200))
    p((12, 8), (140, 25, 25, 200))
    img.save(OUT + "soldier_pill.png")

chakra_blade()
kusanagi()
smoke_bomb()
soldier_pill()
print("All 4 icons generated (16x16 px)")
