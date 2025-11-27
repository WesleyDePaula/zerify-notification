package br.com.furb.zerifynotification.election;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InstanceIdProvider {

    // Prefer an explicit instance id via environment; otherwise generate one.
    @Value("${INSTANCE_ID:}")
    private String configuredId;

    private String instanceId;

    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent e) {
        if (configuredId != null && !configuredId.isBlank()) {
            instanceId = configuredId;
        } else {
            // Use lexicographically comparable id: timestamp-uuid
            instanceId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
        }
    }

    public String getInstanceId() {
        return instanceId;
    }
}
