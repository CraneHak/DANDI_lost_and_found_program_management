package org.example.vision;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Word;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class GoogleVisionClient implements VisionClient {
    private final ColorNameResolver colorNameResolver;

    public GoogleVisionClient(ColorNameResolver colorNameResolver) {
        this.colorNameResolver = colorNameResolver;
    }

    @Override
    public VisionAnalysisResult analyze(byte[] imageBytes, boolean includeOcr) throws IOException {
        Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();

        List<Feature> features = new ArrayList<>();
        features.add(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION).build());
        features.add(Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build());
        if (includeOcr) {
            features.add(Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build());
        }

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addAllFeatures(features)
                .build();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(List.of(request));
            AnnotateImageResponse response = batchResponse.getResponses(0);
            if (response.hasError()) {
                throw new IOException("Vision API error: " + response.getError().getMessage());
            }

            List<String> objects = response.getLocalizedObjectAnnotationsList().stream()
                    .map(LocalizedObjectAnnotation::getName)
                    .distinct()
                    .toList();

            List<String> colors = response.getImagePropertiesAnnotation()
                    .getDominantColors()
                    .getColorsList()
                    .stream()
                    .sorted(Comparator.comparingDouble(ColorInfo::getPixelFraction).reversed())
                    .limit(5)
                    .map(color -> {
                        int red = Math.round(color.getColor().getRed());
                        int green = Math.round(color.getColor().getGreen());
                        int blue = Math.round(color.getColor().getBlue());
                        return colorNameResolver.resolveName(red, green, blue);
                    })
                    .distinct()
                    .toList();

            String rawText = "";
            List<OcrBox> ocrBoxes = List.of();
            if (includeOcr) {
                rawText = response.getTextAnnotationsCount() > 0
                        ? response.getTextAnnotations(0).getDescription()
                        : "";
                ocrBoxes = extractOcrBoxes(response);
            }

            return new VisionAnalysisResult(objects, colors, rawText, ocrBoxes);
        }
    }

    private List<OcrBox> extractOcrBoxes(AnnotateImageResponse response) {
        if (!response.hasFullTextAnnotation()) {
            return List.of();
        }

        List<OcrBox> boxes = new ArrayList<>();
        TextAnnotation annotation = response.getFullTextAnnotation();
        for (Page page : annotation.getPagesList()) {
            for (Block block : page.getBlocksList()) {
                for (Paragraph paragraph : block.getParagraphsList()) {
                    for (Word word : paragraph.getWordsList()) {
                        StringBuilder textBuilder = new StringBuilder();
                        for (Symbol symbol : word.getSymbolsList()) {
                            textBuilder.append(symbol.getText());
                        }
                        String text = textBuilder.toString();
                        if (text.isBlank()) {
                            continue;
                        }
                        int left = Integer.MAX_VALUE;
                        int top = Integer.MAX_VALUE;
                        int right = Integer.MIN_VALUE;
                        int bottom = Integer.MIN_VALUE;
                        for (com.google.cloud.vision.v1.Vertex vertex : word.getBoundingBox().getVerticesList()) {
                            left = Math.min(left, vertex.getX());
                            top = Math.min(top, vertex.getY());
                            right = Math.max(right, vertex.getX());
                            bottom = Math.max(bottom, vertex.getY());
                        }
                        if (left != Integer.MAX_VALUE && top != Integer.MAX_VALUE
                                && right > left && bottom > top) {
                            boxes.add(new OcrBox(left, top, right, bottom, text));
                        }
                    }
                }
            }
        }
        return boxes;
    }
}
