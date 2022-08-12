//
//  ARMoreViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/22.
//

import ARtcKit
import UIKit

class ARMoreViewController: UIViewController, UIGestureRecognizerDelegate {
    @IBOutlet var backView: UIView!
    @IBOutlet var stackView: UIStackView!
    @IBOutlet var layoutButton: UIButton!
    @IBOutlet var earButton: UIButton!
    
    let tap = UITapGestureRecognizer()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        if #available(iOS 11.0, *) {
            backView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        } else {
            // Fallback on earlier versions
        }
        tap.addTarget(self, action: #selector(didClickCloseButton))
        tap.delegate = self
        view.addGestureRecognizer(tap)
        
        let button = stackView.viewWithTag(infoVideoModel.dimensions) as! UIButton
        button.layer.borderColor = UIColor(hexString: "#294BFF").cgColor
        button.setTitleColor(UIColor(hexString: "#314BFF"), for: .normal)
        layoutButton.isSelected = infoVideoModel.layout
        earButton.isSelected = infoVideoModel.wiredHeadset
    }
    
    @IBAction func didClickMoreButton(_ sender: UIButton) {
        if sender.tag <= 3 {
            if infoVideoModel.dimensions != sender.tag {
                let button = stackView.viewWithTag(infoVideoModel.dimensions) as! UIButton
                button.layer.borderColor = UIColor(hexString: "#EBEBF3").cgColor
                button.setTitleColor(UIColor(hexString: "#5A5A67"), for: .normal)
                
                sender.layer.borderColor = UIColor(hexString: "#294BFF").cgColor
                sender.setTitleColor(UIColor(hexString: "#314BFF"), for: .normal)
                
                let videoConfig = ARVideoEncoderConfiguration()
                videoConfig.dimensions = getVideoDimensions(index: sender.tag)
                
               
                switch sender.tag {
                    case 1:
                        videoConfig.bitrate = 500
                        break
                    case 2:
                        videoConfig.bitrate = 800
                        break
                    case 3:
                        videoConfig.bitrate = 1200
                        break
                default:
                    break
                    
                }
               // videoConfig.bitrate = 500
                videoConfig.frameRate = 15
                rtcKit.setVideoEncoderConfiguration(videoConfig)
                infoVideoModel.dimensions = sender.tag
                
                if (liveTranscoding != nil){
                    /*
                    liveTranscoding.size =   videoConfig.dimensions
                    liveTranscoding.videoBitrate = videoConfig.bitrate
                    NotificationCenter.default.post(name: UIResponder.audioLiveNotificationChangeResolution, object: self, userInfo: nil)
                     */
                }
                
            }
        } else if sender.tag == 4 {
            if infoVideoModel.videoState {
                rtcKit.switchCamera()
            }
        } else if sender.tag == 5 {
            if !sender.isSelected {
                if isWiredHeadset() {
                    rtcKit.enable(inEarMonitoring: true)
                    sender.isSelected = true
                    infoVideoModel.wiredHeadset = true
                } else {
                    showToast(text: "请插入有线耳机", image: "icon_tip_head")
                }
            } else {
                rtcKit.enable(inEarMonitoring: false)
                sender.isSelected = false
                infoVideoModel.wiredHeadset = false
            }
        } else if sender.tag == 6 {
            sender.isSelected.toggle()
            infoVideoModel.layout = sender.isSelected
            NotificationCenter.default.post(name: UIResponder.audioLiveNotificationLayout, object: self, userInfo: nil)
            dismiss(animated: true, completion: nil)
        }
    }
    
    @IBAction func didClickCloseButton(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        if touch.view == view {
            dismiss(animated: true, completion: nil)
            return true
        } else {
            return false
        }
    }
}
