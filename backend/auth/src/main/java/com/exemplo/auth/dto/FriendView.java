package com.exemplo.auth.dto;

public record FriendView(
        Long friendshipId,
        Long userId,
        String username,
        String email
) {}
