package com.eu.habbo.database.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;

/**
 * Works out which packaged migrations a database has evidently already been through by
 * looking at the schema itself.
 *
 * <p>A migration that creates tables or adds columns leaves those objects behind. When a
 * hotel's schema carries them but its {@code flyway_schema_history} does not record the
 * migration (the history was dropped, the database was restored from a dump taken without
 * it, or an operator applied the SQL by hand), re-running the migration fails on
 * "already exists". This class reads the DDL out of every packaged migration, checks the
 * objects against {@code information_schema}, and reports the longest unbroken run of
 * migrations whose objects are all present. That run is what the runner records as
 * already applied instead of executing again.
 *
 * <p>Only the prefix counts. A migration further along whose objects happen to exist (a
 * column stock Arcturus already shipped, a table an operator created by hand) is left to
 * run normally, because its own guards are the only thing that can say whether it was
 * applied. Objects that a later migration drops or renames are ignored altogether, since a
 * fully upgraded schema legitimately lacks them. Migrations with no DDL of their own are
 * carried along with their neighbours: they are recorded as applied when they sit below
 * the detected point, and run when they sit above it.
 */
final class SchemaEvidence {

    /** Where the runner keeps the SQL migrations on the classpath. */
    static final String RESOURCE_PREFIX = "db/migration/";

