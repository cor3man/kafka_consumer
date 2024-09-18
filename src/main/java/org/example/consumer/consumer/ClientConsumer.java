package org.example.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.consumer.model.Client;
import org.example.consumer.repository.ClientRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
public class ClientConsumer {

    private final KafkaConsumer<String, String> kafkaConsumer;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;
    private final String clientTopic;
    private final BufferedTransactionService bufferedTransactionService;

    public ClientConsumer(@Qualifier("KafkaClientConsumer") KafkaConsumer<String, String> kafkaConsumer,
            @Value("${client.topic}") String clientTopic,
            final ObjectMapper objectMapper,
            final ClientRepository clientRepository,
            final BufferedTransactionService bufferedTransactionService) {
        this.kafkaConsumer = kafkaConsumer;
        this.clientTopic = clientTopic;
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
        this.bufferedTransactionService = bufferedTransactionService;
    }

    @PostConstruct
    public void startConsumer() {
        new Thread(() -> {
            kafkaConsumer.subscribe(Collections.singletonList(clientTopic));
            while (true) {
                kafkaConsumer.poll(Duration.ofMillis(100)).forEach(record -> {
                    Client client = deserializeClient(record);
                    clientRepository.save(client);
                    bufferedTransactionService.saveBufferedTransactions(client);
                });
            }
        }).start();
    }

    private Client deserializeClient(final ConsumerRecord<String, String> record) {
        log.info("New record has polled >>> {}", record.value());
        Client client = null;
        try {
            client = objectMapper.readValue(record.value(), Client.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return client;
    }
}

