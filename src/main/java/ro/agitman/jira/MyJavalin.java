package ro.agitman.jira;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousHttpClientFactory;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import com.atlassian.jira.rest.client.internal.async.MyAsynchronousJiraRestClient;
import freemarker.template.Configuration;
import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinFreemarker;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static io.javalin.plugin.rendering.template.TemplateUtil.model;

public class MyJavalin {

    public static void main(String[] args) {

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassForTemplateLoading(MyJavalin.class, "/");

        JavalinFreemarker.configure(cfg);

        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/public");
        }).start(7777);

        app.get("/", ctx -> ctx.render("/home.ftl"));

        app.get("/logout", ctx -> {
            ctx.req.getSession().invalidate();
            ctx.render("/home.ftl");
        });

        app.post("/test", ctx -> {
            String url = ctx.formParam("url");
            String user = ctx.formParam("user");
            String pwd = ctx.formParam("password");

            ctx.sessionAttribute("url", url);
            ctx.sessionAttribute("user", user);
            ctx.sessionAttribute("password", pwd);

            DisposableHttpClient httpClient = new AsynchronousHttpClientFactory()
                    .createClient(URI.create(url), new BasicHttpAuthenticationHandler(user, pwd));
            JiraRestClient cl = new MyAsynchronousJiraRestClient(URI.create(url), httpClient);
            try {
                String name = cl.getUserClient().getUser(user).claim().getName();
                ctx.render("/home.ftl", model("JiraUser", name));
            } catch (RestClientException | IllegalArgumentException e) {
                ctx.render("/home.ftl", model("error", e.getMessage()));
            }

        });

        app.post("/query", ctx -> {
            String url = ctx.sessionAttribute("url");
            String user = ctx.sessionAttribute("user");
            String pwd = ctx.sessionAttribute("password");
            String epicId = ctx.formParam("epicId");
            JiraRestClient cl;
            Issue epic;

            try {
                DisposableHttpClient httpClient = new AsynchronousHttpClientFactory()
                        .createClient(URI.create(url), new BasicHttpAuthenticationHandler(user, pwd));
                cl = new MyAsynchronousJiraRestClient(URI.create(url), httpClient);
            } catch (RestClientException e) {
                ctx.render("/home.ftl", model("error", e.getMessage()));
                return;
            }

            try {
                epic = cl.getIssueClient().getIssue(epicId).claim();
            } catch (RestClientException e) {
                ctx.render("/home.ftl", model("error", e.getMessage(),
                        "JiraUser", cl.getUserClient().getUser(user).claim().getName()));
                return;
            }

            if (!"Epic".equals(epic.getIssueType().getName())) {
                ctx.render("/home.ftl", model("error", "Provided Id is not an Epic",
                        "JiraUser", cl.getUserClient().getUser(user).claim().getName()));
                return;
            }

            SearchResult rs = cl.getSearchClient().searchJql("\"Epic Link\"=" + epicId).claim();

            List<MyIssue> issueList = new ArrayList<>(rs.getTotal());
            TimeTrackSum bug = new TimeTrackSum();
            TimeTrackSum task = new TimeTrackSum();
            TimeTrackSum analysis = new TimeTrackSum();
            TimeTrackSum story = new TimeTrackSum();
            TimeTrackSum delivery = new TimeTrackSum();

            for (Issue is : rs.getIssues()) {
                Issue issue = cl.getIssueClient().getIssue(is.getKey(), EnumSet.of(IssueRestClient.Expandos.CHANGELOG)).claim();
                issueList.add(new MyIssue(issue));

                switch (is.getIssueType().getName()) {
                    case "Task":
                        task.add(issue.getTimeTracking().getOriginalEstimateMinutes(),
                                issue.getTimeTracking().getRemainingEstimateMinutes(),
                                issue.getTimeTracking().getTimeSpentMinutes());
                        break;
                    case "Bug":
                        bug.add(issue.getTimeTracking().getOriginalEstimateMinutes(),
                                issue.getTimeTracking().getRemainingEstimateMinutes(),
                                issue.getTimeTracking().getTimeSpentMinutes());
                        break;
                    case "Analysis":
                        analysis.add(issue.getTimeTracking().getOriginalEstimateMinutes(),
                                issue.getTimeTracking().getRemainingEstimateMinutes(),
                                issue.getTimeTracking().getTimeSpentMinutes());
                        break;
                    case "Story":
                        story.add(issue.getTimeTracking().getOriginalEstimateMinutes(),
                                issue.getTimeTracking().getRemainingEstimateMinutes(),
                                issue.getTimeTracking().getTimeSpentMinutes());
                        break;
                    case "Delivery":
                        delivery.add(issue.getTimeTracking().getOriginalEstimateMinutes(),
                                issue.getTimeTracking().getRemainingEstimateMinutes(),
                                issue.getTimeTracking().getTimeSpentMinutes());
                        break;
                }
            }

            ctx.render("/home.ftl",
                    model("JiraUser", cl.getUserClient().getUser(user).claim().getName(),
                            "epic", epicId,
                            "task", task, "bug", bug, "analysis", analysis, "story", story, "delivery", delivery,
                            "issueList", issueList)
            );
        });
    }

}


