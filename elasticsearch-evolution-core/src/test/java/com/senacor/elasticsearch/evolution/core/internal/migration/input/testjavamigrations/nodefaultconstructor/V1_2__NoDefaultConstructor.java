package com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.nodefaultconstructor;

import com.senacor.elasticsearch.evolution.core.api.migration.java.Context;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;

public class V1_2__NoDefaultConstructor implements JavaMigration {
    public V1_2__NoDefaultConstructor(String someParameter) {
        System.out.println(someParameter);
    }

    @Override
    public void migrate(Context context) throws Exception {
        // do nothing
    }
}
