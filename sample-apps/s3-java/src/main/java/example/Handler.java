package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Handler value: example.Handler
public class Handler implements RequestHandler<S3Event, String> {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Logger logger = LoggerFactory.getLogger(Handler.class);
  private final String REGEX = ".*\\.([^\\.]*)";
  private final String JPG_TYPE = "jpg";
  private final String JPG_MIME = "image/jpeg";
  private final String PNG_TYPE = "png";
  private final String PNG_MIME = "image/png";
  @Override
  public String handleRequest(S3Event s3event, Context context) {
    try {
      S3EventNotificationRecord record = s3event.getRecords().get(0);
      
      String srcBucket = "my-sg-src-bucket";

      // Object key may have spaces or unicode non-ASCII characters.
      String srcKey = record.getS3().getObject().getUrlDecodedKey();

      String dstBucket = "my-sg-dest-bucket";
      String dstKey = "somekey";

      // Infer the image type.
      Matcher matcher = Pattern.compile(REGEX).matcher(srcKey);
      if (!matcher.matches()) {
          logger.info("Unable to infer image type for key " + srcKey);
          return "";
      }
      String imageType = matcher.group(1);
      if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
          logger.info("Skipping non-image " + srcKey);
          return "";
      }

      // Download the image from S3 into a stream
      S3Client s3Client = S3Client.builder().build();
      InputStream s3Object = getObject(s3Client, srcBucket, srcKey);

      // Read the source image and resize it
      BufferedImage srcImage = ImageIO.read(s3Object);
      BufferedImage newImage = srcImage;

      // Re-encode image to target format
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(newImage, imageType, outputStream);

      // Upload new image to S3
      putObject(s3Client, outputStream, dstBucket, dstKey, imageType);

      logger.info("Successfully resized " + srcBucket + "/"
              + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
      return "Ok";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private InputStream getObject(S3Client s3Client, String bucket, String key) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build();
    return s3Client.getObject(getObjectRequest);
  }

  private void putObject(S3Client s3Client, ByteArrayOutputStream outputStream,
    String bucket, String key, String imageType) {
      Map<String, String> metadata = new HashMap<>();
      metadata.put("Content-Length", Integer.toString(outputStream.size()));
      if (JPG_TYPE.equals(imageType)) {
        metadata.put("Content-Type", JPG_MIME);
      } else if (PNG_TYPE.equals(imageType)) {
        metadata.put("Content-Type", PNG_MIME);
      }

      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .metadata(metadata)
        .build();

      // Uploading to S3 destination bucket
      logger.info("Writing to: " + bucket + "/" + key);
      try {
        s3Client.putObject(putObjectRequest,
          RequestBody.fromBytes(outputStream.toByteArray()));
      }
      catch(AwsServiceException e)
      {
        logger.error(e.awsErrorDetails().errorMessage());
        System.exit(1);
      }
  }

    public File createFileNonCompliant(File outputFolder, final String fileName) throws IOException {
        File file = new File(outputFolder, fileName);
        // Noncompliant: does not check if createNewFile succeeded or failed.
        file.createNewFile();
        return file;
    }
}