//
//  ARVideoViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/21.
//

import UIKit
import ARtcKit
import ARtmKit
import AttributedString

var rtcKit: ARtcEngineKit!
var rtmEngine: ARtmKit!
var infoVideoModel: ARRoomInfoModel!
var liveTranscoding: ARLiveTranscoding!

class ARVideoViewController: ARBaseViewController {

    @IBOutlet weak var listButton: UIButton!
    @IBOutlet weak var moreButton: UIButton!
    @IBOutlet weak var chatButton0: UIButton!
    @IBOutlet weak var chatButton1: UIButton!
    
    @IBOutlet weak var micButton: UIButton!
    @IBOutlet weak var switchButton: UIButton!
    @IBOutlet weak var videoButton: UIButton!
    @IBOutlet weak var audioButton: UIButton!
    @IBOutlet weak var musicButton: UIButton!
    @IBOutlet weak var stateLabel: UILabel!
    weak var logVC: LogViewController?
    
    private var micStatus: ARMicStatus = .normal
    private var destroy: Bool = false
    private var localVideo: ARVideoView?
    /* 前后台，默认为 true */
    private var isActive: Bool = true
    private var reason: ARLeaveReason = .normal
    /* 视频数组 */
    public var videoArr = [ARVideoView]()
    /* 请求连麦列表 */
    public var micArr = [ARUserModel]()
    
    fileprivate var rtmChannel: ARtmChannel?
    fileprivate var streamKit: ARStreamingKit?
    fileprivate var mediaPlayer: ARMediaPlayer?
    
    lazy var broadcasterVideo: ARVideoView = {
        let video = ARVideoView.videoView(uid: infoVideoModel.ower?.uid)
        video.frame = view.bounds
        video.userName = UserDefaults.string(forKey: .userName)
        video.headUrl = UserDefaults.string(forKey: .avatar)
        return video
    }()
    
    private lazy var animations: CABasicAnimation = {
        let animation = CABasicAnimation.init(keyPath: "transform.rotation.z")
        animation.duration = 2.0
        animation.fromValue = 0.0
        animation.toValue = Double.pi * 2
        animation.repeatCount = MAXFLOAT
        animation.isRemovedOnCompletion = false
        return animation
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        initializeEngine()
        initializeUI()
    }
    
