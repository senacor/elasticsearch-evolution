package com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.nopublicdefaultconstructor;

import com.senacor.elasticsearch.evolution.core.api.migration.java.Context;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;

public class V1_2__NoPublicDefaultConstructor implements JavaMigration {
    V1_2__NoPublicDefaultConstructor() {
        // package-private constructor
    }

    @Override
    public void migrate(Context context) throws Exception {
        // do nothing
    }
}
