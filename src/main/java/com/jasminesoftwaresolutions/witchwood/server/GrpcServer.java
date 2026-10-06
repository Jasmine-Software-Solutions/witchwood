package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.repository.PolicyRepository;
import com.jasminesoftwaresolutions.witchwood.repository.PrincipalRepository;
import com.jasminesoftwaresolutions.witchwood.repository.RoleRepository;
import com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.jooq.DSLContext;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Bootstraps and runs the Witchwood gRPC server.
 */
public class GrpcServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(GrpcServer.class.getName());

    private final Server server;
    private final SnapshotScheduler snapshotScheduler;

    public GrpcServer(int port,
                      DSLContext db,
                      JsonLogicEvaluator evaluator,
                      long snapshotIntervalSeconds,
                      String jwtIssuer,
                      String jwtAudience,
                      String jwtJwksUrl,
                      long jwtJwksTtlSeconds,
                      String[] jwtTrustedSubjects) throws IOException {

        // Create the evaluation engine with an empty initial snapshot
        // (it will be populated by the first recompile)
        var emptySnapshot = new EvaluationSnapshot.Builder().build();
        EvaluationEngine engine = new EvaluationEngine(emptySnapshot, evaluator);

        var principalRepo = new PrincipalRepository(db);
        var policyRepo = new PolicyRepository(db);
        var roleRepo = new RoleRepository(db);
        var serviceRepo = new ServiceRepository(db);

        var interceptor = new JwtAuthInterceptor(
                jwtIssuer, jwtAudience, jwtJwksUrl, jwtJwksTtlSeconds, jwtTrustedSubjects);

        var service = new WitchwoodGrpcService(principalRepo, policyRepo, roleRepo, serviceRepo, engine);

        this.server = ServerBuilder.forPort(port)
                .addService(service)
                .intercept(interceptor)
                .build();

        this.snapshotScheduler = new SnapshotScheduler(engine, db, evaluator, snapshotIntervalSeconds);
    }

    public void start() throws IOException {
        snapshotScheduler.start();
        server.start();
        LOG.info("Witchwood gRPC server started on port " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down...");
            try {
                GrpcServer.this.close();
            } catch (Exception e) {
                LOG.warning("Error during shutdown: " + e.getMessage());
            }
        }));
    }

    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }

    @Override
    public void close() throws Exception {
        snapshotScheduler.close();
        if (server != null) {
            server.shutdown();
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        }
    }
}
