package com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.instantiationfailed;

import com.senacor.elasticsearch.evolution.core.api.migration.java.Context;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;

public class V1_2__Failing_instantiation implements JavaMigration {
    public V1_2__Failing_instantiation() {
        throw new IllegalArgumentException("just failing instantiation");
    }

    @Override
    public void migrate(Context context) throws Exception {
        // do nothing
    }
}
