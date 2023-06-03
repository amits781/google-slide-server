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
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
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
		int index = 0;
		int length = imageDirectory.listFiles().length;
//		Map<String, List<String>> images = Arrays.asList(imageDirectory.listFiles()).stream()
//				.filter(f -> f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length())
//						.equalsIgnoreCase("jpg"))
//				.collect(Collectors.groupingBy(f -> isWideImage(f) ? "Wide" : "Portrait",
//						Collectors.mapping(File::getName, Collectors.toList())));
		for (File f : Arrays.asList(imageDirectory.listFiles())) {
			log.info("processing file: " + index++ + "/" + length);
			if (f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length()).equalsIgnoreCase("jpg")) {
				if (isWideImage(f)) {
					wideImage.add(f.getName());
				} else {
					portraitImage.add(f.getName());
				}
			}
		}
		log.info("Init Done");

	}

	private Boolean isWideImage(File imageFile) {
		boolean isInversed = false;
		int width = 0, height = 0;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
			ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (exifIFD0 != null) {
				int orientation = exifIFD0.getInt(ExifIFD0Directory.TAG_ORIENTATION);
				width = exifIFD0.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
				height = exifIFD0.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
				log.info("processing file: {}", imageFile.getAbsolutePath());
				log.info("orientation: {}", orientation);
				switch (orientation) {
				case 1: // [Exif IFD0] Orientation - Top, left side (Horizontal / normal)
					break;
				case 6: // [Exif IFD0] Orientation - Right side, top (Rotate 90 CW)
					isInversed = true;
					break;
				case 3: // [Exif IFD0] Orientation - Bottom, right side (Rotate 180)
					break;
				case 8: // [Exif IFD0] Orientation - Left side, bottom (Rotate 270 CW)
					isInversed = true;
					break;
				}
				if (isInversed) {
					width = exifIFD0.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
					height = exifIFD0.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
				}
			} else {
				bimg = ImageIO.read(imageFile);
				width = bimg.getWidth();
				height = bimg.getHeight();
			}
		} catch (IOException | ImageProcessingException | MetadataException e) {
			log.error("error in reading image: " + imageFile.getAbsolutePath());
			try {
				bimg = ImageIO.read(imageFile);
			} catch (IOException e1) {
				log.error("error in reading bimg image: " + imageFile.getAbsolutePath());
			}
			width = bimg.getWidth();
			height = bimg.getHeight();

		}

		log.info("imageWidth: {}", width);
		log.info("imageHeight: {}", height);
		log.info("isWide: {}", width > height);
		return width > height;
	}

	private Boolean isWideImage(File imageFile, CascadeClassifier faceDetector) {

		String filename = imageFile.getName();
		Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
		int imgHeigth = image.height();
		int imgWidth = image.width();
		File jsonFile = new File(imageBasePath + "//" + filename + ".json");
		log.debug("JSON FIle: {}", jsonFile.getAbsolutePath());
		log.debug("Image FIle Height: {}", image.height());
		GooglePhotoData photoData = null;
		try {
			photoData = objectMapper.readValue(jsonFile, GooglePhotoData.class);
			if (photoData.getIsProcessed()) {
				log.debug("Reprocessing: {}", filename);
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
