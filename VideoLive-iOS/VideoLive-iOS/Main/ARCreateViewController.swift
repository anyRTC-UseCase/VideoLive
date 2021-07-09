//
//  ARCreateViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/21.
//

import UIKit
import SwiftyJSON

class ARCreateViewController: UIViewController {

    @IBOutlet weak var roomNameTextField: UITextField!
    @IBOutlet weak var createRoomButton: UIButton!
    @IBOutlet weak var stackView: UIStackView!
    var selectedIndex: NSInteger = 6
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        self.navigationItem.title = "创建视频连麦房间"
        for (index, object) in stackView.subviews.enumerated() {
            let button: UIButton = (object as? UIButton)!
            if index == 0 {
                button.layer.borderColor = UIColor(hexString: "#294BFF").cgColor
                button.isSelected = true
            } else {
                button.layer.borderColor = UIColor(hexString: "#EBEBF3").cgColor
            }
        }
        roomNameTextField.addTarget(self, action: #selector(limitRoomName), for: .editingChanged)
    }
    
    @objc func limitRoomName() {
        let roomName = roomNameTextField.text
        if roomName?.count ?? 0 > 9 {
            roomNameTextField.text = String((roomName?.prefix(9))!)
        }
        
        let enable = roomName?.count ?? 0 > 0 && !stringAllIsEmpty(string: roomName ?? "")
        enable ? (createRoomButton.alpha = 1.0) : (createRoomButton.alpha = 0.5)
        createRoomButton.isEnabled = enable
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        self.navigationController?.navigationBar.setBackgroundImage(createImage(UIColor(hexString: "#FFFFFF")), for: .any, barMetrics: .default)
        self.navigationController?.navigationBar.shadowImage = UIImage()
        
        let closeButton = UIButton(type: .custom)
        closeButton.setImage(UIImage(named: "icon_close"), for: .normal)
        closeButton.frame = CGRect.init(x: 0, y: 0, width: 40, height: 40)
        closeButton.addTarget(self, action: #selector(popBack), for: .touchUpInside)
        self.navigationItem.leftBarButtonItem = UIBarButtonItem(customView: closeButton)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
    
    @IBAction func didClickButton(_ sender: UIButton) {
        view.endEditing(true)

        if !sender.isSelected {
            for object in stackView.subviews {
                let button = object as! UIButton
                button.isSelected = false
                button.layer.borderColor = UIColor(hexString: "#EBEBF3").cgColor
            }
            
            sender.isSelected = true
            sender.layer.borderColor = UIColor(hexString: "#294BFF").cgColor
            selectedIndex = sender.tag
        }
    }
    
    @IBAction func createAudioRoom(_ sender: Any) {
        let roomName = roomNameTextField.text
        //rType 6.RTC实时互动 7.客户端推流到CDN 8.服务端推流到CDN
        if roomName?.count ?? 0 > 0 {
            let parameters : NSDictionary = ["cType": 2, "pkg": Bundle.main.infoDictionary!["CFBundleIdentifier"] as Any, "rType": selectedIndex, "roomName": roomName as Any]
            ARNetWorkHepler.getResponseData("addRoom", parameters: parameters as? [String : AnyObject], headers: true, success: { [weak self] (result) in
                if result["code"] == 0 {
                    let jsonData = JSON(result["data"])
                    var model = ARRoomInfoModel.init(jsonData: jsonData)
                    model.rType = self?.selectedIndex
                    model.roomName = roomName
                    model.isBroadcaster = true
                    
                    let storyboard = UIStoryboard.init(name: "Main", bundle: nil)
                    guard let videoVc = storyboard.instantiateViewController(withIdentifier: "VideoLive_Video") as? ARVideoViewController else {return}
                    infoVideoModel = model
                    self?.navigationController?.pushViewController(videoVc, animated: true)
                } else {
                    print(result)
                }
            }) { (error) in
                print(error)
            }
        }
    }
}
