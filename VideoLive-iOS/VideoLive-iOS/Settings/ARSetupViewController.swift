//
//  ARSetupViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/18.
//

import ARtcKit
import SDWebImage
import SnapKit
import UIKit

struct MenuItem {
    var name: String
    var icon: String
    var detail: String?
}

private let reuseIdentifier = "VideoLive_Statement"

class ARSetupViewController: UITableViewController {
    lazy var headView: UIView = {
        let height = ARScreenHeight * 0.138
        let view = UIView(frame: CGRect(x: 0, y: 0, width: ARScreenWidth, height: height))
        view.backgroundColor = UIColor(hexString: "#FFFFFF")
        
        let headImageView = UIImageView(frame: CGRect(x: 15, y: 20, width: 46, height: 46))
        headImageView.layer.cornerRadius = 23
        headImageView.layer.masksToBounds = true
        headImageView.sd_setImage(with: NSURL(string: UserDefaults.string(forKey: .avatar) ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
        view.addSubview(headImageView)
            
        let nameLabel = UILabel(frame: CGRect(x: headImageView.right + 15, y: 20, width: ARScreenWidth - 140, height: 46))
        nameLabel.text = UserDefaults.string(forKey: .userName)
        nameLabel.tag = 99
        nameLabel.font = UIFont(name: PingFangBold, size: 18)
        view.addSubview(nameLabel)
        
        let editButton = UIButton(type: .custom)
        editButton.frame = CGRect(x: 0, y: 0, width: 46, height: 46)
        editButton.right = view.right - 16
        editButton.centerY = headImageView.centerY
        editButton.addTarget(self, action: #selector(editNickname), for: .touchUpInside)
        editButton.setImage(UIImage(named: "icon_edit"), for: .normal)
        view.addSubview(editButton)
        
        let bottomView = UIView(frame: CGRect(x: 0, y: height - 10, width: ARScreenWidth, height: 10))
        bottomView.backgroundColor = UIColor(hexString: "#F5F6FA")
        view.addSubview(bottomView)
        return view
    }()
    
    var menus: [MenuItem] = [
        MenuItem(name: "隐私条例", icon: "icon_lock"),
        MenuItem(name: "免责声明", icon: "icon_log"),
        MenuItem(name: "anyRTC官网", icon: "icon_register"),
        MenuItem(name: "发版时间", icon: "icon_time", detail: "2021.07.15"),
        MenuItem(name: "SDK版本", icon: "icon_sdkversion", detail: String(format: "V %@", ARtcEngineKit.getSdkVersion())),
        MenuItem(name: "软件版本", icon: "icon_appversion", detail: String(format: "V %@", Bundle.main.infoDictionary!["CFBundleShortVersionString"] as! CVarArg))
    ]

    override func viewDidLoad() {
        super.viewDidLoad()

        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false

        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
        
        tableView.tableFooterView = UIView()
        tableView.tableHeaderView = headView
        tableView.tableHeaderView?.height = ARScreenHeight * 0.138
        
        tableView.separatorColor = UIColor(hexString: "#EBEBF3")
    }
    
    @objc func editNickname() {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let editVc = storyboard.instantiateViewController(withIdentifier: "VideoLive_Edit")
        editVc.hidesBottomBarWhenPushed = true
        navigationController?.pushViewController(editVc, animated: true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        hidesBottomBarWhenPushed = false
        navigationController?.navigationBar.setBackgroundImage(createImage(UIColor(hexString: "#FFFFFF")), for: .any, barMetrics: .default)
        navigationController?.navigationBar.shadowImage = UIImage()
        
        let titleLabel = UILabel()
        titleLabel.text = "设置"
        titleLabel.textColor = UIColor(hexString: "#1A1A1E")
        titleLabel.font = UIFont(name: PingFangBold, size: 18)
        titleLabel.frame = CGRect(x: 0, y: 0, width: 100, height: 50)
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: titleLabel)
        
        let nameLabel = headView.viewWithTag(99) as? UILabel
        nameLabel?.text = UserDefaults.string(forKey: .userName)
    }

    // MARK: - Table view data source

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // #warning Incomplete implementation, return the number of rows
        return menus.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        var cell = tableView.dequeueReusableCell(withIdentifier: "reuseIdentifier")
        if cell == nil {
            cell = UITableViewCell(style: .value1, reuseIdentifier: "reuseIdentifier")
        }
        // Configure the cell...
        cell?.backgroundColor = UIColor(hexString: "#FFFFFF")
        cell?.selectionStyle = .none
        cell?.textLabel?.text = menus[indexPath.row].name
        cell?.textLabel?.textColor = UIColor(hexString: "#5A5A67")
        cell?.textLabel?.font = UIFont(name: PingFang, size: 14)
        cell?.imageView?.image = UIImage(named: menus[indexPath.row].icon)
        cell?.detailTextLabel?.textColor = UIColor(hexString: "#C0C0CC")
        cell?.detailTextLabel?.text = menus[indexPath.row].detail
        cell?.detailTextLabel?.font = UIFont(name: PingFang, size: 12)
        return cell!
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if indexPath.row == 0 {
            UIApplication.shared.open(NSURL(string: "https://anyrtc.io/anyrtc/privacy")! as URL, options: [:], completionHandler: nil)
            
        } else if indexPath.row == 1 {
            let storyboard = UIStoryboard(name: "Main", bundle: nil)
            let statementVc = storyboard.instantiateViewController(withIdentifier: reuseIdentifier)
            statementVc.hidesBottomBarWhenPushed = true
            navigationController?.pushViewController(statementVc, animated: true)
            
        } else if indexPath.row == 2 {
            UIApplication.shared.open(NSURL(string: "https://www.anyrtc.io")! as URL, options: [:], completionHandler: nil)
        }
    }
}
