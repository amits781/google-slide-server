package com.aidyn.slideshow.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.aidyn.slideshow.dto.GooglePhotoData;
import com.aidyn.slideshow.dto.ImageInfo;
import com.aidyn.slideshow.dto.ResponseObject;
import com.aidyn.slideshow.util.FileParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SlideService {

	@Value("${target.system}")
	private String targetSystem;

	@Autowired
	private FileParser parser;

	@Autowired
	private ObjectMapper objectMapper;

	private Set<String> parsedImageP = new HashSet<>();
	private Set<String> parsedImageW = new HashSet<>();
	private List<String> wideImage = new ArrayList<>();
	private List<String> portraitImage = new ArrayList<>();
	private Random random = new Random();

	@EventListener(ApplicationReadyEvent.class)
	public void initSystem() {
		if (targetSystem.equalsIgnoreCase("low")) {
			parser.parseImageLite();
		} else {
			parser.parseImage();
		}
		wideImage = parser.getWideImage();
		portraitImage = parser.getPortraitImage();
	}

	public ResponseObject getNextImage(String code) {
		AtomicBoolean isDebug = new AtomicBoolean(false);
		ResponseObject result = ResponseObject.builder().build();
		List<ImageInfo> images = new ArrayList<>();
		AtomicReference<List<ImageInfo>> atomicImages = new AtomicReference<>(images);
		AtomicReference<Set<String>> atomicPeople = new AtomicReference<>(new HashSet<>());
		List<String> imageName = null;
		switch (code) {
		case "D": {
			isDebug.set(true);
		}
		case "": {

			if ((random.nextInt(2) % 2) == 0) {
				imageName = getWideImage();
				result.setNumberOfImages(1);
			} else {
				imageName = getPortraitImage();
				result.setNumberOfImages(2);
			}
			break;
		}
		case "W": {

			imageName = getWideImage();
			result.setNumberOfImages(1);
			break;
		}
		case "P": {

			imageName = getPortraitImage();
			result.setNumberOfImages(2);
			break;
		}
		default:
			log.error("Invalid Code found: " + code);

		}

		imageName.forEach(name -> {
			if (name.contains("-edited")) {
				String ext = name.substring(name.lastIndexOf('.'), name.length());
				name = name.substring(0, name.indexOf("-edited"));
				name = name + ext;
			}

			File jsonFile = new File(parser.getImageBasePath() + "//" + name + ".json");
			try {
				GooglePhotoData photoData = objectMapper.readValue(jsonFile, GooglePhotoData.class);

				ImageInfo imageInfo = ImageInfo.builder().location(photoData.getGeoData().toString())
						.path(isDebug.get() ? "processed/" + name : name).allignVertical(photoData.getAllignVertical())
						.build();
				if (photoData.getPeople() != null && photoData.getPeople().size() > 0) {
					imageInfo.setPeople((photoData.getPeople().stream().map(people -> people.getName())
							.collect(Collectors.toList())));
					atomicPeople.get().addAll(imageInfo.getPeople());
				}
				atomicImages.get().add(imageInfo);
			} catch (IOException e) {
				log.error("Error in parsing json: " + name, e);

			}

		});
		result.setImages(atomicImages.get());
		result.setPeople(atomicPeople.get());
		return result;
	}

	public List<String> getWideImage() {
		if (parsedImageW.size() == wideImage.size()) {
			parsedImageW.clear();
		}
		String fileName = null;
		do {
			fileName = wideImage.get(random.nextInt(wideImage.size()));
		} while (parsedImageW.contains(fileName));
		parsedImageW.add(fileName);
		return Arrays.asList(fileName);
	}

	public List<String> getPortraitImage() {
		if (parsedImageP.size() == portraitImage.size()) {
			parsedImageP.clear();
		}
		String fileName1, fileName2 = null;
		do {
			fileName1 = portraitImage.get(random.nextInt(portraitImage.size()));
		} while (parsedImageP.contains(fileName1));
		parsedImageP.add(fileName1);
		if (parsedImageP.size() == portraitImage.size()) {
			parsedImageP.clear();
		}
		do {
			fileName2 = portraitImage.get(random.nextInt(portraitImage.size()));
		} while ((parsedImageP.contains(fileName2)) && !(fileName1.equals(fileName2)));
		parsedImageP.add(fileName2);
		return Arrays.asList(fileName1, fileName2);
	}
}
