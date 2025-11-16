package com.exemplo.auth.dto;

import java.util.List;

public class FolderDto {

    private Long id;
    private String name;
    private List<CardDto> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CardDto> getItems() { return items; }
    public void setItems(List<CardDto> items) { this.items = items; }
}

