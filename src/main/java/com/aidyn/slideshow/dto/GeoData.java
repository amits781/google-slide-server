package com.aidyn.slideshow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeoData {
	private int latitude;
	private int longitude;
	private int altitude;
	private int latitudeSpan;
	private int longitudeSpan;
}
