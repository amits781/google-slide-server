package com.aidyn.slideshow.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {

	String path;
	@JsonIgnore
	List<String> people;
	String location;
	String allignVertical;
}
