package com.Abhinav.backend.features.authentication.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolvesByTagDto {
    private String tagName;
    private long solvedCount;


    public interface SolvesByTagProjection {
        String getTagName();
        Long getSolvedCount();
    }
}
