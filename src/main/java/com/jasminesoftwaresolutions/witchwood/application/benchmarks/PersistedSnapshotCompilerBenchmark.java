package com.jasminesoftwaresolutions.witchwood.application.benchmarks;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.persistence.PersistedPrincipalSource;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@State(Scope.Benchmark)
public class PersistedSnapshotCompilerBenchmark {
    @Param({ "jdbc:postgresql://localhost:5432/witchwood" })
    protected String jdbcUrl;

    @Param({ "witchwood" })
    protected String jdbcUser;

    @Param({ "secret" })
    protected String jdbcPassword;

    protected java.sql.Connection connection;
    protected DSLContext db;
    protected ServiceDirectory serviceDirectory;
    protected PersistedPrincipalSource source;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
        db = DSL.using(connection);
        serviceDirectory = loadServiceDirectory();
        source = new PersistedPrincipalSource(db, Instant.MAX);
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        if (connection != null)
            connection.close();
    }

    @Benchmark
    public void compile(Blackhole bh) {
        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(serviceDirectory, source, Instant.MAX)
                .compile();

        bh.consume(compiler.snapshot());
    }

    private ServiceDirectory loadServiceDirectory() {
        var actions = db.selectDistinct(DSL.field("action", String.class))
                .from(DSL.table("statement_actions"))
                .fetch()
                .map(r -> r.get(0, String.class));

        Map<String, Set<String>> serviceMap = new LinkedHashMap<>();
        for (String action : actions) {
            if (action.contains("*"))
                continue;

            int colon = action.indexOf(':');
            if (colon < 0)
                continue;

            String service = action.substring(0, colon);
            String key = action.substring(colon + 1);
            serviceMap.computeIfAbsent(service, k -> new LinkedHashSet<>()).add(key);
        }

        return ServiceDirectory.from(serviceMap);
    }
}