    func initializeUI() {
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        UIApplication.shared.isIdleTimerDisabled = true
        updateRoleState(isBroadcaster: infoVideoModel.isBroadcaster)
        
        if infoVideoModel.isBroadcaster {
            // broadcaster
            joinChannel()
            musicButton.isUserInteractionEnabled = true
        } else {
            // audience
            if infoVideoModel?.rType != 6 {
                initializeMediaPlayer()
            } else {
                joinChannel()
            }
        }
        
        NotificationCenter.default.addObserver(self, selector: #selector(changeLayout), name: UIResponder.audioLiveNotificationLayout, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(becomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(resignActive), name: UIApplication.willResignActiveNotification, object: nil)
    }
    
    func initializeEngine() {
        // init ARtcEngineKit
        rtcKit = ARtcEngineKit.sharedEngine(withAppId: UserDefaults.string(forKey: .appid)!, delegate: self)
        rtcKit.setChannelProfile(.liveBroadcasting)
        rtcKit.enableVideo()
        
        if infoVideoModel.isBroadcaster {
            rtcKit.setClientRole(.broadcaster)
            
            let videoCanvas = ARtcVideoCanvas()
            videoCanvas.view = broadcasterVideo.renderView
            rtcKit.setupLocalVideo(videoCanvas)
            view.insertSubview(broadcasterVideo, at: 0)
            videoArr.append(broadcasterVideo)
            
            // setUp videoConfig
            let videoConfig = ARVideoEncoderConfiguration()
            videoConfig.dimensions = getVideoDimensions(index: infoVideoModel.dimensions)
            videoConfig.bitrate = 500
            videoConfig.frameRate = 15
            rtcKit.setVideoEncoderConfiguration(videoConfig)
            
            if infoVideoModel.rType != 6 {
                liveTranscoding = ARLiveTranscoding.default()
                liveTranscoding.backgroundColor =  UIColor(hexString: "#1A1A1E")
                liveTranscoding.size = getVideoDimensions(index: infoVideoModel.dimensions)
            }
        }
        
        // init ARtmKit
        rtmEngine = ARtmKit.init(appId: UserDefaults.string(forKey: .appid)!, delegate: self)
        rtmEngine.login(byToken: infoVideoModel.rtmToken, user: UserDefaults.string(forKey: .uid) ?? "0") { [weak self](errorCode) in
            guard let weakself = self else {return}
            weakself.rtmChannel = rtmEngine.createChannel(withId: infoVideoModel.roomId!, delegate: self)
            weakself.rtmChannel?.join(completion: { (errorCode) in
                let dic: NSDictionary! = ["cmd": "join", "userName": UserDefaults.string(forKey: .userName) as Any]
                weakself.sendChannelMessage(text: weakself.getJSONStringFromDictionary(dictionary: dic))
            })
            
            if !infoVideoModel.isBroadcaster {
                rtmEngine.subscribePeersOnlineStatus([infoVideoModel.ower!.uid!]) { (errorCode) in
                    print("subscribePeersOnlineStatus \(errorCode.rawValue)")
                }
            } else {
                weakself.addOrUpdateChannel(key: "layout", value: "1")
                weakself.addOrUpdateChannel(key: "musicState", value: "0")
            }
        }
    }
    
    //------------ RTC 实时互动 ------------------
    func joinChannel() {
        let uid = UserDefaults.string(forKey: .uid)
        rtcKit.joinChannel(byToken: infoVideoModel.rtcToken, channelId: infoVideoModel.roomId!, uid: uid) { [weak self](channel, uid, elapsed) in
            guard let weakself = self else {return}
            if infoVideoModel.isBroadcaster {
                if infoVideoModel.rType == 7 {
                    weakself.initializeStreamingKit()
                } else if infoVideoModel.rType == 8 {
                    weakself.initializeAddPublishStreamUrl()
                }
            }
        }
    }
    
    func leaveChannel() {
        rtcKit.leaveChannel { (stats) in
            print("leaveChannel")
        }
    }
    
    //------------ 客户端推流到 CDN ------------------
    func initializeStreamingKit() {
        streamKit = ARStreamingKit()
        streamKit?.setRtcEngine(rtcKit)
        streamKit?.setMode(.vidMix)

        let transCodingUser = ARLiveTranscodingUser()
        transCodingUser.uid = "0"
        transCodingUser.rect = broadcasterVideo.frame
        liveTranscoding.transcodingUsers = [transCodingUser]
        streamKit?.setLiveTranscoding(liveTranscoding)
        streamKit?.pushStream(infoVideoModel.pushUrl ?? "")
    }
    
    //------------ 服务端推流到 CDN ------------------
    func initializeAddPublishStreamUrl() {
        let transCodingUser = ARLiveTranscodingUser()
        transCodingUser.uid = UserDefaults.string(forKey: .uid) ?? "0"
        transCodingUser.rect = broadcasterVideo.frame
        liveTranscoding.transcodingUsers = [transCodingUser]
        rtcKit.setLiveTranscoding(liveTranscoding)
        
        rtcKit.addPublishStreamUrl(infoVideoModel.pushUrl ?? "", transcodingEnabled: true)
    }
    
    //------------ 播放器 -- 游客 ------------------
    func initializeMediaPlayer() {
        broadcasterVideo.frame = view.bounds
        view.insertSubview(broadcasterVideo, at: 0)
        broadcasterVideo.placeholderView.isHidden = false
        
        mediaPlayer = ARMediaPlayer(delegate: self)
        mediaPlayer?.setView(broadcasterVideo.renderView)
        mediaPlayer?.open(infoVideoModel.pullRtmpUrl!, startPos: 0)
        mediaPlayer?.play()
        videoLayout()
    }
    
    @IBAction func didClickVideoButton(_ sender: UIButton) {
        sender.isSelected.toggle()
        
        if sender.tag == 50 {
            // 音乐 - 主持人
            if infoVideoModel.isBroadcaster {
                if sender.isSelected {
                    rtcKit.startAudioMixing(randomMusic(), loopback: false, replace: false, cycle: -1)
                } else {
                    rtcKit.stopAudioMixing()
                }
                
                updateMusicState(state: sender.isSelected)
                addOrUpdateChannel(key: "musicState", value: sender.isSelected ? "1" : "2")
            }
        } else if sender.tag == 51 {
            // 聊天
            chatTextField.becomeFirstResponder()
        } else if sender.tag == 52 {
            // 切换摄像头 - 游客
            if !videoButton.isSelected {
                rtcKit.switchCamera()
            }
        } else if sender.tag == 53 {
            // 视频
            rtcKit.muteLocalVideoStream(sender.isSelected)
            rtcKit.enableLocalVideo(!sender.isSelected)
            
            if infoVideoModel.isBroadcaster {
                broadcasterVideo.placeholderView.isHidden = !sender.isSelected
            } else {
                localVideo?.placeholderView.isHidden = !sender.isSelected
            }
            
            infoVideoModel.videoState = !sender.isSelected
            showToast(text: sender.isSelected ? "已关闭摄像头" : "已打开摄像头", image: sender.isSelected ? "icon_tip_video_close" : "icon_tip_video_open")
        } else if sender.tag == 54 {
            // 音频
            rtcKit.muteLocalAudioStream(sender.isSelected)
            
            if !infoVideoModel.isBroadcaster {
                localVideo?.audioImageView.isHidden = !sender.isSelected
            }
            
            showToast(text: sender.isSelected ? "已关闭麦克风" : "已打开麦克风", image: sender.isSelected ? "icon_tip_audio_close" : "icon_tip_audio_open")
        } else if sender.tag == 55 {
            
            if micStatus != .exist {
                // 上麦
                var dic: NSDictionary!
                if sender.isSelected {
                    dic = ["cmd": "apply", "userName": UserDefaults.string(forKey: .userName) as Any, "avatar": UserDefaults.string(forKey: .avatar) as Any]
                    micStatus = .cancle
                } else {
                    dic = ["cmd": "cancelApply"]
                    micStatus = .normal
                }
                
                let message: ARtmMessage = ARtmMessage.init(text: getJSONStringFromDictionary(dictionary: dic))
                rtmEngine.send(message, toPeer: (infoVideoModel.ower?.uid)!, sendMessageOptions: ARtmSendMessageOptions()) { (errorCode) in
                    print("errorCode:\(errorCode.rawValue)")
                }
            } else {
                // 下麦
                micStatus = .normal
                sender.isSelected = false
                sender.setTitle("上麦", for: .normal)
                updateRoleState(isBroadcaster: false)
                
                if infoVideoModel.rType == 6 {
                    for (index, video) in videoArr.enumerated() {
                        if video == localVideo {
                            localVideo?.removeFromSuperview()
                            localVideo = nil
                            videoArr.remove(at: index)
                            break
                        }
                    }
                    rtcKit.setClientRole(.audience)
                } else {
                    for video in videoArr {
                        video.removeFromSuperview()
                    }
                    videoArr.removeAll()
                    stateLabel.text = ""
                    leaveChannel()
                    initializeMediaPlayer()
                }
                videoLayout()
            }
            
        } else if sender.tag == 56 {
            // 离开
            if infoVideoModel.isBroadcaster {
                UIAlertController.showAlert(in: self, withTitle: "结束直播", message: "是否结束直播", cancelButtonTitle: "取消", destructiveButtonTitle: nil, otherButtonTitles: ["结束"]) { [unowned self] (alertVc, action, index) in
                    if index == 2 {
                        self.reason = .normal
                        self.destroyRoom()
                    }
                }
            } else {
                reason = .normal
                destroyRoom()
            }
        }
    }
    
    func addOrUpdateChannel(key: String, value: String) {
        // 更新频道属性
        let channelAttribute = ARtmChannelAttribute()
        channelAttribute.key = key
        channelAttribute.value = value
        
        let attributeOptions = ARtmChannelAttributeOptions()
        attributeOptions.enableNotificationToChannelMembers = true
        
        rtmEngine.addOrUpdateChannel(infoVideoModel.roomId!, attributes: [channelAttribute], options: attributeOptions) { (errorCode) in
            print("addOrUpdateChannel code: \(errorCode.rawValue)")
        }
    }
    
    @objc func changeLayout() {
        // 等分、大小屏
        addOrUpdateChannel(key: "layout", value: infoVideoModel.layout ? "2" : "1")
        
        videoLayout()
    }
    
    func getUserInfo(uid: String) {
        // 获取用户信息
        let parameters : NSDictionary = ["uid": uid]
        ARNetWorkHepler.getResponseData("getUserInfo", parameters: parameters as? [String : AnyObject], headers: true) { [weak self](result) in
            if result["code"] == 0 {
                for video in self!.videoArr {
                    if video.uid == uid {
                        video.userName = result["data"]["userName"].stringValue
                        video.headUrl = result["data"]["avatar"].stringValue
                        break
                    }
                }
            }
        } error: { (error) in

        }
    }
    
    func updateMusicState(state: Bool) {
        // 更新音乐状态
        musicButton.isSelected = state
        if state {
            musicButton.imageView?.layer.add(animations, forKey: "CABasicAnimation")
        } else {
            musicButton.imageView?.layer.removeAnimation(forKey: "CABasicAnimation")
        }
    }
    
    func updateRoleState(isBroadcaster: Bool) {
        // 更新 role 状态
        if infoVideoModel.isBroadcaster {
            listButton.isHidden = !isBroadcaster
            moreButton.isHidden = !isBroadcaster
            micButton.isHidden = true
        } else {
            switchButton.isHidden = !isBroadcaster
            micButton.isHidden = false
        }

        videoButton.isHidden = !isBroadcaster
        audioButton.isHidden = !isBroadcaster
        chatButton0.isHidden = isBroadcaster
        chatButton1.isHidden = !isBroadcaster
        videoButton.isSelected = false
        audioButton.isSelected = false
        rtcKit.muteLocalVideoStream(false)
        rtcKit.muteLocalAudioStream(false)
    }
    
    override func didSendChatTextField() {
        let text = chatTextField.text
        if text?.count ?? 0 > 0 && !stringAllIsEmpty(string: text ?? "") {
            
            let dic: NSDictionary! = ["cmd": "msg", "content": chatTextField.text as Any, "userName": UserDefaults.string(forKey: .userName) as Any]
            sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
            self.logVC?.log(logModel: ARLogModel(userName: UserDefaults.string(forKey: .userName), uid: UserDefaults.string(forKey: .uid), text: text))
            
            chatTextField.text = ""
            chatTextField.resignFirstResponder()
            confirmButton.alpha = 0.3
        }
    }
    
    @objc func sendChannelMessage(text: String) {
        // 发送频道消息
        let rtmMessage: ARtmMessage = ARtmMessage.init(text: text)
        let options: ARtmSendMessageOptions = ARtmSendMessageOptions()
        rtmChannel?.send(rtmMessage, sendMessageOptions: options) { (errorCode) in
            print("Send Channel Message")
        }
    }
    
    @objc func becomeActive(notifi: Notification) {
        // 后台切前台
        if !isActive {
            rtcKit.muteLocalVideoStream(false)
            isActive = true
        }
    }
    
    @objc func resignActive(notifi: Notification) {
        // 前台切回后台
        if (infoVideoModel.isBroadcaster && !videoButton.isSelected) || (!infoVideoModel.isBroadcaster && !videoButton.isSelected && micStatus == .exist) {
            rtcKit.muteLocalVideoStream(true)
            isActive = false
        }
    }
    
    @objc func destroyRoom() {
        destroy = true
        leaveXHToast(reason: reason)
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(destroyRoom), object: nil)
        dismissLoadingView()
        
        let topVc = topViewController()
        if !(topVc is ARVideoViewController) {
            topVc.dismiss(animated: false, completion: nil)
        }
        
        if infoVideoModel.isBroadcaster {
            let dic: NSDictionary! = ["cmd": "exit"]
            sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
        }
        
        if infoVideoModel.isBroadcaster {
            if infoVideoModel.rType == 7 {
                streamKit?.destroy()
            }
            deleteRoom(roomId: (infoVideoModel.roomId)!)
        } else {
            if mediaPlayer != nil {
                mediaPlayer?.destroy()
            }
            leaveRoom(roomId: (infoVideoModel.roomId)!)
            rtmEngine.unsubscribePeersOnlineStatus([infoVideoModel.ower!.uid!], completion: nil)
        }
        
        leaveChannel()
        ARtcEngineKit.destroy()
        rtmEngine.destroyChannel(withId: (infoVideoModel.roomId)!)
        rtmEngine.aRtmDelegate = nil
        rtmEngine.logout(completion: nil)
        
        rtcKit = nil; rtmEngine = nil; infoVideoModel = nil; liveTranscoding = nil
        self.navigationController?.popToRootViewController(animated: true)
    }
    
