package com.scopewise.lunarthursday;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** Resolution-independent decorative Hijri sky; no bitmap, network or external fonts. */
final class MoonSkyView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    MoonSkyView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        canvas.scale(getWidth() / 360f, getHeight() / 160f);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 355, 163,
                new int[]{Color.rgb(1, 47, 50), Color.rgb(33, 75, 98), Color.rgb(134, 124, 169)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 360, 160, p);
        p.setShader(null);

        p.setColor(Color.rgb(253, 226, 156));
        float[][] stars = {{31,29,1.5f},{68,54,1.4f},{124,20,1.3f},{179,44,1.2f},
                {239,22,1.8f},{300,41,1.4f},{326,92,1.4f},{262,78,1f},{99,104,1f}};
        for (float[] s:stars) canvas.drawCircle(s[0], s[1], s[2], p);
        Path moon = new Path();
        moon.setFillType(Path.FillType.EVEN_ODD);
        moon.addCircle(180, 51, 31, Path.Direction.CW);
        moon.addCircle(193, 41, 27, Path.Direction.CW);
        p.setColor(Color.rgb(255, 228, 159));
        canvas.drawPath(moon, p);

        // Distant ridges and a subtle skyline keep the illustration legible at small sizes.
        Path hills = new Path();
        hills.moveTo(0, 141);
        hills.cubicTo(60, 126, 93, 151, 144, 136);
        hills.cubicTo(203, 113, 244, 142, 296, 127);
        hills.lineTo(360, 145);
        hills.lineTo(360, 160);
        hills.lineTo(0, 160);
        hills.close();
        p.setColor(Color.rgb(28, 65, 72));
        canvas.drawPath(hills, p);

        // Rounded mosque silhouette and minarets, deliberately non-specific.
        p.setColor(Color.rgb(6, 43, 48));
        canvas.drawRect(110, 128, 252, 160, p);
        Path dome = new Path();
        dome.moveTo(130, 132);
        dome.cubicTo(131, 92, 229, 92, 230, 132);
        dome.close();
        canvas.drawPath(dome, p);
        canvas.drawRect(121, 118, 136, 160, p);
        canvas.drawRect(224, 118, 239, 160, p);
        for (float x:new float[]{84, 271}) {
            canvas.drawRect(x, 96, x + 10, 160, p);
            Path cap = new Path();
            cap.moveTo(x-4, 96); cap.quadTo(x+5, 72, x+14, 96);cap.close();
            canvas.drawPath(cap, p);
            canvas.drawRect(x+4, 72, x+6, 84, p);
        }
        p.setColor(Color.rgb(245, 205, 129));
        canvas.drawCircle(180, 101, 3, p);
        canvas.drawRect(178.7f, 95, 181.3f, 103, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(Color.rgb(222, 181, 114));
        Path arch = new Path();
        arch.moveTo(52, 160);
        arch.lineTo(52, 79);
        arch.cubicTo(52, 54, 67, 40, 90, 31);
        arch.cubicTo(135, 15, 147, 7, 180, 3);
        arch.cubicTo(213, 7, 225, 15, 270, 31);
        arch.cubicTo(293, 40, 308, 54, 308, 79);
        arch.lineTo(308, 160);
        canvas.drawPath(arch, p);
        p.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }
}
