package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.ClusterSettingsResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;

public class ClusterSettingsResponseHandler implements TransportResponseHandler<ClusterSettingsResponse> {
    private static final Logger logger = LogManager.getLogger(ClusterSettingsResponseHandler.class);

    @Override
    public void handleResponse(ClusterSettingsResponse response) {
        logger.info("received {}", response);
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("ClusterSettingRequest failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public ClusterSettingsResponse read(StreamInput in) throws IOException {
        return new ClusterSettingsResponse(in);
    }
}
