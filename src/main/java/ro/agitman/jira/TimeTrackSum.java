package ro.agitman.jira;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeTrackSum {

    private Integer est = 0;
    private Integer rem = 0;
    private Integer spt = 0;
    private int count = 0;

    public void add(Integer est, Integer rem, Integer spt) {
        count++;
        if (est != null)
            this.est = this.est + est;
        if (rem != null)
            this.rem = this.rem + rem;
        if (spt != null)
            this.spt = this.spt + spt;
    }
}
