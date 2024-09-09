package org.example.consumer.buffer;

import java.util.List;
import org.example.consumer.model.Transaction;

public interface TransactionBuffer {
    void addTransactionToBuffer(Long clientId, Transaction transaction);
    List<Transaction> getBufferedTransactions(Long clientId);
}
