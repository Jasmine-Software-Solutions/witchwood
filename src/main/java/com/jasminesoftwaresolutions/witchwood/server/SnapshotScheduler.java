package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.models.PrincipalSource;
import com.jasminesoftwaresolutions.witchwood.persistence.PersistedPrincipalSource;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.jooq.DSLContext;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically recompiles the {@link EvaluationSnapshot} from the database
 * and swaps it into the {@link EvaluationEngine}.
 */
public class SnapshotScheduler implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(SnapshotScheduler.class.getName());

    private final EvaluationEngine engine;
    private final DSLContext db;
    private final ScheduledExecutorService scheduler;
    private final long intervalSeconds;
    private final JsonLogicEvaluator evaluator;

    public SnapshotScheduler(EvaluationEngine engine,
                             DSLContext db,
                             JsonLogicEvaluator evaluator,
                             long intervalSeconds) {
        this.engine = engine;
        this.db = db;
        this.evaluator = evaluator;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snapshot-compiler");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::recompile,
                0,
                intervalSeconds,
                TimeUnit.SECONDS);
    }

    private void recompile() {
        try {
            LOG.fine("Recompiling evaluation snapshot...");
            Instant asOf = Instant.now();

            var source = new PersistedPrincipalSource(db, asOf);
            var compiler = new ModelSnapshotCompiler(
                    source.serviceDirectory(), source, asOf);
            EvaluationSnapshot snapshot = compiler.compile().snapshot();

            engine.snapshot(snapshot);
            LOG.fine("Snapshot recompiled successfully");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to recompile snapshot", e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
