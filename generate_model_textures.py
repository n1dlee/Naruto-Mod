"""Generate 64x32 model textures for chakra_blade and kusanagi (same UV layout as katana)."""
from PIL import Image

OUT = "src/main/resources/assets/narutomod/textures/items/weapons/model/"

def chakra_blade_model():
    """Cyan/blue blade, dark teal handle, gold guard."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p = img.putpixel

    # Handle area: UV roughly x=0-10, y=0-10 (handle wrap texture)
    # Dark teal handle wrapping
    for y in range(2, 10):
        for x in range(0, 10):
            base = 40 + (x % 2) * 15
            p((x, y), (base - 10, base + 30, base + 40, 255))
    # Handle top/bottom caps
    for x in range(0, 10):
        p((x, 0), (30, 70, 80, 255))
        p((x, 1), (35, 75, 85, 255))

    # Guard area: UV x=0-15, y=10-13 (gold)
    for y in range(10, 14):
        for x in range(0, 15):
            shade = 190 + (x % 3) * 10
            p((x, y), (shade, shade - 30, 40, 255))

    # Blade area: UV x=32-48, y=0-26 (two faces of the blade)
    # Blade face 1 (right side): x=32-40
    for y in range(0, 26):
        grad = int(60 + (y / 26.0) * 40)  # darker at tip
        for x in range(32, 40):
            cx = x - 32
            highlight = max(0, 20 - abs(cx - 4) * 8)
            p((x, y), (grad + highlight, grad + 100 + highlight, grad + 160 + highlight, 255))

    # Blade face 2 (left side): x=40-48
    for y in range(0, 26):
        grad = int(50 + (y / 26.0) * 35)
        for x in range(40, 48):
            cx = x - 40
            highlight = max(0, 15 - abs(cx - 3) * 6)
            p((x, y), (grad + highlight, grad + 80 + highlight, grad + 140 + highlight, 255))

    img.save(OUT + "chakra_blade.png")
    print("  model/chakra_blade.png OK")

def kusanagi_model():
    """White/silver blade, dark purple handle, gold guard."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p = img.putpixel

    # Handle: dark purple wrapping
    for y in range(2, 10):
        for x in range(0, 10):
            base = 50 + (x % 2) * 12
            p((x, y), (base + 10, base - 20, base + 30, 255))
    for x in range(0, 10):
        p((x, 0), (50, 20, 60, 255))
        p((x, 1), (55, 25, 65, 255))

    # Guard: ornate gold
    for y in range(10, 14):
        for x in range(0, 15):
            shade = 200 + (x % 3) * 12
            p((x, y), (min(shade, 255), shade - 25, 50, 255))

    # Blade face 1: bright silver/white
    for y in range(0, 26):
        grad = int(190 + (26 - y) / 26.0 * 50)  # brighter at tip
        for x in range(32, 40):
            cx = x - 32
            highlight = max(0, 25 - abs(cx - 4) * 8)
            val = min(255, grad + highlight)
            p((x, y), (val, val, min(255, val + 5), 255))

    # Blade face 2: slightly darker silver
    for y in range(0, 26):
        grad = int(175 + (26 - y) / 26.0 * 40)
        for x in range(40, 48):
            cx = x - 40
            highlight = max(0, 18 - abs(cx - 3) * 6)
            val = min(255, grad + highlight)
            p((x, y), (val - 5, val - 3, val, 255))

    img.save(OUT + "kusanagi.png")
    print("  model/kusanagi.png OK")

chakra_blade_model()
kusanagi_model()
print("Done — 3D model textures generated (64x32)")
