package com.scopewise.lunarthursday;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
public final class SunsetMathTest {
  private static int checks;
  private static final ZoneId PKT=ZoneId.of("Asia/Karachi");
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
  static void near(LocalDate day,double lat,double lon,int hour,int minute){
    LocalTime got=SunsetMath.sunsetPakistan(day,lat,lon);
    check(got!=null&&Math.abs(got.toSecondOfDay()/60-(hour*60+minute))<=12,"Sunset "+day+" was "+got);
  }
  public static void main(String[] args) {
    LocalDate date=LocalDate.of(2026,11,13);
    near(date,31.5204,74.3587,17,4);
    near(date,24.8607,67.0011,17,46);
    near(date,33.6844,73.0479,17,3);
    near(LocalDate.of(2026,6,21),24.8607,67.0011,19,23);
    check(SunsetMath.sunsetPakistan(date,91,75)==null,"Bad latitude");
    check(SunsetMath.sunsetPakistan(date,30,Double.NaN)==null,"Bad longitude");
    LocalTime sun=SunsetMath.sunsetPakistan(date,31.5204,74.3587);
    Instant justBefore=date.atTime(sun.minusMinutes(1)).atZone(PKT).toInstant();
    Instant at=date.atTime(sun).atZone(PKT).toInstant();
    check(SunsetMath.hijriCivilDate(justBefore,sun).equals(date),"Before Maghrib stays date");
    check(SunsetMath.hijriCivilDate(at,sun).equals(date.plusDays(1)),"At sunset advances");
    check(SunsetMath.hijriCivilDate(at,null).equals(date),"No city: no invented precision");
    Instant midnight=Instant.parse("2026-11-12T19:00:00Z");
    check(SunsetMath.hijriCivilDate(midnight,sun).equals(date),"Midnight is not second Hijri transition");
    System.out.println("SunsetMathTest PASS: "+checks+" checks");
  }
}