    private static final Pattern CREATE_TABLE =
            Pattern.compile("\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_TABLE =
            Pattern.compile("\\bALTER\\s+TABLE\\s+`?(\\w+)`?([^;]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_COLUMN = Pattern.compile(
            "\\bADD\\s+(?:COLUMN\\s+)?(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_COLUMN =
            Pattern.compile("\\bDROP\\s+(?:COLUMN\\s+)?(?:IF\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_TABLE =
            Pattern.compile("\\bDROP\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RENAME_TABLE =
            Pattern.compile("\\bRENAME\\s+TABLE\\s+`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("(?m)(?:--|#)[^\\n]*");

    /** Words that follow ADD or DROP in an ALTER TABLE without naming a column. */
    private static final Set<String> NOT_A_COLUMN = Set.of(
            "INDEX",
            "KEY",
            "CONSTRAINT",
            "FOREIGN",
            "UNIQUE",
            "PRIMARY",
            "FULLTEXT",
            "SPATIAL",
            "PARTITION",
            "CHECK",
            "COLUMNS");

    /** A column, keyed by lower-cased table and column name. */
    record Column(String table, String name) {
        @Override
        public String toString() {
            return table + "." + name;
        }
    }

    /**
     * The objects one packaged migration creates, plus the ones it drops or renames so a
     * later migration's removals can be discounted from an earlier one's evidence.
     */
    record Migration(
            String version,
            String description,
            Set<String> tables,
            Set<Column> columns,
            Set<String> droppedTables,
            Set<Column> droppedColumns) {
        boolean hasEvidence() {
            return !tables.isEmpty() || !columns.isEmpty();
        }

        int objectCount() {
            return tables.size() + columns.size();
        }

        boolean dropsTable(String table) {
            return droppedTables.contains(table);
        }

        boolean dropsColumn(Column column) {
            return droppedColumns.contains(column);
        }
    }

    /** How much of a migration's evidence the schema carries. */
    enum Presence {
        /** Every table and column the migration creates exists. */
        PRESENT,
        /** Some exist and some do not; the migration is treated as not applied. */
        PARTIAL,
        /** None exist. */
        ABSENT,
        /** The migration creates nothing that can be checked. */
        NO_EVIDENCE
    }

    /**
     * The verdict for a run of candidate migrations.
     *
     * @param appliedThrough the highest version whose objects, and those of every
     *     evidence-bearing candidate before it, all exist; {@code null} when the first
     *     evidence-bearing candidate is missing objects
     * @param presence each candidate's presence, in version order
     */
    record Assessment(String appliedThrough, Map<String, Presence> presence) {
        /** Candidates at or below {@link #appliedThrough}, in version order. */
        List<String> evidentlyApplied() {
            if (appliedThrough == null) return List.of();
            MigrationVersion limit = MigrationVersion.fromVersion(appliedThrough);
            List<String> applied = new ArrayList<>();
            for (String version : presence.keySet()) {
                if (MigrationVersion.fromVersion(version).compareTo(limit) <= 0) applied.add(version);
            }
            return applied;
        }

        /** Candidates above {@link #appliedThrough} whose objects nevertheless exist. */
        List<String> presentBeyond() {
            MigrationVersion limit = appliedThrough == null ? null : MigrationVersion.fromVersion(appliedThrough);
            List<String> beyond = new ArrayList<>();
            for (Map.Entry<String, Presence> entry : presence.entrySet()) {
                boolean above = limit == null
                        || MigrationVersion.fromVersion(entry.getKey()).compareTo(limit) > 0;
                if (above && entry.getValue() == Presence.PRESENT) beyond.add(entry.getKey());
            }
            return beyond;
        }
    }

    private SchemaEvidence() {}

    /**
     * Reads the evidence of every packaged versioned migration, in version order, with
     * objects that a later migration drops or renames removed.
     */
    static List<Migration> packaged(Flyway flyway) {
        List<Migration> migrations = new ArrayList<>();
        for (MigrationInfo info : flyway.info().all()) {
            if (info.getVersion() == null) continue;
            String version = info.getVersion().getVersion();
            String script = info.getScript();
            String sql = script != null && script.toLowerCase(Locale.ROOT).endsWith(".sql") ? readResource(script) : "";
            migrations.add(parse(version, info.getDescription(), sql));
        }
        migrations.sort((a, b) ->
                MigrationVersion.fromVersion(a.version()).compareTo(MigrationVersion.fromVersion(b.version())));
        return stabilise(migrations);
    }

    /** Extracts the tables and columns a migration creates from its SQL. */
    static Migration parse(String version, String description, String sql) {
        String text =
                LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(sql).replaceAll(" ")).replaceAll(" ");

        Set<String> tables = new LinkedHashSet<>();
        Matcher create = CREATE_TABLE.matcher(text);
        while (create.find()) tables.add(create.group(1).toLowerCase(Locale.ROOT));

        Set<Column> columns = new LinkedHashSet<>();
        Matcher alter = ALTER_TABLE.matcher(text);
        while (alter.find()) {
            String table = alter.group(1).toLowerCase(Locale.ROOT);
            Matcher add = ADD_COLUMN.matcher(alter.group(2));
            while (add.find()) {
                String name = add.group(1);
                if (NOT_A_COLUMN.contains(name.toUpperCase(Locale.ROOT))) continue;
                columns.add(new Column(table, name.toLowerCase(Locale.ROOT)));
            }
        }
        // A table created and then dropped or renamed within the same migration leaves nothing behind.
        Set<String> droppedTables = droppedTables(text);
        Set<Column> droppedColumns = droppedColumns(text);
        tables.removeAll(droppedTables);
        columns.removeAll(droppedColumns);
        return new Migration(
                version,
                description,
                Collections.unmodifiableSet(tables),
                Collections.unmodifiableSet(columns),
                Collections.unmodifiableSet(droppedTables),
                Collections.unmodifiableSet(droppedColumns));
    }

    /** Removes objects that a later migration drops or renames, since an upgraded schema lacks them. */
    static List<Migration> stabilise(List<Migration> migrations) {
        Set<String> unstableTables = new HashSet<>();
        Set<Column> unstableColumns = new HashSet<>();
        for (Migration migration : migrations) {
            for (String table : migration.tables()) {
                for (Migration later : migrations) {
                    if (compare(later.version(), migration.version()) > 0 && later.dropsTable(table)) {
                        unstableTables.add(table);
                    }
                }
            }
            for (Column column : migration.columns()) {
                for (Migration later : migrations) {
                    if (compare(later.version(), migration.version()) > 0
                            && (later.dropsTable(column.table()) || later.dropsColumn(column))) {
                        unstableColumns.add(column);
                    }
                }
            }
        }
        List<Migration> stable = new ArrayList<>(migrations.size());
        for (Migration migration : migrations) {
            Set<String> tables = new LinkedHashSet<>(migration.tables());
            tables.removeAll(unstableTables);
            Set<Column> columns = new LinkedHashSet<>();
            for (Column column : migration.columns()) {
                if (!unstableColumns.contains(column) && !unstableTables.contains(column.table())) columns.add(column);
            }
            stable.add(new Migration(
                    migration.version(),
                    migration.description(),
                    Collections.unmodifiableSet(tables),
                    Collections.unmodifiableSet(columns),
                    migration.droppedTables(),
                    migration.droppedColumns()));
        }
        return stable;
    }

    /**
     * Checks the candidates, which must be in version order, against the live schema and
     * finds the longest prefix whose evidence is fully present.
     */
    static Assessment assess(Connection connection, List<Migration> candidates) throws SQLException {
        Map<String, Presence> presence = new LinkedHashMap<>();
        String appliedThrough = null;
        boolean prefixIntact = true;
        for (Migration migration : candidates) {
            Presence state = presence(connection, migration);
            presence.put(migration.version(), state);
            if (state == Presence.NO_EVIDENCE) continue;
            if (state == Presence.PRESENT && prefixIntact) {
                appliedThrough = migration.version();
            } else {
                prefixIntact = false;
            }
        }
        return new Assessment(appliedThrough, Collections.unmodifiableMap(presence));
    }

    static Presence presence(Connection connection, Migration migration) throws SQLException {
        if (!migration.hasEvidence()) return Presence.NO_EVIDENCE;
        int found = 0;
        for (String table : migration.tables()) {
            if (tableExists(connection, table)) found++;
        }
        for (Column column : migration.columns()) {
            if (columnExists(connection, column.table(), column.name())) found++;
        }
        if (found == 0) return Presence.ABSENT;
        return found == migration.objectCount() ? Presence.PRESENT : Presence.PARTIAL;
    }

    private static int compare(String a, String b) {
        return MigrationVersion.fromVersion(a).compareTo(MigrationVersion.fromVersion(b));
    }

    private static Set<String> droppedTables(String text) {
        Set<String> dropped = new HashSet<>();
        Matcher drop = DROP_TABLE.matcher(text);
        while (drop.find()) dropped.add(drop.group(1).toLowerCase(Locale.ROOT));
        Matcher rename = RENAME_TABLE.matcher(text);
        while (rename.find()) dropped.add(rename.group(1).toLowerCase(Locale.ROOT));
        return dropped;
    }

    private static Set<Column> droppedColumns(String text) {
        Set<Column> dropped = new HashSet<>();
        Matcher alter = ALTER_TABLE.matcher(text);
        while (alter.find()) {
            String table = alter.group(1).toLowerCase(Locale.ROOT);
            Matcher drop = DROP_COLUMN.matcher(alter.group(2));
            while (drop.find()) {
                String name = drop.group(1);
                if (NOT_A_COLUMN.contains(name.toUpperCase(Locale.ROOT))) continue;
                dropped.add(new Column(table, name.toLowerCase(Locale.ROOT)));
            }
        }
        return dropped;
    }

    private static String readResource(String script) {
        try (InputStream in = SchemaEvidence.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + script)) {
            if (in == null) return "";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MigrationException("Could not read packaged migration " + script, e);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            statement.setString(1, table);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
