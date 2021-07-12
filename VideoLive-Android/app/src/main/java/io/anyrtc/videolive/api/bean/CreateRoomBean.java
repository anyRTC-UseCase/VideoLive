package io.anyrtc.videolive.api.bean;

public class CreateRoomBean {

    /**
     * {"code":0,"msg":"success.","data":{"roomId":"911224","roomTs":1609236149}}
     */

    private int code;
    private String msg;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        private String pushUrl;
        private String roomId;
        private String roomTs;
        private String rtcToken;
        private String rtmToken;

        public String getPushUrl() {
            return pushUrl;
        }

        public void setPushUrl(String pushUrl) {
            this.pushUrl = pushUrl;
        }

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getRoomTs() {
            return roomTs;
        }

        public void setRoomTs(String roomTs) {
            this.roomTs = roomTs;
        }

        public String getRtcToken() {
            return rtcToken;
        }

        public void setRtcToken(String rtcToken) {
            this.rtcToken = rtcToken;
        }

        public String getRtmToken() {
            return rtmToken;
        }

        public void setRtmToken(String rtmToken) {
            this.rtmToken = rtmToken;
        }
    }
}