    func updateLiveTranscoding() {
        // 更新合流参数
        if infoVideoModel.isBroadcaster {
            if infoVideoModel.rType != 6 {
                liveTranscoding.size = CGSize(width: ARScreenWidth, height: ARScreenHeight)
                liveTranscoding.transcodingUsers = nil
                
                for video in self.videoArr {
                    let transcodingUser = ARLiveTranscodingUser()
                    transcodingUser.uid = video.uid!
                    if video.uid == infoVideoModel.ower?.uid  && infoVideoModel.rType == 7 {
                        transcodingUser.uid = "0"
                    }
                    transcodingUser.rect = video.frame
                    liveTranscoding.add(transcodingUser)
                }
                
                if infoVideoModel.rType == 7 {
                    // 本地推流
                    self.streamKit?.setLiveTranscoding(liveTranscoding)
                } else {
                    // 服务端推流
                    rtcKit.setLiveTranscoding(liveTranscoding)
                }
            }
        }
    }
    
    func connectionStateChanged(state: Int) {
        // 连接状态发生改变
        if !destroy {
            NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(destroyRoom), object: nil)
            if state == 1 {
                reason = .timeOut
                showLoadingView(text: "连接中...", count: Float(Int.max))
                self.perform(#selector(destroyRoom), with: nil, afterDelay: TimeInterval(12))
            } else if state == 3 {
                reason = .normal
                dismissLoadingView()
            }
        }
    }
    
