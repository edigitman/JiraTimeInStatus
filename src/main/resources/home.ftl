<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>JCRT</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bulma@0.8.2/css/bulma.min.css">
    <script defer src="https://use.fontawesome.com/releases/v5.3.1/js/all.js"></script>
</head>
<body>
<section class="section">
    <div class="container">
        <div style="text-align: center">
            <h3 class="title">Jira Custom Report Tool</h3>
        </div>

        <div class="columns is-centered">

            <div class="column is-half" <#if JiraUser??>style="display: none"</#if> >
                <form class="form" action="/test" method="post">
                    <label class="label">Jira Url:</label>
                    <input class="input" type="text" name="url" required placeholder="https://jira.flowbird.group"><br/>
                    <label class="label">Jira Login:</label>
                    <input class="input" type="text" name="user" required><br/>
                    <label class="label">Jira Password:</label>
                    <input class="input" type="password" name="password" required><br/>

                    <input class="button" style="margin-top: 10px" type="submit" value="Login"
                        onclick="document.getElementById('loginNotification').style.display=''">
                </form>
                <span id="loginNotification" style="display: none; color: cadetblue">Trying to login ...</span>
                <#if error??>
                    <span style="color: firebrick">Error while authenticating : ${error}</span>
                </#if>
            </div>

            <#if JiraUser??>
            <div class="column is-half">
                <span>You are authenticated as <b>${JiraUser}</b>. (<a href="logout">logout</a>)</span>

                <form class="form" action="/query" method="post">
                    <label class="label">Scan for Epic Id:</label>
                    <input class="input" type="text" name="epicId" required><br/>

                    <input class="button" style="margin-top: 10px" type="submit" value="Scan"
                           onclick="document.getElementById('scanInProgressNotification').style.display='';
                                    document.getElementById('epicErrorId').style.display='none'">
                </form>
                <span id="scanInProgressNotification" style="display: none; color: cadetblue">Scanning in progress ...</span>
                <#if error??>
                    <span id="epicErrorId" style="color: firebrick">${error}</span>
                </#if>
                <#if epic??>
                <h4>Results for Epic <b>${epic}</b></h4>

                <table class="table is-bordered is-hoverable">
                    <thead>
                    <tr>
                        <th>Issues Type</th>
                        <th>Count</th>
                        <th>Estimated</th>
                        <th>Remaining</th>
                        <th>Spend</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>Task</td>
                        <td>${task.count}</td>
                        <td>${task.est / 60}</td>
                        <td>${task.rem / 60}</td>
                        <td>${task.spt / 60}</td>
                    </tr>
                    <tr>
                        <td>Bug</td>
                        <td>${bug.count}</td>
                        <td>${bug.est / 60}</td>
                        <td>${bug.rem / 60}</td>
                        <td>${bug.spt / 60}</td>
                    </tr>
                    <tr>
                        <td>Analysis</td>
                        <td>${analysis.count}</td>
                        <td>${analysis.est / 60}</td>
                        <td>${analysis.rem / 60}</td>
                        <td>${analysis.spt / 60}</td>
                    </tr>
                    <tr>
                        <td>Story</td>
                        <td>${story.count}</td>
                        <td>${story.est / 60}</td>
                        <td>${story.rem / 60}</td>
                        <td>${story.spt / 60}</td>
                    </tr>
                    <tr>
                        <td>Delivery</td>
                        <td>${delivery.count}</td>
                        <td>${delivery.est / 60}</td>
                        <td>${delivery.rem / 60}</td>
                        <td>${delivery.spt / 60}</td>
                    </tr>
                    </tbody>
                </table>
            </div> <!-- close column -->
        </div> <!-- close columns -->
        <div class="columns is-centered" style="font-size: 11px">
            <div class="column">
                <table class="table is-bordered is-hoverable is-fullwidth">
                    <thead>
                    <tr>
                        <th>Issue #</th>
                        <th>Component</th>
                        <th>Type</th>
                        <th>Summary</th>
                        <th>Estimated</th>
                        <th>Remaining</th>
                        <th>Spend</th>
                        <th>Spend / Remaining %</th>
                        <th>Analysis in Progress</th>
                        <th>Waiting Development</th>
                        <th>Development In Progress</th>
                        <th>Waiting Code Review</th>
                        <th>In Review</th>
                        <th>Waiting Test</th>
                    </tr>
                    </thead>
                    <tbody>

                    <#list issueList as is>
                        <tr>
                            <td>${is.key}</td>
                            <td>${is.component}</td>
                            <td>${is.type}</td>
                            <td>${is.summary}</td>
                            <td>${is.est / 60}</td>
                            <td>${is.rem / 60}</td>
                            <td>${is.spt / 60}</td>
                            <td><#if is.spt gt 0 >${(is.spt/(is.spt+is.rem))*100}<#else>0</#if> %</td>
                            <td>
                                <#if is.tis??>
                                    <#list is.tis?keys as key>
                                        <#if key == 'Analysis In Progress'>
                                            ${is.tis[key]}
                                        </#if>
                                    </#list>
                                </#if>
                            </td>
                            <td>
                                <#if is.tis??>
                                    <#list is.tis?keys as key>
                                        <#if key == 'Waiting Development'>
                                            ${is.tis[key]}
                                        </#if>
                                    </#list>
                                </#if>
                            </td>
                            <td>
                                <#if is.tis??>
                                    <#list is.tis?keys as key>
                                        <#if key == 'Development In Progress'>
                                            ${is.tis[key]}
                                        </#if>
                                    </#list>
                                </#if>
                            </td>
                            <td>
                                <#if is.tis??>
                                    <#list is.tis?keys as key>
                                        <#if key == 'WAITING CODE REVIEW'>
                                            ${is.tis[key]}
                                        </#if>
                                    </#list>
                                </#if>
                            </td>
                            <td>
                                <#if is.tis??>
                                    <#list is.tis?keys as key>
                                        <#if key == 'In Review'>
                                            ${is.tis[key]}
                                        </#if>
                                    </#list>
                                </#if>
                            </td>
                            <td>
                                <#if is.tis??>
                                    <#list is.tis?keys as key>
                                        <#if key == 'Waiting Test'>
                                            ${is.tis[key]}
                                        </#if>
                                    </#list>
                                </#if>
                            </td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
                </#if>
            </div>
        </div>
        </#if>
    </div>
    </div>
</section>
</body>
</html>
