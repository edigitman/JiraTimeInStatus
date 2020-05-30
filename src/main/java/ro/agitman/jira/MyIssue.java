package ro.agitman.jira;

import com.atlassian.jira.rest.client.api.domain.*;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;

import java.util.*;

@Getter
@Setter
public class MyIssue {

    private String key;
    private String summary;
    private String component;
    private String type;
    private Integer est = 0;
    private Integer rem = 0;
    private Integer spt = 0;
    private Map<String, Integer> tis = new HashMap<>();

    public MyIssue(Issue issue) {
        this.key = issue.getKey();
        this.summary = issue.getSummary();
        this.type = issue.getIssueType().getName();
        if (issue.getTimeTracking().getOriginalEstimateMinutes() != null)
            this.est = issue.getTimeTracking().getOriginalEstimateMinutes();
        if (issue.getTimeTracking().getRemainingEstimateMinutes() != null)
            this.rem = issue.getTimeTracking().getRemainingEstimateMinutes();
        if (issue.getTimeTracking().getTimeSpentMinutes() != null)
            this.spt = issue.getTimeTracking().getTimeSpentMinutes();
        if (issue.getComponents() != null) {
            StringBuilder sb = new StringBuilder();
            for (BasicComponent c : issue.getComponents()) {
                sb.append(c.getName()).append(",");
            }
            if (sb.toString().contains(",")) {
                this.component = sb.deleteCharAt(sb.lastIndexOf(",")).toString();
            } else {
                this.component = sb.toString();
            }
        }

        computeTimeInState(issue);
    }

    private void computeTimeInState(Issue issue) {
        DateTime startInStatus = null;
        String statusStart = null;

        Iterator<ChangelogGroup> it = issue.getChangelog().iterator();
        while (it.hasNext()) {
            ChangelogGroup cg = it.next();
            Iterator<ChangelogItem> i = cg.getItems().iterator();
            while (i.hasNext()) {
                ChangelogItem ci = i.next();
                if ("status".equals(ci.getField())) {

                    if (statusStart == null) {
                        statusStart = ci.getToString();
                        startInStatus = cg.getCreated();
                    } else {
                        if (!statusStart.equals(ci.getFromString())) {
                            throw new IllegalStateException("State not matching: " + statusStart + " vs. " + ci.getFromString());
                        }

                        int hours = new DateTimeMath().countHours(startInStatus.toDate(), cg.getCreated().toDate());

                        if (!tis.containsKey(statusStart)) {
                            tis.put(statusStart, 0);
                        }
                        tis.put(statusStart, tis.get(statusStart) + hours);

                        statusStart = ci.getToString();
                        startInStatus = cg.getCreated();
                    }
                }
            }
        }
    }
}
