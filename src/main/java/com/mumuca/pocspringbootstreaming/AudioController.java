package com.mumuca.pocspringbootstreaming;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class AudioController {

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("classpath:audios/never-gonna-give-you-up.mp3")
    private String MP3_RESOURCE_PATH;

    @GetMapping("/music/stream")
    public ResponseEntity<InputStreamResource> streamMp3(
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            File mp3File = resourceLoader.getResource(this.MP3_RESOURCE_PATH).getFile();

            if (!mp3File.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            long fileLength = mp3File.length();
            long start = 0;
            long end = fileLength - 1;

            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                HttpRange range = HttpRange.parseRanges(rangeHeader).getFirst();
                start = range.getRangeStart(fileLength);
                end = range.getRangeEnd(fileLength);
            }

            long contentLength = end - start + 1;
            InputStream inputStream = new BufferedInputStream(new FileInputStream(mp3File));
            inputStream.skip(start);

            HttpHeaders headers = createHttpHeaders(start, end, fileLength, contentLength);

            return ResponseEntity
                    .status(rangeHeader == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private HttpHeaders createHttpHeaders(long start, long end, long fileLength, long contentLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "audio/mpeg");
        headers.set("Accept-Ranges", "bytes");
        headers.setContentLength(contentLength);
        headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        return headers;
    }
}
