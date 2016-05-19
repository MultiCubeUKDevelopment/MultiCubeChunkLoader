package io.github.multicubeuk.multicubechunkloader.database;

import io.github.multicubeuk.multicubechunkloader.ChunkLoaderConfiguration;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;
import io.github.multicubeuk.multicubechunkloader.util.UUIDUtils;
import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MySqlDataProvider extends DataProvider
{

    private final ChunkLoaderConfiguration config;
    private Connection connection;

    public MySqlDataProvider(MultiCubeChunkLoader plugin) {
        super(plugin);

        this.config = plugin.getConfiguration();

        try {
            initializeDatabase();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize chunk loader database!", ex);
            throw new IllegalArgumentException("Server or database is invalid!", ex);
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error destructing DataProvider!", ex);
        }

        plugin = null;

        super.finalize();
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && this.connection.isValid(100))
            return this.connection;

        String cs = String.format(
                "jdbc:mysql://%s:%s/%s",
                config.getMySqlServer(),
                config.getMySqlPort(),
                config.getMySqlDatabase()
        );

        plugin.getLogger().info("Connecting to database with following values:");
        plugin.getLogger().info(cs);

        this.connection = DriverManager.getConnection(
                cs,
                config.getMySqlUsername(),
                config.getMySqlPassword()
        );

        return this.connection;
    }

    @Override
    public PlayerInfo getPlayerInfo(UUID uuid) {
        try {
            // Check if player exists
            PreparedStatement ps = getConnection().prepareStatement(String.format("select * from %splayer where player_uuid = ?", config.getMySqlTablePrefix()));
            ps.setString(1, uuid.toString());

            ResultSet r = ps.executeQuery();

            PlayerInfo playerInfo;

            if (r.next()) {
                // Player exists
                playerInfo = new PlayerInfo(
                        uuid,
                        r.getString("player_name"),
                        r.getInt("world_chunks"),
                        r.getInt("personal_chunks")
                );

            } else { // Player does not exist
                OfflinePlayer op = UUIDUtils.getPlayer(uuid);

                int initialWorldChunks = 0;
                int initialPersonalChunks = 0;

                if (op.isOnline()) {
                    Player player = op.getPlayer();

                    for (Map.Entry<String, Integer> entry : config.getPersonalChunks().entrySet()) {
                        if ((player.hasPermission(String.format("multicubechunkloader.group.%s", entry.getKey()))) || entry.getKey().equalsIgnoreCase("default")) {
                            initialPersonalChunks = (entry.getValue() > initialPersonalChunks && initialPersonalChunks > -1) || entry.getValue() == -1 ? entry.getValue() : initialPersonalChunks;
                        }
                    }

                    for (Map.Entry<String, Integer> entry : config.getWorldChunks().entrySet()) {
                        if ((player.hasPermission(String.format("multicubechunkloader.group.%s", entry.getKey()))) || entry.getKey().equalsIgnoreCase("default")) {
                            initialWorldChunks = (entry.getValue() > initialWorldChunks && initialWorldChunks > -1) || entry.getValue() == -1 ? entry.getValue() : initialWorldChunks;
                        }
                    }
                } else {
                    initialPersonalChunks = config.getPersonalChunks().get("default");
                    initialWorldChunks = config.getWorldChunks().get("default");
                }

                ps = getConnection().prepareStatement(String.format("insert into %splayer (player_uuid, player_name, world_chunks, personal_chunks, created_date, last_updated_date) values(?,?,?,?,?,?)", config.getMySqlTablePrefix()));
                ps.setString(1, uuid.toString());
                ps.setString(2, op.getName());
                ps.setInt(3, initialWorldChunks);
                ps.setInt(4, initialPersonalChunks);
                ps.setTimestamp(5, now());
                ps.setTimestamp(6, now());

                ps.execute();
                ps.close();

                playerInfo = new PlayerInfo(uuid, op.getName(), initialWorldChunks, initialPersonalChunks);
            }

            r.close();
            return playerInfo;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while loading player info!", ex);
            return null;
        }
    }

    @Override
    public void updatePlayerInfo(PlayerInfo playerInfo) {
        PlayerInfo oldPi = getPlayerInfo(playerInfo.getUuid());

        try {
            if (oldPi == null) {
                PreparedStatement ps = getConnection().prepareStatement(String.format("insert into %splayer (player_uuid, player_name, world_chunks, personal_chunks, created_date, last_updated_date) values(?,?,?,?,?,?)", config.getMySqlTablePrefix()));
                ps.setString(1, playerInfo.getUuid().toString());
                ps.setString(2, playerInfo.getName());
                ps.setInt(3, playerInfo.getWorldChunks());
                ps.setInt(4, playerInfo.getPersonalChunks());
                ps.setTimestamp(5, now());
                ps.setTimestamp(6, now());

                ps.execute();
                ps.close();
            } else {
                PreparedStatement ps = getConnection().prepareStatement(String.format("update %splayer set player_name = ?, world_chunks = ?, personal_chunks = ?, last_updated_date = ? where player_uuid = ?", config.getMySqlTablePrefix()));
                ps.setString(1, playerInfo.getName());
                ps.setInt(2, playerInfo.getWorldChunks());
                ps.setInt(3, playerInfo.getPersonalChunks());
                ps.setTimestamp(4, now());
                ps.setString(5, playerInfo.getUuid().toString());
                ps.execute();
                ps.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error while updating player info!", ex);
        }
    }

    @Override
    public List<ChunkLoader> getPersonalChunks(UUID uuid) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select loaded_chunk_id, owner, chunk_type, location, chunk_count, created_date from %sloaded_chunk where chunk_type = 'personal' and owner = ?", config.getMySqlTablePrefix()));
            ps.setString(1, uuid.toString());
            ResultSet result = ps.executeQuery();
            List<ChunkLoader> chunks = new ArrayList<>();

            while (result.next()) {
                ChunkLoader lc = new ChunkLoader(
                        plugin,
                        result.getInt("loaded_chunk_id"),
                        UUID.fromString(result.getString("owner")),
                        ChunkLoader.ChunkType.PERSONAL,
                        result.getString("location"),
                        result.getInt("chunk_count"),
                        result.getDate("created_date")
                );

                chunks.add(lc);
            }

            result.close();
            return chunks;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while loading personal chunks!", ex);
            return null;
        }
    }

    @Override
    public List<ChunkLoader> getPersonalChunks(String world) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select loaded_chunk_id, owner, chunk_type, location, chunk_count, created_date from %sloaded_chunk where chunk_type = 'personal' and world = ?", config.getMySqlTablePrefix()));
            ps.setString(1, world);
            ResultSet result = ps.executeQuery();
            List<ChunkLoader> chunks = new ArrayList<>();

            while (result.next()) {
                ChunkLoader lc = new ChunkLoader(
                        plugin,
                        result.getInt("loaded_chunk_id"),
                        UUID.fromString(result.getString("owner")),
                        ChunkLoader.ChunkType.PERSONAL,
                        result.getString("location"),
                        result.getInt("chunk_count"),
                        result.getDate("created_date")
                );

                chunks.add(lc);
            }

            result.close();
            return chunks;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while loading personal chunks!", ex);
            return null;
        }
    }

    @Override
    public List<ChunkLoader> getPersonalChunks() {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select loaded_chunk_id, owner, chunk_type, location, chunk_count, created_date from %sloaded_chunk where chunk_type = 'personal'", config.getMySqlTablePrefix()));
            ResultSet result = ps.executeQuery();
            List<ChunkLoader> chunks = new ArrayList<>();

            while (result.next()) {
                ChunkLoader lc = new ChunkLoader(
                        plugin,
                        result.getInt("loaded_chunk_id"),
                        UUID.fromString(result.getString("owner")),
                        ChunkLoader.ChunkType.PERSONAL,
                        result.getString("location"),
                        result.getInt("chunk_count"),
                        result.getDate("created_date")
                );

                chunks.add(lc);
            }

            result.close();
            return chunks;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while loading personal chunks!", ex);
            return null;
        }
    }

    @Override
    public ChunkLoader getChunkLoader(int id) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select loaded_chunk_id, owner, chunk_type, location, chunk_count, created_date from %sloaded_chunk where loaded_chunk_id = ?", config.getMySqlTablePrefix()));
            ps.setInt(1, id);
            ResultSet result = ps.executeQuery();
            List<ChunkLoader> chunks = new ArrayList<>();

            while (result.next()) {
                ChunkLoader lc = new ChunkLoader(
                        plugin,
                        result.getInt("loaded_chunk_id"),
                        UUID.fromString(result.getString("owner")),
                        ChunkLoader.ChunkType.PERSONAL,
                        result.getString("location"),
                        result.getInt("chunk_count"),
                        result.getDate("created_date")
                );

                chunks.add(lc);
            }

            result.close();

            if (chunks.size() == 1)
                return chunks.get(0);

            return null;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while loading personal chunks!", ex);
            return null;
        }
    }

    @Override
    public List<ChunkLoader> getWorldChunks() {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select loaded_chunk_id, owner, chunk_type, location, chunk_count, created_date from %sloaded_chunk where chunk_type in ('world', 'creative')", config.getMySqlTablePrefix()));
            ResultSet result = ps.executeQuery();
            List<ChunkLoader> chunks = new ArrayList<>();

            while (result.next()) {
                ChunkLoader lc = new ChunkLoader(
                        plugin,
                        result.getInt("loaded_chunk_id"),
                        UUID.fromString(result.getString("owner")),
                        ChunkLoader.ChunkType.valueOf(result.getString("chunk_type").toUpperCase()),
                        result.getString("location"),
                        result.getInt("chunk_count"),
                        result.getDate("created_date")
                );

                chunks.add(lc);
            }

            result.close();
            return chunks;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while loading world/creative chunks!", ex);
            return null;
        }
    }

    @Override
    public void saveChunkLoader(ChunkLoader chunk) {
        try {
            if (chunk.getId() == -1) {
                PreparedStatement ps = getConnection().prepareStatement(String.format("insert into %sloaded_chunk (owner, chunk_type, location, chunk_count, created_date, last_updated_date) values (?,?,?,?,?,?)", config.getMySqlTablePrefix()), Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, chunk.getOwner().toString());
                ps.setString(2, chunk.getChunkType().toString().toLowerCase());
                ps.setString(3, chunk.getLocationString());
                ps.setInt(4, chunk.getSize());
                ps.setTimestamp(5, now());
                ps.setTimestamp(6, now());

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                int id = rs.getInt(1);

                plugin.getLogger().info("Saved new chunk loader and returned ID:" + id);
                chunk.setId(id);
            } else {
                PreparedStatement ps = getConnection().prepareStatement(String.format("update %sloaded_chunk set chunk_count = ?, last_updated_date = ? where loaded_chunk_id = ?", config.getMySqlTablePrefix()));
                ps.setInt(1, chunk.getSize());
                ps.setTimestamp(2, now());
                ps.setInt(3, chunk.getId());

                ps.execute();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception while saving chunk!", ex);
        }

    }

    @Override
    public ChunkLoader getChunkLoaderAtLocation(String locationString) {
        plugin.getLogger().info("Searching database for chunk loader at location " + locationString);
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select * from %sloaded_chunk where location = ?", config.getMySqlTablePrefix()));
            ps.setString(1, locationString);

            ResultSet result = ps.executeQuery();

            if (result.next()) {


                ChunkLoader cl = new ChunkLoader(
                        plugin,
                        result.getInt("loaded_chunk_id"),
                        UUID.fromString(result.getString("owner")),
                        ChunkLoader.ChunkType.valueOf(result.getString("chunk_type").toUpperCase()),
                        locationString,
                        result.getInt("chunk_count"),
                        result.getDate("created_date")
                );

                result.close();
                return cl;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error loading ChunkLoader from location string!", ex);
        }
        return null;
    }

    @Override
    public void deleteChunkLoader(ChunkLoader chunkLoader) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("delete from %sloaded_chunk where loaded_chunk_id = ?", config.getMySqlTablePrefix()));
            ps.setInt(1, chunkLoader.getId());
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, String.format("Error while deleting ChunkLoader at location: %s", chunkLoader.getLocationString()));
            return;
        }
        plugin.getLogger().info("Successfully deleted chunk loader!");
    }

    @Override
    public UUID getUUID(String name) {
        try {
            PreparedStatement ps = getConnection().prepareStatement(String.format("select player_uuid from %splayer where player_name = ?", config.getMySqlTablePrefix()));
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return UUID.fromString(rs.getString("player_uuid"));

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error while looking up UUID!", ex);
        }
        return null;
    }

    private Timestamp now() {
        return new Timestamp(new java.util.Date().getTime());
    }

    private void initializeDatabase() throws SQLException {
        Connection conn = this.getConnection();
        Statement stmt = conn.createStatement();

        // Players
        stmt.execute(String.format(
                        "CREATE TABLE IF NOT EXISTS `%splayer` (" +
                                "`player_uuid` char(36) NOT NULL," +
                                "`player_name` varchar(32) DEFAULT NULL," +
                                "`world_chunks` int(11) NOT NULL DEFAULT '0'," +
                                "`personal_chunks` int(11) NOT NULL DEFAULT '0'," +
                                "`created_date` timestamp NOT NULL," +
                                "`last_updated_date` timestamp NOT NULL," +
                                "        PRIMARY KEY (`player_uuid`)," +
                                "        UNIQUE KEY `player_uuid_UNIQUE` (`player_uuid`)" +
                                ");"
                        , config.getMySqlTablePrefix())
        );

        // Chunks
        stmt.execute(String.format(
                        "CREATE TABLE IF NOT EXISTS `%sloaded_chunk` (" +
                                "`loaded_chunk_id` int(11) NOT NULL AUTO_INCREMENT," +
                                "`owner` char(36) NOT NULL," +
                                "`chunk_type` varchar(10) NOT NULL," +
                                "`location` varchar(255) NOT NULL," +
                                "`chunk_count` int(11) NOT NULL DEFAULT 1," +
                                "`created_date` timestamp NOT NULL," +
                                "`last_updated_date` timestamp NOT NULL," +
                                "        PRIMARY KEY (`loaded_chunk_id`)" +
                                ");"
                        , config.getMySqlTablePrefix())
        );
    }
}
