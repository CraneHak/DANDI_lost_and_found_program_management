package org.example.vision;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ColorNameResolver {
    private static final List<NamedColor> PALETTE = List.of(
            new NamedColor("검정", 0, 0, 0),
            new NamedColor("짙은 회색", 64, 64, 64),
            new NamedColor("회색", 128, 128, 128),
            new NamedColor("밝은 회색", 192, 192, 192),
            new NamedColor("흰색", 255, 255, 255),
            new NamedColor("빨강", 220, 20, 60),
            new NamedColor("진홍", 178, 34, 34),
            new NamedColor("버건디", 128, 0, 32),
            new NamedColor("분홍", 255, 105, 180),
            new NamedColor("주황", 255, 140, 0),
            new NamedColor("갈색", 139, 69, 19),
            new NamedColor("베이지", 222, 184, 135),
            new NamedColor("노랑", 255, 215, 0),
            new NamedColor("올리브", 128, 128, 0),
            new NamedColor("연두", 154, 205, 50),
            new NamedColor("초록", 34, 139, 34),
            new NamedColor("청록", 0, 139, 139),
            new NamedColor("민트", 102, 205, 170),
            new NamedColor("하늘", 135, 206, 235),
            new NamedColor("파랑", 30, 144, 255),
            new NamedColor("남색", 25, 25, 112),
            new NamedColor("보라", 138, 43, 226)
    );

    public String resolveName(int red, int green, int blue) {
        LabColor target = rgbToLab(red, green, blue);
        NamedColor nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (NamedColor namedColor : PALETTE) {
            LabColor paletteLab = rgbToLab(namedColor.red(), namedColor.green(), namedColor.blue());
            double distance = distance(target, paletteLab);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = namedColor;
            }
        }

        return nearest != null ? nearest.name() : "기타";
    }

    private double distance(LabColor a, LabColor b) {
        double dl = a.l() - b.l();
        double da = a.a() - b.a();
        double db = a.b() - b.b();
        return Math.sqrt(dl * dl + da * da + db * db);
    }

    private LabColor rgbToLab(int red, int green, int blue) {
        double r = pivotRgb(red / 255.0);
        double g = pivotRgb(green / 255.0);
        double b = pivotRgb(blue / 255.0);

        double x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047;
        double y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
        double z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883;

        x = pivotXyz(x);
        y = pivotXyz(y);
        z = pivotXyz(z);

        double l = Math.max(0, 116 * y - 16);
        double a = 500 * (x - y);
        double bb = 200 * (y - z);
        return new LabColor(l, a, bb);
    }

    private double pivotRgb(double value) {
        return value > 0.04045
                ? Math.pow((value + 0.055) / 1.055, 2.4)
                : value / 12.92;
    }

    private double pivotXyz(double value) {
        return value > 0.008856
                ? Math.cbrt(value)
                : (7.787 * value) + (16.0 / 116.0);
    }

    private record NamedColor(String name, int red, int green, int blue) {
    }

    private record LabColor(double l, double a, double b) {
    }
}
