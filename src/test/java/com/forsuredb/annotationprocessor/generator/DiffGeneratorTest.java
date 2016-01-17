/*
   forsuredbcompiler, an annotation processor and code generator for the forsuredb project

   Copyright 2015 Ryan Scott

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.forsuredb.annotationprocessor.generator;

import com.forsuredb.annotationprocessor.TableContext;
import com.forsuredb.migration.Migration;

import com.forsuredb.migration.MigrationSet;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.forsuredb.TestData.*;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DiffGeneratorTest {

    private MigrationSet actualMigrationSet;

    private final int sourceDbVersion;
    private final TableContext migrationContext;
    private final TableContext processingContext;
    private final MigrationSet expectedMigrationSet;

    public DiffGeneratorTest(int sourceDbVersion,
                             TableContext migrationContext,
                             TableContext processingContext,
                             MigrationSet expectedMigrationSet) {
        this.sourceDbVersion = sourceDbVersion;
        this.migrationContext = migrationContext;
        this.processingContext = processingContext;
        this.expectedMigrationSet = expectedMigrationSet;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // No diff (identical contexts)
                {   // 0
                        4,
                        tableContextBase().build(),
                        tableContextBase().build(),
                        MigrationSet.builder().dbVersion(5)
                                .orderedMigrations(new ArrayList<Migration>())
                                .targetSchema(tableContextBase().build().tableMap())
                                .build()
                },
                // The processing context has a table that the migration context does not have--table has no extra columns
                {   // 1
                        1,
                        tableContextBase().build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build(),
                        MigrationSet.builder().dbVersion(2)
                                .orderedMigrations(Lists.newArrayList(createTableMigration("table_2")))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build().tableMap())
                                .build()
                },
                // The processing context has a table that the migration context does not have--table has extra columns
                {   // 2
                        10,
                        tableContextBase().build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().build(), intCol().build()).build()).build(),
                        MigrationSet.builder().dbVersion(11)
                                .orderedMigrations(Lists.newArrayList(createTableMigration("table_2"),
                                        addColumnMigration("table_2", stringCol().build().getColumnName()),
                                        addColumnMigration("table_2", intCol().build().getColumnName())))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().build(), intCol().build()).build()).build().tableMap())
                                .build()
                },
                // The processing context has a non-unique, non foreign-key column that the migration context does not have
                {   // 3
                        3,
                        tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", longCol().build()).build()).build(),
                        MigrationSet.builder().dbVersion(4)
                                .orderedMigrations(Lists.newArrayList(addColumnMigration("table_2", longCol().build().getColumnName())))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2", longCol().build()).build()).build().tableMap()    )
                                .build()
                },
                // The processing context has a foreign key the migration context does not know about (default delete and update actions)
                {   // 4
                        2,
                        tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", longCol().foreignKeyInfo(cascadeFKI("_id").build()).build()).build()).build(),
                        MigrationSet.builder().dbVersion(3)
                                .orderedMigrations(Lists.newArrayList(addForeignKeyMigration("table_2", longCol().build().getColumnName())))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2", longCol().foreignKeyInfo(cascadeFKI("_id").build()).build()).build()).build().tableMap())
                                .build()
                },
                // The processing context has a unique index column the migration context doesn't know about
                {   // 5
                        4364,
                        tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().unique(true).build()).build()).build(),
                        MigrationSet.builder().dbVersion(4365)
                                .orderedMigrations(Lists.newArrayList(addUniqueColumnMigration("table_2", stringCol().build().getColumnName())))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().unique(true).build()).build()).build().tableMap())
                                .build()
                },
                // The processing context has a unique index on a column the migration context knows about, but doesn't know is unique
                {   // 6
                        8,
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().build()).build()).build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().unique(true).build()).build()).build(),
                        MigrationSet.builder().dbVersion(9)
                                .orderedMigrations(Lists.newArrayList(addUniqueIndexMigration("table_2", stringCol().build().getColumnName())))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().unique(true).build()).build()).build().tableMap())
                                .build()
                },
                // The processing context does not have a table the migration context knows about (a table deletion)
                {   // 7
                        47,
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().columnName("table_2_string").build()).build()).build(),
                        tableContextBase().build(),
                        MigrationSet.builder().dbVersion(48)
                                .orderedMigrations(Lists.newArrayList(dropTableMigration("table_2")))
                                .targetSchema(tableContextBase().build().tableMap())
                                .build()
                },
                // The processing context does not have a column the migration context knows about (a column deletion)
                {   // 8
                        91,
                        tableContextBase(table("table_2", "com.fsryan.test.Table2", stringCol().columnName("table_2_string").build()).build()).build(),
                        tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build(),
                        MigrationSet.builder().dbVersion(92)
                                .orderedMigrations(Lists.newArrayList(dropColumnMigration("table_2", "table_2_string")))
                                .targetSchema(tableContextBase(table("table_2", "com.fsryan.test.Table2").build()).build().tableMap())
                                .build()
                }
        });
    }

    @Before
    public void setUp() {
        actualMigrationSet = new DiffGenerator(migrationContext, sourceDbVersion).analyzeDiff(processingContext);
    }

    @Test
    public void shouldHaveCorrectTargetDbVersion() {
        assertEquals(expectedMigrationSet.getDbVersion(), actualMigrationSet.getDbVersion());
    }

    @Test
    public void shouldHaveCorrectNumberOfMigrations() {
        assertEquals(expectedMigrationSet.getOrderedMigrations().size(), actualMigrationSet.getOrderedMigrations().size());
    }

    @Test
    public void shouldMatchMigrationsInOrderAndContent() {
        final List<Migration> expected = expectedMigrationSet.getOrderedMigrations();
        final List<Migration> actual = actualMigrationSet.getOrderedMigrations();
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("migration index " + i, expected.get(i), actual.get(i));
        }
    }

    @Test
    public void shouldContainTargetContext() {
        assertEquals(processingContext.tableMap(), actualMigrationSet.getTargetSchema());
    }
}
