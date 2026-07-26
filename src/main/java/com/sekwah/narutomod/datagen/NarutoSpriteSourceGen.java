package com.sekwah.narutomod.datagen;

import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SpriteSourceProvider;

public class NarutoSpriteSourceGen extends SpriteSourceProvider {
    public NarutoSpriteSourceGen(PackOutput output, ExistingFileHelper fileHelper, String modid) {
        super(output, fileHelper, modid);
    }

    @Override
    protected void addSources() {
        // Since 1.19.3 only directories declared here end up on the block atlas; anything
        // else a model references renders as the missing-texture magenta. "blocks" itself
        // was never listed, which is why the imported 3D weapon/scroll models came out
        // purple until it was added.
        this.atlas(BLOCKS_ATLAS)
                .addSource(addFolder("blocks"))
                .addSource(addFolder("blocks/weapons"))
                .addSource(addFolder("blocks/deco"))
                .addSource(addFolder("items"));
    }

    private DirectoryLister addFolder(String folderName) {
        return new DirectoryLister(folderName, folderName + "/");
    }
}
