package com.acooldog.toolbox.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.acooldog.toolbox.utils.NmeaUtils;

import org.junit.Test;

import java.util.List;

public class CompleteNmeaGeneratorTest {
    private final CompleteNmeaGenerator generator = new CompleteNmeaGenerator();

    @Test
    public void generateCompleteNmea_returnsAllRequiredSentencesWithValidChecksums() {
        String nmea = generator.generateCompleteNmea(
                36.667662d,
                117.027707d,
                55.4d,
                1.2f,
                90f,
                8,
                CompleteNmeaGenerator.SIGNAL_QUALITY_STRONG,
                1.5f,
                1714262400000L
        );

        String[] sentences = nmea.split("\\n");

        assertEquals(6, sentences.length);
        assertTrue(sentences[0].startsWith("$GPRMC,000000.00,A,3640.0597,N,11701.6624,E,2.3,90.0,280424"));
        assertTrue(sentences[1].startsWith("$GPGGA,000000.00,3640.0597,N,11701.6624,E,1,08,1.5,55.4,M"));
        assertTrue(sentences[2].startsWith("$GPGSV,2,1,08,01,17,030,48"));
        assertTrue(sentences[3].startsWith("$GPGSV,2,2,08,05,45,150,50"));
        assertTrue(sentences[4].startsWith("$GPGSA,A,3,01,02,03,04,05,06,07,08"));
        assertTrue(sentences[5].startsWith("$GPVTG,90.0,T,,M,2.3,N,4.3,K"));
        for (String sentence : sentences) {
            assertTrue(NmeaUtils.hasValidChecksum(sentence));
        }
    }

    @Test
    public void generateGpgsv_splitsTwelveSatellitesAcrossThreeSentences() {
        List<String> sentences = generator.generateGpgsv(
                12,
                CompleteNmeaGenerator.SIGNAL_QUALITY_MEDIUM
        );

        assertEquals(3, sentences.size());
        assertTrue(sentences.get(0).startsWith("$GPGSV,3,1,12,01,17,030,38"));
        assertTrue(sentences.get(1).startsWith("$GPGSV,3,2,12,05,45,150,40"));
        assertTrue(sentences.get(2).startsWith("$GPGSV,3,3,12,09,73,270,42"));
        for (String sentence : sentences) {
            assertTrue(NmeaUtils.hasValidChecksum(sentence));
        }
    }

    @Test
    public void normalizeInputs_clampsNmeaParametersToSupportedRanges() {
        assertEquals(1, CompleteNmeaGenerator.normalizeSatelliteCount(-1));
        assertEquals(12, CompleteNmeaGenerator.normalizeSatelliteCount(20));
        assertEquals(0, CompleteNmeaGenerator.normalizeSignalQuality(-1));
        assertEquals(2, CompleteNmeaGenerator.normalizeSignalQuality(9));
        assertEquals(0.5f, CompleteNmeaGenerator.normalizeHdop(0.1f), 0.001f);
        assertEquals(CompleteNmeaGenerator.DEFAULT_HDOP, CompleteNmeaGenerator.normalizeHdop(Float.NaN), 0.001f);
    }
}
