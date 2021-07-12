package io.anyrtc.videolive.api.bean;

public class SignUpBean {
    /**
     * {
     * 	"code": 0,
     * 	"msg": "success.",
     * 	"data": {
     * 		"uid": "13712434",
     * 		"userName": "YAL-AL00"
     *        }
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

    public static class DataBean{
        private String uid;
        private String userName;

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

}
