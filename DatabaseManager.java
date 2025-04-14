import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/databasemanager";
    private static final String USER = "postgres";
    private static final String PASSWORD = "9148";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected!");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database.");
            e.printStackTrace();
        }
    }

    public void addPlayer(String name) throws SQLException {
        // Check if player already exists
        String sql = "SELECT id FROM players WHERE name = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            // If player doesn't exist, add them
            String insertSql = "INSERT INTO players (name) VALUES (?)";
            PreparedStatement insertStmt = connection.prepareStatement(insertSql);
            insertStmt.setString(1, name);
            insertStmt.executeUpdate();
            System.out.println("Player added: " + name);
        } else {
            System.out.println("Player already exists: " + name);
        }
    }

    public void updateHighScore(String name, int score) throws SQLException {
        // Check the current high score
        String sql = "SELECT high_score FROM players WHERE name = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int currentHighScore = rs.getInt("high_score");

            // Only update if the new score is higher
            if (score > currentHighScore) {
                String updateSql = "UPDATE players SET high_score = ? WHERE name = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                updateStmt.setInt(1, score);
                updateStmt.setString(2, name);
                updateStmt.executeUpdate();
                System.out.println("High score updated for " + name + ": " + score);
            } else {
                System.out.println("Score not high enough to update for " + name);
            }
        }
    }

    public void addToLeaderboard(String name, int score) throws SQLException {
        System.out.println("Inserting leaderboard entry for: " + name + " with score: " + score);

        // Get the player ID
        String getIdSql = "SELECT id FROM players WHERE name = ?";
        PreparedStatement getIdStmt = connection.prepareStatement(getIdSql);
        getIdStmt.setString(1, name);
        ResultSet rs = getIdStmt.executeQuery();

        if (rs.next()) {
            int playerId = rs.getInt("id");

            // Insert score into leaderboard
            String insertSql = "INSERT INTO leaderboard (player_id, score) VALUES (?, ?)";
            PreparedStatement insertStmt = connection.prepareStatement(insertSql);
            insertStmt.setInt(1, playerId);
            insertStmt.setInt(2, score);
            insertStmt.executeUpdate();
            System.out.println("Leaderboard entry added!");
        } else {
            System.out.println("Player not found in database: " + name);
        }
    }

    public ResultSet getTopPlayers(int limit) throws SQLException {
        // Create a scrollable, insensitive ResultSet
        String sql = "SELECT players.name, leaderboard.score FROM leaderboard " +
                     "JOIN players ON leaderboard.player_id = players.id " +
                     "ORDER BY score DESC LIMIT ?";
        PreparedStatement stmt = connection.prepareStatement(sql, 
                                 ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, limit);
        return stmt.executeQuery();
    }   

    public void close() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection.");
            e.printStackTrace();
        }
    }
}
