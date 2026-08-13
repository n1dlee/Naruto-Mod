"""Report the world-space extents of a converted model, so the entity hitbox can be
sized to what is actually drawn instead of guessed at."""
import re
import sys

src = open(sys.argv[1], encoding="utf-8").read()

# name -> (parent, pose xyz, [boxes])
pat = re.compile(
    r"PartDefinition (\w+) = (\w+)\.addOrReplaceChild\(\"[^\"]+\", (.*?), "
    r"PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);")
box = re.compile(r"addBox\(([^)]*?), new CubeDeformation")

parts = {}
for m in pat.finditer(src):
    name, parent, cubes, kind, pose = m.groups()
    nums = [float(v.strip().rstrip("f")) for v in pose.split(",")]
    boxes = []
    for b in box.finditer(cubes):
        boxes.append([float(v.strip().rstrip("f")) for v in b.group(1).split(",")])
    parts[name] = (parent, nums[:3], boxes)

lo = [1e9] * 3
hi = [-1e9] * 3


def walk(name, ox, oy, oz):
    parent, pose, boxes = parts[name]
    px, py, pz = ox + pose[0], oy + pose[1], oz + pose[2]
    for (bx, by, bz, w, h, d) in boxes:
        for (a, b) in ((px + bx, px + bx + w), (py + by, py + by + h), (pz + bz, pz + bz + d)):
            pass
        for i, (a, b) in enumerate(((px + bx, px + bx + w), (py + by, py + by + h),
                                    (pz + bz, pz + bz + d))):
            lo[i] = min(lo[i], a, b)
            hi[i] = max(hi[i], a, b)
    for child, (cp, _, _) in parts.items():
        if cp == name:
            walk(child, px, py, pz)


for n, (p, _, _) in parts.items():
    if p == "partdefinition":
        walk(n, 0, 0, 0)

# Rotations are ignored: these are axis-aligned extents of the rest pose, which is what a
# hitbox wants anyway.
print("x %.2f..%.2f  y %.2f..%.2f  z %.2f..%.2f  (model units)" %
      (lo[0], hi[0], lo[1], hi[1], lo[2], hi[2]))
print("height %.2f units = %.3f blocks at scale 1" % (hi[1] - lo[1], (hi[1] - lo[1]) / 16.0))
print("width  %.2f units = %.3f blocks at scale 1" % (hi[0] - lo[0], (hi[0] - lo[0]) / 16.0))
print("depth  %.2f units = %.3f blocks at scale 1" % (hi[2] - lo[2], (hi[2] - lo[2]) / 16.0))
print("feet at y = %.2f units below origin" % hi[1])
