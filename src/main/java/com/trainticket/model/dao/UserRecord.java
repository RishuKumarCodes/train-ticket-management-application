package com.trainticket.model.dao;

import com.trainticket.model.User;

/**
 * Internal record holding a domain User alongside its cryptographic credentials
 * for authentication verification in the data access layer.
 */
public record UserRecord(User user, String passwordHash, String salt) {
}
