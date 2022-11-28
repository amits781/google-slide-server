package com.aidyn.slideshow.dto;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseObject {

	Integer numberOfImages;
	List<ImageInfo> images;
	Set<String> people;
}