    func videoLayout() {
        let allVideo = NSMutableArray.init(array: videoArr)
        if !infoVideoModel.layout {
            // 大小屏
            broadcasterVideo.frame = view.bounds
            allVideo.remove(broadcasterVideo as Any)
        }
        
        let rate: CGFloat = 16/9
        if !infoVideoModel.layout || (infoVideoModel.layout && allVideo.count == 4) {
            
            let column: NSInteger = infoVideoModel.layout ? 2 : 1
            
            let video_height: CGFloat = (ARScreenHeight - 240)/(infoVideoModel.layout ? 2 : 3);
            let video_width: CGFloat = infoVideoModel.layout ? view.frame.size.width/CGFloat(column) : (video_height * 9/16)
            
            for (index,video) in allVideo.enumerated() {
                if (infoVideoModel.layout && index < 4) || (!infoVideoModel.layout && index < 3) {
                    let row: NSInteger = index / column
                    let col: NSInteger = index % column
                    let margin: CGFloat = infoVideoModel.layout ? 1 : 8
                    let picX: CGFloat = infoVideoModel.layout ? (margin + (video_width + margin) * CGFloat(col)) : (ARScreenWidth - 126)
                    let picY: CGFloat = 130 + margin + (video_height + margin) * CGFloat(row)
                    let videoView = video as! ARVideoView
                    videoView.frame = CGRect.init(x: picX, y: picY, width: video_width, height: video_height)
                }
            }
        }  else if allVideo.count == 3 {
            let video_width = view.frame.size.width/2
            let video_height = (ARScreenHeight - 240)/2
            
            for (index,video) in allVideo.enumerated() {
                let videoView = video as! ARVideoView
                if index == 0 {
                    videoView.frame = CGRect.init(x: (view.frame.size.width - video_width)/2, y: 130, width: video_width, height: video_height)
                } else {
                    videoView.frame = CGRect.init(x: (video_width + 1) * CGFloat(index - 1), y: (video_height + 1) + 130, width: video_width, height: video_height)
                }
            }
        } else if allVideo.count == 2 {
            let video_width = view.frame.size.width/2
            let video_height = video_width * rate
            for (index,video) in allVideo.enumerated() {
                let videoView = video as! ARVideoView
                videoView.frame = CGRect.init(x: CGFloat(index) * (video_width + 1), y: 130, width: video_width, height: video_height)
            }
        } else if allVideo.count == 1 {
            let videoView = allVideo[0] as! ARVideoView
            videoView.frame = view.frame
        }
        updateLiveTranscoding()
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        guard let identifier = segue.identifier else {
            return
        }
        
        if identifier == "EmbedLogViewController",
            let vc = segue.destination as? LogViewController {
            self.logVC = vc
        }  else if identifier == "ARMicViewController" {
            let vc: ARMicViewController = (segue.destination as? ARMicViewController)!
            vc.videoVc = self
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController?.navigationBar.isHidden = true
        self.navigationController?.navigationBar.barStyle = .black
        removeLogoImage()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.navigationController?.navigationBar.isHidden = false
        self.navigationController?.navigationBar.barStyle = .default
        addLogoImage()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        print("deinit")
    }
}

//MARK: - ARtcEngineDelegate

extension ARVideoViewController: ARtcEngineDelegate {
    
