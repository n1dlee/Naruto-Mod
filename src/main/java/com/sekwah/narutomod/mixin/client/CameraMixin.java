package com.sekwah.narutomod.mixin.client;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooFullBody;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Unique private static final double[] narutomod$CAMERA_OFFSETS = {-.13, .13};
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Unique private float narutomod$zoom;
    @Unique private float narutomod$oldZoom;
    @Unique private float narutomod$standing;
    @Unique private float narutomod$oldStanding;
    @Unique private float narutomod$partial;

    @Inject(method = "tick", at = @At("TAIL"))
    private void narutomod$tickSusanooFraming(CallbackInfo callback) {
        Entity subject = ((Camera) (Object) this).getEntity();
        if (!(subject instanceof LocalPlayer player) || player != Minecraft.getInstance().player) {
            narutomod$oldZoom = narutomod$zoom = 0;
            narutomod$oldStanding = narutomod$standing = 0;
            return;
        }
        int form = player.isSleeping() || player.isSpectator() ? 0 : player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isSusanooActive() ? data.getSusanooStage() : 0).orElse(0);
        boolean standing = SusanooFullBody.standing(form);
        float zoom = (float) (SusanooForm.perfect(form) ? SusanooFullBody.PERFECT_CAMERA_BACK : standing ? SusanooFullBody.STANDING_CAMERA_BACK : 0);
        narutomod$oldZoom = narutomod$zoom;
        narutomod$zoom = Mth.approach(narutomod$zoom, zoom, zoom > narutomod$zoom ? 1.95f : 2.72f);
        narutomod$oldStanding = narutomod$standing;
        narutomod$standing = Mth.approach(narutomod$standing, standing ? 1 : 0, standing ? .135f : .17f);
    }

    @Inject(method = "setup", at = @At("HEAD"))
    private void narutomod$capturePartial(BlockGetter level, Entity subject, boolean detached, boolean reverse,
                                         float partial, CallbackInfo callback) {
        narutomod$partial = partial;
    }

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void narutomod$raiseStandingCamera(BlockGetter level, Entity subject, boolean detached, boolean reverse,
                                              float partial, CallbackInfo callback) {
        if (subject != Minecraft.getInstance().player) return;
        float weight = Mth.lerp(partial, narutomod$oldStanding, narutomod$standing);
        if (weight <= 0) return;
        Vec3 base = ((Camera) (Object) this).getPosition();
        double rise = SusanooFullBody.rise(base.y, subject.getPosition(partial).y, weight);
        if (rise <= .0001) return;
        double top = base.y + rise;
        double limited = narutomod$ceiling(level, subject, base, top);
        for (double x : narutomod$CAMERA_OFFSETS) {
            for (double z : narutomod$CAMERA_OFFSETS) {
                limited = Math.min(limited, narutomod$ceiling(level, subject, base.add(x, 0, z), top));
            }
        }
        setPosition(base.x, Math.max(base.y, limited), base.z);
    }

    @Unique
    private static double narutomod$ceiling(BlockGetter level, Entity subject, Vec3 from, double top) {
        var hit = level.clip(new ClipContext(from, new Vec3(from.x, top, from.z), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, subject));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y - .18 : top;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), index = 0)
    private double narutomod$frameCompleteBody(double distance) {
        if (((Camera) (Object) this).getEntity() != Minecraft.getInstance().player) {
            return distance;
        }
        return distance + Mth.lerp(narutomod$partial, narutomod$oldZoom, narutomod$zoom);
    }
}
