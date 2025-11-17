package com.exemplo.auth.dto;

public record FriendView(
        Long friendshipId,
        Long friendId,
        String friendUsername,
        String friendEmail,
        String friendAvatarUrl
) {}
