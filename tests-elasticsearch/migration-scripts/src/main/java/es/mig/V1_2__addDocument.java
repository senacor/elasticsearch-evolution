package es.mig;

import com.senacor.elasticsearch.evolution.core.api.migration.java.Context;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstraction.HttpMethod;

import java.util.Map;

public class V1_2__addDocument implements JavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        String body = """
                {
                  "searchable": {
                    "version": "2",
                    "locked": false
                  },
                  "doc": {
                    "version": "2",
                    "locked": false,
                    "success": true,
                    "a": "a a a",
                    "b": true,
                    "c": "c"
                  }
                }""";
        final EvolutionRestResponse res = context.getEvolutionRestClient().execute(HttpMethod.PUT,
                "/%s/_doc/2?refresh".formatted(context.getConfiguration().getPlaceholders().get("index")),
                Map.of("Content-Type", "application/json"),
                null,
                body);
        if (res.statusCode() != 201) {
            throw new IllegalArgumentException("Failed to add document. " + res.asString());
        }
    }
}
