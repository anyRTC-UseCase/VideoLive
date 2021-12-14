//
//  ARMicViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/22.
//

import ARtmKit
import UIKit

class ARMicCell: UITableViewCell {
    @IBOutlet var headImageView: UIImageView!
    @IBOutlet var rejuctButton: UIButton!
    @IBOutlet var acceptButton: UIButton!
    @IBOutlet var nameLabel: UILabel!
    
    var onButtonTapped: ((_ index: NSInteger) -> Void)?
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
        if userCount < 4, infoVideoModel.agreeMicArr.count < 3 {
            if let onButtonTapped = self.onButtonTapped {
                var cmd: String?
                if sender.tag == 50 {
                    cmd = "acceptLine"
                    infoVideoModel.agreeMicArr.append(userModel?.uid ?? "")
                } else {
                    cmd = "rejectLine"
                }
                
                let dic: NSDictionary! = ["cmd": cmd as Any]
                let message = ARtmMessage(text: getJSONStringFromDictionary(dictionary: dic))
                rtmEngine.send(message, toPeer: (userModel?.uid)!, sendMessageOptions: ARtmSendMessageOptions()) { errorCode in
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
    @IBOutlet var numberLabel: UILabel!
    @IBOutlet var backView: UIView!
    @IBOutlet var tableView: UITableView!
    @IBOutlet var noMicLabel: UILabel!
    let tap = UITapGestureRecognizer()
    var videoVc: ARVideoViewController!

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

extension ARMicViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell: ARMicCell = tableView.dequeueReusableCell(withIdentifier: "ARMicCellID") as! ARMicCell
        cell.selectionStyle = .none
        cell.updateMicCell(model: videoVc.micArr[indexPath.row], count: videoVc.videoArr.count)
        cell.onButtonTapped = { [weak self] _ in
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
