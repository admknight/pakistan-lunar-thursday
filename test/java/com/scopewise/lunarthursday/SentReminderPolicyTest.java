package com.scopewise.lunarthursday;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
public final class SentReminderPolicyTest {
  static int checks;
  static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
  public static void main(String[]args){
    LocalTime eleven=LocalTime.of(11,0);
    // Forecast: Jumada al-Thani 1448 starts Thu 12 Nov 2026, reminder that day.
    LunarEngine.Month forecast=LunarEngine.month(1448,6,Collections.emptyMap());
    long first=forecast.remindAt(eleven).toEpochMilli();
    String sent=SentReminderPolicy.token(forecast.key(),first);
    ok(sent.equals("1448-6@2026-11-12"),"Marker holds month and Pakistan date");
    ok(SentReminderPolicy.alreadySent(sent,"1448-6",first),"Same trigger is a duplicate");

    // Reminder time changed later the same Thursday: still the same day, no repeat.
    long sameDayLater=forecast.remindAt(LocalTime.of(15,0)).toEpochMilli();
    ok(SentReminderPolicy.alreadySent(sent,"1448-6",sameDayLater),"Same Thursday, new time is a duplicate");
    long sameDayLate=forecast.remindAt(LocalTime.of(23,59)).toEpochMilli();
    ok(SentReminderPolicy.alreadySent(sent,"1448-6",sameDayLate),"23:59 PKT is still the same Pakistan date");
    long sameDayEarly=forecast.remindAt(LocalTime.MIDNIGHT).toEpochMilli();
    ok(SentReminderPolicy.alreadySent(sent,"1448-6",sameDayEarly),"00:00 PKT is still the same Pakistan date");

    // Late correction: month actually starts Fri 13 Nov, so first Thursday is 19 Nov.
    Map<String,LocalDate> corrected=new HashMap<>();
    corrected.put("1448-6",LocalDate.of(2026,11,13));
    Instant afterFirst=Instant.parse("2026-11-12T15:00:00Z");
    LunarEngine.Month next=LunarEngine.upcoming(afterFirst,corrected,1,eleven).get(0);
    ok(next.key().equals("1448-6"),"Engine reschedules the same month");
    ok(next.thursday().equals(LocalDate.of(2026,11,19)),"Corrected first Thursday");
    long second=next.remindAt(eleven).toEpochMilli();
    ok(!SentReminderPolicy.alreadySent(sent,next.key(),second),"Corrected Thursday must notify");
    String resent=SentReminderPolicy.token(next.key(),second);
    ok(resent.equals("1448-6@2026-11-19"),"Marker moves to corrected date");
    ok(SentReminderPolicy.alreadySent(resent,"1448-6",second),"Corrected Thursday notifies once");
    ok(!SentReminderPolicy.alreadySent(resent,"1448-6",first),"Old date no longer marked");

    // Other months are never blocked.
    LunarEngine.Month rajab=LunarEngine.month(1448,7,Collections.emptyMap());
    ok(!SentReminderPolicy.alreadySent(sent,rajab.key(),rajab.remindAt(eleven).toEpochMilli()),"Next month notifies");
    ok(!SentReminderPolicy.alreadySent("1448-6@2026-11-12","1448-60",first),"No prefix confusion between month keys");

    // Markers written by v1.9.1 and earlier (month key only) keep month-wide meaning.
    ok(SentReminderPolicy.alreadySent("1448-6","1448-6",first),"Legacy marker blocks its month");
    ok(SentReminderPolicy.alreadySent("1448-6","1448-6",second),"Legacy marker has no date to compare");
    ok(!SentReminderPolicy.alreadySent("1448-6","1448-7",second),"Legacy marker leaves other months alone");

    // Nothing stored or bad input: never treated as sent.
    ok(!SentReminderPolicy.alreadySent("","1448-6",first),"Empty marker");
    ok(!SentReminderPolicy.alreadySent(null,"1448-6",first),"Null marker");
    ok(!SentReminderPolicy.alreadySent(sent,null,first),"Null month key");
    ok(!SentReminderPolicy.alreadySent(sent,"1448-6",-1),"Invalid trigger");
    for(Object[] bad:new Object[][]{{null,first},{"",first},{"1448-6",0L}}){
      try{SentReminderPolicy.token((String)bad[0],(Long)bad[1]);throw new AssertionError("Accepted invalid marker input");}
      catch(IllegalArgumentException expected){checks++;}
    }
    System.out.println("SentReminderPolicyTest PASS: "+checks+" checks");
  }
}
