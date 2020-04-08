package no.cantara.tools.visuale.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.media.jsonb.server.JsonBindingSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.cantara.tools.visuale.domain.Health;
import no.cantara.tools.visuale.domain.HealthMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

public class StatusResource implements Service {
    public static final Logger logger = LoggerFactory.getLogger(StatusResource.class);
    public static ObjectMapper mapper = new ObjectMapper().configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    StatusService statusService = new StatusService();


    /**
     * A service registers itself by updating the routine rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void update(Routing.Rules rules) {
        rules.get("/status", JsonBindingSupport.create(), this::showEnvironment)
                .options("/status", JsonBindingSupport.create(), this::showEnvironmentOptionHeaders)
                .get("/api/status", JsonBindingSupport.create(), this::showEnvironment)
                .options("/api/status", JsonBindingSupport.create(), this::showEnvironmentOptionHeaders)
                .put("/status", JsonBindingSupport.create(), this::updateHealthInfo)
                .put("/api/status", JsonBindingSupport.create(), this::updateHealthInfo)
                .put("/status/{env}/{service}/{node}", JsonBindingSupport.create(), this::updateFullHealthInfo)
                .put("/api/status/{env}/{service}/{node}", JsonBindingSupport.create(), this::updateFullHealthInfo);
    }


    /**
     * @return {@link JsonObject}
     */
    @SuppressWarnings("checkstyle:designforextension")
    public synchronized void showEnvironment(final ServerRequest request, final ServerResponse response) {
        String msg = statusService.getEnvironmentAsString();
        response.status(200).send(msg);
    }

    /**
     * @return {@link JsonObject}
     */
    @SuppressWarnings("checkstyle:designforextension")
    public synchronized void showEnvironmentOptionHeaders(final ServerRequest request, final ServerResponse response) {
     response.status(200).headers().add("Content-Type: application/json"
                , "Access-Control-Allow-Origin: *"
                , "Access-Control-Allow-Methods: GET, OPTIONS"
                , "Access-Control-Allow-Headers: *"
                , "Access-Control-Allow-Credentials: true");
    }

    /**
     * @return {@link Response}
     */
    @SuppressWarnings("checkstyle:designforextension")
    public void updateHealthInfo(final ServerRequest request, final ServerResponse response) {
        logger.debug("updateHealthInfo");
        request.content().as(String.class)
                .thenAccept(jo -> updateHealthInfoFromJson(jo))
                .thenAccept(jo -> response.status(204).send());

    }


    /**
     * @return {@link Response}
     */
    @SuppressWarnings("checkstyle:designforextension")
    public void updateFullHealthInfo(final ServerRequest request, final ServerResponse response) {
        logger.debug("updateFullHealthInfo");
        String envName = request.path().param("env");
        String serviceName = request.path().param("service");
        String nodeName = request.path().param("node");

        request.content().as(String.class)
                .thenAccept(jo -> getHealthInfoFromJson(jo, envName, serviceName, nodeName))
                .thenAccept(jo -> response.status(204).send());
    }


    private Health updateHealthInfoFromJson(String healthJsonString) {
        try {
            Health myHealth = null;
            if (healthJsonString != null || healthJsonString.toString().length() < 1) {
                myHealth = HealthMapper.fromRealWorldJson(healthJsonString);
                statusService.updateHealthMap(myHealth);
            }
            return myHealth;
        } catch (Exception e) {
            logger.error("Unable to patse and update health info for payload: {}, {}", healthJsonString, e);
        }
        return null;
    }

    private Health getHealthInfoFromJson(String healthJsonString, String envName, String serviceName, String nodeName) {
        try {
            Health myHealth = null;
            if (healthJsonString != null || healthJsonString.toString().length() < 1) {
                myHealth = HealthMapper.fromRealWorldJson(healthJsonString);
            }
            statusService.updateEnvironment(envName, serviceName, nodeName, myHealth);
            return myHealth;
        } catch (Exception e) {
            logger.error("Unable to patse and update health info for payload: {}, {}", healthJsonString, e);
        }
        return null;
    }

    public StatusService getStatusService() {
        return statusService;
    }
}

