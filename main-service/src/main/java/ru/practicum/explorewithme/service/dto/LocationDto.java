package ru.practicum.explorewithme.service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explorewithme.service.model.Location;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    @NotNull(message = "Широта не может быть пустой")
    @DecimalMin(value = "-90.0", message = "Широта должна быть не менее -90")
    @DecimalMax(value = "90.0", message = "Широта должна быть не более 90")
    private Double lat;

    @NotNull(message = "Долгота не может быть пустой")
    @DecimalMin(value = "-180.0", message = "Долгота должна быть не менее -180")
    @DecimalMax(value = "180.0", message = "Долгота должна быть не более 180")
    private Double lon;

    public Location toEntity() {
        return Location.builder()
                .lat(lat)
                .lon(lon)
                .build();
    }

    public static LocationDto fromEntity(Location entityLocation) {
        if (entityLocation == null) {
            return null;
        }
        return new LocationDto(entityLocation.getLat(), entityLocation.getLon());
    }
}