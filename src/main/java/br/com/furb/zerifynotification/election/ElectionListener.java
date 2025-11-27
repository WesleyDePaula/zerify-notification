package br.com.furb.zerifynotification.election;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ElectionListener {

    @Autowired
    private ElectionService electionService;

    @RabbitListener(bindings = @org.springframework.amqp.rabbit.annotation.QueueBinding(
            value = @org.springframework.amqp.rabbit.annotation.Queue(value = "", durable = "false", autoDelete = "true"),
            exchange = @org.springframework.amqp.rabbit.annotation.Exchange(value = "zerify.election", type = "fanout")
    ))
    public void onMessage(ElectionMessage msg) {
        if (msg == null || msg.getType() == null) return;
        var from = msg.getFromId();
        String tipo;
        switch (msg.getType()) {
            case HEARTBEAT -> tipo = "sinal de vida";
            case ELECTION -> tipo = "pedido de eleição";
            case OK -> tipo = "confirmação (OK)";
            case COORDINATOR -> tipo = "anúncio de coordenador";
            default -> tipo = msg.getType().name();
        }
        log.debug("[ELEIÇÃO][{}] Mensagem recebida: {} de {}", electionService.getInstanceId(), tipo, from);
        switch (msg.getType()) {
            case HEARTBEAT -> electionService.onHeartbeat(from);
            case ELECTION -> {
                log.info("[ELEIÇÃO][{}] Processando pedido de eleição vindo de {}", electionService.getInstanceId(), from);
                electionService.onElection(from);
            }
            case OK -> {
                log.info("[ELEIÇÃO][{}] Recebida confirmação 'OK' de {}", electionService.getInstanceId(), from);
                electionService.onOk(from);
            }
            case COORDINATOR -> {
                log.info("[ELEIÇÃO][{}] Anúncio de coordenador: {} (origem={})", electionService.getInstanceId(), msg.getLeaderId(), from);
                electionService.onCoordinator(msg.getLeaderId());
            }
        }
    }
}
