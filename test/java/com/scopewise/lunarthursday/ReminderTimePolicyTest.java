package com.scopewise.lunarthursday;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.Collections;
public final class ReminderTimePolicyTest {
  static int checks;
  static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
  public static void main(String[]args){
    ok(ReminderTimePolicy.fromSaved(11,0).equals(LocalTime.of(11,0)),"Legacy 11 AM default");
    ok(ReminderTimePolicy.fromSaved(0,0).equals(LocalTime.MIDNIGHT),"Midnight supported");
    ok(ReminderTimePolicy.fromSaved(23,59).equals(LocalTime.of(23,59)),"End of day supported");
    ok(ReminderTimePolicy.fromSaved(25,80).equals(ReminderTimePolicy.DEFAULT),"Invalid saved prefs fallback");
    for(int[] x:new int[][]{{24,0},{-1,0},{12,60},{12,-1}}){
      try{ReminderTimePolicy.validate(x[0],x[1]);throw new AssertionError("Accepted invalid time");}
      catch(IllegalArgumentException expected){checks++;}
    }
    LunarEngine.Month current=LunarEngine.month(1448,6,Collections.emptyMap());
    ok(current.remindAt(LocalTime.of(6,30)).equals(Instant.parse("2026-11-12T01:30:00Z")),"06:30 PKT");
    ok(current.remindAt(LocalTime.of(22,45)).equals(Instant.parse("2026-11-12T17:45:00Z")),"22:45 PKT");
    ok(current.remindAt().equals(Instant.parse("2026-11-12T06:00:00Z")),"Original default");
    Instant now=Instant.parse("2026-11-12T10:00:00Z");
    ok(LunarEngine.upcoming(now,Collections.emptyMap(),1,LocalTime.of(6,30))
      .get(0).hijriMonth==7,"Earlier time already passed");
    ok(LunarEngine.upcoming(now,Collections.emptyMap(),1,LocalTime.of(22,45))
      .get(0).hijriMonth==6,"Later time still future");
    ok(LunarEngine.upcoming(now,Collections.emptyMap(),1,LocalTime.of(6,30))
      .get(0).thursday().equals(LocalDate.of(2026,12,17)),"Day rule unchanged");
    System.out.println("ReminderTimePolicyTest PASS: "+checks+" checks");
  }
}