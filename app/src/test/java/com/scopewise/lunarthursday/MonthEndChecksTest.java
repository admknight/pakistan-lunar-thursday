package com.scopewise.lunarthursday;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
public final class MonthEndChecksTest {
  static int checks;
  static void ok(boolean v,String msg){if(!v)throw new AssertionError(msg);checks++;}
  public static void main(String[]args){
    LocalDate start=LocalDate.parse("2026-11-12");
    List<MonthEndChecks.Slot> slots=MonthEndChecks.plan(
        Instant.parse("2026-11-15T10:00:00Z"),start,LocalDate.parse("2026-12-11"),
        date->SunsetMath.sunsetPakistan(date,31.5204,74.3587),LocalTime.of(11,0));
    ok(slots.size()==7,"6 month end plus 1 pre-reminder");
    ok(slots.get(0).slot==0,"29th evening first");
    ok(slots.get(0).when.atZone(MonthEndChecks.PKT).toLocalDate()
      .equals(LocalDate.parse("2026-12-10")),"Day 29 Dec 10");
    ok(slots.get(1).when.atZone(MonthEndChecks.PKT).toLocalTime()
      .equals(LocalTime.of(0,15)),"Past-midnight check 00:15");
    ok(slots.get(2).when.atZone(MonthEndChecks.PKT).toLocalTime()
      .equals(LocalTime.of(7,0)),"Morning 07:00");
    ok(slots.get(3).when.atZone(MonthEndChecks.PKT).toLocalDate()
      .equals(LocalDate.parse("2026-12-11")),"Day30 evening");
    ok(slots.stream().anyMatch(x->x.slot==6
      && x.when.equals(Instant.parse("2026-12-17T04:45:00Z"))),"Before 11am reminder");
    List<MonthEndChecks.Slot> later=MonthEndChecks.plan(
      Instant.parse("2026-12-11T03:00:00Z"),start,LocalDate.parse("2026-12-11"),
      date->null,LocalTime.of(2,45));
    ok(later.stream().noneMatch(x->x.slot<2),"Never requeue past checks");
    ok(later.stream().anyMatch(x->x.slot==3
      && x.when.atZone(MonthEndChecks.PKT).toLocalTime()
         .equals(LocalTime.of(20,45))),"No city late evening fallback");
    ok(MonthEndChecks.plan(Instant.parse("2027-03-01T00:00:00Z"),start,
      LocalDate.parse("2026-12-11"),date->null,LocalTime.of(11,0)).isEmpty(),
      "No old month requeues");
    List<MonthEndChecks.Slot> early=MonthEndChecks.plan(
      Instant.parse("2026-10-20T00:00:00Z"),LocalDate.parse("2026-10-14"),
      LocalDate.parse("2026-11-12"),
      date->SunsetMath.sunsetPakistan(date,31.5204,74.3587),LocalTime.of(4,0));
    ok(early.stream().anyMatch(x->x.slot==6),"Early user reminder guarded");
    System.out.println("MonthEndChecksTest PASS: "+checks+" checks");
  }
}