package org.example.consumer.buffer;

import java.util.List;
import org.example.consumer.model.Transaction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisTransactionBuffer implements TransactionBuffer {

    private static final String TRANSACTION_BUFFER_KEY_PREFIX = "transactionBuffer::";
    private final ListOperations<String, Transaction> listOps;
    private final RedisTemplate<String, Transaction> redisTemplate;

    public RedisTransactionBuffer(final RedisTemplate<String, Transaction> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.listOps = redisTemplate.opsForList();
    }

    @Override
    public void addTransactionToBuffer(final Long clientId, final Transaction transaction) {
        log.info("Pushing Transaction for clientId={} to the buffer", clientId);
        String key = getBufferKey(clientId);
        listOps.leftPush(key, transaction);
    }

    @Override
    public List<Transaction> getBufferedTransactions(final Long clientId) {
        log.info("Restore Transactions for clientId={} from the buffer", clientId);
        String key = getBufferKey(clientId);
        List<Transaction> transactions = listOps.range(key, 0, -1);
        redisTemplate.delete(key);
        return transactions;
    }

    private String getBufferKey(Long clientId) {
        return TRANSACTION_BUFFER_KEY_PREFIX + clientId;
    }

}
