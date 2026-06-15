package com.acooldog.toolbox.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.acooldog.toolbox.utils.NmeaUtils;

import org.junit.Test;

public class NmeaGeneratorTest {
    @Test
    public void checksum_matchesNmeaExample() {
        assertEquals(
                "$GPRMC,081836,A,3751.65,S,14507.36,E,000.0,360.0,130998,011.3,E*62",
                NmeaUtils.withChecksum("GPRMC,081836,A,3751.65,S,14507.36,E,000.0,360.0,130998,011.3,E")
        );
    }

    @Test
    public void generateStandardSentences_returnsValidRmcAndGga() {
        String nmea = NmeaGenerator.generateStandardSentences(
                36.667662d,
                117.027707d,
                55.4d,
                1.2d,
                90d,
                1714262400000L,
                7
        );
        String[] sentences = nmea.split("\\n");

        assertEquals(2, sentences.length);
        assertTrue(sentences[0].startsWith("$GPRMC,000000,A,3640.0597,N,11701.6624,E,2.3,90.0,280424"));
        assertTrue(sentences[1].startsWith("$GPGGA,000000,3640.0597,N,11701.6624,E,1,07,0.9,55.4,M"));
        assertTrue(NmeaUtils.hasValidChecksum(sentences[0]));
        assertTrue(NmeaUtils.hasValidChecksum(sentences[1]));
    }
}
