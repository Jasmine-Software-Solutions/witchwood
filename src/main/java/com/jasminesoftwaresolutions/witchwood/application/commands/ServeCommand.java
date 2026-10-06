package com.jasminesoftwaresolutions.witchwood.application.commands;

import com.jasminesoftwaresolutions.witchwood.application.Env;
import com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository;
import com.jasminesoftwaresolutions.witchwood.server.GrpcServer;
import com.jasminesoftwaresolutions.witchwood.server.WitchwoodActions;
import com.jasminesoftwaresolutions.witchwood.server.WitchwoodResources;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import picocli.CommandLine;

import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "serve",
        mixinStandardHelpOptions = true,
        description = "Start the Witchwood gRPC server.")
public class ServeCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        Flyway.configure()
                .dataSource(Env.DATABASE_URL, Env.DATABASE_USERNAME, Env.DATABASE_PASSWORD)
                .locations("filesystem:/app/db/migration")
                .load()
                .migrate();

        var conn = DriverManager.getConnection(Env.DATABASE_URL, Env.DATABASE_USERNAME, Env.DATABASE_PASSWORD);
        DSLContext db = DSL.using(conn, SQLDialect.POSTGRES);

        ServiceRepository serviceRepository = new ServiceRepository(db);
        serviceRepository.registerService(WitchwoodResources.WITCHWOOD_FQDN);
        serviceRepository.putActions(WitchwoodResources.WITCHWOOD_FQDN, WitchwoodActions.ALL.stream()
                .map(it -> it.substring(WitchwoodResources.WITCHWOOD_FQDN.length() + 1))
                .toList());

        JsonLogicEvaluator evaluator = new JsonLogicEvaluator(java.util.List.of());

        try (var server = new GrpcServer(Env.PORT, db, evaluator, Env.SNAPSHOT_REFRESH_INTERVAL,
                Env.JWT_ISSUER, Env.JWT_AUDIENCE, Env.JWT_JWKS_URL, Env.JWT_JWKS_TTL, Env.JWT_TRUSTED_SUBJECTS)) {
            server.start();
            server.blockUntilShutdown();
        }

        return 0;
    }
}
