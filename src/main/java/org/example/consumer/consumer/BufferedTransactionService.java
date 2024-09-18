package org.example.consumer.consumer;

import org.example.consumer.buffer.TransactionBuffer;
import org.example.consumer.model.Client;
import org.example.consumer.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BufferedTransactionService {
    private final TransactionBuffer buffer;
    private final TransactionRepository transactionRepository;

    public BufferedTransactionService(final TransactionBuffer buffer,
            final TransactionRepository transactionRepository) {
        this.buffer = buffer;
        this.transactionRepository = transactionRepository;
    }

    public void saveBufferedTransactions(Client client) {
        buffer.getBufferedTransactions(client.getClientId()).forEach(bufferedTransaction -> {
            bufferedTransaction.setClient(client);
            transactionRepository.save(bufferedTransaction);
        });
    }
}
