package org.example.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.consumer.buffer.TransactionBuffer;
import org.example.consumer.model.Client;
import org.example.consumer.model.Transaction;
import org.example.consumer.repository.ClientRepository;
import org.example.consumer.repository.TransactionRepository;
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
import java.util.Optional;

@Service
@Slf4j
public class TransactionConsumer {

    private final KafkaConsumer<String, String> kafkaConsumer;
    private final TransactionRepository transactionRepository;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;
    private final String transactionTopic;
    private final TransactionBuffer buffer;

    public TransactionConsumer(@Qualifier("KafkaTransactionConsumer") KafkaConsumer<String, String> kafkaConsumer,
            @Value("${transaction.topic}") String transactionTopic, final TransactionRepository transactionRepository,
            final ClientRepository clientRepository, final ObjectMapper objectMapper, final TransactionBuffer buffer) {
        this.kafkaConsumer = kafkaConsumer;
        this.transactionRepository = transactionRepository;
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
        this.transactionTopic = transactionTopic;
        this.buffer = buffer;
    }

    @PostConstruct
    public void startConsumer() {
        new Thread(() -> {
            kafkaConsumer.subscribe(Collections.singletonList(transactionTopic));
            while (true) {
                kafkaConsumer.poll(Duration.ofMillis(100)).forEach(record -> {
                    Transaction transaction = deserializeTransaction(record);
                    transaction.setCost(transaction.getQuantity() * transaction.getPrice());
                    processTransaction(transaction);
                });
            }
        }).start();
    }

    private Transaction deserializeTransaction(final ConsumerRecord<String, String> record) {
        log.info("Transaction has polled >>> {}", record.value());
        Transaction transaction = null;
        try {
            transaction = objectMapper.readValue(record.value(), Transaction.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return transaction;
    }

    private void processTransaction(final Transaction transaction) {
        Optional<Client> client = clientRepository.findById(transaction.getClientId());
        if (client.isPresent()) {
            transaction.setClient(client.get());
            transactionRepository.save(transaction);
        } else {
            log.warn("Corresponding ClientId not found. Transaction has been buffered");
            buffer.addTransactionToBuffer(transaction.getClientId(), transaction);
        }
    }
}
