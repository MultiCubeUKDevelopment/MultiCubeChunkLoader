package io.github.multicubeuk.multicubechunkloader.util;

import com.google.common.collect.ImmutableList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Modification of https://gist.github.com/evilmidget38/a5c971d2f2b2c3b3fb37 to work with the new UUID->name api
 *
 * @author https://github.com/evilmidget38
 * @author https://github.com/cnordbakk
 */
public class NameFetcher implements Callable<Map<UUID, String>> {
    private static final String PROFILE_URL = "https://api.mojang.com/user/profiles/%s/names";
    private final JSONParser jsonParser = new JSONParser();
    private final List<UUID> uuids;

    public NameFetcher(List<UUID> uuids) {
        this.uuids = ImmutableList.copyOf(uuids);
    }

    @Override
    public Map<UUID, String> call() throws Exception {
        Map<UUID, String> uuidStringMap = new HashMap<>();
        for (UUID uuid : uuids) {

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(String.format(PROFILE_URL, uuid.toString().replace("-", ""))).openConnection();
                JSONArray response = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));

                String name = null;

                for (Object entry : response) {
                    name = (String) ((JSONObject) entry).get("name");
                }

                uuidStringMap.put(uuid, name);
            } catch (Exception ex) {
                throw new IllegalStateException("Error fetching name history!", ex);
            }

        }
        return uuidStringMap;
    }
}