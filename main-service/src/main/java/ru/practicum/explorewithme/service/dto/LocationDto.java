package ru.practicum.explorewithme.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explorewithme.service.model.Location;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    private Double lat;
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