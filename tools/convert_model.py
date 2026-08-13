"""
1.12.2 ModelBase bytecode -> 1.20.1 LayerDefinition converter.

The legacy mod ships no Java source, only .class files, so the geometry has to come
out of `javap -p -c -constants` output. Every one of these models is a straight-line
constructor: allocate a ModelRenderer, set its rotation point, hang boxes off it,
parent it to something. That is regular enough to run through a tiny stack machine
rather than a real decompiler.

Usage: python convert_model.py <javap_dump.txt> <JavaClassName> <registry_name>
"""
import re
import sys

SELF = object()


class New:
    def __init__(self, kind):
        self.kind = kind


class Field:
    def __init__(self, name):
        self.name = name

    def __repr__(self):
        return "Field(%s)" % self.name


class Data:
    """A primitive array. Several of these models carry rotation lookup tables next to the
    geometry; they have to be walked past without being mistaken for a list of parts."""


class Arr:
    """A ModelRenderer[] field: elements get synthetic names like "tail0", "tail1"."""

    def __init__(self, name):
        self.name = name
        self.elements = {}


class Part:
    def __init__(self, name):
        self.name = name
        self.pos = (0.0, 0.0, 0.0)
        self.rot = (0.0, 0.0, 0.0)
        self.boxes = []          # (u, v, x, y, z, w, h, d, delta, mirror)
        self.parent = None
        self.children = []


def descriptor_arg_count(signature):
    """Number of operand-stack values a call's arguments occupy.

    Reads the real JVM descriptor out of the javap comment. Long and double genuinely take
    two slots, but this walker pushes one value per constant, so they count as one here.
    """
    if "(" not in signature or ")" not in signature:
        return 0
    args = signature[signature.index("(") + 1:signature.index(")")]
    count, i = 0, 0
    while i < len(args):
        while i < len(args) and args[i] == "[":
            i += 1
        if i >= len(args):
            break
        if args[i] == "L":
            i = args.index(";", i) + 1
        else:
            i += 1
        count += 1
    return count


