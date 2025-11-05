// dto/CreateFolderRequest.java
package com.exemplo.auth.dto;
import jakarta.validation.constraints.NotBlank;

public record CreateFolderRequest(@NotBlank String name) {}
