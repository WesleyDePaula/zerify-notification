package br.com.furb.zerifynotification.election;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class ElectionService {

    private final InstanceIdProvider idProvider;
    private final RabbitTemplate rabbitTemplate;

    // últimos timestamps recebidos dos nós
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private final AtomicReference<String> leader = new AtomicReference<>();

    private volatile boolean electionInProgress = false;

    // tempo máximo aceitável desde o último sinal de vida do líder (ms)
    private static final long LEADER_TIMEOUT_MS = 6000;

    @Autowired
    public ElectionService(InstanceIdProvider idProvider, RabbitTemplate rabbitTemplate) {
        this.idProvider = idProvider;
        this.rabbitTemplate = rabbitTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start(ApplicationReadyEvent e) {
        // confia no heartbeat agendado; não publica aqui para evitar corrida na inicialização do id da instância
        log.info("[ELEIÇÃO][{}] Serviço de eleição iniciado", getInstanceId());
    }

    public String getInstanceId() { return idProvider.getInstanceId(); }

    public boolean isLeader() {
        var l = leader.get();
        return l != null && l.equals(getInstanceId());
    }

    public String currentLeader() { return leader.get(); }

    @Scheduled(fixedDelay = 2000)
    public void publishHeartbeat() {
        var id = getInstanceId();
        if (id == null) return; // não inicializado ainda
        var msg = new ElectionMessage(ElectionMessage.Type.HEARTBEAT, id);
        try {
            rabbitTemplate.convertAndSend("zerify.election", "", msg);
            log.debug("[ELEIÇÃO][{}] Enviando sinal de vida", id);
        } catch (Exception e) {
            log.warn("[ELEIÇÃO][{}] Erro ao publicar sinal de vida: {}", id, e.getMessage());
        }
    }

    public void onHeartbeat(String fromId) {
        if (fromId == null) return;
        lastSeen.put(fromId, Instant.now());
        log.debug("[ELEIÇÃO][{}] Recebido sinal de vida de {}", getInstanceId(), fromId);
        // se não houver coordenador conhecido, verificar se devo iniciar eleição
        if (leader.get() == null) {
            if (isHighestKnown()) {
                log.info("[ELEIÇÃO][{}] Não há coordenador conhecido; como meu ID é o maior entre os conhecidos, inicio a eleição", getInstanceId());
                startElection();
            }
        }
    }

    private boolean isHighestKnown() {
        var myId = getInstanceId();
        if (myId == null) return false;
        for (var id : lastSeen.keySet()) {
            if (compareIds(id, myId) > 0) return false;
        }
        return true;
    }

    public void onElection(String fromId) {
        if (fromId == null) return;
        log.info("[ELEIÇÃO][{}] Pedido de eleição recebido de {}", getInstanceId(), fromId);
        // se eu tenho ID maior, respondo OK e inicio minha própria eleição
        if (compareIds(getInstanceId(), fromId) > 0) {
            try {
                var ok = new ElectionMessage(ElectionMessage.Type.OK, getInstanceId());
                rabbitTemplate.convertAndSend("zerify.election", "", ok);
                log.info("[ELEIÇÃO][{}] Enviando 'OK' para {}", getInstanceId(), fromId);
            } catch (Exception e) {
                log.warn("[ELEIÇÃO][{}] Erro ao enviar 'OK': {}", getInstanceId(), e.getMessage());
            }
            startElection();
        } else {
            log.debug("[ELEIÇÃO][{}] Meu ID não é maior que {}; não envio 'OK'", getInstanceId(), fromId);
        }
    }

    public void onOk(String fromId) {
        log.debug("[ELEIÇÃO][{}] 'OK' recebido de {}", getInstanceId(), fromId);
        // alguém com ID maior respondeu; aguardar anúncio de coordenador
        electionInProgress = false;
    }

    public void onCoordinator(String leaderId) {
        if (leaderId != null) {
            leader.set(leaderId);
            lastSeen.put(leaderId, Instant.now());
            electionInProgress = false;
            log.info("[ELEIÇÃO][{}] Coordenador definido: {}", getInstanceId(), leaderId);
        }
    }

    private void startElection() {
        if (electionInProgress) return;
        electionInProgress = true;
        var msg = new ElectionMessage(ElectionMessage.Type.ELECTION, getInstanceId());
        try {
            rabbitTemplate.convertAndSend("zerify.election", "", msg);
            log.info("[ELEIÇÃO][{}] Iniciando eleição", getInstanceId());
        } catch (Exception e) {
            log.warn("[ELEIÇÃO][{}] Erro ao enviar pedido de eleição: {}", getInstanceId(), e.getMessage());
        }

        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
            if (electionInProgress) {
                try {
                    var coord = new ElectionMessage(ElectionMessage.Type.COORDINATOR, getInstanceId(), getInstanceId());
                    rabbitTemplate.convertAndSend("zerify.election", "", coord);
                    log.info("[ELEIÇÃO][{}] Anunciando coordenador e me declarando líder", getInstanceId());
                } catch (Exception e) {
                    log.warn("[ELEIÇÃO][{}] Erro ao enviar anúncio de coordenador: {}", getInstanceId(), e.getMessage());
                }
                leader.set(getInstanceId());
                lastSeen.put(getInstanceId(), Instant.now());
                electionInProgress = false;
            }
        }).start();
    }

    // Verifica periodicamente se o coordenador atual continua respondendo sinais de vida.
    @Scheduled(fixedDelay = 2000)
    public void checkLeaderLiveness() {
        var current = leader.get();
        if (current == null) return;
        Instant last = lastSeen.get(current);
        if (last == null) {
            log.warn("[ELEIÇÃO][{}] O coordenador conhecido ({}) não tem registro de sinal de vida; vou tentar assumir ou iniciar nova eleição", getInstanceId(), current);
            leader.set(null);
            if (isHighestKnown()) {
                // se sou o maior conhecido, declaro-me coordenador imediatamente
                try {
                    var coord = new ElectionMessage(ElectionMessage.Type.COORDINATOR, getInstanceId(), getInstanceId());
                    rabbitTemplate.convertAndSend("zerify.election", "", coord);
                    leader.set(getInstanceId());
                    lastSeen.put(getInstanceId(), Instant.now());
                    log.info("[ELEIÇÃO][{}] Sem sinal de vida do antigo coordenador; como sou o maior conhecido, declaro-me coordenador e anunciei isso", getInstanceId());
                    return;
                } catch (Exception e) {
                    log.warn("[ELEIÇÃO][{}] Erro ao anunciar coordenador: {}", getInstanceId(), e.getMessage());
                }
            }
            // se não for o maior, inicia eleição
            startElection();
            return;
        }
        long ageMs = Duration.between(last, Instant.now()).toMillis();
        if (ageMs > LEADER_TIMEOUT_MS) {
            log.warn("[ELEIÇÃO][{}] O coordenador ({}) não responde há {} ms (limite {} ms); vou tentar assumir ou iniciar nova eleição", getInstanceId(), current, ageMs, LEADER_TIMEOUT_MS);
            leader.set(null);
            if (isHighestKnown()) {
                try {
                    var coord = new ElectionMessage(ElectionMessage.Type.COORDINATOR, getInstanceId(), getInstanceId());
                    rabbitTemplate.convertAndSend("zerify.election", "", coord);
                    leader.set(getInstanceId());
                    lastSeen.put(getInstanceId(), Instant.now());
                    log.info("[ELEIÇÃO][{}] Sou o maior conhecido; declaro-me coordenador e anunciei isso", getInstanceId());
                    return;
                } catch (Exception e) {
                    log.warn("[ELEIÇÃO][{}] Erro ao anunciar coordenador: {}", getInstanceId(), e.getMessage());
                }
            }
            startElection();
        } else {
            log.debug("[ELEIÇÃO][{}] Coordenador ({}) visto há {} ms", getInstanceId(), current, ageMs);
        }
    }

    private int compareIds(String a, String b) {
        if (a == null || b == null) return 0;
        // comparação lexicográfica é suficiente porque ids são timestamp-uuid ou fornecidos
        return a.compareTo(b);
    }

}
