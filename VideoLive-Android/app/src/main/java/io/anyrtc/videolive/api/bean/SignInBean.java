package io.anyrtc.videolive.api.bean;


public class SignInBean {
    /**
     * {
     *   "code": 0,
     *   "msg": "success.",
     *   "data": {
     *     "appid": "111",
     *     "rtmToken": "006111IACLoEfhHk9a7cXTRSBxR6+Ls5REID9w9fjbWxOebcgKdvEif/EAAAAAEAAHgN0AiCzsXwEA6AMQ4upf",
     *     "userToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MDkyOTkwMTYsImlhdCI6MTYwOTIyNzAxNiwidXNlcmlkIjoiOTc4MDQ5MTcifQ.ec2BYWKH7fGp-zfyXiP5m9MMUsaatYnIpssiVMRm02w",
     *     "userName": "string",
     *     "avatar": "https://oss.agrtc.cn/oss/fdfs/6cd71dca89be027ec4b0f0f513018d75.jpg"
     *   }
     * }
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
        private String appid;
        private String rtmToken;
        private String userToken;
        private String userName;
        private String avatar;

        public String getAppid() {
            return appid;
        }

        public void setAppid(String appid) {
            this.appid = appid;
        }

        public String getRtmToken() {
            return rtmToken;
        }

        public void setRtmToken(String rtmToken) {
            this.rtmToken = rtmToken;
        }

        public String getUserToken() {
            return userToken;
        }

        public void setUserToken(String userToken) {
            this.userToken = userToken;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

    }
}
