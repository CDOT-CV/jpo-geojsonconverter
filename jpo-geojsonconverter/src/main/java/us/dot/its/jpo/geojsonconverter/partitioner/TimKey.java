package us.dot.its.jpo.geojsonconverter.partitioner;

import java.util.Objects;

public class TimKey {

    private String packetId;
    private Integer msgCnt;

    public TimKey() {}

    public TimKey(String packetId, Integer msgCnt) {
        this.packetId = packetId;
        this.msgCnt = msgCnt;
    }

    public String getPacketId() {
        return this.packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public Integer getMsgCnt() {
        return this.msgCnt;
    }

    public void setMsgCnt(Integer msgCnt) {
        this.msgCnt = msgCnt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TimKey)) {
            return false;
        }
        TimKey timKey = (TimKey) o;
        return Objects.equals(packetId, timKey.getPacketId()) && Objects.equals(msgCnt, timKey.getMsgCnt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(packetId, msgCnt);
    }
    



    @Override
    public String toString() {
        return "{" +
            " packetId='" + getPacketId() + "'" +
            ", msgCnt='" + getMsgCnt() + "'" +
            "}";
    }

}
