# Rule: JDBC & Database Transaction Patterns

This rule governs how data persistence is handled in RailFlow.

## JDBC Rules
1. **Never build raw queries with String concatenation**:
   - Always use `PreparedStatement` with parameterized placeholders (`?`).
   - Prevents SQL injection and enables driver-level query caching.
2. **Resource Management**:
   - Use Java 7+ `try-with-resources` for all `Connection`, `PreparedStatement`, and `ResultSet` instances.
   ```java
   String sql = "SELECT * FROM trains WHERE id = ?";
   try (Connection conn = DatabaseConnectionPool.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
       stmt.setLong(1, trainId);
       try (ResultSet rs = stmt.executeQuery()) {
           if (rs.next()) {
               return mapRowToTrain(rs);
           }
       }
   }
   ```
3. **Transactions**:
   - Booking operations involve seat locks and payment records.
   - Always set `conn.setAutoCommit(false)` before multi-table operations.
   - On error, `conn.rollback()` must be called immediately.
4. **Data Types**:
   - Store monetary values using `BigDecimal` (`DECIMAL(10, 2)` in SQL), never floating-point `double` or `float`.
   - Store timestamps in UTC using `java.time.LocalDateTime` or `java.time.Instant`.
