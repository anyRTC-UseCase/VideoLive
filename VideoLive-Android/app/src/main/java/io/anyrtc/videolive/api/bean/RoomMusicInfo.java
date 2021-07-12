package io.anyrtc.videolive.api.bean;

public class RoomMusicInfo {

    /**
     * code : 0
     * msg : success.
     * data : {"musicState":0,"musicId":0,"musicName":"","singer":"","musicUrl":""}
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
        /**
         * musicState : 0
         * musicId : 0
         * musicName :
         * singer :
         * musicUrl :
         */

        private int musicState;
        private int musicId;
        private String musicName;
        private String singer;
        private String musicUrl;

        public int getMusicState() {
            return musicState;
        }

        public void setMusicState(int musicState) {
            this.musicState = musicState;
        }

        public int getMusicId() {
            return musicId;
        }

        public void setMusicId(int musicId) {
            this.musicId = musicId;
        }

        public String getMusicName() {
            return musicName;
        }

        public void setMusicName(String musicName) {
            this.musicName = musicName;
        }

        public String getSinger() {
            return singer;
        }

        public void setSinger(String singer) {
            this.singer = singer;
        }

        public String getMusicUrl() {
            return musicUrl;
        }

        public void setMusicUrl(String musicUrl) {
            this.musicUrl = musicUrl;
        }
    }
}
