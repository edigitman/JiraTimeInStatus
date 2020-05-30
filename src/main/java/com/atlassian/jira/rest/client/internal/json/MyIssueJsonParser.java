package com.atlassian.jira.rest.client.internal.json;

import com.atlassian.jira.rest.client.api.domain.*;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

import static com.atlassian.jira.rest.client.api.domain.IssueFieldId.*;
import static com.atlassian.jira.rest.client.api.domain.IssueFieldId.LABELS_FIELD;
import static com.atlassian.jira.rest.client.internal.json.JsonParseUtil.getStringKeys;
import static com.atlassian.jira.rest.client.internal.json.JsonParseUtil.parseOptionalJsonObject;

public class MyIssueJsonParser implements JsonObjectParser<Issue> {

    private static Set<String> SPECIAL_FIELDS = Sets.newHashSet(IssueFieldId.ids());

    public static final String SCHEMA_SECTION = "schema";
    public static final String NAMES_SECTION = "names";

    private final BasicIssueJsonParser basicIssueJsonParser = new BasicIssueJsonParser();
    private final IssueLinkJsonParserV5 issueLinkJsonParserV5 = new IssueLinkJsonParserV5();
    private final BasicVotesJsonParser votesJsonParser = new BasicVotesJsonParser();
    private final StatusJsonParser statusJsonParser = new StatusJsonParser();
    private final JsonObjectParser<BasicWatchers> watchersJsonParser = WatchersJsonParserBuilder.createBasicWatchersParser();
    private final VersionJsonParser versionJsonParser = new VersionJsonParser();
    private final BasicComponentJsonParser basicComponentJsonParser = new BasicComponentJsonParser();
    private final AttachmentJsonParser attachmentJsonParser = new AttachmentJsonParser();
    private final CommentJsonParser commentJsonParser = new CommentJsonParser();
    private final IssueTypeJsonParser issueTypeJsonParser = new IssueTypeJsonParser();
    private final BasicProjectJsonParser projectJsonParser = new BasicProjectJsonParser();
    private final BasicPriorityJsonParser priorityJsonParser = new BasicPriorityJsonParser();
    private final ResolutionJsonParser resolutionJsonParser = new ResolutionJsonParser();
    private final UserJsonParser userJsonParser = new UserJsonParser();
    private final SubtaskJsonParser subtaskJsonParser = new SubtaskJsonParser();
    private final ChangelogJsonParser changelogJsonParser = new ChangelogJsonParser();
    private final OperationsJsonParser operationsJsonParser = new OperationsJsonParser();
    private final JsonWeakParserForString jsonWeakParserForString = new JsonWeakParserForString();

    private static final String FIELDS = "fields";
    private static final String VALUE_ATTR = "value";

    private final JSONObject providedNames;
    private final JSONObject providedSchema;

    public MyIssueJsonParser() {
        providedNames = null;
        providedSchema = null;
    }

    public MyIssueJsonParser(final JSONObject providedNames, final JSONObject providedSchema) {
        this.providedNames = providedNames;
        this.providedSchema = providedSchema;
    }

    static Iterable<String> parseExpandos(final JSONObject json) throws JSONException {
        final String expando = json.getString("expand");
        return Splitter.on(',').split(expando);
    }


    private <T> Collection<T> parseArray(final JSONObject jsonObject, final JsonWeakParser<T> jsonParser, final String arrayAttribute)
            throws JSONException {
//        String type = jsonObject.getString("type");
//        final String name = jsonObject.getString("name");
        final JSONArray valueObject = jsonObject.optJSONArray(arrayAttribute);
        if (valueObject == null) {
            return new ArrayList<T>();
        }
        Collection<T> res = new ArrayList<T>(valueObject.length());
        for (int i = 0; i < valueObject.length(); i++) {
            res.add(jsonParser.parse(valueObject.get(i)));
        }
        return res;
    }

    private <T> Collection<T> parseOptionalArrayNotNullable(final JSONObject json, final JsonWeakParser<T> jsonParser, final String... path)
            throws JSONException {
        Collection<T> res = parseOptionalArray(json, jsonParser, path);
        return res == null ? Collections.<T>emptyList() : res;
    }

