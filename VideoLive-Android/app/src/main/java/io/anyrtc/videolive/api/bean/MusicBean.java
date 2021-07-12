package io.anyrtc.videolive.api.bean;

import java.util.List;

public class MusicBean {

    /**
     * {
     * 	"code": 0,
     * 	"msg": "success.",
     * 	"data": [{
     * 		"musicId": 1,
     * 		"musicName": "红磨坊",
     * 		"singer": "周杰伦",
     * 		"musicUrl": "https://oss.agrtc.cn/oss/fdfs/6a392547105d4f1ca68c4b706c84f64f.mp3"
     *        }, {
     * 		"musicId": 2,
     * 		"musicName": "阳光宅男",
     * 		"singer": "周杰伦",
     * 		"musicUrl": "https://oss.agrtc.cn/oss/fdfs/fa3afab672df2e0ad674b58da7254e7d.mp3"
     *    }, {
     * 		"musicId": 3,
     * 		"musicName": "完美主义",
     * 		"singer": "周杰伦",
     * 		"musicUrl": "https://oss.agrtc.cn/oss/fdfs/db2ece4fec8998a306000eff2d648aba.mp3"
     *    }]
     * }
     */

    private int code;
    private String msg;
    private List<DataBean> data;

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

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }



    public static class DataBean {
        private int musicId;
        private String musicName;
        private String singer;
        private String musicUrl;

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
