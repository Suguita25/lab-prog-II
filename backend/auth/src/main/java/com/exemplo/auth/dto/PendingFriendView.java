package com.exemplo.auth.dto;

public record PendingFriendView(
        Long friendshipId,
        Long requesterId,
        String requesterUsername,
        String requesterEmail
) {}

