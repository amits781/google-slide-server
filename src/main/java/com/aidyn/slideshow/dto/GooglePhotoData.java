package com.aidyn.slideshow.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GooglePhotoData {
	private String title;
	private String description;
	private String imageViews;
	private CreationTime creationTime;
	private PhotoTakenTime photoTakenTime;
	private GeoData geoData;
	private GeoDataExif geoDataExif;
	private List<People> people;
	private GooglePhotosOrigin googlePhotosOrigin;
	private String allignVertical;
	@Builder.Default
	private Boolean isProcessed = false;
}
