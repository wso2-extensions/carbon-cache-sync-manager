package org.wso2.carbon.cache.sync.jms.manager;

import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.core.clustering.api.CoordinatedActivity;

/**
 * This class contains the logic for starting the JMS client service after the node becomes coordinator.
 */
public class HybridModeCoordinatorListener implements CoordinatedActivity {

    private final ComponentContext context;
    private static volatile HybridModeCoordinatorListener instance;

    private HybridModeCoordinatorListener(ComponentContext context) {

        this.context = context;
    }

    public static HybridModeCoordinatorListener getInstance(ComponentContext context) {

        if (instance == null) {
            synchronized (JMSConsumer.class) {
                if (instance == null) {
                    instance = new HybridModeCoordinatorListener(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void execute() {

        JMSUtils.startOSGIService(this.context);
    }
}
