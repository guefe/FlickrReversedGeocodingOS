package application;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by cestevez on 18/12/16.
 */
public class ResourceConfiguration extends ResourceConfig {
    public ResourceConfiguration() {
        packages("application");
        register(Services.class);
    }

}