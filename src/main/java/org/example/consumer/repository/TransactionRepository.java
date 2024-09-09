package org.example.consumer.repository;


import java.util.List;
import org.example.consumer.model.Client;
import org.example.consumer.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByClient(Client client);
}