def parse(path):
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        lines = fh.readlines()

    # Only the constructor body. A constructor is the one member whose name, stripped of
    # modifiers, is exactly the fully-qualified class name -- inner-class constructors take
    # the outer instance as a hidden first argument, so an empty parameter list is no test.
    owner_class = None
    for line in lines:
        m = re.match(r"^\s*(?:public |final |abstract )*class ([\w.$]+)", line)
        if m:
            owner_class = m.group(1)
            break
    if owner_class is None:
        raise SystemExit("could not find the class declaration in %s" % path)

    body, inside = [], False
    for line in lines:
        stripped = line.strip()
        decl = re.match(r"^(?:public |protected |private |static |final )*([\w.$]+)\(.*\);$", stripped)
        if decl:
            inside = decl.group(1) == owner_class
            continue
        if inside:
            if re.match(r"^(public|protected|private|static|final)\s", stripped) and "Code:" not in stripped:
                break
            body.append(stripped)
    if not body:
        raise SystemExit("no constructor body found in %s" % path)

    parts = {}
    tex_w, tex_h = 64, 64
    stack = []
    slots = {}
    arrays = {}
    synthetic = [0]

    def part(name):
        if name not in parts:
            parts[name] = Part(name)
        return parts[name]

    trace = []
    for raw in body:
        m = re.match(r"^\d+:\s+(\S+)\s*(.*)$", raw)
        if not m:
            continue
        trace.append(raw)
        if len(trace) > 14:
            trace.pop(0)

        def nm(x):
            if not isinstance(x, Field):
                raise SystemExit("expected a part ref, got %r\nlast instructions:\n  %s\nstack: %r"
                                 % (x, "\n  ".join(trace), stack))
            return x.name

        def ar(x):
            """The ModelRenderer[] an array instruction is addressing."""
            if isinstance(x, Arr):
                return x
            if isinstance(x, Field):
                # An array field read before it was ever stored, or held in a local.
                return arrays.setdefault(x.name, Arr(x.name))
            raise SystemExit("expected an array ref, got %r\nlast instructions:\n  %s\nstack: %r"
                             % (x, "\n  ".join(trace), stack))
        op, rest = m.group(1), m.group(2)
        comment = rest.split("//", 1)[1].strip() if "//" in rest else ""

        if op == "aload_0":
            stack.append(SELF)
        elif op.startswith("aload"):
            slot = int(op[6:]) if op.startswith("aload_") else int(rest.split("//")[0].strip())
            stack.append(slots.get(slot, SELF))
        elif op.startswith("astore"):
            slot = int(op[7:]) if op.startswith("astore_") else int(rest.split("//")[0].strip())
            value = stack.pop()
            # An unnamed sub-part held in a local. Give it a stable synthetic name so the
            # emitted hierarchy can still refer to it.
            if isinstance(value, New) and value.kind == "ModelRenderer":
                synthetic[0] += 1
                name = "part%d" % synthetic[0]
                part(name)
                slots[slot] = Field(name)
            else:
                slots[slot] = value
        elif op == "dup":
            stack.append(stack[-1])
        elif op == "pop":
            stack.pop()
        elif op == "new":
            stack.append(New("ModelBox" if "ModelBox" in comment else "ModelRenderer"))
        elif op.startswith("iconst_"):
            stack.append(int(op[-1]))
        elif op == "iconst_m1":
            stack.append(-1)
        elif op in ("bipush", "sipush"):
            stack.append(int(rest.split("//")[0].strip()))
        elif op.startswith("fconst_"):
            stack.append(float(op[-1]))
        elif op in ("ldc", "ldc_w", "ldc2_w"):
            val = comment
            if val.startswith("float "):
                stack.append(float(val[6:].rstrip("f")))
            elif val.startswith("int "):
                stack.append(int(val[4:]))
            elif val.startswith("double "):
                stack.append(float(val[7:]))
            else:
                stack.append(val)
        elif op in ("newarray", "multianewarray"):
            # A primitive array: an animation lookup table, never geometry.
            stack.pop()
            stack.append(Data())
        elif op == "anewarray":
            stack.pop()
            stack.append(Arr(None) if "ModelRenderer" in comment else Data())
        elif op == "arraylength":
            stack.pop()
            stack.append(0)
        elif op in ("fastore", "iastore", "dastore", "lastore",
                    "bastore", "sastore", "castore"):
            stack.pop(); stack.pop(); stack.pop()
        elif op in ("faload", "iaload", "daload", "laload", "baload", "saload", "caload"):
            stack.pop(); stack.pop()
            stack.append(0.0)
        elif op == "aaload":
            idx = stack.pop()
            owner = stack.pop()
            if isinstance(owner, Data):
                stack.append(Data())
                continue
            arr = ar(owner)
            name = "%s%d" % (arr.name, idx)
            part(name)
            stack.append(Field(name))
        elif op == "aastore":
            value = stack.pop()
            idx = stack.pop()
            owner = stack.pop()
            if isinstance(owner, Data) or isinstance(value, Data):
                continue
            part("%s%d" % (ar(owner).name, idx))
        elif op == "getfield":
            fname = comment.split("Field ", 1)[1].split(":")[0].split(".")[-1]
            owner = stack.pop()
            if ":[" in comment:
                stack.append(arrays.setdefault(fname, Arr(fname)))
                continue
            # ModelRenderer.cubeList: keep naming the owner, the list itself is anonymous.
            if fname == "field_78804_l" and isinstance(owner, Field):
                stack.append(owner)
            else:
                stack.append(Field(fname))
        elif op == "putfield":
            fname = comment.split("Field ", 1)[1].split(":")[0].split(".")[-1]
            value = stack.pop()
            stack.pop()
            if fname == "field_78090_t":
                tex_w = value
            elif fname == "field_78089_u":
                tex_h = value
            elif isinstance(value, Arr):
                value.name = fname
                arrays[fname] = value
            elif isinstance(value, New) and value.kind == "ModelRenderer":
                part(fname)
            elif isinstance(value, Field) and value.name in parts:
                # A part built in a local and only then stored to a field: adopt the
                # field's name so the hierarchy reads like the original source.
                moved = parts.pop(value.name)
                moved.name = fname
                parts[fname] = moved
                for other in parts.values():
                    other.children = [fname if c == value.name else c for c in other.children]
                    if other.parent == value.name:
                        other.parent = fname
                for slot, ref in slots.items():
                    if isinstance(ref, Field) and ref.name == value.name:
                        slots[slot] = Field(fname)
        elif op in ("invokespecial", "invokevirtual", "invokeinterface", "invokestatic"):
            sig = comment
            if "ModelRenderer.\"<init>\":(Lnet/minecraft/client/model/ModelBase;II)" in sig:
                v, u, _base, ref = stack.pop(), stack.pop(), stack.pop(), stack.pop()
                stack.append(New("ModelRenderer"))
                stack[-1].texoffs = (u, v)
            elif "ModelRenderer.\"<init>\":(Lnet/minecraft/client/model/ModelBase;)" in sig:
                stack.pop(); stack.pop()
            elif "ModelBox.\"<init>\"" in sig:
                mirror = stack.pop()
                delta = stack.pop()
                d, h, w = stack.pop(), stack.pop(), stack.pop()
                z, y, x = stack.pop(), stack.pop(), stack.pop()
                v, u = stack.pop(), stack.pop()
                owner = stack.pop()      # ModelRenderer the box belongs to
                part(nm(owner)).boxes.append((u, v, x, y, z, w, h, d, delta, bool(mirror)))
            elif "func_78793_a" in sig:            # setRotationPoint(F,F,F)
                z, y, x = stack.pop(), stack.pop(), stack.pop()
                target = stack.pop()
                part(nm(target)).pos = (x, y, z)
            elif "func_78792_a" in sig:            # addChild(ModelRenderer)
                child = stack.pop()
                parent = stack.pop()
                cname, pname = nm(child), nm(parent)
                part(cname).parent = pname
                part(pname).children.append(cname)
            elif "setRotationAngle" in sig:
                z, y, x = stack.pop(), stack.pop(), stack.pop()
                target = stack.pop()
                stack.pop()                         # the ModelBase receiver
                part(nm(target)).rot = (x, y, z)
            elif "func_78784_a" in sig:            # setTextureOffset -> not used by these
                stack.pop(); stack.pop(); stack.pop()
            elif "List.add" in sig:
                stack.pop(); stack.pop()
                stack.append(1)
            else:
                # Any other call: balance the stack from its real descriptor. Counting
                # letters in the signature text instead was what silently desynced the
                # models that call a superclass constructor, and a desynced stack turns
                # into nonsense geometry several hundred instructions later.
                nargs = descriptor_arg_count(sig)
                if op != "invokestatic":
                    nargs += 1  # the receiver
                for _ in range(min(nargs, len(stack))):
                    stack.pop()
                if not sig.rstrip().endswith(")V") and ")" in sig:
                    stack.append(0)  # a non-void return leaves something behind
        elif op in ("return", "nop"):
            pass
        elif op.startswith("if") or op in ("goto", "iinc", "istore_1", "istore"):
            # Straight-line walking stops at the first branch. Models that build a repeating
            # chain in a loop (the snake's spine) get the loop transcribed by hand instead of
            # guessed at -- see GiantSnakeModel.
            sys.stderr.write("  stopped at control flow (%s); loop body not converted\n" % op)
            break

    return parts, tex_w, tex_h


