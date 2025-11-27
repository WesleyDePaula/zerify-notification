package br.com.furb.zerifynotification.election;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ElectionController {

    private final ElectionService electionService;
    private final RabbitListenerEndpointRegistry registry;

    @Autowired
    public ElectionController(ElectionService electionService, RabbitListenerEndpointRegistry registry) {
        this.electionService = electionService;
        this.registry = registry;
    }

    // Garante periodicamente que apenas o líder esteja consumindo a fila
    @Scheduled(fixedDelay = 1000)
    public void reconcile() {
        boolean iAmLeader = electionService.isLeader();
        var container = registry.getListenerContainer("notificationListener");
        if (container == null) return;
        if (iAmLeader) {
            if (!container.isRunning()) {
                log.info("[ELEIÇÃO][{}] Estou como coordenador; iniciando consumo da fila de notificações", electionService.getInstanceId());
                container.start();
            } else {
                log.debug("[ELEIÇÃO][{}] Já estou consumindo a fila de notificações", electionService.getInstanceId());
            }
        } else {
            if (container.isRunning()) {
                log.info("[ELEIÇÃO][{}] Não sou coordenador; paro o consumo da fila de notificações", electionService.getInstanceId());
                container.stop();
            } else {
                log.debug("[ELEIÇÃO][{}] Não sou coordenador; consumo da fila já está parado", electionService.getInstanceId());
            }
        }
    }
}
