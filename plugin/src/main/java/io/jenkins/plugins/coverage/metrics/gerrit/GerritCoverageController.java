package io.jenkins.plugins.coverage.metrics.gerrit;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.json.JsonHttpResponse;
import org.kohsuke.stapler.verb.GET;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.UnprotectedRootAction;

import io.jenkins.plugins.coverage.metrics.restapi.ModifiedLinesCoverageApi;
import io.jenkins.plugins.coverage.metrics.steps.CoverageBuildAction;
import io.jenkins.plugins.util.JenkinsFacade;

@Extension
public class GerritCoverageController implements UnprotectedRootAction {

    private static final String CONSTANT_URL = "coverage";
    private static final String CHANGE_ID = "GERRIT_CHANGE_NUMBER";
    private static final String PATCH_ID = "GERRIT_PATCHSET_NUMBER";

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getUrlName() {
        return CONSTANT_URL;
    }

    private Optional<Job<?, ?>> getJob(final String jobName) {
        JenkinsFacade f = new JenkinsFacade();
        return f.getJob(jobName);
        //return Jenkins.get().getItemByFullName(jobName, Job.class);
    }

    /**
     * Access point for getting all coverage data of lines added in a specific change_id and patch-set from Gerrit.
     * Calling it:
     * <p>{root_url}/coverage/get?job={jobName}&amp;change={id}&amp;patchset={p_id}</p>
     */
    @GET
    @WebMethod(name = "get")
    public JsonHttpResponse getModifiedLinessPerChangePatch(@QueryParameter(required = true) final String job,
            @QueryParameter(required = true) final String change,
            @QueryParameter(required = true) final String patchset)
            throws JsonProcessingException {

        JSONObject response = new JSONObject();

        if (getJob(job).isPresent()) {
            Run<?, ?> run = getLastMatchingBuild(getJob(job).get(), change, patchset);

            var action = run.getAction(CoverageBuildAction.class);
            var api = new ModifiedLinesCoverageApi(action.getResult());
            var objectMapper = new ObjectMapper();
            var json = objectMapper.writeValueAsString(api.getFilesWithModifiedLines());

            response.put("coverage", json);
            return new JsonHttpResponse(response, 200);
        }
        else {
            return new JsonHttpResponse(response, 404);
        }
    }

    /**
     * Finds the last build that matches the changeID and patchset passed in the arguments.
     *
     * @return build that matches changeId and patchset, or null if not found!
     */
    private Run<?, ?> getLastMatchingBuild(final Job<?, ?> job, final String changeId, final String patchset) {
        for (Run<?, ?> currentRun : job.getBuilds()) {
            ParametersAction parameters = currentRun.getAction(ParametersAction.class);

            // If parameters are even present
            if (parameters != null) {

                // Check matching change id
                if (currentRun.getAction(ParametersAction.class).getParameters().stream()
                        .anyMatch(value -> value.getName().equals(CHANGE_ID) && Objects.equals(value.getValue(),
                                changeId))) {
                    // Check matching patchset
                    if (currentRun.getAction(ParametersAction.class).getParameters().stream()
                            .anyMatch(value -> value.getName().equals(PATCH_ID) && Objects.equals(value.getValue(),
                                    patchset))) {
                        return currentRun;
                    }
                }
            }
        }
        return null;
    }

}