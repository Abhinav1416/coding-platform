package com.Abhinav.backend.features.authentication.dto;


import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityHeatmapDto {
    private LocalDate date;
    private int count;
}