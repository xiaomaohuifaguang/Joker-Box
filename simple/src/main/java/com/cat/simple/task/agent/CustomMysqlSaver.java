package com.cat.simple.task.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.CreateOption;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class CustomMysqlSaver implements BaseCheckpointSaver {

    private static final Logger log = LoggerFactory.getLogger(CustomMysqlSaver.class);

    // DDL statements
    private static final String CREATE_THREAD_TABLE = """
			CREATE TABLE IF NOT EXISTS cat_ai_alibaba_GRAPH_THREAD (
			   thread_id VARCHAR(36) PRIMARY KEY,
			   thread_name VARCHAR(255),
			   is_released BOOLEAN DEFAULT FALSE NOT NULL
			)""";

    private static final String INDEX_THREAD_TABLE = """
			CREATE UNIQUE INDEX IDX_cat_ai_alibaba_GRAPH_THREAD_NAME_RELEASED
			  ON cat_ai_alibaba_GRAPH_THREAD(thread_name, is_released)
			""";

    private static final String CREATE_CHECKPOINT_TABLE = """
			CREATE TABLE IF NOT EXISTS cat_ai_alibaba_GRAPH_CHECKPOINT (
			   checkpoint_id VARCHAR(36) PRIMARY KEY,
			   thread_id VARCHAR(36) NOT NULL,
			   node_id VARCHAR(255),
			   next_node_id VARCHAR(255),
			   state_data JSON NOT NULL,
			   saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			
			   CONSTRAINT GRAPH_FK_THREAD
			       FOREIGN KEY(thread_id)
			       REFERENCES cat_ai_alibaba_GRAPH_THREAD(thread_id)
			       ON DELETE CASCADE
			)""";

    private static final String DROP_CHECKPOINT_TABLE = "DROP TABLE IF EXISTS cat_ai_alibaba_GRAPH_CHECKPOINT";
    private static final String DROP_THREAD_TABLE = "DROP TABLE IF EXISTS cat_ai_alibaba_GRAPH_THREAD";

    // DML statements
    private static final String UPSERT_THREAD = """
			INSERT INTO cat_ai_alibaba_GRAPH_THREAD (thread_id, thread_name, is_released)
			VALUES (?, ?, FALSE)
			ON DUPLICATE KEY UPDATE thread_id = thread_id
			""";

    private static final String INSERT_CHECKPOINT = """
			INSERT INTO cat_ai_alibaba_GRAPH_CHECKPOINT(checkpoint_id, thread_id, node_id, next_node_id, state_data)
			SELECT ?, thread_id, ?, ?, ?
			FROM cat_ai_alibaba_GRAPH_THREAD
			WHERE thread_name = ? AND is_released = FALSE
			""";

    private static final String UPDATE_CHECKPOINT = """
			UPDATE cat_ai_alibaba_GRAPH_CHECKPOINT c
			INNER JOIN cat_ai_alibaba_GRAPH_THREAD t ON c.thread_id = t.thread_id
			SET
			  c.checkpoint_id = ?,
			  c.node_id = ?,
			  c.next_node_id = ?,
			  c.state_data = ?
			WHERE t.thread_name = ? AND t.is_released != TRUE
			  AND c.checkpoint_id = ?
			""";

    private static final String SELECT_CHECKPOINTS = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  JSON_UNQUOTE(JSON_EXTRACT(c.state_data, '$.binaryPayload')) AS base64_data
			FROM cat_ai_alibaba_GRAPH_CHECKPOINT c
			  INNER JOIN cat_ai_alibaba_GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			ORDER BY c.saved_at DESC
			""";

    private static final String RELEASE_THREAD = """
			UPDATE cat_ai_alibaba_GRAPH_THREAD SET is_released = TRUE WHERE thread_name = ? AND is_released = FALSE
			""";

    private static final String SELECT_LATEST_CHECKPOINT = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  JSON_UNQUOTE(JSON_EXTRACT(c.state_data, '$.binaryPayload')) AS base64_data
			FROM cat_ai_alibaba_GRAPH_CHECKPOINT c
			  INNER JOIN cat_ai_alibaba_GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			ORDER BY c.saved_at DESC
			LIMIT 1
			""";

    private static final String SELECT_CHECKPOINT_BY_ID = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  JSON_UNQUOTE(JSON_EXTRACT(c.state_data, '$.binaryPayload')) AS base64_data
			FROM cat_ai_alibaba_GRAPH_CHECKPOINT c
			  INNER JOIN cat_ai_alibaba_GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			  AND c.checkpoint_id = ?
			""";

    // Configuration
    private final DataSource dataSource;
    private final CreateOption createOption;
    private final StateSerializer stateSerializer;

    private final Map<String, Checkpoint> latestCheckpointCache;
    private final ReentrantLock lock = new ReentrantLock();
    private final int maxCachedThreads;

    /**
     * Private constructor used by the builder to create a new instance of
     * CustomMysqlSaver.
     *
     * @param builder the builder
     */
    private CustomMysqlSaver(CustomMysqlSaver.Builder builder) {
        this.dataSource = builder.dataSource;
        this.createOption = builder.createOption;
        this.stateSerializer = builder.stateSerializer;
        this.maxCachedThreads = builder.maxCachedThreads;
        this.latestCheckpointCache = createLatestCheckpointCache(builder.maxCachedThreads);
        initTables();
    }

    /**
     * Creates an instance of a builder that allows to configure and create a new
     * instance of CustomMysqlSaver.
     *
     * @return a new instance of the builder.
     */
    public static CustomMysqlSaver.Builder builder() {
        return new CustomMysqlSaver.Builder();
    }

    /**
     * Rolls back a transaction and logs the error.
     *
     * @param conn      the database connection
     * @param checkpoint the checkpoint being processed
     * @param threadId  the thread ID
     */
    private void rollback(Connection conn, Checkpoint checkpoint, String threadId) {
        if (conn == null) {
            return;
        }

        requireNonNull(checkpoint, "checkpoint cannot be null");

        try {
            conn.rollback();
            log.warn("Transaction rolled back for checkpoint {}", checkpoint.getId());
        }
        catch (SQLException exRollback) {
            log.error("Failed to rollback transaction for checkpoint id {} in thread {}",
                    checkpoint.getId(),
                    threadId,
                    exRollback);
        }
    }

    /**
     * Rolls back a transaction and logs the error (for operations without checkpoint).
     *
     * @param conn     the database connection
     * @param threadId the thread ID
     */
    private void rollback(Connection conn, String threadId) {
        if (conn == null) {
            return;
        }

        try {
            conn.rollback();
            log.warn("Transaction rolled back for thread {}", threadId);
        }
        catch (SQLException exRollback) {
            log.error("Failed to rollback transaction for thread {}", threadId, exRollback);
        }
    }

    private String encodeState(Map<String, Object> data) throws IOException {
        var binaryData = stateSerializer.dataToBytes(data);
        var base64Data = Base64.getEncoder().encodeToString(binaryData);
        return format("""
				{"binaryPayload": "%s"}
				""", base64Data);
    }

    private Map<String, Object> decodeState(byte[] binaryPayload) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(binaryPayload);
        return stateSerializer.dataFromBytes(bytes);
    }

    /**
     * Initializes the database according the create options.
     */
    protected void initTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (createOption == CreateOption.CREATE_OR_REPLACE) {
                // Drop tables (indexes are automatically dropped with tables in MySQL)
                statement.addBatch(DROP_CHECKPOINT_TABLE);
                statement.addBatch(DROP_THREAD_TABLE);
                statement.executeBatch();
            }
            if (createOption == CreateOption.CREATE_OR_REPLACE ||
                    createOption == CreateOption.CREATE_IF_NOT_EXISTS) {
                statement.execute(CREATE_THREAD_TABLE);
                statement.execute(CREATE_CHECKPOINT_TABLE);

                // Try to create index, ignore error if it already exists
                try {
                    statement.execute(INDEX_THREAD_TABLE);
                }
                catch (SQLException e) {
                    // Ignore "Duplicate key name" error (error code 1061)
                    if (e.getErrorCode() != 1061) {
                        throw e;
                    }
                }
            }
        }
        catch (SQLException sqlException) {
            throw new RuntimeException("Unable to create tables", sqlException);
        }
    }

    private Checkpoint readCheckpoint(ResultSet resultSet)
            throws SQLException, IOException, ClassNotFoundException {
        return Checkpoint.builder()
                .id(resultSet.getString(1))
                .nodeId(resultSet.getString(2))
                .nextNodeId(resultSet.getString(3))
                .state(decodeState(resultSet.getBytes(4)))
                .build();
    }

    /**
     * Loads full checkpoint history on demand without retaining it in cache.
     */
    private LinkedList<Checkpoint> selectCheckpoints(String threadName) throws Exception {
        LinkedList<Checkpoint> checkpoints = new LinkedList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINTS)) {

            preparedStatement.setString(1, threadName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    checkpoints.add(readCheckpoint(resultSet));
                }
            }
        }
        catch (SQLException | IOException | ClassNotFoundException ex) {
            throw new Exception("Unable to load checkpoints", ex);
        }
        return checkpoints;
    }

    private Optional<Checkpoint> selectLatestCheckpoint(String threadName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_LATEST_CHECKPOINT)) {

            preparedStatement.setString(1, threadName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readCheckpoint(resultSet));
                }
                return Optional.empty();
            }
        }
        catch (SQLException | IOException | ClassNotFoundException ex) {
            throw new Exception("Unable to load latest checkpoint", ex);
        }
    }

    private Optional<Checkpoint> selectCheckpointById(String threadName, String checkpointId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINT_BY_ID)) {

            preparedStatement.setString(1, threadName);
            preparedStatement.setString(2, checkpointId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readCheckpoint(resultSet));
                }
                return Optional.empty();
            }
        }
        catch (SQLException | IOException | ClassNotFoundException ex) {
            throw new Exception("Unable to load checkpoint", ex);
        }
    }

    private void insertCheckpoint(String threadName, Checkpoint checkpoint) throws Exception {
        Connection conn = null;
        try (Connection ignored = conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement upsertStatement = conn.prepareStatement(UPSERT_THREAD);
                 PreparedStatement insertCheckpointStatement = conn.prepareStatement(INSERT_CHECKPOINT)) {

                upsertStatement.setString(1, UUID.randomUUID().toString());
                upsertStatement.setString(2, threadName);
                upsertStatement.execute();

                insertCheckpointStatement.setString(1, checkpoint.getId());
                insertCheckpointStatement.setString(2, checkpoint.getNodeId());
                insertCheckpointStatement.setString(3, checkpoint.getNextNodeId());
                insertCheckpointStatement.setString(4, encodeState(checkpoint.getState()));
                insertCheckpointStatement.setString(5, threadName);
                insertCheckpointStatement.execute();
            }

            conn.commit();
            log.debug("Checkpoint {} for thread {} inserted successfully.", checkpoint.getId(), threadName);
        }
        catch (SQLException | IOException ex) {
            log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadName, ex);
            rollback(conn, checkpoint, threadName);
            throw new Exception("Unable to insert checkpoint", ex);
        }
    }

    private void updateCheckpoint(String threadName, String checkpointId, Checkpoint checkpoint) throws Exception {
        Connection conn = null;
        try (Connection ignored = conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement preparedStatement = conn.prepareStatement(UPDATE_CHECKPOINT)) {
                preparedStatement.setString(1, checkpoint.getId());
                preparedStatement.setString(2, checkpoint.getNodeId());
                preparedStatement.setString(3, checkpoint.getNextNodeId());
                preparedStatement.setString(4, encodeState(checkpoint.getState()));
                preparedStatement.setString(5, threadName);
                preparedStatement.setString(6, checkpointId);
                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    throw new NoSuchElementException(format("Checkpoint with id %s not found!", checkpointId));
                }
            }

            conn.commit();
            log.debug("Checkpoint with id {} for thread {} updated successfully.", checkpoint.getId(), threadName);
        }
        catch (SQLException | IOException ex) {
            log.error("Error updating checkpoint with id {} in thread {}", checkpoint.getId(), threadName, ex);
            rollback(conn, checkpoint, threadName);
            throw new Exception("Unable to update checkpoint", ex);
        }
    }

    private void releaseThread(String threadName) throws Exception {
        Connection conn = null;
        try (Connection ignored = conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement preparedStatement = conn.prepareStatement(RELEASE_THREAD)) {
                preparedStatement.setString(1, threadName);
                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    throw new IllegalStateException(format("Thread '%s' not found or already released", threadName));
                }
            }

            conn.commit();
            log.debug("Thread {} released successfully.", threadName);
        }
        catch (SQLException ex) {
            log.error("Error releasing thread {}", threadName, ex);
            rollback(conn, threadName);
            throw new Exception("Unable to release checkpoint", ex);
        }
    }

    /**
     * Lists active checkpoints for the configured thread.
     */
    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        lock.lock();
        try {
            String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
            LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadName);
            if (!checkpoints.isEmpty()) {
                cacheLatest(threadName, checkpoints.peek());
            }
            return Collections.unmodifiableCollection(checkpoints);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Gets a checkpoint for the configured thread.
     */
    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        lock.lock();
        try {
            String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
            if (config.checkPointId().isPresent()) {
                return selectCheckpointById(threadName, config.checkPointId().get());
            }

            Optional<Checkpoint> cached = getCachedLatest(threadName);
            if (cached.isPresent()) {
                return cached;
            }

            Optional<Checkpoint> latest = selectLatestCheckpoint(threadName);
            latest.ifPresent(checkpoint -> cacheLatest(threadName, checkpoint));
            return latest;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Inserts or updates a checkpoint.
     */
    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        lock.lock();
        try {
            String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
            if (config.checkPointId().isPresent()) {
                String checkpointId = config.checkPointId().get();
                updateCheckpoint(threadName, checkpointId, checkpoint);
                getCachedLatest(threadName)
                        .filter(latest -> latest.getId().equals(checkpointId))
                        .ifPresent(latest -> cacheLatest(threadName, checkpoint));
                return config;
            }

            insertCheckpoint(threadName, checkpoint);
            cacheLatest(threadName, checkpoint);
            return RunnableConfig.builder(config)
                    .checkPointId(checkpoint.getId())
                    .build();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Releases the active thread and returns the released checkpoints.
     */
    @Override
    public Tag release(RunnableConfig config) throws Exception {
        lock.lock();
        try {
            String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
            LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadName);
            releaseThread(threadName);
            removeCachedLatest(threadName);
            return new Tag(threadName, checkpoints);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Creates a bounded LRU cache for latest checkpoints.
     */
    private static Map<String, Checkpoint> createLatestCheckpointCache(int maxCachedThreads) {
        if (maxCachedThreads == 0) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Checkpoint> eldest) {
                return size() > maxCachedThreads;
            }
        };
    }

    private Optional<Checkpoint> getCachedLatest(String threadName) {
        if (maxCachedThreads == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(latestCheckpointCache.get(threadName));
    }

    private void cacheLatest(String threadName, Checkpoint checkpoint) {
        if (maxCachedThreads > 0) {
            latestCheckpointCache.put(threadName, checkpoint);
        }
    }

    private void removeCachedLatest(String threadName) {
        if (maxCachedThreads > 0) {
            latestCheckpointCache.remove(threadName);
        }
    }

    /**
     * A builder for CustomMysqlSaver.
     */
    public static class Builder {
        private DataSource dataSource;
        private CreateOption createOption = CreateOption.CREATE_IF_NOT_EXISTS;
        private StateSerializer stateSerializer;
        private int maxCachedThreads = 1024;

        /**
         * Sets the maximum number of latest checkpoints retained in memory.
         *
         * @param maxCachedThreads max cached threads, or 0 to disable the cache
         * @return this builder
         */
        public CustomMysqlSaver.Builder maxCachedThreads(int maxCachedThreads) {
            if (maxCachedThreads < 0) {
                throw new IllegalArgumentException("maxCachedThreads must be greater than or equal to 0");
            }
            this.maxCachedThreads = maxCachedThreads;
            return this;
        }

        /**
         * Sets the state serializer
         *
         * @param stateSerializer the state serializer
         * @return this builder
         */
        public CustomMysqlSaver.Builder stateSerializer(StateSerializer stateSerializer) {
            this.stateSerializer = stateSerializer;
            return this;
        }

        /**
         * Sets the datasource
         *
         * @param dataSource the datasource
         * @return this builder
         */
        public CustomMysqlSaver.Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * Sets the create options (default {@link CreateOption#CREATE_IF_NOT_EXISTS}.
         *
         * @param createOption the create options
         * @return this builder
         */
        public CustomMysqlSaver.Builder createOption(CreateOption createOption) {
            this.createOption = createOption;
            return this;
        }

        /**
         * Creates a new instance of CustomMysqlSaver
         *
         * @return the new instance of CustomMysqlSaver.
         */
        public CustomMysqlSaver build() {
            if (stateSerializer == null) {
                this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
            }
            return new CustomMysqlSaver(this);
        }
    }
}
