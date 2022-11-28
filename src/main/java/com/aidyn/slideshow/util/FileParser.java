package com.aidyn.slideshow.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.aidyn.slideshow.dto.GooglePhotoData;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Getter
public class FileParser {

	@Value("${image.path}")
	private String imageBasePath;

	@Autowired
	private ObjectMapper objectMapper;

	private File imageDirectory;
	private BufferedImage bimg;

	private List<String> wideImage = new ArrayList<>();
	private List<String> portraitImage = new ArrayList<>();

	private List<String> extentions = Arrays.asList(new String[] { "mp4", "m4v", "mpeg", "webm" });

	public void parseImage() {
		imageDirectory = new File(imageBasePath);

		CascadeClassifier faceDetector = new CascadeClassifier();
		try {
			File file = ResourceUtils.getFile("classpath:haarcascade_frontalface_alt.xml");
			log.info("File found: " + file.getAbsolutePath());

			faceDetector.load(file.getAbsolutePath());
		} catch (FileNotFoundException e) {
			log.error("File not found: classpath:haarcascade_frontalface_alt.xml");

		}

		Map<String, List<String>> images = Arrays.asList(imageDirectory.listFiles()).stream()
				.filter(f -> f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length())
						.equalsIgnoreCase("jpg"))
				.collect(Collectors.groupingBy(f -> isWideImage(f, faceDetector) ? "Wide" : "Portrait",
						Collectors.mapping(File::getName, Collectors.toList())));
		wideImage = images.get("Wide");
		portraitImage = images.get("Portrait");
		log.info("Init Done");

	}

	public void parseImageLite() {
		imageDirectory = new File(imageBasePath);

		Map<String, List<String>> images = Arrays.asList(imageDirectory.listFiles()).stream()
				.filter(f -> f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length())
						.equalsIgnoreCase("jpg"))
				.collect(Collectors.groupingBy(f -> isWideImage(f) ? "Wide" : "Portrait",
						Collectors.mapping(File::getName, Collectors.toList())));
		wideImage = images.get("Wide");
		portraitImage = images.get("Portrait");
		log.info("Init Done");

	}

	private Boolean isWideImage(File imageFile) {
		try {
			bimg = ImageIO.read(imageFile);
		} catch (IOException e) {
			log.error("error in reading image: " + imageFile.getAbsolutePath());

		}
		int width = bimg.getWidth();
		int height = bimg.getHeight();
		return width > height;
	}

	private Boolean isWideImage(File imageFile, CascadeClassifier faceDetector) {

		String filename = imageFile.getName();
		Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
		int imgHeigth = image.height();
		int imgWidth = image.width();
		File jsonFile = new File(imageBasePath + "//" + filename + ".json");
		GooglePhotoData photoData = null;
		try {
			photoData = objectMapper.readValue(jsonFile, GooglePhotoData.class);
			if (photoData.getIsProcessed()) {
				return imgWidth > imgHeigth;
			}
		} catch (IOException e1) {
			log.error("error in reading JSON file: " + filename + ".json");
			return imgWidth > imgHeigth;
		}

		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);

		String pos = "top";
		int min = imgHeigth;
		Point bottomLeft = new Point(min, min);
		Boolean faceFound = false;
		for (Rect rect : faceDetections.toArray()) {
			if (rect.height > 360 || rect.width > 360) {
				faceFound = true;
				Imgproc.rectangle(image, new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 8);
				if (rect.y < min) {
					min = rect.y;
					double points[] = { rect.x + 10, rect.y };
					bottomLeft.set(points);
					;
				}
			}

		}
		if (faceFound) {
			if (min < (image.height() / 4)) {

				pos = "top";
			} else if ((min > (image.height() / 4)) && (min < ((image.height() / 4) * 2))) {

				pos = "center";
			} else {
				pos = "bottom";
			}
		}

		try {

			photoData.setAllignVertical(pos);
			photoData.setIsProcessed(true);
			objectMapper.writeValue(jsonFile, photoData);

		} catch (IOException e) {
			log.error("Error in writting json: " + filename + ".json");

		}
		Imgproc.putText(image, pos, bottomLeft, 3, 4d, new Scalar(0, 255, 0), 8);

		Imgcodecs.imwrite(imageBasePath + "\\processed\\" + filename, image);

		return imgWidth > imgHeigth;
	}

}
