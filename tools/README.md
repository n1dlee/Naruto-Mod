# Model porting tools

The 1.12.2 mod in `narutomod-1-12-2-jutsus/` ships **only compiled `.class` files** — no Java
source anywhere in the tree. Its entity models are hand-written `ModelBase` subclasses, so
porting one means reading its constructor bytecode and re-emitting the geometry as a 1.20.1
`LayerDefinition`. These two scripts are that pipeline.

## convert_model.py

Walks the constructor of a decompiled `ModelBase` and emits a 1.20.1 `Model` subclass.

```bash
javap -p -c -constants "narutomod-1-12-2-jutsus/entity/EntityToad\$ModelToad.class" > toad.txt
python tools/convert_model.py toad.txt GiantToadModel giant_toad > GiantToadModel.java
```

Arguments: `<javap dump> <JavaClassName> <model layer registry name> [package]`.

It is a small stack machine over the javap listing, not a decompiler. It understands what
these models actually do — `new ModelRenderer`, `setRotationPoint`, `addChild`,
`setRotationAngle`, `new ModelBox`, parts held in locals, and parts held in a
`ModelRenderer[]` field — and nothing else.

**It stops at the first branch instruction and says so on stderr.** A model that builds a
repeating chain in a loop (the snake's spine) gets its straight-line part converted and the
loop transcribed by hand; see the spine block in `GiantSnakeModel`. Silently guessing at loop
bodies would be worse than not converting them.

Box coordinates and pivots come out as the originals, so the result keeps the 1.12.2
+Y-downward authoring convention. The renderer still has to apply the usual
`scale(-S, -S, S)` flip.

## model_extents.py

Reports the rest-pose bounding box of a converted model, in model units and in blocks at
scale 1:

```bash
python tools/model_extents.py GiantToadModel.java
```

Use it to size the entity hitbox to what is actually drawn, and to find how far below the
model's origin its feet sit — none of the imported models put the origin on the ground, so
the renderer has to lift by exactly that much or the entity stands buried.
