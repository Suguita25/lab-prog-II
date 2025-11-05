// dto/AddCardManualRequest.java
package com.exemplo.auth.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCardManualRequest(
        @NotNull Long folderId,
        @NotBlank String cardName
) {}
