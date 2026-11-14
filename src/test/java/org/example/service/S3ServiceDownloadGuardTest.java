package org.example.service;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/** 서버가 사진을 읽을 때, 우리 버킷 주소가 아니면 아예 열지 않는다. */
class S3ServiceDownloadGuardTest {

    private static final String BUCKET = "dandi-lost-items";
    private static final String REGION = "ap-northeast-2";
    private static final String OWN = "https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/lost-items/a.jpg";

    private static void rejected(String url) {
        assertThrows(IllegalArgumentException.class, () -> S3Service.requireOwnBucketUrl(url, BUCKET, REGION));
    }

    @Test void ownBucketHttpsUrlIsAccepted() {
        URI uri = S3Service.requireOwnBucketUrl(OWN, BUCKET, REGION);
        assertEquals("/lost-items/a.jpg", uri.getPath());
    }

    @Test void otherHostsAreRejected() {
        rejected("https://evil.example.com/lost-items/a.jpg");
        rejected("https://other-bucket.s3.ap-northeast-2.amazonaws.com/a.jpg");
        rejected("http://169.254.169.254/latest/meta-data/");
    }

    @Test void lookAlikeHostsAreRejected() {
        rejected("https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com.evil.com/a.jpg");
        rejected("https://evil.com/?https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/a.jpg");
        rejected("https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com@evil.com/a.jpg");
    }

    @Test void plainHttpAndOddSchemesAreRejected() {
        rejected("http://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/lost-items/a.jpg");
        rejected("file:///etc/passwd");
        rejected("ftp://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/a.jpg");
    }

    @Test void blankOrBrokenUrlsAreRejected() {
        rejected(null);
        rejected("  ");
        rejected("https://dandi lost.s3/ a.jpg");
        rejected("/files/vision/mosaic/a.jpeg");
    }

    @Test void extensionIsDetectedOnlyForJpegAndPng() {
        byte[] jpeg = new byte[16];
        jpeg[0] = (byte) 0xFF; jpeg[1] = (byte) 0xD8; jpeg[2] = (byte) 0xFF;
        assertEquals("jpeg", S3Service.detectImageExtension(jpeg));
        byte[] png = new byte[16];
        png[0] = (byte) 0x89; png[1] = 'P'; png[2] = 'N'; png[3] = 'G';
        assertEquals("png", S3Service.detectImageExtension(png));
        assertThrows(IllegalArgumentException.class, () -> S3Service.detectImageExtension(new byte[16]));
    }
}
