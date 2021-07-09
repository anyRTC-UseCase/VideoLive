//
//  ARMicViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/22.
//

import UIKit
import ARtmKit

class ARMicCell: UITableViewCell {
    
    @IBOutlet weak var headImageView: UIImageView!
    @IBOutlet weak var rejuctButton: UIButton!
    @IBOutlet weak var acceptButton: UIButton!
    @IBOutlet weak var nameLabel: UILabel!
    
    var onButtonTapped : ((_ index: NSInteger) -> Void)? = nil
    var userModel: ARUserModel?
    var userCount: Int = 0
    
    func updateMicCell(model: ARUserModel?, count: Int) {
        userModel = model
        userCount = count
        
        headImageView.sd_setImage(with: NSURL(string: userModel?.avatar ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
        nameLabel.text = userModel?.userName
        (count >= 4 || infoVideoModel.agreeMicArr.count > 3) ? (acceptButton.backgroundColor = UIColor(hexString: "#C0C0CC")) : (acceptButton.backgroundColor = UIColor(hexString: "#294BFF"))
    }
    
    @IBAction func didClickControlButton(_ sender: UIButton) {
        if userCount < 4 && infoVideoModel.agreeMicArr.count < 3 {
            if let onButtonTapped = self.onButtonTapped {
                var cmd: String?
                if sender.tag == 50 {
                    cmd = "acceptLine"
                    infoVideoModel.agreeMicArr.append(userModel?.uid ?? "")
                } else {
                    cmd = "rejectLine"
                }
                
                let dic: NSDictionary! = ["cmd": cmd as Any]
                let message: ARtmMessage = ARtmMessage.init(text: getJSONStringFromDictionary(dictionary: dic))
                rtmEngine.send(message, toPeer: (userModel?.uid)!, sendMessageOptions: ARtmSendMessageOptions()) { (errorCode) in
                    print("sendMessage code = \(errorCode.rawValue)")
                }
                
                onButtonTapped(sender.tag)
            }
        } else {
            showToast(text: "上麦人数已达上限", image: "icon_tip_warning")
        }
    }
}

class ARMicViewController: UIViewController, UIGestureRecognizerDelegate {
    
    @IBOutlet weak var numberLabel: UILabel!
    @IBOutlet weak var backView: UIView!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var noMicLabel: UILabel!
    let tap = UITapGestureRecognizer()
    var videoVc: ARVideoViewController!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        if #available(iOS 11.0, *) {
            backView.layer.maskedCorners = [.layerMinXMinYCorner,.layerMaxXMinYCorner]
        } else {
            // Fallback on earlier versions
        }
        tap.addTarget(self, action: #selector(didClickCloseButton))
        tap.delegate = self
        self.view.addGestureRecognizer(tap)
        
        tableView.tableFooterView = UIView()
        tableView.separatorStyle = .none
        updateMicState()
        
        NotificationCenter.default.addObserver(self, selector: #selector(refreshMicList), name: UIResponder.audioLiveNotificationRefreshMicList, object: nil)
    }
    
    @objc func refreshMicList() {
        updateMicState()
        tableView.reloadData()
    }
    
    func updateMicState() {
        let number = videoVc?.micArr.count ?? 0
        noMicLabel.isHidden = (number != 0)
        numberLabel.text = "排麦队列 \(number)"
        videoVc.listButton.badgeValue = "\(number)"
    }
    
    @IBAction func didClickCloseButton(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        if(touch.view == self.view) {
            self.dismiss(animated: true, completion: nil)
            return true
        } else {
            return false
        }
    }
}

extension ARMicViewController: UITableViewDelegate,UITableViewDataSource {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell: ARMicCell = tableView.dequeueReusableCell(withIdentifier: "ARMicCellID") as! ARMicCell
        cell.selectionStyle = .none
        cell.updateMicCell(model: videoVc.micArr[indexPath.row], count: videoVc.videoArr.count)
        cell.onButtonTapped =  { [weak self] (index) in
            self?.videoVc.micArr.remove(at: indexPath.row)
            self?.tableView.reloadData()
            self?.updateMicState()
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return videoVc.micArr.count
    }
}
