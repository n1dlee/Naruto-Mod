package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class SusanooImpactGeometry {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private SusanooImpactGeometry() {
    }

    public static Contact sample(SusanooPose.Frame frame, SusanooForm form, @Nullable Vec3 source, Vec3 incident) {
        if (source == null && incident.lengthSqr() < 1e-6) {
            return new Contact(frame.at(0, SusanooFullBody.hasLegs(form.id()) ? 8.6 * SusanooFullBody.scale(form.id()) : 1.65, 0), UP);
        }
        double y = source == null ? 1.65 : source.y - frame.feet().y;
        double side = source == null ? 0 : source.subtract(frame.feet()).dot(frame.right());
        Section section = section(form, y, side);
        Vec3 center = frame.at(section.offset(), section.height(), section.depth());
        Vec3 toward = incident.lengthSqr() >= 1e-6 ? incident.normalize().scale(-1) : source.subtract(center);
        double x = toward.dot(frame.right());
        double z = toward.dot(frame.forward());
        if (x * x + z * z < 1e-8) {
            double sign = toward.y < 0 ? -1 : 1;
            return new Contact(center.add(0, .16 * sign, 0), UP.scale(sign));
        }
        double width = section.width();
        double depth = section.radius();
        double scale = 1 / Math.sqrt(x * x / (width * width) + z * z / (depth * depth));
        Vec3 delta = frame.right().scale(x * scale).add(frame.forward().scale(z * scale));
        Vec3 normal = frame.right().scale(x / (width * width)).add(frame.forward().scale(z / (depth * depth))).normalize();
        return new Contact(center.add(delta).add(normal.scale(.035)), normal);
    }

    private static Section section(SusanooForm form, double y, double side) {
        if (SusanooFullBody.hasLegs(form.id())) {
            double scale = SusanooFullBody.scale(form.id());
            return fullBody(y / scale, side).scaled(scale);
        }
        double scale = form.torsoScale();
        if (form == SusanooForm.RIBS) {
            return new Section(0, Mth.clamp(y, 1.1, 2.49), -.10, 1.08, .78);
        }
        if (y > 3.15 * scale) {
            return new Section(0, Mth.clamp(y, 3.15 * scale, 4.48 * scale), -.03, .66, .55);
        }
        return new Section(0, Mth.clamp(y, 1.08 * scale, 3.15 * scale), -.12, 1.35 * scale, .84 * scale);
    }

    private static Section fullBody(double y, double side) {
        if (y < 7.6) return new Section(Math.copySign(1.55, side), Mth.clamp(y, .65, 7.6), .12, .94, .88);
        if (y > 14) return new Section(0, Mth.clamp(y, 14.5, 17.2), -.16, 1.25, 1.02);
        return new Section(0, Mth.clamp(y, 7.6, 14), -.13, 2.95, 1.45);
    }

    public record Contact(Vec3 point, Vec3 normal) {
    }

    private record Section(double offset, double height, double depth, double width, double radius) {
        private Section scaled(double scale) {
            return new Section(offset * scale, height * scale, depth * scale, width * scale, radius * scale);
        }
    }
}
