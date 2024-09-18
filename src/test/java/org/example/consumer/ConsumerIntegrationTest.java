package org.example.consumer;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.consumer.model.Client;
import org.example.consumer.model.Transaction;
import org.example.consumer.model.TransactionType;
import org.example.consumer.repository.ClientRepository;
import org.example.consumer.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redis.testcontainers.RedisContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class ConsumerIntegrationTest {

    @Container
    public static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Container
    public static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:7.0.0"))
            .withExposedPorts(6379);

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private KafkaProducer<String, String> producer;
    private static ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(producerProps);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AfterEach
    void tearDown() {
        producer.close();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaContainer.getBootstrapServers());
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Test
    @Transactional
    void testClientConsumer() throws InterruptedException, JsonProcessingException {
        Client client = new Client();
        client.setClientId(1L);
        client.setEmail("client@example.com");
        client.setFirstName("John");
        client.setLastName("Doe");

        String clientJson = new ObjectMapper().writeValueAsString(client);

        ProducerRecord<String, String> clientRecord = new ProducerRecord<>("client-topic", clientJson);
        producer.send(clientRecord);

        Thread.sleep(2000);


        Client savedClient = clientRepository.findById(1L).orElse(null);
        assertNotNull(savedClient);
        assertEquals("John", savedClient.getFirstName());
        assertEquals("Doe", savedClient.getLastName());
    }

    @Test
    @Transactional
    void testTransactionConsumerWithBufferedTransactions() throws InterruptedException, JsonProcessingException {
        Transaction transaction = new Transaction();
        transaction.setClientId(1L);
        transaction.setBank("Some Bank");
        transaction.setQuantity(10);
        transaction.setPrice(100.0);
        transaction.setOrderType(TransactionType.INCOME);
        transaction.setCreatedAt(LocalDateTime.now());

        String transactionJson = objectMapper.writeValueAsString(transaction);

        ProducerRecord<String, String> transactionRecord = new ProducerRecord<>("transaction-topic", transactionJson);
        producer.send(transactionRecord);

        Thread.sleep(2000);

        assertEquals(0, transactionRepository.findAll().size());

        Client client = new Client();
        client.setClientId(1L);
        client.setEmail("client@example.com");
        client.setFirstName("John");
        client.setLastName("Doe");

        String clientJson = objectMapper.writeValueAsString(client);
        ProducerRecord<String, String> clientRecord = new ProducerRecord<>("client-topic", clientJson);
        producer.send(clientRecord);

        Thread.sleep(2000);

        Transaction savedTransaction = transactionRepository.findByClient(client).get(0);
        assertNotNull(savedTransaction);
        assertEquals(1000.0, savedTransaction.getCost());
    }
}