    func rtcEngine(_ engine: ARtcEngineKit, tokenPrivilegeWillExpire token: String) {
        // Token 过期回调
        if infoVideoModel.isBroadcaster {
            let dic: NSDictionary! = ["cmd": "tokenPastDue"]
            sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
        }
        
        reason = .tokenExpire
        destroyRoom()
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, firstRemoteVideoDecodedOfUid uid: String, size: CGSize, elapsed: Int) {
        // 已解码远端视频首帧的回调
        for video in videoArr {
            if video.uid == uid {
                video.placeholderView.isHidden = true
                break
            }
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, firstLocalVideoFrameWith size: CGSize, elapsed: Int) {
        // 已显示本地视频首帧的回调
        if infoVideoModel.isBroadcaster && !audioButton.isSelected {
            broadcasterVideo.placeholderView.isHidden = true
        } else {
            localVideo?.placeholderView.isHidden = true
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, didJoinedOfUid uid: String, elapsed: Int) {
        // 远端用户/主播加入回调
        if videoArr.count < 4 {
            let videoCanvas = ARtcVideoCanvas()
            videoCanvas.uid = uid
            if !infoVideoModel.isBroadcaster && infoVideoModel.ower?.uid == uid {
                videoCanvas.view = broadcasterVideo.renderView
                videoArr.insert(broadcasterVideo, at: 0)
                view.insertSubview(broadcasterVideo, at: 0)
            } else {
                let remoteVideo = ARVideoView.videoView(uid: uid)
                view.insertSubview(remoteVideo, at: 1)
                
                videoCanvas.view = remoteVideo.renderView
                videoArr.append(remoteVideo)
            }
            rtcKit.setupRemoteVideo(videoCanvas)
            videoLayout()
            getUserInfo(uid: uid)
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, didOfflineOfUid uid: String, reason: ARUserOfflineReason) {
        // 远端用户（通信场景）/主播（互动场景）离开当前频道回调
        for (index, video) in videoArr.enumerated() {
            if video.uid == uid {
                video.removeFromSuperview()
                videoArr.remove(at: index)
                videoLayout()
                break
            }
        }
        
        for (index, userId) in infoVideoModel.agreeMicArr.enumerated() {
            if userId == uid {
                infoVideoModel.agreeMicArr.remove(at: index)
                if topViewController() is ARMicViewController && infoVideoModel.isBroadcaster {
                    NotificationCenter.default.post(name: UIResponder.audioLiveNotificationRefreshMicList, object: self, userInfo:nil)
                }
                break;
            }
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, remoteVideoStateChangedOfUid uid: String, state: ARVideoRemoteState, reason: ARVideoRemoteStateReason, elapsed: Int) {
        // 远端视频状态发生改变回调
        if reason == .remoteMuted || reason == .remoteUnmuted {
            for video in videoArr {
                if video.uid == uid {
                    video.placeholderView.isHidden = (reason == .remoteMuted) ? false : true
                    break
                }
            }
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, remoteAudioStateChangedOfUid uid: String, state: ARAudioRemoteState, reason: ARAudioRemoteStateReason, elapsed: Int) {
        // 远端音频状态发生改变回调
        if reason == .reasonRemoteMuted || reason == .reasonRemoteUnmuted {
            for video in videoArr {
                if video.uid == uid {
                    video.audioImageView.isHidden = (reason == .reasonRemoteMuted) ? false : true
                    break
                }
            }
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, reportRtcStats stats: ARChannelStats) {
        // 当前通话统计回调
        if infoVideoModel.isBroadcaster || micStatus == .exist {
            // 上行
            stateLabel.attributed.text = .init("""
            \(.image(#imageLiteral(resourceName: "icon_green"), .custom(.center, size: .init(width: 20, height: 20)))) 丢包率：\(stats.txPacketLossRate)%  RTT：\(stats.gatewayRtt)ms
            """)
        } else {
            // 下行
            stateLabel.attributed.text = .init("""
            \(.image(#imageLiteral(resourceName: "icon_red"), .custom(.center, size: .init(width: 20, height: 20)))) 丢包率：\(stats.rxPacketLossRate)%  RTT：\(stats.gatewayRtt)ms
            """)
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, connectionChangedTo state: ARConnectionStateType, reason: ARConnectionChangedReason) {
        // 网络连接状态已改变回调
        connectionStateChanged(state: state.rawValue)
    }
}

//MARK: - ARtmDelegate,ARtmChannelDelegate

extension ARVideoViewController: ARtmDelegate,ARtmChannelDelegate {
    
    func rtmKit(_ kit: ARtmKit, connectionStateChanged state: ARtmConnectionState, reason: ARtmConnectionChangeReason) {
        // 连接状态改变回调
        connectionStateChanged(state: state.rawValue)
    }
    
    func rtmKit(_ kit: ARtmKit, messageReceived message: ARtmMessage, fromPeer peerId: String) {
        //收到点对点消息回调
        let dic = getDictionaryFromJSONString(jsonString: message.text)
        let value: String? = dic.object(forKey: "cmd") as? String
        if value == "apply" {
            showToast(text: "\(dic.object(forKey: "userName") ?? "") 请求连麦", image: "icon_tip_warning")
            micArr.append(ARUserModel(jsonData: ["userName": dic.object(forKey: "userName") as Any, "uid": peerId, "avatar": dic.object(forKey: "avatar") as Any]))
        } else if value == "rejectLine" {
            // 拒绝上麦请求
            micButton.isSelected = false
            micStatus = .normal
            showToast(text: "主播拒绝了你的上麦请求", image: "icon_tip_warning")
        } else if value == "acceptLine" {
            // 同意上麦请求
            micButton.isSelected = false
            micButton.setTitle("下麦", for: .normal)
            micStatus = .exist
            updateRoleState(isBroadcaster: true)
            
            if infoVideoModel.rType != 6 {
                broadcasterVideo.removeFromSuperview()
                broadcasterVideo.placeholderView.isHidden = false
                
                mediaPlayer?.setView(nil)
                mediaPlayer?.destroy()
                mediaPlayer = nil
                joinChannel()
            }
            
            rtcKit.setClientRole(.broadcaster)
            
            localVideo = ARVideoView.videoView(uid: UserDefaults.string(forKey: .uid))
            localVideo?.userName = UserDefaults.string(forKey: .userName)
            localVideo?.headUrl = UserDefaults.string(forKey: .avatar)
            view.insertSubview(localVideo!, at: 1)
            
            let videoCanvas = ARtcVideoCanvas()
            videoCanvas.view = localVideo?.renderView
            rtcKit.setupLocalVideo(videoCanvas)
            videoArr.append(localVideo!)
            videoLayout()
            
        } else if value == "cancelApply" {
            for (index, model) in micArr.enumerated() {
                if model.uid == peerId {
                    micArr.remove(at: index)
                    break
                }
            }
        }
        listButton.badgeValue = "\(micArr.count)"
        
        if value == "cancelApply" || value == "apply" && topViewController() is ARMicViewController && infoVideoModel.isBroadcaster {
            NotificationCenter.default.post(name: UIResponder.audioLiveNotificationRefreshMicList, object: self, userInfo:nil)
        }
    }
    
    func rtmKit(_ kit: ARtmKit, peersOnlineStatusChanged onlineStatus: [ARtmPeerOnlineStatus]) {
        // 被订阅用户在线状态改变回调
        for status: ARtmPeerOnlineStatus in onlineStatus {
            if status.peerId == infoVideoModel.ower?.uid {
                if status.state == .offline {
                    reason = .broadcastOffline
                    self.perform(#selector(destroyRoom), with: nil, afterDelay: TimeInterval(12))
                } else if status.state == .online {
                    reason = .normal
                    NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(destroyRoom), object: nil)
                }
                break
            }
        }
    }
    
    func channel(_ channel: ARtmChannel, messageReceived message: ARtmMessage, from member: ARtmMember) {
        //收到频道消息回调
        let dic = getDictionaryFromJSONString(jsonString: message.text)
        let value: String? = dic.object(forKey: "cmd") as? String
        if value == "msg" {
            logVC?.log(logModel: ARLogModel(userName: dic.object(forKey: "userName") as? String, uid: member.uid, text:  dic.object(forKey: "content") as? String))
        } else if value == "tokenPastDue" {
            reason = .tokenExpire
            destroyRoom()
        } else if value == "exit" {
            reason = .broadcastOffline
            destroyRoom()
        }
    }
    
    func channel(_ channel: ARtmChannel, attributeUpdate attributes: [ARtmChannelAttribute]) {
        // 频道属性更新
        for attribute in attributes {
            if attribute.key == "musicState" {
                updateMusicState(state: (attribute.value == "1") ? true : false)
            } else if attribute.key == "layout" {
                if !infoVideoModel.isBroadcaster {
                    infoVideoModel.layout = (attribute.value == "1") ? false : true
                    videoLayout()
                }
            }
        }
    }
    
    func channel(_ channel: ARtmChannel, memberLeft member: ARtmMember) {
        // 频道成员离开频道回调
        for (index, userModel) in micArr.enumerated() {
            if userModel.uid == member.uid {
                micArr.remove(at: index)
                if topViewController() is ARMicViewController && infoVideoModel.isBroadcaster {
                    NotificationCenter.default.post(name: UIResponder.audioLiveNotificationRefreshMicList, object: self, userInfo:nil)
                }
                listButton.badgeValue = "\(micArr.count)"
                break
            }
        }
    }
}

//MARK: - ARMediaPlayerDelegate

extension ARVideoViewController: ARMediaPlayerDelegate {
    func rtcMediaPlayer(_ playerKit: ARMediaPlayer, didChangedTo state: ARMediaPlayerState, error: ARMediaPlayerError) {
        //报告播放器的状态
        print("rtcMediaPlayer \(state.rawValue)  \(error.rawValue)")
        if state == .playing {
            broadcasterVideo.placeholderView.isHidden = true
        } else if state == .stopped {
            broadcasterVideo.placeholderView.isHidden = false
        }
    }
}
