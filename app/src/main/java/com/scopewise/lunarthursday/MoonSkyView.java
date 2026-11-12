package com.scopewise.lunarthursday;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import java.time.LocalTime;

/** Battery-friendly time-aware sky and optional location-weather effects. */
final class MoonSkyView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private WeatherManager.Scene scene;

    MoonSkyView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setWeather(WeatherManager.Scene value) { scene=value; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(getWidth()==0||getHeight()==0) return;
        int save=canvas.save();
        canvas.scale(getWidth()/360f,getHeight()/160f);
        int hour=LocalTime.now().getHour();
        boolean early=hour>=5&&hour<9,late=hour>=17&&hour<20;
        boolean night=hour<5||hour>=20;
        if(scene!=null) {
            // Forecast daylight flag is authoritative for current local weather; clock selects dawn/dusk palette.
            night=!scene.day;
            early=scene.day&&hour>=5&&hour<9;
            late=scene.day&&hour>=17&&hour<20;
        }
        int[] palette;
        if(night)palette=new int[]{0xFF012B39,0xFF295475,0xFF6B6190};
        else if(early)palette=new int[]{0xFF5E9AAA,0xFFF2B4A0,0xFFFFE8B0};
        else if(late)palette=new int[]{0xFF315B80,0xFFE49886,0xFFF6C681};
        else palette=new int[]{0xFF2B8DB3,0xFF7FC6DC,0xFFD5F0E9};
        int kind=scene==null?WeatherCode.CLEAR:WeatherCode.category(scene.code);
        if(kind==WeatherCode.STORM||kind==WeatherCode.RAIN)palette=new int[]{0xFF263D53,0xFF52708B,0xFF8D9CAB};
        if(kind==WeatherCode.FOG)palette=new int[]{0xFF648C94,0xFF9DBAC3,0xFFC7D4CB};
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0,0,355,160,palette,null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,360,160,p);p.setShader(null);

        if(night||late) {
            p.setColor(0xFFFEE6AD);
            float[][] stars={{31,29,1.3f},{69,53,1.2f},{126,21,1.1f},{239,20,1.6f},{300,41,1.5f},{326,92,1.0f},{263,76,1f},{99,104,1f}};
            for(float[] s:stars)canvas.drawCircle(s[0],s[1],s[2],p);
            Path crescent=new Path();crescent.setFillType(Path.FillType.EVEN_ODD);
            crescent.addCircle(180,49,29,Path.Direction.CW);
            crescent.addCircle(193,40,25,Path.Direction.CW);
            canvas.drawPath(crescent,p);
        } else {
            p.setColor(0xFFFFE7A9);
            canvas.drawCircle(184,48,early?21:27,p);
        }
        if(kind!=WeatherCode.CLEAR) {
            cloud(canvas,72,55,0.88f,night?0xFF859AB1:0xFFE6EBEE);
            cloud(canvas,254,38,0.94f,night?0xFF7792AA:0xFFF3F7F6);
            if(kind==WeatherCode.FOG){
                p.setColor(0x99D8E1E1);
                for(int y=68;y<132;y+=16)canvas.drawRoundRect(18,y,342,y+6,3,3,p);
            }
            if(kind==WeatherCode.RAIN||kind==WeatherCode.STORM){
                p.setColor(0xC6DDE9F5);p.setStrokeWidth(2.2f);p.setStyle(Paint.Style.STROKE);
                for(int i=0;i<25;i++){
                    float x=16+(i*41)%328,y=58+(i*13)%80;
                    canvas.drawLine(x,y,x-5,y+13,p);
                }
                p.setStyle(Paint.Style.FILL);
            }
            if(kind==WeatherCode.SNOW){
                p.setColor(0xFFF7FAFF);
                for(int i=0;i<25;i++)canvas.drawCircle(12+(i*37)%340,52+(i*29)%97,1.8f,p);
            }
            if(kind==WeatherCode.STORM){
                p.setColor(0xFFFFE7AB);
                Path bolt=new Path();bolt.moveTo(280,66);bolt.lineTo(267,90);bolt.lineTo(280,90);
                bolt.lineTo(269,113);bolt.lineTo(290,83);bolt.lineTo(277,83);bolt.close();canvas.drawPath(bolt,p);
            }
        }
        Path hills=new Path();hills.moveTo(0,143);hills.cubicTo(65,126,96,151,146,134);
        hills.cubicTo(205,112,249,139,300,127);hills.lineTo(360,144);hills.lineTo(360,160);
        hills.lineTo(0,160);hills.close();p.setColor(night?0xFF154354:0xFF356F76);canvas.drawPath(hills,p);
        // Stylized mosque and arch are decorative artwork, not a live camera view or official lunar-phase chart.
        p.setColor(night?0xFF072D37:0xFF234E58);
        canvas.drawRect(110,128,252,160,p);
        Path dome=new Path();dome.moveTo(129,132);dome.cubicTo(132,91,230,91,230,132);dome.close();canvas.drawPath(dome,p);
        canvas.drawRect(120,117,137,160,p);canvas.drawRect(224,117,240,160,p);
        for(float x:new float[]{84,271}){
            canvas.drawRect(x,95,x+10,160,p);
            Path cap=new Path();cap.moveTo(x-4,96);cap.quadTo(x+5,72,x+14,96);cap.close();canvas.drawPath(cap,p);
            canvas.drawRect(x+4,72,x+6,84,p);
        }
        p.setColor(night||late?0xFFFFD995:0xFFDAA45E);
        canvas.drawCircle(180,103,2.5f,p);
        canvas.drawRect(178.6f,95,181.4f,104,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(night?0xFFEAC68D:0xFFEEE5CB);
        Path arch=new Path();arch.moveTo(52,160);arch.lineTo(52,79);arch.cubicTo(52,54,68,39,90,31);
        arch.cubicTo(135,15,147,7,180,3);arch.cubicTo(213,7,225,15,270,31);
        arch.cubicTo(293,40,308,54,308,79);arch.lineTo(308,160);canvas.drawPath(arch,p);
        p.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }

    private void cloud(Canvas canvas,float x,float y,float scale,int color){
        p.setColor(color);
        canvas.drawOval(x-29*scale,y-11*scale,x+32*scale,y+9*scale,p);
        canvas.drawCircle(x-11*scale,y-11*scale,17*scale,p);
        canvas.drawCircle(x+11*scale,y-14*scale,22*scale,p);
    }
}
