package com.scopewise.lunarthursday;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Source refresh windows near possible last days; never proof of moon sighting. */
final class MonthEndChecks {
    static final ZoneId PKT = ZoneId.of("Asia/Karachi");
    // No location? A late-evening fallback, NOT a claimed Maghrib prayer time.
    static final LocalTime UNKNOWN_LOCATION_EVENING = LocalTime.of(20,45);
    static final LocalTime AFTER_MIDNIGHT = LocalTime.of(0,15);
    static final LocalTime MORNING = LocalTime.of(7,0);
    static final long HORIZON_DAYS = 45;

    static final class Slot {
        final int slot;
        final Instant when;
        final String description;
        Slot(int slot, Instant when, String description) {
            this.slot=slot; this.when=when; this.description=description;
        }
    }
    private MonthEndChecks() { }

    /** Check at Maghrib+10m (or fallback), 00:15, 07:00 on both potential
     * final days, then 75m before the next selected first-Thursday reminder. */
    static List<Slot> plan(Instant now, LocalDate firstCivilDay, LocalDate nextMonthStart,
                           Function<LocalDate,LocalTime> localSunset, LocalTime reminderTime) {
        if (now==null || firstCivilDay==null || nextMonthStart==null ||
                localSunset==null || reminderTime==null)
            throw new IllegalArgumentException("Missing month-end planning input");
        List<Slot> result=new ArrayList<>();
        Instant until=now.plusSeconds(HORIZON_DAYS*86400L);
        for(int possibleEnd=0; possibleEnd<2; possibleEnd++){
            LocalDate day=firstCivilDay.plusDays(28L+possibleEnd);
            LocalTime sunset=localSunset.apply(day);
            LocalTime evening=sunset==null ? UNKNOWN_LOCATION_EVENING : sunset.plusMinutes(10);
            add(result,possibleEnd*3,day,evening,now,until,"month-end evening");
            add(result,possibleEnd*3+1,day.plusDays(1),AFTER_MIDNIGHT,
                    now,until,"after-midnight feed follow-up");
            add(result,possibleEnd*3+2,day.plusDays(1),MORNING,
                    now,until,"morning feed follow-up");
        }
        if(!nextMonthStart.isBefore(firstCivilDay.plusDays(28))
                && !nextMonthStart.isAfter(firstCivilDay.plusDays(31))){
            LocalDate reminderDay=LunarEngine.firstThursday(nextMonthStart);
            Instant before=reminderDay.atTime(reminderTime).atZone(PKT)
                    .toInstant().minusSeconds(75*60L);
            if(before.isAfter(now)&&!before.isAfter(until))
                result.add(new Slot(6,before,"before selected reminder"));
        }
        return result;
    }

    private static void add(List<Slot> dst,int id,LocalDate date,LocalTime time,
                            Instant now,Instant horizon,String purpose){
        Instant at=date.atTime(time).atZone(PKT).toInstant();
        if(at.isAfter(now)&&!at.isAfter(horizon))dst.add(new Slot(id,at,purpose));
    }
}
