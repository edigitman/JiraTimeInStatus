package ro.agitman.jira;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeDiffTest {

    private SimpleDateFormat sdf;

    @Before
    public void beforeEach(){
        sdf = new SimpleDateFormat("dd MM yyyy HH:mm");
    }

    @Test
    public void resetUpperTime() {
        //2020-05-25T20:23:52.850
        Calendar c = Calendar.getInstance();
        c.set(2020,Calendar.MAY,25, 20, 31);
        Date d = c.getTime();

        Assert.assertEquals("26 05 2020 09:00", sdf.format(new DateTimeMath().getNextWorking(d)));
    }

    @Test
    public void resetUpperTimeOnFriday() {
        //2020-05-25T20:23:52.850
        Calendar c = Calendar.getInstance();
        c.set(2020,Calendar.MAY,22, 20, 31);
        Date d = c.getTime();

        Assert.assertEquals("25 05 2020 09:00", sdf.format(new DateTimeMath().getNextWorking(d)));
    }

    @Test
    public void resetUnderTime() {
        //2020-05-25T20:23:52.850
        Calendar c = Calendar.getInstance();
        c.set(2020,Calendar.MAY,22, 5, 31);
        Date d = c.getTime();

        Assert.assertEquals("22 05 2020 09:00", sdf.format(new DateTimeMath().getNextWorking(d)));
    }


    @Test
    public void hDiffTest() {
        //2020-05-25T20:23
        Calendar cf = Calendar.getInstance();
        cf.set(2020,Calendar.MAY,21, 14, 23);
        //2020-05-26T10:36
        Calendar ct = Calendar.getInstance();
        ct.set(2020,Calendar.MAY,26, 10, 36);

        DateTimeMath dtm = new DateTimeMath();

        System.out.println(dtm.countHours(cf.getTime(), (ct.getTime())));
    }

}