def f(v):
    return "%.4ff" % float(v)


def emit(parts, tex_w, tex_h, class_name, reg_name, package):
    roots = [p for p in parts.values() if p.parent is None]
    lines = []
    a = lines.append

    a("package %s;" % package)
    a("")
    a("import com.mojang.blaze3d.vertex.PoseStack;")
    a("import com.mojang.blaze3d.vertex.VertexConsumer;")
    a("import com.sekwah.narutomod.NarutoMod;")
    a("import net.minecraft.client.model.Model;")
    a("import net.minecraft.client.model.geom.ModelLayerLocation;")
    a("import net.minecraft.client.model.geom.ModelPart;")
    a("import net.minecraft.client.model.geom.PartPose;")
    a("import net.minecraft.client.model.geom.builders.*;")
    a("import net.minecraft.resources.ResourceLocation;")
    a("")
    a("/**")
    a(" * Geometry imported from the 1.12.2 mod's %s." % class_name.replace("Model", ""))
    a(" * Machine-converted from bytecode: box coordinates and pivots are the originals,")
    a(" * so this model shares their +Y-downward authoring convention.")
    a(" */")
    a("public class %s extends Model {" % class_name)
    a("")
    a("    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(")
    a("            new ResourceLocation(NarutoMod.MOD_ID, \"%s\"), \"main\");" % reg_name)
    a("")
    a("    private final ModelPart root;")
    a("")
    a("    public %s(ModelPart root) {" % class_name)
    a("        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);")
    a("        this.root = root;")
    a("    }")
    a("")
    a("    public static LayerDefinition createBodyLayer() {")
    a("        MeshDefinition mesh = new MeshDefinition();")
    a("        PartDefinition partdefinition = mesh.getRoot();")

    emitted = set()

    def emit_part(p, parent_var):
        var = p.name
        cubes = "CubeListBuilder.create()"
        if not p.boxes:
            cubes = "CubeListBuilder.create()"
        for (u, v, x, y, z, w, h, d, delta, mirror) in p.boxes:
            if mirror:
                cubes += ".mirror()"
            cubes += ".texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))" % (
                int(u), int(v), f(x), f(y), f(z), f(w), f(h), f(d), f(delta))
            if mirror:
                cubes += ".mirror(false)"
        px, py, pz = p.pos
        rx, ry, rz = p.rot
        if (rx, ry, rz) == (0.0, 0.0, 0.0):
            pose = "PartPose.offset(%s, %s, %s)" % (f(px), f(py), f(pz))
        else:
            pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (
                f(px), f(py), f(pz), f(rx), f(ry), f(rz))
        a("        PartDefinition %s = %s.addOrReplaceChild(\"%s\", %s, %s);"
          % (var, parent_var, p.name, cubes, pose))
        emitted.add(p.name)
        for childname in p.children:
            if childname not in emitted:
                emit_part(parts[childname], var)

    for r in roots:
        if r.name not in emitted:
            emit_part(r, "partdefinition")

    a("        return LayerDefinition.create(mesh, %d, %d);" % (tex_w, tex_h))
    a("    }")
    a("")
    a("    @Override")
    a("    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,")
    a("                               int packedOverlay, float red, float green, float blue, float alpha) {")
    a("        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);")
    a("    }")
    a("")
    a("    public ModelPart root() {")
    a("        return this.root;")
    a("    }")
    a("}")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    dump, cls, reg = sys.argv[1], sys.argv[2], sys.argv[3]
    pkg = sys.argv[4] if len(sys.argv) > 4 else "com.sekwah.narutomod.client.model.entity"
    parsed, tw, th = parse(dump)
    total = sum(len(p.boxes) for p in parsed.values())
    sys.stderr.write("%s: %d parts, %d boxes, texture %dx%d\n" % (cls, len(parsed), total, tw, th))
    sys.stdout.write(emit(parsed, tw, th, cls, reg, pkg))