    @Nullable
    private <T> Collection<T> parseOptionalArray(final JSONObject json, final JsonWeakParser<T> jsonParser, final String... path)
            throws JSONException {
        final JSONArray jsonArray = JsonParseUtil.getNestedOptionalArray(json, path);
        if (jsonArray == null) {
            return null;
        }
        final Collection<T> res = new ArrayList<T>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            res.add(jsonParser.parse(jsonArray.get(i)));
        }
        return res;
    }

    private String getFieldStringValue(final JSONObject json, final String attributeName) throws JSONException {
        final JSONObject fieldsJson = json.getJSONObject(FIELDS);

        final Object summaryObject = fieldsJson.get(attributeName);
        if (summaryObject instanceof JSONObject) { // pre JIRA 5.0 way
            return ((JSONObject) summaryObject).getString(VALUE_ATTR);
        }
        if (summaryObject instanceof String) { // JIRA 5.0 way
            return (String) summaryObject;
        }
        throw new JSONException("Cannot parse [" + attributeName + "] from available fields");
    }

    private JSONObject getFieldUnisex(final JSONObject json, final String attributeName) throws JSONException {
        final JSONObject fieldsJson = json.getJSONObject(FIELDS);
        final JSONObject fieldJson = fieldsJson.getJSONObject(attributeName);
        if (fieldJson.has(VALUE_ATTR)) {
            return fieldJson.getJSONObject(VALUE_ATTR); // pre 5.0 way
        } else {
            return fieldJson; // JIRA 5.0 way
        }
    }

    @Nullable
    private String getOptionalFieldStringUnisex(final JSONObject json, final String attributeName)
            throws JSONException {
        final JSONObject fieldsJson = json.getJSONObject(FIELDS);
        return JsonParseUtil.getOptionalString(fieldsJson, attributeName);
    }

    private String getFieldStringUnisex(final JSONObject json, final String attributeName) throws JSONException {
        final JSONObject fieldsJson = json.getJSONObject(FIELDS);
        final Object fieldJson = fieldsJson.get(attributeName);
        if (fieldJson instanceof JSONObject) {
            return ((JSONObject) fieldJson).getString(VALUE_ATTR); // pre 5.0 way
        }
        return fieldJson.toString(); // JIRA 5.0 way
    }

    @Override
    public Issue parse(final JSONObject issueJson) throws JSONException {
        final BasicIssue basicIssue = basicIssueJsonParser.parse(issueJson);
        final Iterable<String> expandos = parseExpandos(issueJson);
        final JSONObject jsonFields = issueJson.getJSONObject(FIELDS);
        final JSONObject commentsJson = jsonFields.optJSONObject(COMMENT_FIELD.id);
        final Collection<Comment> comments = (commentsJson == null) ? Collections.<Comment>emptyList()
                : parseArray(commentsJson, new JsonWeakParserForJsonObject<Comment>(commentJsonParser), "comments");

        final String summary = getFieldStringValue(issueJson, SUMMARY_FIELD.id);
        final String description = getOptionalFieldStringUnisex(issueJson, DESCRIPTION_FIELD.id);

        final Collection<Attachment> attachments = new ArrayList<>();
        //parseOptionalArray(issueJson, new JsonWeakParserForJsonObject<Attachment>(attachmentJsonParser), FIELDS, ATTACHMENT_FIELD.id);
        final Collection<IssueField> fields = parseFields(issueJson);

        final IssueType issueType = issueTypeJsonParser.parse(getFieldUnisex(issueJson, ISSUE_TYPE_FIELD.id));
        final DateTime creationDate = JsonParseUtil.parseDateTime(getFieldStringUnisex(issueJson, CREATED_FIELD.id));
        final DateTime updateDate = JsonParseUtil.parseDateTime(getFieldStringUnisex(issueJson, UPDATED_FIELD.id));

        final String dueDateString = getOptionalFieldStringUnisex(issueJson, DUE_DATE_FIELD.id);
        final DateTime dueDate = dueDateString == null ? null : JsonParseUtil.parseDateTimeOrDate(dueDateString);

        final BasicPriority priority = getOptionalNestedField(issueJson, PRIORITY_FIELD.id, priorityJsonParser);
        final Resolution resolution = getOptionalNestedField(issueJson, RESOLUTION_FIELD.id, resolutionJsonParser);
        final User assignee = getOptionalNestedField(issueJson, ASSIGNEE_FIELD.id, userJsonParser);
        final User reporter = getOptionalNestedField(issueJson, REPORTER_FIELD.id, userJsonParser);

        final BasicProject project = projectJsonParser.parse(getFieldUnisex(issueJson, PROJECT_FIELD.id));
        final Collection<IssueLink> issueLinks;
        issueLinks = parseOptionalArray(issueJson, new JsonWeakParserForJsonObject<IssueLink>(issueLinkJsonParserV5), FIELDS, LINKS_FIELD.id);

        Collection<Subtask> subtasks = parseOptionalArray(issueJson, new JsonWeakParserForJsonObject<Subtask>(subtaskJsonParser), FIELDS, SUBTASKS_FIELD.id);

        final BasicVotes votes = getOptionalNestedField(issueJson, VOTES_FIELD.id, votesJsonParser);
        final Status status = statusJsonParser.parse(getFieldUnisex(issueJson, STATUS_FIELD.id));

        final Collection<Version> fixVersions = parseOptionalArray(issueJson, new JsonWeakParserForJsonObject<Version>(versionJsonParser), FIELDS, FIX_VERSIONS_FIELD.id);
        final Collection<Version> affectedVersions = parseOptionalArray(issueJson, new JsonWeakParserForJsonObject<Version>(versionJsonParser), FIELDS, AFFECTS_VERSIONS_FIELD.id);
        final Collection<BasicComponent> components = parseOptionalArray(issueJson, new JsonWeakParserForJsonObject<BasicComponent>(basicComponentJsonParser), FIELDS, COMPONENTS_FIELD.id);

        final Collection<Worklog> worklogs;
        final URI selfUri = basicIssue.getSelf();

        final String transitionsUriString;
        if (issueJson.has(IssueFieldId.TRANSITIONS_FIELD.id)) {
            Object transitionsObj = issueJson.get(IssueFieldId.TRANSITIONS_FIELD.id);
            transitionsUriString = (transitionsObj instanceof String) ? (String) transitionsObj : null;
        } else {
            transitionsUriString = getOptionalFieldStringUnisex(issueJson, IssueFieldId.TRANSITIONS_FIELD.id);
        }
        final URI transitionsUri = parseTransisionsUri(transitionsUriString, selfUri);

        if (JsonParseUtil.getNestedOptionalObject(issueJson, FIELDS, WORKLOG_FIELD.id) != null) {
            worklogs = parseOptionalArray(issueJson,
                    new JsonWeakParserForJsonObject<Worklog>(new WorklogJsonParserV5(selfUri)),
                    FIELDS, WORKLOG_FIELD.id, WORKLOGS_FIELD.id);
        } else {
            worklogs = Collections.emptyList();
        }


        final BasicWatchers watchers = getOptionalNestedField(issueJson, WATCHER_FIELD.id, watchersJsonParser);
        final TimeTracking timeTracking = getOptionalNestedField(issueJson, TIMETRACKING_FIELD.id, new TimeTrackingJsonParserV5());

        final Set<String> labels = Sets
                .newHashSet(parseOptionalArrayNotNullable(issueJson, jsonWeakParserForString, FIELDS, LABELS_FIELD.id));

        final Collection<ChangelogGroup> changelog = parseOptionalArray(
                issueJson, new JsonWeakParserForJsonObject<ChangelogGroup>(changelogJsonParser), "changelog", "histories");
        final Operations operations = parseOptionalJsonObject(issueJson, "operations", operationsJsonParser);

        return new Issue(summary, selfUri, basicIssue.getKey(), basicIssue.getId(), project, issueType, status,
                description, priority, resolution, attachments, reporter, assignee, creationDate, updateDate,
                dueDate, affectedVersions, fixVersions, components, timeTracking, fields, comments,
                transitionsUri, issueLinks,
                votes, worklogs, watchers, expandos, subtasks, changelog, operations, labels);
    }

    private URI parseTransisionsUri(final String transitionsUriString, final URI selfUri) {
        return transitionsUriString != null
                ? JsonParseUtil.parseURI(transitionsUriString)
                : UriBuilder.fromUri(selfUri).path("transitions").queryParam("expand", "transitions.fields").build();
    }

    @Nullable
    private <T> T getOptionalNestedField(final JSONObject s, final String fieldId, final JsonObjectParser<T> jsonParser)
            throws JSONException {
        final JSONObject fieldJson = JsonParseUtil.getNestedOptionalObject(s, FIELDS, fieldId);
        // for fields like assignee (when unassigned) value attribute may be missing completely
        if (fieldJson != null) {
            return jsonParser.parse(fieldJson);
        }
        return null;
    }

    private Collection<IssueField> parseFields(final JSONObject issueJson) throws JSONException {
        final JSONObject names = (providedNames != null) ? providedNames : issueJson.optJSONObject(NAMES_SECTION);
        final Map<String, String> namesMap = parseNames(names);
        final JSONObject schema = (providedSchema != null) ? providedSchema : issueJson.optJSONObject(SCHEMA_SECTION);
        final Map<String, String> typesMap = parseSchema(schema);

        final JSONObject json = issueJson.getJSONObject(FIELDS);
        final ArrayList<IssueField> res = new ArrayList<IssueField>(json.length());
        @SuppressWarnings("unchecked") final Iterator<String> iterator = json.keys();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            try {
                if (SPECIAL_FIELDS.contains(key)) {
                    continue;
                }
                // TODO: JRJC-122
                // we should use fieldParser here (some new version as the old one probably won't work)
                // enable IssueJsonParserTest#testParseIssueWithUserPickerCustomFieldFilledOut after fixing this
                final Object value = json.opt(key);
                res.add(new IssueField(key, namesMap.get(key), typesMap.get("key"), value != JSONObject.NULL ? value : null));
            } catch (final Exception e) {
                throw new JSONException("Error while parsing [" + key + "] field: " + e.getMessage()) {
                    @Override
                    public Throwable getCause() {
                        return e;
                    }
                };
            }
        }
        return res;
    }

    private Map<String, String> parseSchema(final JSONObject json) throws JSONException {
        final HashMap<String, String> res = Maps.newHashMap();
        final Iterator<String> it = JsonParseUtil.getStringKeys(json);
        while (it.hasNext()) {
            final String fieldId = it.next();
            JSONObject fieldDefinition = json.getJSONObject(fieldId);
            res.put(fieldId, fieldDefinition.getString("type"));

        }
        return res;
    }

    private Map<String, String> parseNames(final JSONObject json) throws JSONException {
        final HashMap<String, String> res = Maps.newHashMap();
        final Iterator<String> iterator = getStringKeys(json);
        while (iterator.hasNext()) {
            final String key = iterator.next();
            res.put(key, json.getString(key));
        }
        return res;
    }
}