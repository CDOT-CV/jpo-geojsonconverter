package us.dot.its.jpo.geojsonconverter.partitioner;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka key for TIM messages. Messages are grouped by RSU when the ODE supplies a stable RSU identifier; otherwise,
 * the full key is hashed by {@link RsuTimPartitioner}.
 *
 * <p>The no-argument constructor is required by the JSON serde when Kafka keys are deserialized. Application code
 * should use a value-setting constructor when creating a new key.
 */
@Data
@NoArgsConstructor
public class RsuTimKey implements RsuIdKey {

    /**
     * The ODE metadata {@code originIp} for RSU-originated TIMs, or another stable RSU identifier if the source evolves.
     */
    private String rsuId;
    private String packetId;
    private Integer msgCnt;

    public RsuTimKey(String rsuId, String packetId, Integer msgCnt) {
        this.rsuId = rsuId;
        this.packetId = packetId;
        this.msgCnt = msgCnt;
    }

    /**
     * Create a key from RSU ID and packet ID only
     */
    public RsuTimKey(String rsuId, String packetId) {
        this.rsuId = rsuId;
        this.packetId = packetId;
        this.msgCnt = null;
    }

    /**
     * Create a key from RSU ID only (for error cases)
     */
    public RsuTimKey(String rsuId) {
        this.rsuId = rsuId;
        this.packetId = null;
        this.msgCnt = null;
    }

    @Override
    public String toString() {
        return "{" + " rsuId='" + getRsuId() + "'" + ", packetId='" + getPacketId() + "'" + ", msgCnt='" + getMsgCnt()
                + "'" + "}";
    }
}
