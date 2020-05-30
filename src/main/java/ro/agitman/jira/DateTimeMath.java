package ro.agitman.jira;

import java.util.Calendar;
import java.util.Date;

public class DateTimeMath {

    final private int openHour = 9;
    final private int closeHour = 18;

    public int countHours(Date from, Date to) {
        from = getNextWorking(from);
        to = getNextWorking(to);
        int h = 0;
        Calendar cf = Calendar.getInstance();
        cf.setTime(from);
        Calendar ct = Calendar.getInstance();
        ct.setTime(to);

        while (cf.before(ct)) {

            if (cf.get(Calendar.HOUR_OF_DAY) >= closeHour) {
                cf.setTime(getNextWorking(cf.getTime()));
            } else {
                cf.add(Calendar.HOUR_OF_DAY, 1);
                h++;
            }
        }
        return h;
    }


    Date getNextWorking(Date date) {
        Date ownD = (Date) date.clone();
        Calendar c = Calendar.getInstance();
        c.setTime(ownD);

        if (c.get(Calendar.HOUR_OF_DAY) >= closeHour) {
            c.add(Calendar.DATE, 1);
            c.set(Calendar.HOUR_OF_DAY, openHour);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
        }
        if (c.get(Calendar.HOUR_OF_DAY) < openHour) {
            c.set(Calendar.HOUR_OF_DAY, openHour);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
        }

        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY)
            c.add(Calendar.DAY_OF_MONTH, 2);
        if (dow == Calendar.SUNDAY)
            c.add(Calendar.DAY_OF_MONTH, 1);

        return c.getTime();
    }
}
