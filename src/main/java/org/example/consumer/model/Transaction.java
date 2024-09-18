package org.example.consumer.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @JsonInclude
    @JsonProperty("clientId")
    @Transient
    private Long clientId;

    private String bank;

    @Enumerated(EnumType.STRING)
    private TransactionType orderType;

    private Integer quantity;

    private Double price;

    private Double cost;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}


