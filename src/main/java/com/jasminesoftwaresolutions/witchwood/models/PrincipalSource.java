package com.jasminesoftwaresolutions.witchwood.models;

/**
 * A streaming source of {@link IPrincipal} instances for compilation.
 *
 * <p>Implementations stream principals from a backing store (database, file,
 * in-memory list) without requiring the full set to be materialized in memory
 * simultaneously. The compiler calls {@link #accept(PrincipalConsumer)} and
 * folds each principal into the snapshot immediately, then discards it.</p>
 *
 * <pre>{@code
 * // Persistence-backed example:
 * PrincipalSource source = consumer -> {
 *     try (var rs = db.query("SELECT id FROM principals")) {
 *         while (rs.next()) {
 *             consumer.accept(principalRepo.load(rs.getUUID("id"), asOf));
 *         }
 *     }
 * };
 * }</pre>
 */
@FunctionalInterface
public interface PrincipalSource {

    /**
     * Feed principals to the given consumer one at a time.
     *
     * @param consumer the callback that accepts each principal
     */
    void accept(PrincipalConsumer consumer);

    /**
     * A callback that receives one principal at a time during compilation.
     */
    @FunctionalInterface
    interface PrincipalConsumer {
        void accept(IPrincipal principal);
    }
}
